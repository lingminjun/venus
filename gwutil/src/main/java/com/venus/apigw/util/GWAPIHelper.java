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
        return deployJar(jarPath,packageName,gwHost,gwPort,rsaPriKey,null);
    }
    public static String deployJar(String jarPath, String packageName, String gwHost, int gwPort, String rsaPriKey, Set<Class<?>> excludeClasses) throws Exception {
        return deployJar(jarPath,packageName,gwHost,gwPort,rsaPriKey,excludeClasses,null);
    }
    public static String deployJar(String jarPath, String packageName, String gwHost, int gwPort, String rsaPriKey, Set<Class<?>> excludeClasses, Set<String> excludeMethods) throws Exception {

        File file = new File(jarPath);
        if (file == null) {
            return null;
        }
        EngineClassLoader loader = new EngineClassLoader();//Thread.currentThread().getContextClassLoader();
        loader.addURL(file.toURI().toURL());
        Set<Class<?>> set = PackageUtil.getClasses(packageName,loader);
        List<ESBAPIInfo> list = loadAll(set,excludeClasses);
        if (list.size() > 0) {
            return deploy(list,gwHost,gwPort,rsaPriKey,excludeMethods,null);
        }
        return null;
    }

    //将某个package包下所有接口发布到网关
    public static String deployPackage(String packageName, String gwHost, int gwPort, String rsaPriKey) throws Exception {
        return deployPackage(packageName,gwHost,gwPort,rsaPriKey,null);
    }
    public static String deployPackage(String packageName, String gwHost, int gwPort, String rsaPriKey, Set<Class<?>> excludeClasses) throws Exception {
        return deployPackage(packageName,gwHost,gwPort,rsaPriKey,excludeClasses,null);
    }
    public static String deployPackage(String packageName, String gwHost, int gwPort, String rsaPriKey, Set<Class<?>> excludeClasses, Set<String> excludeMethods) throws Exception {
        Set<Class<?>> set = PackageUtil.getClasses(packageName);
        List<ESBAPIInfo> list = loadAll(set,excludeClasses);
        if (list.size() > 0) {
            return deploy(list,gwHost,gwPort,rsaPriKey,excludeMethods,null);
        }
        return null;
    }

    //将某个Service所有接口发布到网关
    public static String deployService(Class<?> serviceClass, String gwHost, int gwPort, String rsaPriKey) throws Exception {
        return deployService(serviceClass,gwHost,gwPort,rsaPriKey,null);
    }

    //发布整个service除了部分
    public static String deployService(Class<?> serviceClass, String gwHost, int gwPort, String rsaPriKey, Set<String> exclude) throws Exception {
        List<ESBAPIInfo> list = ESBAPIHelper.generate(serviceClass,null,true);
        if (list.size() > 0) {
            return deploy(list,gwHost,gwPort,rsaPriKey,exclude,null);
        }
        return null;
    }

    //仅仅发布指定的接口
    public static String deployServiceSpecial(Class<?> serviceClass, String gwHost, int gwPort, String rsaPriKey, Set<String> special) throws Exception {
        List<ESBAPIInfo> list = ESBAPIHelper.generate(serviceClass,null,true);
        if (list.size() > 0) {
            return deploy(list,gwHost,gwPort,rsaPriKey,null,special);
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
        params.put("API_INFO",jsonAPIList);
        addSign(rsaPriKey,params);
        return params.get(ESBSTDKeys.SIGN_KEY);
    }

    // 添加 refresh接口和sso接口
    public static String uploadESBSpecialAPIs(String gwHost, int gwPort, String rsaPriKey) throws Exception {
        StringBuilder builder = new StringBuilder("[");
        builder.append(REFRESH_API);
        builder.append(",");
        builder.append(SSO_API);
        builder.append("]");
        HashMap<String,String> params = new HashMap<>();
        params.put("API_INFO",builder.toString());
        addSign(rsaPriKey,params);
        String result = HTTP.post(gwAPIUrl(gwHost,gwPort),params);
        return result;
    }

    private static String gwAPIUrl(String gwHost, int gwPort) {
        if (gwPort == 443) {
            return "https://" + gwHost + "/gw.do";
        } else {
            return "http://" + gwHost + ":" + (gwPort <= 0 ? 80 : gwPort) + "/gw.do";
        }
    }

    private static List<ESBAPIInfo> loadAll(Set<Class<?>> set, Set<Class<?>> exclude) {
        List<ESBAPIInfo> list = new ArrayList<>();
        for (Class<?> clazz : set) {

            //排除不需要的类
            if (exclude != null && exclude.contains(clazz)) {
                continue;
            }

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
    private static String deploy(List<ESBAPIInfo> list, String gwHost, int gwPort, String rsaPriKey, Set<String> exclude, Set<String> limit) throws Exception {
        List<ESBAPIInfo> apis = new ArrayList<>();
        for (ESBAPIInfo info : list) {
            if (info.api != null) {
                String api = info.api.getAPISelector();
                if ((exclude == null || exclude.isEmpty() || !exclude.contains(api)) // 排除在外的
                        && (limit == null || limit.isEmpty() || limit.contains(api))) // 包含在内的
                {
                    apis.add(info);
                }
            }
        }

        if (apis.isEmpty()) {
            return "empty apis";
        }

        String json = JSON.toJSONString(apis,ESBConsts.FASTJSON_SERIALIZER_FEATURES);
        HashMap<String,String> params = new HashMap<>();
        params.put("API_INFO",json);
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

    private static final String REFRESH_API = "{\"api\":{\"desc\":\"刷新token\",\"detail\":\"\",\"domain\":\"esb\",\"methodName\":\"ESBSpecial\",\"module\":\"auth\",\"needVerify\":false,\"openAPI\":false,\"owner\":\"\",\"params\":[{\"defaultValue\":\"\",\"desc\":\"secret token\",\"isArray\":false,\"name\":\"_stk\",\"required\":true,\"type\":\"java.lang.String\"},{\"defaultValue\":\"\",\"desc\":\"refresh token\",\"isArray\":false,\"name\":\"_rtk\",\"required\":true,\"type\":\"java.lang.String\"}],\"returned\":{\"coreType\":\"com.venus.esb.lang.ESBToken\",\"declareType\":\"com.venus.esb.lang.ESBToken\",\"desc\":\"令牌认证类型\",\"displayType\":\"com.venus.esb.lang.ESBToken\",\"finalType\":\"com.venus.esb.lang.ESBToken\",\"isArray\":false,\"type\":\"com.venus.esb.lang.ESBToken\"},\"security\":256,\"structs\":{\"com.venus.esb.lang.ESBToken\":{\"desc\":\"令牌认证类型\",\"fields\":[{\"desc\":\"认证是否成功\",\"isArray\":false,\"name\":\"success\",\"required\":false,\"type\":\"boolean\"},{\"desc\":\"access_token：表示访问令牌，必选项。将被写入到cookie，js可读（httpOnly=false）\",\"isArray\":false,\"name\":\"token\",\"required\":false,\"type\":\"java.lang.String\"},{\"desc\":\"secret_token: 用于在不同domain间传递csrftoken, 只能在https(secure=true)传入, 将被写入到cookie，js不可读（httpOnly=true）\",\"isArray\":false,\"name\":\"stoken\",\"required\":false,\"type\":\"java.lang.String\"},{\"desc\":\"refresh_token：用于刷新access_token（同理会刷新secret_token），可选项，不写入cookie，客户端保留好。\",\"isArray\":false,\"name\":\"refresh\",\"required\":false,\"type\":\"java.lang.String\"},{\"desc\":\"issu_key：颁发公钥，加签秘钥（私钥保存在token中），必选项，不写入cookie，客户端保留好。\",\"isArray\":false,\"name\":\"key\",\"required\":false,\"type\":\"java.lang.String\"},{\"desc\":\"expires_in：表示过期时间点，单位为秒。此字段随为entrust，但只有等于零时才被网关覆盖\",\"isArray\":false,\"name\":\"expire\",\"required\":false,\"type\":\"long\"},{\"desc\":\"scope：表示权限范围，如果与客户端申请的范围一致，此项可省略。推荐【global,user,device,token,temporary】,可自定义\",\"isArray\":false,\"name\":\"scope\",\"required\":false,\"type\":\"java.lang.String\"},{\"desc\":\"设备did\",\"isArray\":false,\"name\":\"did\",\"required\":false,\"type\":\"java.lang.String\"},{\"desc\":\"用户id\",\"isArray\":false,\"name\":\"uid\",\"required\":false,\"type\":\"java.lang.String\"},{\"desc\":\"账号id\",\"isArray\":false,\"name\":\"acct\",\"required\":false,\"type\":\"java.lang.String\"}],\"type\":\"com.venus.esb.lang.ESBToken\"}}}}";
    private static final String SSO_API = "{\"api\":{\"desc\":\"三方授权或者免登接口声明\",\"detail\":\"\",\"domain\":\"esb\",\"methodName\":\"ESBSpecial\",\"module\":\"sso\",\"needVerify\":false,\"openAPI\":false,\"owner\":\"\",\"params\":[{\"defaultValue\":\"\",\"desc\":\"授权域用sso token换取user token\",\"isArray\":false,\"name\":\"_sso_tk\",\"required\":false,\"type\":\"java.lang.String\"},{\"defaultValue\":\"\",\"desc\":\"有user token的应用授权sso token时需要用到secret token\",\"isArray\":false,\"name\":\"_stk\",\"required\":false,\"type\":\"java.lang.String\"},{\"defaultValue\":\"\",\"desc\":\"请求免登或者需要授权的 Application Id\",\"isArray\":false,\"name\":\"_sso_aid\",\"required\":false,\"type\":\"int\"},{\"defaultValue\":\"\",\"desc\":\"请求免登或者需要授权的 device Id\",\"isArray\":false,\"name\":\"_sso_did\",\"required\":false,\"type\":\"long\"},{\"defaultValue\":\"\",\"desc\":\"请求免登或者需要授权的域\",\"isArray\":false,\"name\":\"_sso_domain\",\"required\":false,\"type\":\"java.lang.String\"}],\"returned\":{\"coreType\":\"com.venus.esb.lang.ESBSSOToken\",\"declareType\":\"com.venus.esb.lang.ESBSSOToken\",\"desc\":\"sso令牌认证类型\",\"displayType\":\"com.venus.esb.lang.ESBSSOToken\",\"finalType\":\"com.venus.esb.lang.ESBSSOToken\",\"isArray\":false,\"type\":\"com.venus.esb.lang.ESBSSOToken\"},\"security\":256,\"structs\":{\"com.venus.esb.lang.ESBSSOToken\":{\"desc\":\"sso令牌认证类型\",\"fields\":[{\"desc\":\"认证是否成功\",\"isArray\":false,\"name\":\"success\",\"required\":false,\"type\":\"boolean\"},{\"desc\":\"sso_token：表示访问令牌，必选项。将被写入到cookie\",\"isArray\":false,\"name\":\"ssoToken\",\"required\":false,\"type\":\"java.lang.String\"},{\"desc\":\"expires_in：表示过期时间点，单位为秒。如果省略该参数，必须其他方式设置过期时间。\",\"isArray\":false,\"name\":\"expire\",\"required\":false,\"type\":\"long\"}],\"type\":\"com.venus.esb.lang.ESBSSOToken\"}}}}";
}
