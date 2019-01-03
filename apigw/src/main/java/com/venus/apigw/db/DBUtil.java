package com.venus.apigw.db;

import com.alibaba.fastjson.JSON;
import com.venus.apigw.config.GWConfig;
import com.venus.esb.ESBAPIInfo;
import com.venus.esb.annotation.ESBAPI;
import com.venus.esb.lang.ESBConsts;
import com.venus.esb.lang.ESBDispatchQueue;
import com.venus.esb.lang.ESBT;
import com.venus.esb.utils.DateUtils;
import com.venus.esb.utils.MD5;
import org.apache.tomcat.jdbc.pool.DataSource;
import org.apache.tomcat.jdbc.pool.PoolProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.*;
import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * Description:
 * User: lingminjun
 * Date: 2018-10-03
 * Time: 上午10:14
 */
public final class DBUtil {
    private static final Logger logger           = LoggerFactory.getLogger(DBUtil.class);

    private static final String API_DB_TABLE = "apigw_api";
    private static final String API_HISTORY_DB_TABLE = "apigw_api_history";
    private static final String[] API_KEYS = new String[]{"domain","module","method","json","detail","owner","security","status","created","modified","md5","upstamp","thestamp","rollback"};
    private static final String[] API_HISTORY_KEYS = new String[]{"upstamp","domain","module","method","json","detail","owner","security","status","created","modified","md5","thestamp","rollback"};

    private DB db;
    private String database = "apigw";

    private DBUtil() {
        //加载配置
        //jdbc:mysql://127.0.0.1:3306/apigw?useUnicode=true&characterEncoding=utf8
        String url = GWConfig.getInstance().getDatasourceUrl();
        this.db = new DB(url,
                GWConfig.getInstance().getDatasourceUsername(),
                GWConfig.getInstance().getDatasourcePassword(),
                GWConfig.getInstance().getDatasourceDriverClass());
        //获取database
        if (url.toLowerCase().startsWith("jdbc:mysql")) {
            url = "https" + url.substring("jdbc:mysql".length());
        }
        try {
//            this.db.getConnection().getSchema();
            URL u = new URL(url);
            String p = u.getPath();
            if (p != null && p.length() > 0 && !p.equals("/")) {
                String[] ss = p.split("/");
                for (int i = 1; i <= ss.length; i++) {
                    String s = ss[ss.length - i];
                    if (s.length() > 0) {
                        database = s;
                        break;
                    }
                }

            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

    }

    public static DBUtil pool() {
        return DBPoolSingletonHolder.INSTANCE;
    }

    private static class DBPoolSingletonHolder {
        private static DBUtil INSTANCE = new DBUtil();
    }

    // 返回所有待更新的selectors
    public static List<String> upapis(List<ESBAPIInfo> apis) {
        long timestamp = System.currentTimeMillis();//毫秒
        String thestamp = DateUtils.toYYYYMMDDHHMMSSSSS(timestamp);

        DB db = pool().db;
        List<String> sels = new ArrayList<>();
        List<APIPojo> pojos = new ArrayList<>();
        for (ESBAPIInfo api : apis) {
            if (api.api == null
                    || api.api.domain == null
                    || api.api.module == null
                    || api.api.methodName == null
                    ) {
                continue;
            }

            // 暂时忽略一些无关的配置，设置成默认值
            api.mock = false;//接口打标 [用于扩展意义]
            api.createAt = 0;//创建日期
            api.modifyAt = 0;//修改日期

            String json = JSON.toJSONString(api, ESBConsts.FASTJSON_SERIALIZER_FEATURES);
            String md5 = MD5.md5(json);

            //检查是否需要更新
            APIPojo old = getCurrentAPI(db,api.api.domain,api.api.module,api.api.methodName);
            if (old != null && md5.equals(old.md5)) {//没有变化，直接不需要更新
                continue;
            }

            // 确定需要更新接口
            APIPojo pojo = new APIPojo();
            pojo.modified = System.currentTimeMillis();
            pojo.created = old == null ? pojo.modified : null;
            pojo.domain = api.api.domain;
            pojo.module = api.api.module;
            pojo.method = api.api.methodName;
            pojo.detail = api.api.desc;
            pojo.owner = api.api.owner;
            pojo.security = api.api.security;
            pojo.status = 0;

            pojo.json = json;
            pojo.md5 = md5;
            pojo.upstamp = timestamp; //设置统一的timestamp
            pojo.thestamp = thestamp;
            pojo.rollback = 0;
            pojo.prestamp = old != null ? old.prestamp : 0; //记录前一个更新位置

            sels.add(api.api.getAPISelector());
            pojos.add(pojo);


        }

        //更新条数较多，为了运维能更好的回滚，将记录回滚表
        if (pojos.size() >= 10) {
            backupAPITable(db,thestamp);
        }

        // 插入数据
        for (APIPojo pojo : pojos) {
            db.upinsert(API_DB_TABLE,API_KEYS,pojo);
            db.upinsert(API_HISTORY_DB_TABLE,API_HISTORY_KEYS,pojo);
        }

        return sels;
    }

    public static List<StampPojo> latestUpdateStamp() {
        DB db = pool().db;

        HashMap<String,StampPojo> stamps = new HashMap<>();
        List<StampPojo> list = new ArrayList<>();
        List<APIPojo> preList = queryLatestHistory(db);
        for (APIPojo pojo : preList) {
            StampPojo stamp = stamps.get(pojo.thestamp);
            if (stamp == null) {
                stamp = new StampPojo();
                stamp.thestamp = pojo.thestamp;
                stamp.apis = new ArrayList<>();
                stamps.put(pojo.thestamp,stamp);
                list.add(stamp);
            }

            String sel = pojo.getAPISelector();
            stamp.apis.add(sel);
        }

        return list;
    }

    public static List<StampPojo> latestUpdateStamp(String selector) {
        if (selector == null || selector.length() == 0) {
            logger.error("api rollback failed! please input right selector.");
            return null;
        }

        String[] ss = selector.split("\\.",-1);
        if (ss.length != 3) {
            logger.error("api rollback failed! please input right selector( domain.module.method ).");
            return null;
        }
        String domain = ss[0];
        String module = ss[1];
        String method = ss[2];
        if (domain == null || domain.length() == 0
                || module == null || module.length() == 0
                || method == null || method.length() == 0
        ) {
            logger.error("api rollback failed! please input right selector( domain.module.method ).");
            return null;
        }

        DB db = pool().db;

        HashMap<String,StampPojo> stamps = new HashMap<>();
        List<StampPojo> list = new ArrayList<>();
        List<APIPojo> preList = queryLatestHistory(db,domain,module,method);
        for (APIPojo pojo : preList) {
            StampPojo stamp = stamps.get(pojo.thestamp);
            if (stamp == null) {
                stamp = new StampPojo();
                stamp.thestamp = pojo.thestamp;
                stamps.put(pojo.thestamp,stamp);
                list.add(stamp);
            }
        }

        return list;
    }


    // 还原回某个节点
    public static List<String> rollback(String thestamp) {
        long times = DateUtils.dateYYYYMMDDHHMMSSSSS(thestamp);//毫秒
        if (times <= 0) {
            logger.error("api rollback failed! please input right timestemp.");
            return new ArrayList<>();
        }

        DB db = pool().db;
        List<String> sels = new ArrayList<>();

        // 取待回滚数据
        List<APIPojo> preList = getBeRollback(db,times);
        if (preList == null || preList.size() == 0) {
            return sels;
        }

        //将待处理的api分类放置
        HashMap<String,List<APIPojo>> maps = new HashMap<>();
        for (APIPojo pojo : preList) {
            String sel = pojo.getAPISelector();
            List<APIPojo> list = maps.get(sel);
            if (list == null) {
                list = new ArrayList<>();
                maps.put(sel,list);
            }
            list.add(pojo);
        }

        //开始处理每一个记录
        for (Map.Entry<String,List<APIPojo>> entry : maps.entrySet()) {
            List<APIPojo> list = entry.getValue();
            list.sort(API_COMP);
            APIPojo pojo = list.get(0);

            sels.add(entry.getKey());

            // 删除
            if (pojo.prestamp == null || pojo.prestamp == 0) {
                deletedAPI(db,pojo.domain,pojo.module,pojo.method);
            } else {//更新
                APIPojo bkpojo = getHistoryAPI(db,pojo.domain,pojo.module,pojo.method,pojo.prestamp);
                bkpojo.rollback = 0;
                if (bkpojo != null) {
                    db.upinsert(API_DB_TABLE,API_KEYS,bkpojo);
                    db.upinsert(API_HISTORY_DB_TABLE,API_HISTORY_KEYS,bkpojo);
                }
            }
        }

        // 最后删除回滚记录
        deletedRollbackHistory(db,times,thestamp);

        return sels;
    }


    // 回滚某个接口
    public static String rollback(String thestamp, String selector) {
        long times = DateUtils.dateYYYYMMDDHHMMSSSSS(thestamp);//毫秒
        if (times <= 0) {
            logger.error("api rollback failed! please input right timestemp.");
            return null;
        }

        if (selector == null || selector.length() == 0) {
            logger.error("api rollback failed! please input right selector.");
            return null;
        }

        String[] ss = selector.split("\\.",-1);
        if (ss.length != 3) {
            logger.error("api rollback failed! please input right selector( domain.module.method ).");
            return null;
        }
        String domain = ss[0];
        String module = ss[1];
        String method = ss[2];
        if (domain == null || domain.length() == 0
                || module == null || module.length() == 0
                || method == null || method.length() == 0
        ) {
            logger.error("api rollback failed! please input right selector( domain.module.method ).");
            return null;
        }

        DB db = pool().db;

        // 取待回滚数据
        List<APIPojo> list = getBeRollback(db,domain,module,method,times);
        if (list == null || list.size() == 0) {
            return null;
        }

        //开始处理每一个记录
        list.sort(API_COMP);
        APIPojo pojo = list.get(0);

        // 删除
        if (pojo.prestamp == null || pojo.prestamp == 0) {
            deletedAPI(db,pojo.domain,pojo.module,pojo.method);
        } else {//更新
            APIPojo bkpojo = getHistoryAPI(db,pojo.domain,pojo.module,pojo.method,pojo.prestamp);
            bkpojo.rollback = 0;
            if (bkpojo != null) {
                db.upinsert(API_DB_TABLE,API_KEYS,bkpojo);
                db.upinsert(API_HISTORY_DB_TABLE,API_HISTORY_KEYS,bkpojo);
            }
        }

        // 最后删除回滚记录
        deletedRollbackHistory(db,domain,module,method,times,thestamp);

        return selector;
    }

    public static APIThirdSecurityPojo getThirdPartySecurityInfo(Long tid) {
        List<APIThirdSecurityPojo> results = pool().db.query("apigw_third_party_secret","`id` = ?", new Object[]{tid}, APIThirdSecurityPojo.class);
        if (results != null && results.size() > 0) {
            return results.get(0);
        }
        return null;
    }

    public static List<ESBAPIInfo> allAPIsForStatus(int status) {
        //修改成数据库查询接口
        List<APIPojo> results = pool().db.query("apigw_api","`status` = ?", new Object[]{status}, APIPojo.class);
        List<ESBAPIInfo> list = new ArrayList<>();
        for (APIPojo pojo : results) {
            list.add(pojo.getInfo());
        }
        return list;
    }



    private static Comparator<APIPojo> API_COMP = new Comparator<APIPojo>() {
        @Override
        public int compare(APIPojo o1, APIPojo o2) {
            return (int)(o1.upstamp - o2.upstamp);
        }
    };

    public static ESBAPIInfo getAPI(String domain, String module, String method) {
        APIPojo pojo = getCurrentAPI(pool().db,domain,module,method);
        if (pojo != null) {
            return pojo.getInfo();
        }
        return null;
    }

    // 当前API
    private static APIPojo getCurrentAPI(DB db, String domain, String module, String method) {
        List<APIPojo> pojo = db.query(API_DB_TABLE,
                "`domain` = ? and `module` = ? and `method` = ?",
                new Object[]{domain,module,method},
                APIPojo.class);
        if (pojo != null && pojo.size() > 0) {
            return pojo.get(0);
        }
        return null;
    }

    // 查找历史的API
    private static APIPojo getHistoryAPI(DB db, String domain, String module, String method, Long timestamp) {
        List<APIPojo> pojo = db.query(API_HISTORY_DB_TABLE,
                "`upstamp`= ? and `domain` = ? and `module` = ? and `method` = ? and `rollback` = 0",
                new Object[]{timestamp,domain,module,method},
                APIPojo.class);
        if (pojo != null && pojo.size() > 0) {
            return pojo.get(0);
        }
        return null;
    }

    // 查找最近历史
    private static List<APIPojo> queryLatestHistory(DB db) {
        return db.query(API_HISTORY_DB_TABLE,
                "`rollback` = ? order by `upstamp` desc",
                new Object[]{0L},
                APIPojo.class);
    }

    // 查找最近历史
    private static List<APIPojo> queryLatestHistory(DB db, String domain, String module, String method) {
        return db.query(API_HISTORY_DB_TABLE,
                "`domain` = ? and `module` = ? and `method` = ? and `rollback` = 0 order by `upstamp` desc",
                new Object[]{domain,module,method},
                APIPojo.class);
    }

    // 待回滚内容(查找历史)
    private static List<APIPojo> getBeRollback(DB db, Long timestamp) {
        return db.query(API_HISTORY_DB_TABLE,
                "`upstamp` > ? and `rollback` = 0 ",
                new Object[]{timestamp},
                APIPojo.class);
    }

    // 单个接口的待回滚内容
    private static List<APIPojo> getBeRollback(DB db, String domain, String module, String method, Long timestamp) {
        return db.query(API_HISTORY_DB_TABLE,
                "`domain` = ? and `module` = ? and `method` = ? and `upstamp` > ? and `rollback` = 0 ",
                new Object[]{timestamp,domain,module,method},
                APIPojo.class);
    }

    // 删除批量
    private static void deletedRollbackHistory(DB db, Long timestamp, String thestamp) {
        String sql = "UPDATE `" + API_HISTORY_DB_TABLE + "` SET `rollback` = 1 WHERE `upstamp` > ? and `rollback` = 0";
        //标记回滚
        logger.info("注意：回滚到节点["+thestamp+"]");
        db.execute(sql,new Object[]{timestamp});
//        db.delete(API_HISTORY_DB_TABLE,
//                "`timestamp` > ? ",
//                new Object[]{timestamp});
    }

    // 删除批量-单个
    private static void deletedRollbackHistory(DB db, String domain, String module, String method, Long timestamp, String thestamp) {
        String sql = "UPDATE `" + API_HISTORY_DB_TABLE + "` SET `rollback` = 1 WHERE `domain` = ? and `module` = ? and `method` = ? and `upstamp` > ? and `rollback` = 0";
        //标记回滚
        logger.info("注意：回滚到节点["+thestamp+"]");
        db.execute(sql,new Object[]{domain,module,method,timestamp});
//        db.delete(API_HISTORY_DB_TABLE,
//                "`timestamp` > ? ",
//                new Object[]{timestamp});
    }

    private static void deletedAPI(DB db, String domain, String module, String method) {
        db.delete(API_DB_TABLE,
                "`domain` = ? and `module` = ? and `method` = ?",
                new Object[]{domain,module,method});
    }

    private static void backupAPITable(DB db, String thestamp) {
        String table = API_DB_TABLE + "_bk_" + thestamp;
        String sql = "CREATE TABLE `" + table + "` AS SELECT * FROM `" + API_DB_TABLE + "` WHERE 1 = ?";
        logger.info("注意：创建备份表["+sql+"]");
        db.execute(sql, new Object[]{1});//防止注入

        // 清理表，当记录多于50个后，清理掉30个，异步
        ESBDispatchQueue.commonQueue().execute(new Runnable() {
            @Override
            public void run() {
                checkAndDropBackTables();
            }
        });

    }


    /**
     * 检查并删除备份表
     */
    public static void checkAndDropBackTables() {
        DB db = pool().db;
        String prefix = API_DB_TABLE + "_bk_";
        List<TablePojo> tables = db.query("information_schema",
                "tables",
                "`table_schema` = ? and `table_type` = ? and `table_name` like ?",
                new Object[]{pool().database,"base table",prefix + "%"},
                TablePojo.class);
//        System.out.println("打印下查到的表结构：" + JSON.toJSONString(tables));
        if (tables.size() >= 100) {//表结构按照升序排列，我们删掉前面的就可以了
            StringBuilder dropsql = new StringBuilder("drop table ");
            int leave = tables.size() - 20;//留下20个够了
            for (int i = 0; i < leave; i++) {
                if (i > 0) {
                    dropsql.append(",");
                }
                TablePojo pojo = tables.get(i);
                dropsql.append("`");
                dropsql.append(pojo.table_name);
                dropsql.append("`");
            }
            logger.info("注意：清理掉API备份表["+dropsql.toString()+"]");
            db.execute(dropsql.toString(),new Object[0]);
        }
    }
}
