package com.venus.apigw.manager;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.remoting.zookeeper.ChildListener;
import com.alibaba.dubbo.remoting.zookeeper.ZookeeperClient;
import com.alibaba.dubbo.remoting.zookeeper.curator.CuratorZookeeperTransporter;
import com.alibaba.fastjson.JSON;
import com.venus.apigw.document.entities.ApiMethodInfo;
import com.venus.esb.ESBAPIInfo;
import com.venus.esb.config.ESBConfigCenter;
import com.venus.esb.lang.ESBT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created with IntelliJ IDEA.
 * Description: 用于ESB取api目录下jar包调用
 * User: lingminjun
 * Date: 2018-09-18
 * Time: 下午8:46
 */
public final class APIManager_New {
    private static final Logger logger = LoggerFactory.getLogger(APIManager_New.class);
    private static final String GW_NODE_ROOT = "/venus-gw";

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
        String registor = ESBConfigCenter.instance().getRegistry();
        if (registor != null && registor.length() > 0) {
            zkClient = new CuratorZookeeperTransporter().connect(URL.valueOf(registor));//创建zk客户端，启动会话
            zkClient.addChildListener(GW_NODE_ROOT, listener);
//            zkClient.addStateListener(new StateListener() {
//                @Override
//                public void stateChanged(int i) {
//
//                }
//            });
        }

    }

    private Map<String,ESBAPIInfo> apis = new ConcurrentHashMap<>();
    private ServletContext context = null;

    private List<String> hosts = new ArrayList<>();
    private ZookeeperClient zkClient = null;
    private ChildListener listener = new ChildListener() {
        @Override
        public void childChanged(String s, List<String> list) {
            hosts.clear();
            hosts.addAll(list);
            System.out.println("\n网关服务列表：\n" + JSON.toJSONString(list));
        }
    };

    public List<String> getHosts() {
        return hosts;
    }

    public String getHttpIP() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        return null;
    }

    public int getHttpPort() {
        try {
            MBeanServer server = null;
            if (MBeanServerFactory.findMBeanServer(null).size() > 0) {
                server = MBeanServerFactory.findMBeanServer(null).get(0);
            }

            Set names = server.queryNames(new ObjectName("Catalina:type=Connector,*"), null);
            Iterator iterator = names.iterator();
            ObjectName name = null;
            while (iterator.hasNext()) {
                name = (ObjectName) iterator.next();
                String protocol = server.getAttribute(name, "protocol").toString();
                String scheme = server.getAttribute(name, "scheme").toString();
                String port = server.getAttribute(name, "port").toString();
                System.out.println(protocol + " : " + scheme + " : " + port);
                if (protocol.toUpperCase().startsWith("HTTP") && scheme.equalsIgnoreCase("http")) {
                    return ESBT.integer(port);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 8080;//默认端口
    }

    public static void listClientIP() {
        Enumeration<NetworkInterface> netInterfaces = null;
        try {
            netInterfaces = NetworkInterface.getNetworkInterfaces();
            while (netInterfaces.hasMoreElements()) {
                NetworkInterface ni = netInterfaces.nextElement();
                System.out.println("DisplayName:" + ni.getDisplayName());
                System.out.println("Name:" + ni.getName());
                Enumeration<InetAddress> ips = ni.getInetAddresses();
                while (ips.hasMoreElements()) {
                    System.out.println("IP:" + ips.nextElement().getHostAddress());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

//        Enumeration<NetworkInterface> netInterfaces = null;
//        try {
//            netInterfaces = NetworkInterface.getNetworkInterfaces();
//            while (netInterfaces.hasMoreElements()) {
//                NetworkInterface ni = netInterfaces.nextElement();
//                System.out.println("DisplayName:" + ni.getDisplayName());
//                System.out.println("Name:" + ni.getName());
//                Enumeration<InetAddress> ips = ni.getInetAddresses();
//                while (ips.hasMoreElements()) {
//                    System.out.println("IP:" + ips.nextElement().getHostAddress());
//                }
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
    }

    //加载完善
    public void ready(ServletContextEvent sce) {
        context = sce.getServletContext();
        //注册服务
        registerHosts();
    }

    //注销服务
    public void destroy() {
        unregisterHosts();
    }

    private String LOCAL_IP = null;
    private int LOCAL_PORT = 8080;

    public String currentClient() {
        if (LOCAL_IP != null) {
            return LOCAL_IP + ":" + LOCAL_PORT;
        }
        return null;
    }

    private void registerHosts() {
        LOCAL_IP = getHttpIP();
        if (LOCAL_IP != null && zkClient != null) {
            LOCAL_PORT = getHttpPort();
            zkClient.create(GW_NODE_ROOT + "/" + LOCAL_IP + ":" + LOCAL_PORT, false);
        }
    }

    private void unregisterHosts() {
        if (zkClient != null && LOCAL_IP != null) {
            zkClient.delete(GW_NODE_ROOT + "/" + LOCAL_IP + ":" + LOCAL_PORT);
            zkClient.removeChildListener(GW_NODE_ROOT, listener);
            zkClient.close();
            zkClient = null;
            System.out.println("关闭网关注册【" + GW_NODE_ROOT + "/" + LOCAL_IP + ":" + LOCAL_PORT + "】");
        }
    }

    public ESBAPIInfo getAPI(String selector) {
        return apis.get(selector);
    }

    private static Object static_service = new Object();
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
