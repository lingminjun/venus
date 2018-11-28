package com.venus.apigw.util;

import com.alibaba.fastjson.JSON;
import com.venus.esb.ESBAPIInfo;
import com.venus.esb.annotation.ESBGroup;
import com.venus.esb.helper.ESBAPIHelper;
import com.venus.esb.lang.ESBConsts;
import com.venus.esb.lang.ESBSTDKeys;
import com.venus.esb.sign.ESBAPIAlgorithm;
import com.venus.esb.sign.ESBAPISignature;
import com.venus.esb.sign.utils.Signable;
import com.venus.esb.utils.HTTP;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLEncoder;
import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * Description:
 * User: lingminjun
 * Date: 2018-11-27
 * Time: 下午2:59
 * 自动更新到网关
 */
public final class GWAPIHelper {
    //将某个jar内所有接口发布到网关
    public static String deployJar(String jarPath, String packageName, String gwHost, int gwPort, String rsaPriKey) throws Exception {

        File file = new File(jarPath);
        if (file == null) {
            return null;
        }
        EngineClassLoader loader = new EngineClassLoader();//Thread.currentThread().getContextClassLoader();
        loader.addURL(file.toURI().toURL());
        Set<Class<?>> set = PackageUtil.getClasses(packageName,loader);
        List<ESBAPIInfo> list = loadAll(set);
        if (list.size() > 0) {
            return deploy(list,gwHost,gwPort,rsaPriKey);
        }
        return null;
    }

    //将某个package包下所有接口发布到网关
    public static String deployPackage(String packageName, String gwHost, int gwPort, String rsaPriKey) throws Exception {
        Set<Class<?>> set = PackageUtil.getClasses(packageName);
        List<ESBAPIInfo> list = loadAll(set);
        if (list.size() > 0) {
            return deploy(list,gwHost,gwPort,rsaPriKey);
        }
        return null;
    }

    //将某个Service所有接口发布到网关
    public static String deployService(Class<?> serviceClass, String gwHost, int gwPort, String rsaPriKey) throws Exception {
        List<ESBAPIInfo> list = ESBAPIHelper.generate(serviceClass,null,true);
        if (list.size() > 0) {
            return deploy(list,gwHost,gwPort,rsaPriKey);
        }
        return null;
    }

    //将某个Service所有接口发布到网关
    public static String rollback(String theStamp, String gwHost, int gwPort, String rsaPriKey) throws Exception {
        HashMap<String,String> params = new HashMap<>();
        params.put("ROLLBACK_STAMP",theStamp);
        addSign(rsaPriKey,params);
        String result = HTTP.post(gwAPIUrl(gwHost,gwPort),params);
        return result;
    }

    //将某个Service所有接口发布到网关
    public static String uploadSign(String jsonAPIList, String rsaPriKey) {
        HashMap<String,String> params = new HashMap<>();
        params.put("API",jsonAPIList);
        addSign(rsaPriKey,params);
        return params.get(ESBSTDKeys.SIGN_KEY);
    }

    private static String gwAPIUrl(String gwHost, int gwPort) {
        if (gwPort == 443) {
            return "https://" + gwHost + "/gw.do";
        } else {
            return "http://" + gwHost + ":" + (gwPort <= 0 ? 80 : gwPort) + "/gw.do";
        }
    }

    private static List<ESBAPIInfo> loadAll(Set<Class<?>> set) {
        List<ESBAPIInfo> list = new ArrayList<>();
        for (Class<?> clazz : set) {
            ESBGroup group = clazz.getAnnotation(ESBGroup.class);
            if (group == null) {
                continue;
            }

            List<ESBAPIInfo> tlist = ESBAPIHelper.generate(clazz,null,true);
            if (tlist != null && tlist.size() > 0) {
                list.addAll(tlist);
            }
        }
        return list;
    }


    // 发布到接口
    private static String deploy(List<ESBAPIInfo> list, String gwHost, int gwPort, String rsaPriKey) throws Exception {
        String json = JSON.toJSONString(list,ESBConsts.FASTJSON_SERIALIZER_FEATURES);
        HashMap<String,String> params = new HashMap<>();
        params.put("API",json);
        addSign(rsaPriKey,params);
        String result = HTTP.post(gwAPIUrl(gwHost,gwPort),params);
        return result;
    }

    //加签参数
    private static void addSign(String key, Map<String,String> params) {
        StringBuilder builder = ESBAPISignature.getSortedParameters(params);
        Signable signature = ESBAPISignature.getSignable(ESBAPIAlgorithm.RSA.name(), null ,key);
        String sign = signature.sign(builder.toString().getBytes(ESBConsts.UTF8));
        params.put(ESBSTDKeys.SIGN_KEY,sign);
    }

    /**
     * 自定义类加载器
     */
    private static class EngineClassLoader extends URLClassLoader {

        public EngineClassLoader() {
            this(getSystemClassLoader());
        }

        public EngineClassLoader(ClassLoader parent) {
            super(new URL[] {}, parent);
        }

        public void addURL(URL... urls) {
            if (urls != null) {
                for (URL url : urls) {
                    super.addURL(url);
                }
            }
        }

        public void addFile(File... files) throws IOException {
            if (files != null) {
                for (File file : files) {
                    if (file != null) {
                        super.addURL(file.toURI().toURL());
                    }
                }
            }
        }
    }
}
