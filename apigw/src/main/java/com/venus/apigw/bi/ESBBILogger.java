package com.venus.apigw.bi;

import com.venus.apigw.config.GWConfig;
import com.venus.apigw.db.DB;
import com.venus.esb.*;
import com.venus.esb.lang.ESBCookie;
import com.venus.esb.lang.ESBException;
import com.venus.esb.lang.ESBT;
import com.venus.esb.utils.MD5;
import com.venus.esb.utils.URLs;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * Created with IntelliJ IDEA.
 * Description:
 * User: lingminjun
 * Date: 2018-09-21
 * Time: 下午9:22
 */
public class ESBBILogger implements ESB.APILogger {

//    private static final String PV_DB_TABLE = "bi_pv_history";
    private static final String METHOD_DB_TABLE = "bi_invoked_history";

    private static final String VID_KEY = "_v";

//    private static final String[] PV_KEYS = new String[]{"htm","app","did","md5","tml","ref","uid","acct"};
    private static final String[] METHOD_KEYS = new String[]{"aid","app","did","tid","cip","mthd","uid","acct","cvc","cvn","cost","scs","at","tml","md5","ref","qry","vid","src","spm"};

    // 三天
    private static final long THREE_DAYS = 3 * 24 * 3600 * 1000;
    private static final long SEVEN_DAYS = 7 * 24 * 3600 * 1000;
    private static final long RETAIN_DAYS = THREE_DAYS;

    private final BlockingQueue<PVPojo> pending = new LinkedBlockingQueue(5000);//防止积压,尽量保持
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);

    private DB db;
    private long clearAt = 0l;


    @Override
    public void request(ESB esb, ESBAPIContext context, Map<String, String> params, Map<String, String> header, Map<String, ESBCookie> cookies, List<ESBResponse> result, ESBException e) {
        // nothing
    }

    @Override
    public void access(ESB esb, ESBAPIInfo info, ESBAPIContext context, Map<String, String> params, Map<String, String> header, Map<String, ESBCookie> cookies, ESBInvocation invocation, Object result, ESBException e) {
        accessToKafka(esb,info,context,params,header,cookies,invocation,result,e);
        accessToDB(esb,info,context,params,header,cookies,invocation,result,e);
    }

    private void accessToKafka(ESB esb, ESBAPIInfo info, ESBAPIContext context, Map<String, String> params, Map<String, String> header, Map<String, ESBCookie> cookies, ESBInvocation invocation, Object result, ESBException e) {
        // 同步到数据库
        if (!GWConfig.getInstance().isLogSyncKafka()) {
            return;
        }

        PVPojo pojo = constructPOJO(info,context,result,e);

        // 未来实现
    }

    private void accessToDB(ESB esb, ESBAPIInfo info, ESBAPIContext context, Map<String, String> params, Map<String, String> header, Map<String, ESBCookie> cookies, ESBInvocation invocation, Object result, ESBException e) {

        // 同步到数据库
        if (!GWConfig.getInstance().isLogSyncDB()) {
            return;
        }

        PVPojo pojo = constructPOJO(info,context,result,e);

        if(!pending.offer(pojo)) {// 临时延迟
            this.scheduler.scheduleWithFixedDelay(new Runnable() {
                @Override
                public void run() {
                    try {
                        pending.offer(pojo);
                    } catch (Throwable e) {}
                }
            }, 0l, 500l, TimeUnit.MILLISECONDS);
        }
    }

    private static PVPojo constructPOJO(ESBAPIInfo info, ESBAPIContext context, Object result, ESBException e) {
        long now = System.currentTimeMillis();
        long cost = now - context.at;

        PVPojo pojo = new PVPojo();
        pojo.aid = ESBT.integer(context.aid);
        pojo.app = ESBT.integer(context.app);
        pojo.tml = ESBT.integer(context.tml);
        pojo.did = ESBT.longInteger(context.did);
        pojo.uid = ESBT.longInteger(context.uid);
        pojo.acct = ESBT.longInteger(context.acct);
        pojo.mthd = info.api.getAPISelector();
        pojo.tid = context.tid;

        // 为了更少的数据存储，将referer整理，也是方便后面搜索
        if (ESBT.isEmpty(context.referer)) {
            pojo.ref = URLs.tidyURI(context.referer, "https", null);
            // 尽量统一到一起
            pojo.md5 = pojo.ref == null ? null : MD5.md5(pojo.ref);
            HashMap<String,Object> query =  URLs.URLQuery(context.referer);
            if (query != null && query.size() > 0) {
                // 明文存储
                pojo.qry = URLs.URLQueryString(query,false);

                // 存在vid
                if (query.containsKey(VID_KEY)) {
                    pojo.vid = query.get(VID_KEY).toString();
                }
            }
        }

        pojo.src = context.src;
        pojo.spm = context.spm;

        pojo.cvc = context.cvc;
        pojo.cvn = context.cvn;
        pojo.cip = context.cip;

        pojo.cost = cost;
        pojo.scs = e == null ? 1 : 0;

        pojo.at = context.at;
//        pojo.htm = ((long)(context.at / 3600000)) * 3600000 ;

        return pojo;
    }

    public ESBBILogger() {
        //加载配置
        //jdbc:mysql://127.0.0.1:3306/apigw?useUnicode=true&characterEncoding=utf8
        if (GWConfig.getInstance().isLogSyncDB()) {
            this.db = new DB(
                    GWConfig.getInstance().getBidatasourceUrl(),
                    GWConfig.getInstance().getBidatasourceUsername(),
                    GWConfig.getInstance().getBidatasourcePassword(),
                    GWConfig.getInstance().getBidatasourceDriverClass());

            // 开启处理线程
            this.scheduler.execute(new Runnable() {
                @Override
                public void run() {
                    while (true) {
                        try {
                            dealBIData();
                        } catch (Throwable e) {
                        }
                    }
                }
            });
        }
    }

    private void dealBIData() throws InterruptedException {
        PVPojo pojo = pending.take(); // 等到有数据才继续
        if (pojo != null) {
//            db.upinsert(PV_DB_TABLE,PV_KEYS,pojo);
            db.upinsert(METHOD_DB_TABLE,METHOD_KEYS,pojo);
        }
        clearHistory();
    }

    // 删除3天后的数据
    private void clearHistory() {
        // 每 3 填检查一次
        long now = System.currentTimeMillis();
        if (clearAt + RETAIN_DAYS < now) {
            clearAt = now;
            long stamp = now - RETAIN_DAYS;
            int result = 0;
//            result = db.delete(PV_DB_TABLE,"`htm` < ?", new Object[]{stamp});
//            System.out.println("清除了bi_pv_history表" + result + "条数据");
            result = db.delete(METHOD_DB_TABLE,"`at` < ?",new Object[]{stamp});
            System.out.println("清除了bi_invoked_history表" + result + "条数据");
        }
    }
}
