package com.venus.apigw.config;

import com.alibaba.fastjson.JSON;
import com.venus.esb.lang.ESBT;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * gw相关静态配置
 */
public class GWConfig {
    public static final boolean isDebug = false;

    private static final Logger logger = LoggerFactory.getLogger(GWConfig.class);
    private static final String DEFAULT_SEVICE_VERSION = "default";

    private static class SingletonHolder {
        private static GWConfig instance = new GWConfig();
    }

    private GWConfig() {
        Properties prop = new Properties();
        ClassLoader loader = Thread.currentThread().getContextClassLoader();

        InputStream input = null;
        try {
            input = loader.getResourceAsStream("config.properties");
            prop.load(input);
        } catch (Throwable e) {
            throw new RuntimeException("缺少网关相关配置",e);
        }

        this.setApiJarPath(prop.getProperty("com.venus.apigw.jarPath"));
        this.setOpenAPIDocument(prop.getProperty("com.venus.apigw.open.ducoment"));
        this.setBlackPath(prop.getProperty("com.venus.apigw.risk.black.ips.path"));
        this.setVfCodeCheckUrl(prop.getProperty("com.venus.apigw.risk.vfcode.check.url"));
        this.setOssAccessKey(prop.getProperty("oss.bucket.accessKey"));
        this.setOssAccessSecret(prop.getProperty("oss.bucket.accessSecret"));
        this.setOssEndPoint(prop.getProperty("oss.bucket.endPoint"));
        this.setZkAddress(prop.getProperty("dubbo.registry.url"));
        this.setServiceVersion(prop.getProperty("dubbo.reference.version"));

        this.setOssOrder(prop.getProperty("oss.bucket.order.info"));
        this.setOssUserPrivate(prop.getProperty("oss.bucket.user.private"));
        this.setOssPublic(prop.getProperty("oss.bucket.public"));

        this.setDatasourceUrl(prop.getProperty("com.venus.gw.datasource.url"));
        this.setDatasourceUsername(prop.getProperty("com.venus.gw.datasource.username"));
        this.setDatasourcePassword(prop.getProperty("com.venus.gw.datasource.password"));
        this.setDatasourceDriverClass(prop.getProperty("com.venus.gw.datasource.driverClassName"));

        this.setLogSyncDB(ESBT.bool(prop.getProperty("com.venus.apigw.log.sync.db")));
        this.setLogSyncKafka(ESBT.bool(prop.getProperty("com.venus.apigw.log.sync.kafka")));

        this.setBidatasourceUrl(prop.getProperty("com.venus.bi.datasource.url"));
        this.setBidatasourceUsername(prop.getProperty("com.venus.bi.datasource.username"));
        this.setBidatasourcePassword(prop.getProperty("com.venus.bi.datasource.password"));
        this.setBidatasourceDriverClass(prop.getProperty("com.venus.bi.datasource.driverClassName"));

        if (input != null) {
            try {
                input.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static final GWConfig getInstance() {
        return SingletonHolder.instance;
    }


    /**
     * apigw在注册中心的名字
     */
    private String applicationName = "apigw";

    public String getApplicationName() {
        return applicationName;
    }


    private String apiJarPath = null;

    private void setApiJarPath(String apiJarPath) {
        this.apiJarPath = apiJarPath;
        if (GWConfig.isDebug)
            logger.info("[ApiConfig.init]com.venus.apigw.jarPath:{}", this.apiJarPath);
    }

    public String getApiJarPath() {
        return apiJarPath;
    }

    private boolean openAPIDocument = false;
    public boolean isOpenAPIDocument() {
        return openAPIDocument;
    }

    private void setOpenAPIDocument(String openAPIDocument) {
        if (openAPIDocument != null) {
            try {
                this.openAPIDocument = Boolean.parseBoolean(openAPIDocument);
            } catch (Throwable e) {
                logger.error("com.venus.apigw.open.ducoment 参数设置错误",e);
            }
        }
        if (GWConfig.isDebug) {
            logger.info("[ApiConfig.init]com.venus.apigw.open.ducoment:{}", this.openAPIDocument);
        }
    }

    private String blackPath = null;
    private void setBlackPath(String apiJarPath) {
        this.blackPath = apiJarPath;
        if (GWConfig.isDebug)
            logger.info("[ApiConfig.init] com.venus.apigw.risk.black.ips.path :{}", this.blackPath);
    }
    public String getBlackPath() {
        return blackPath;
    }


    private String vfCodeCheckUrl = null;
    private void setVfCodeCheckUrl(String apiJarPath) {
        this.vfCodeCheckUrl = apiJarPath;
        if (GWConfig.isDebug)
            logger.info("[ApiConfig.init] com.venus.apigw.risk.vfcode.check.url :{}", this.vfCodeCheckUrl);
    }
    public String getVfCodeCheckUrl() {
        return vfCodeCheckUrl;
    }


    private String zkAddress = null;

    private void setZkAddress(String zkAddress) {
        if (zkAddress == null || zkAddress.isEmpty()) {
            throw new RuntimeException("can not find zk address config");
        }
        this.zkAddress = zkAddress;
        if (GWConfig.isDebug)
            logger.info("[ApiConfig.init]dubbo.registry.url:{}", this.zkAddress);
    }

    public String getZkAddress() {
        return zkAddress;
    }

    private String serviceVersion = null;

    private void setServiceVersion(String serviceVersion) {
        if (serviceVersion != null && !serviceVersion.isEmpty()) {
            if (!serviceVersion.trim().isEmpty() && !serviceVersion.equalsIgnoreCase(DEFAULT_SEVICE_VERSION)) {
                this.serviceVersion = serviceVersion;
            } else {
                this.serviceVersion = "";
            }
        } else {
            throw new RuntimeException("can not find service version config");
        }
        if (GWConfig.isDebug)
            logger.info("[ApiConfig.init]dubbo.reference.version:{}", this.serviceVersion);
    }

    public String getServiceVersion() {
        return serviceVersion;
    }


    private String ossUserPrivate = null;

    private void setOssUserPrivate(String name) {
        if (name != null && name.trim().length() > 0) {
            this.ossUserPrivate = name;
        }
        if (GWConfig.isDebug)
            logger.info("[ApiConfig.init] oss.bucket.user.private:{}", this.ossUserPrivate);
    }

    public String getOssUserPrivate() {
        return this.ossUserPrivate;
    }

    private String ossPublic = null;

    private void setOssPublic(String name) {
        if (name != null && name.trim().length() > 0) {
            this.ossPublic = name;
        }
        if (GWConfig.isDebug)
            logger.info("[ApiConfig.init] oss.bucket.public:{}", this.ossPublic);
    }

    public String getOssPublic() {
        return this.ossPublic;
    }

    private String ossOrder = null;

    private void setOssOrder(String name) {
        if (name != null && name.trim().length() > 0) {
            this.ossOrder = name;
        }
        if (GWConfig.isDebug)
            logger.info("[ApiConfig.init] oss.bucket.order.info:{}", this.ossOrder);
    }

    public String getOssOrder() {
        return this.ossOrder;
    }

    private String ossAccessKey = null;

    private void setOssAccessKey(String key) {
        if (key != null && key.trim().length() > 0) {
            this.ossAccessKey = key;
        }
        if (GWConfig.isDebug)
            logger.info("[ApiConfig.init]oss.bucket.accessKey:{}", this.ossAccessKey);
    }

    public String getOssAccessKey() {
        return this.ossAccessKey;
    }

    private String ossAccessSecret = null;

    private void setOssAccessSecret(String secret) {
        if (secret != null && secret.trim().length() > 0) {
            this.ossAccessSecret = secret;
        }
        if (GWConfig.isDebug)
            logger.info("[ApiConfig.init]oss.bucket.accessSecret:{}", this.ossAccessSecret);
    }

    public String getOssAccessSecret() {
        return this.ossAccessSecret;
    }

    private String ossEndPoint = null;

    private void setOssEndPoint(String endPoint) {
        if (endPoint != null && endPoint.trim().length() > 0) {
            this.ossEndPoint = endPoint;
        }
        if (GWConfig.isDebug)
            logger.info("[ApiConfig.init]oss.bucket.endPoint:{}", this.ossEndPoint);
    }

    public String getOssEndPoint() {
        return this.ossEndPoint;
    }



    // 数据库配置
    private String datasourceUrl;
    private String datasourceUsername;
    private String datasourcePassword;
    private String datasourceDriverClass;

    public String getDatasourceUrl() {
        return datasourceUrl;
    }

    public void setDatasourceUrl(String datasourceUrl) {
        this.datasourceUrl = datasourceUrl;
    }

    public String getDatasourceUsername() {
        return datasourceUsername;
    }

    public void setDatasourceUsername(String datasourceUsername) {
        this.datasourceUsername = datasourceUsername;
    }

    public String getDatasourcePassword() {
        return datasourcePassword;
    }

    public void setDatasourcePassword(String datasourcePassword) {
        this.datasourcePassword = datasourcePassword;
    }

    public String getDatasourceDriverClass() {
        return datasourceDriverClass;
    }

    public void setDatasourceDriverClass(String datasourceDriverClass) {
        this.datasourceDriverClass = datasourceDriverClass;
    }


    private boolean logSyncDB;
    private boolean logSyncKafka;

    // 数据库配置
    private String bidatasourceUrl;
    private String bidatasourceUsername;
    private String bidatasourcePassword;
    private String bidatasourceDriverClass;

    public boolean isLogSyncDB() {
        return logSyncDB;
    }

    public void setLogSyncDB(boolean logSyncDB) {
        this.logSyncDB = logSyncDB;
    }

    public boolean isLogSyncKafka() {
        return logSyncKafka;
    }

    public void setLogSyncKafka(boolean logSyncKafka) {
        this.logSyncKafka = logSyncKafka;
    }

    public String getBidatasourceUrl() {
        return bidatasourceUrl;
    }

    public void setBidatasourceUrl(String bidatasourceUrl) {
        this.bidatasourceUrl = bidatasourceUrl;
    }

    public String getBidatasourceUsername() {
        return bidatasourceUsername;
    }

    public void setBidatasourceUsername(String bidatasourceUsername) {
        this.bidatasourceUsername = bidatasourceUsername;
    }

    public String getBidatasourcePassword() {
        return bidatasourcePassword;
    }

    public void setBidatasourcePassword(String bidatasourcePassword) {
        this.bidatasourcePassword = bidatasourcePassword;
    }

    public String getBidatasourceDriverClass() {
        return bidatasourceDriverClass;
    }

    public void setBidatasourceDriverClass(String bidatasourceDriverClass) {
        this.bidatasourceDriverClass = bidatasourceDriverClass;
    }


//=================================注意分割线=================================
    // 建议放入动态配置
    /** 开启ip风控 */
    private boolean                 riskOpen = true;

    /** 风控阀值数据值配置 */
    private long riskNoneDangerValue = 8000;
    private long riskNoneDeniedValue = 15000;
    private long riskDeviceDangerValue = 15000;
    private long riskDeviceDeniedValue = 20000;

    /** 风控读取黑白名单缓存时间 */
    private long riskCacheDuration = 30 * 1000;

    /** 风控白名单ip */
    private Set<String> whiteIps = new HashSet<String>();

    /** 风控黑名单ip */
    private Set<String> blackIps = new HashSet<String>();

    private Map<String,String> markvalue = new HashMap<String, String>();

    private void initDynamicConfig(Properties properties) {
        this.riskOpen = ESBT.bool(properties.getProperty("com.venus.apigw.risk.open"),true);
        this.riskNoneDangerValue = ESBT.longInteger(properties.getProperty("com.venus.apigw.risk.noneDangerValue"),8000);
        this.riskNoneDeniedValue = ESBT.longInteger(properties.getProperty("com.venus.apigw.risk.noneDeniedValue"),15000);
        this.riskDeviceDangerValue = ESBT.longInteger(properties.getProperty("com.venus.apigw.risk.deviceDangerValue"),15000);
        this.riskDeviceDeniedValue = ESBT.longInteger(properties.getProperty("com.venus.apigw.risk.deviceDeniedValue"),20000);
        this.riskCacheDuration = ESBT.longInteger(properties.getProperty("com.venus.apigw.risk.cacheDuration"),30000);
        this.whiteIps = buildSetByStr(properties.getProperty("com.venus.apigw.risk.whiteIps"));
        this.blackIps = buildSetByStr(properties.getProperty("com.venus.apigw.risk.blackIps"));
        this.markvalue = buildMapByStr(properties.getProperty("com.venus.apigw.risk.markvalue"));

        this.setOriginWhiteList(properties.getProperty("com.venus.apigw.originWhiteList"));
    }
    private Set<String> buildSetByStr(String str) {

        if (StringUtils.isBlank(str)) {
            return new HashSet<String>();
        }
        String[] strList = str.split(",");
        HashSet<String> list = new HashSet<String>(strList.length);
        for (int i = 0; i < strList.length; i++) {
            list.add(strList[i]);
        }
        return list;
    }


    private Map<String,String> buildMapByStr(String str) {

        if (StringUtils.isBlank(str)) {
            return new HashMap<String, String>();
        }

        Map map = JSON.parseObject(str, HashMap.class);
        return map;
    }

    public boolean isRiskOpen() {
        return riskOpen;
    }

    public long getRiskNoneDangerValue() {
        return riskNoneDangerValue;
    }

    public long getRiskNoneDeniedValue() {
        return riskNoneDeniedValue;
    }

    public long getRiskDeviceDangerValue() {
        return riskDeviceDangerValue;
    }

    public long getRiskDeviceDeniedValue() {
        return riskDeviceDeniedValue;
    }

    public long getRiskCacheDuration() {
        return riskCacheDuration;
    }

    public Set<String> getWhiteIps() {
        return whiteIps;
    }

    public Set<String> getBlackIps() {
        return blackIps;
    }

    public Map<String, String> getMarkvalue() {
        return markvalue;
    }

    /**
     * 域名白名单
     */
    private HashMap<String, String> originWhiteList = new HashMap<String, String>();

    private void setOriginWhiteList(String list) {
        if (list != null && list.length() > 0) {
            String[] os = list.split(",");
            for (String o : os) {
                String domain = o.trim();
                int index = domain.lastIndexOf('.', domain.lastIndexOf('.', domain.length()) - 1);
                originWhiteList.put(domain, domain.substring(index));
            }
        }
    }

    public HashMap<String, String> getOriginWhiteList() {
        return originWhiteList;
    }
}
