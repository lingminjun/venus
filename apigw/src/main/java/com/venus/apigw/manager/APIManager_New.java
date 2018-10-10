package com.venus.apigw.manager;

import com.alibaba.dubbo.config.ApplicationConfig;
import com.alibaba.dubbo.config.ReferenceConfig;
import com.alibaba.dubbo.config.RegistryConfig;
import com.venus.apigw.config.GWConfig;
import com.venus.apigw.document.InfoServlet;
import com.venus.apigw.document.entities.ApiMethodInfo;
import com.venus.esb.ESBAPIInfo;
import com.venus.esb.annotation.ESBGroup;
import com.venus.esb.helper.ESBAPIHelper;
import org.apache.catalina.loader.WebappClassLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarFile;

/**
 * Created with IntelliJ IDEA.
 * Description: 用于ESB取api目录下jar包调用
 * User: lingminjun
 * Date: 2018-09-18
 * Time: 下午8:46
 */
public final class APIManager_New {
    private static final Logger logger = LoggerFactory.getLogger(APIManager_New.class);


    /**
     * 单例
     * @return
     */
    public static APIManager_New shared() {
        return APIManager_New.SingletonHolder.INSTANCE;
    }
    private static class SingletonHolder {
        private static APIManager_New INSTANCE = new APIManager_New();
    }

    private APIManager_New() {
    }

    private Map<String,ESBAPIInfo> apis = new ConcurrentHashMap<>();
    private ServletContext context = null;

    //加载完善
    public void ready(ServletContextEvent sce) {
        context = sce.getServletContext();

        loadAPIInfo(sce);

        //代码迁移
        if (GWConfig.getInstance().isOpenAPIDocument()) {
            Collection<ESBAPIInfo> esbApis = apis.values();
            List<ApiMethodInfo> methods = convertToMethodInfo(esbApis);
            InfoServlet.setApiMethodInfos(methods.toArray(new ApiMethodInfo[0]));
        }
    }

    public ESBAPIInfo getAPI(String selector) {


        return apis.get(selector);
    }

    // 加载 jar api
    private void loadAPIInfo(ServletContextEvent sce) {
        try {

//            List<ESBAPIInfo> list = JSON.parseArray(json,ESBAPIInfo.class);
//            for (ESBAPIInfo api : list) {
//                apis.put(api.api.getAPISelector(),api);
//            }
//
//            list = JSON.parseArray(json1,ESBAPIInfo.class);
//            for (ESBAPIInfo api : list) {
//                apis.put(api.api.getAPISelector(),api);
//            }

            WebappClassLoader loader = (WebappClassLoader)getClass().getClassLoader();
            File apiJarDirectory = new File(GWConfig.getInstance().getApiJarPath());


            ApplicationConfig application = new ApplicationConfig();
            application.setName(GWConfig.getInstance().getApplicationName());
            // 连接注册中心配置
            String[] addressArray = GWConfig.getInstance().getZkAddress().split(" ");
            List<RegistryConfig> registryConfigList = new LinkedList<RegistryConfig>();
            for (String zkAddress : addressArray) {
                RegistryConfig registry = new RegistryConfig();
                registry.setAddress(zkAddress);
                registry.setProtocol("dubbo");
                if (GWConfig.isDebug) {
                    Socket socket = new Socket();
                    try {
                        int index1 = zkAddress.indexOf("://");
                        int index2 = zkAddress.lastIndexOf(":");
                        if (index1 < 0) {index1 = 0;}
                        String domain = zkAddress.substring(index1 + 3, index2);
                        String port = zkAddress.substring(index2 + 1);
                        socket.connect(new InetSocketAddress(domain, Integer.parseInt(port)), 1000);
                        registryConfigList.add(registry);
                    } catch (Exception e) {
                        // do nothing
                    } finally {
                        socket.close();
                    }
                } else {
                    registryConfigList.add(registry);
                }
            }

            //业务服务
            if (apiJarDirectory.exists() && apiJarDirectory.isDirectory()) {
                File[] files = apiJarDirectory.listFiles(new FilenameFilter() {
                    @Override
                    public boolean accept(File file, String s) {
                        return s.endsWith(".jar");
                    }
                });
                if (files != null) {
                    for (File f : files) {
                        JarFile jf = null;
                        try {
                            jf = new JarFile(f);
                            if ("dubbo".equals(jf.getManifest().getMainAttributes().getValue("Api-Dependency-Type"))) {
                                String ns = jf.getManifest().getMainAttributes().getValue("Api-Export");
                                String[] names = ns.split(" ");
                                loader.addRepository(f.toURI().toURL().toString());
                                for (String name : names) {
                                    if (name != null) {
                                        name = name.trim();
                                        if (name.length() > 0) {
                                            Class<?> clazz = Class.forName(name);

                                            //现在走ESB,这个方案实际不需要service
                                            Object service = loadService(application,registryConfigList,jf,clazz);

                                            if (service == null) {
                                                logger.error("cannot find dubbo service for " + clazz.getName());
                                            }

//                                            ApiManager.parseApi(clazz, service);
                                            try {
                                                List<ESBAPIInfo> esbApis = parseApi(clazz);
                                                //将api存起来
                                                if (esbApis != null) {
                                                    for (ESBAPIInfo api : esbApis) {
                                                        if (api.api != null) {
                                                            apis.put(api.api.getAPISelector(), api);
                                                        }
                                                    }
                                                }
                                            } catch (Throwable e) {
                                                logger.error("parse dubbo service api error " + clazz.getName());
                                            }
                                        }
                                    }
                                }
                            }
                        } catch (Throwable t) {
                            logger.error("load api failed. " + f.getName(), t);
                        } finally {
                            if (jf != null) {
                                jf.close();
                            }
                        }
                    }
                }
            }
        } catch (Throwable t) {
            logger.error("load api failed.", t);
        }
    }

    private static Object static_service = new Object();
    private static Object loadService(ApplicationConfig application,List<RegistryConfig> registryConfigList, JarFile jf, Class<?> clazz ) throws IOException {
        //现在走ESB,service
        if (static_service != null) {
            return static_service;
        }


        // 注意：ReferenceConfig为重对象，内部封装了与注册中心的连接，以及与服务提供方的连接
        // 引用远程服务
        ReferenceConfig reference = new ReferenceConfig(); // 此实例很重，封装了与注册中心的连接以及与提供者的连接，请自行缓存，否则可能造成内存和连接泄漏
        reference.setApplication(application);
        if (registryConfigList.size() > 0) {
            reference.setRegistries(registryConfigList);// 多个注册中心可以用setRegistries()
        }
        reference.setInterface(clazz);
        reference.setCheck(false);
        reference.setAsync(false);
        if (GWConfig.isDebug) {
            String ver = jf.getManifest().getMainAttributes().getValue("Api-Debug-Version");
            if (ver != null && ver.length() > 0) {
                reference.setVersion(ver);
            } else if (GWConfig.getInstance().getServiceVersion() != null
                    && !GWConfig.getInstance().getServiceVersion().isEmpty()) {
                reference.setVersion(GWConfig.getInstance().getServiceVersion());
            }
        } else {
            if (GWConfig.getInstance().getServiceVersion() != null
                    && !GWConfig.getInstance().getServiceVersion().isEmpty()) {
                reference.setVersion(GWConfig.getInstance().getServiceVersion());
            }
        }
        // 和本地bean一样使用xxxService
        reference.setRetries(0);
        Object service = null;
        if (GWConfig.isDebug) {
            if (registryConfigList.size() > 0) {
                try {
                    service = reference.get(); // 注意：此代理对象内部封装了所有通讯细节，对象较重，请缓存复用
                } catch (Exception e) {
                    logger.error("get reference failed. " + clazz.getName(), e);
                    service = new Object();
                }
            } else {
                service = null;
            }
        } else {
//                                                service = new Object();
            service = reference.get(); // 注意：此代理对象内部封装了所有通讯细节，对象较重，请缓存复用
        }
        return service;
    }

    public static List<ESBAPIInfo> parseApi(Class<?> clazz) {
        ESBGroup groupAnnotation = clazz.getAnnotation(ESBGroup.class);
        if (groupAnnotation == null) {
            return null;
        }

        List<ESBAPIInfo> list = ESBAPIHelper.generate(clazz,null,true);
        if (list.size() == 0) {
            throw new RuntimeException("[API] api method not found. class:" + clazz.getName());
        }
        return list;
    }

    public static ApiMethodInfo convertToMethodInfo(ESBAPIInfo info) {
        ApiMethodInfo apiInfo = new ApiMethodInfo();
        apiInfo.apiInfo = info;

                /*
                if (!GWConfig.isDebug) {
                    if (serviceInstance == null) {
                        if (!clazz.isInterface() && !Modifier.isAbstract(clazz.getModifiers())) {
                            try {
                                apiInfo.serviceInstance = clazz.newInstance();
                            } catch (Exception e) {
                                throw new RuntimeException("服务实例化失败" + clazz.getName(), e);
                            }
                        } else {
                            throw new RuntimeException("服务实例不存在" + clazz.getName());
                        }
                    }
                }*/

        //给api暴露的provider service一定要注意,不可有状态
        apiInfo.serviceInstance = static_service;
        apiInfo.dubboInterface = Object.class;
        return apiInfo;
    }

    public static List<ApiMethodInfo> convertToMethodInfo(Collection<ESBAPIInfo> esbApis) {
        try {
            List<ApiMethodInfo>  apis = new ArrayList<>();

            for (ESBAPIInfo info : esbApis) {
                apis.add(convertToMethodInfo(info));
            }

            return apis;
        } catch (Throwable t) {
            logger.error("convert api to method info failed.", t);

        }
        return null;
    }
}
