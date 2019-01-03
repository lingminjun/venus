package com.venus.apigw.servlet;

import com.alibaba.dubbo.config.ProtocolConfig;
import com.venus.apigw.config.GWConfig;
import com.venus.apigw.manager.APIManager_New;
import com.venus.esb.lang.ESBConsts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * 启动监听器
 */
@WebListener
public class StartupListener implements ServletContextListener {
    private static final Logger logger = LoggerFactory.getLogger(StartupListener.class.getName());

    static {
        try {
            GWConfig.getInstance();
            //设置esb debug模式
            ESBConsts.IS_DEBUG = true;

            APIManager_New.shared();
        } catch (Throwable e) {
            logger.error("application init failed. ====================================================================================", e);
            throw new RuntimeException(e);
        }
    }

    public void contextInitialized(ServletContextEvent sce) {
        APIManager_New.shared().ready(sce);
    }

    public void contextDestroyed(ServletContextEvent arg0) {
        APIManager_New.shared().destroy();
        ProtocolConfig.destroyAll();
    }
}
