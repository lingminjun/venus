package com.venus.apigw.risk;

import com.venus.apigw.common.VFCodeUtils;
import com.venus.apigw.config.GWConfig;
import com.venus.esb.ESBSecurityLevel;
import com.venus.esb.lang.ESBT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.HashMap;
import java.util.Set;

/**
 * Created by MJ Ling on 15/7/22.
 */
@Deprecated //请重载 ESBAPIRisky 实现风控
public class RiskManager {
    private static final Logger logger = LoggerFactory.getLogger(RiskManager.class);

    private static final String SPLIT_CHAR = ", ";
//    private static final long CACHE_TIME = 30 * 1000;//30秒

    private static RiskManager getInstance() {
        return RiskManagerHolder.manager;
    }

    public static RiskLevel allowAccess(int appId, long deviceId, long userId, String callId, String clientIp, int authTarget, String captcha, String compatibility) {
        if (!GWConfig.getInstance().isRiskOpen()) {
            return RiskLevel.OK;
        }

        RiskManager manager = getInstance();
        if (manager != null) {
            RiskLevel rt = manager.allow(appId, deviceId, userId, callId, clientIp, authTarget);

            //对图片验证码进行验证,放个是人的操作行为
            if (rt == RiskLevel.DANGER) {
                //此处主要是为了兼容老版本中登录图片验证码和短信验证码,因为验证码不能多次验证,只能提交个服务验证
                if (!ESBT.isEmpty(compatibility)) {
                    return RiskLevel.OK;
                }
                try {
                    if (!ESBT.isEmpty(captcha) && VFCodeUtils.checkVfCode(captcha,true)) {
                        return RiskLevel.OK;
                    }
                } catch (Throwable e) {
                    logger.error("检查此ip访问风险性,调用图片验证码失败",e);
                }

            }

            if (rt != RiskLevel.OK) {
                logger.info("_danger_ip:{}, _level:{}",clientIp,rt);
            }

            return rt;
        }
        return RiskLevel.OK;
    }

    private RiskManager() {

    }

    private RiskLevel allow(int appId, long deviceId, long userId, String callId, String clientIp, int authTarget) {
        try {
            //内部调用,直接放过
            if (ESBSecurityLevel.integrated.check(authTarget)) {return RiskLevel.OK;}
            else if (ESBSecurityLevel.userAuth.check(authTarget)) {return RiskLevel.OK;}
            else if (ESBSecurityLevel.deviceAuth.check(authTarget)) {
                //只做最高级验证
                RiskLevel rt = checkIP(clientIp,GWConfig.getInstance().getRiskDeviceDangerValue(),GWConfig.getInstance().getRiskDeviceDeniedValue());
//                logger.info("检查ip{}风险值{}",clientIp,rt);
                return rt;
            } else if (ESBSecurityLevel.isNone(authTarget)) {
                //开始验证文件
                RiskLevel rt = checkIP(clientIp,GWConfig.getInstance().getRiskNoneDangerValue(),GWConfig.getInstance().getRiskNoneDeniedValue());
//                logger.info("检查ip{}风险值{}",clientIp,rt);
                return rt;
            }
        } catch (Exception e) {
//            RiskManagerHolder.manager = null;
            logger.error("risk manager load failed!", e);
        }
        return RiskLevel.OK;
    }

    /**
     * 检测ip
     * @param ip
     * @param ok
     * @param denied
     * @return
     */
    private RiskLevel checkIP(String ip, long ok, long denied) {
        if (ESBT.isEmpty(ip)) {
            return RiskLevel.OK;
        }

        if (inBlackIPTablesFile(ip)) {
            return RiskLevel.DENIED;
        }

        if (inWhiteIPTablesFile(ip)) {
            return RiskLevel.OK;
        }

        //另外一个监控进程不断写入black文件
        String black = GWConfig.getInstance().getBlackPath();
        if (!ESBT.isEmpty(black)) {
            return readIPTablesFile(black,ip,ok,denied);
        }

        return RiskLevel.OK;
    }

    private boolean inBlackIPTablesFile(String ip) {
        boolean rt = false;
        try {
            Set<String> ips = GWConfig.getInstance().getBlackIps();
            if (ips != null) {
                return ips.contains(ip);
            }
        } catch (Throwable e) {
            logger.error("匹配白名单错误",e);
        }
        return rt;
    }

    private boolean inWhiteIPTablesFile(String ip) {
        boolean rt = false;
        try {
            Set<String> ips = GWConfig.getInstance().getWhiteIps();
            if (ips != null) {
                return ips.contains(ip);
            }
        } catch (Throwable e) {
            logger.error("匹配白名单错误",e);
        }
        return rt;
    }

    private volatile RiskCacheNode<HashMap<String,Long>> blackIpsNode = null;//做局部缓存,提高效率,减少io
    private RiskLevel readIPTablesFile(String path, String ip, long ok, long denied) {
        RiskLevel rt = RiskLevel.OK;
        try {

            long now = System.currentTimeMillis();
            RiskCacheNode<HashMap<String,Long>> node = blackIpsNode;
            if (node != null && now <= node.getAt() + GWConfig.getInstance().getRiskCacheDuration()) {//一分钟更换一次
                Long value = node.getObj().get(ip);
                if (value != null) {
                    return judgeTimes(value,ok,denied);
                } else {
                    return RiskLevel.OK;
                }
            }

            File file = new File(path);
            //文件不存在
            if (!file.exists()) {
                blackIpsNode = null;
                return RiskLevel.OK;
//                logger.info("未获取到risk black配置文件! {}",path);
            }

            FileReader reader = new FileReader(path);
            BufferedReader br = new BufferedReader(reader);

            String str = null;
            HashMap<String,Long> ips = new HashMap<String,Long>();

            //将数据全部读入
            while ((str = br.readLine()) != null) {

                if (str == null) {
                    continue;
                }

                String strs[] = str.split(SPLIT_CHAR);
                if (strs.length == 2) {
                    long value = longInteger(strs[1], 0);
                    if (value > 0) {
                        ips.put(strs[0],value);
                    }
                }
            }

            //验证次数合法性
            Long value = ips.get(ip);
            if (value != null) {
                rt = judgeTimes(value, ok, denied);
            }

            br.close();
            reader.close();

            //数据构造完在缓存
            RiskCacheNode<HashMap<String,Long>> nwNode = new RiskCacheNode<HashMap<String, Long>>();
            nwNode.setAt(now);
            nwNode.setObj(ips);

            blackIpsNode = nwNode;
        }
        catch(FileNotFoundException e) {
            e.printStackTrace();
            logger.error("risk check black file failed!", e);
            blackIpsNode = null;
        }
        catch(IOException e) {
            e.printStackTrace();
            logger.error("risk check black file failed!", e);
            blackIpsNode = null;
        }

        return rt;
    }

    private RiskLevel judgeTimes(long value, long ok, long denied) {
        if (value < ok) {
            return RiskLevel.OK;
        } else if (value >= denied) {
            return RiskLevel.DENIED;
        } else {
            return RiskLevel.DANGER;
        }
    }

    public static long longInteger(final CharSequence value, final long defaultValue) {
        if (value == null) {return defaultValue;}

        String b = value.toString().trim();
        if (b == null || b.length() == 0) {
            return defaultValue;
        }

        try {
            return Long.parseLong(b);
        } catch (Throwable e) {}

        return defaultValue;
    }

    public static class RiskManagerHolder {
        public static RiskManager manager;

        static {
            try {
                manager = null;

                manager = new RiskManager();

            } catch (Exception e) {
                manager = null;
                logger.error("risk manager load failed!", e);
            }
        }
    }
}
