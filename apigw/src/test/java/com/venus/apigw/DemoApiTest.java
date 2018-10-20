package com.venus.apigw;

import com.alibaba.fastjson.JSON;
import com.venus.apigw.document.entities.Response;
import com.venus.esb.lang.ESBConsts;
import com.venus.esb.lang.ESBDeviceToken;
import com.venus.esb.lang.ESBSTDKeys;
import com.venus.esb.lang.ESBToken;
import com.venus.esb.sign.ESBAPISignature;
import com.venus.esb.sign.utils.Signable;
import com.venus.esb.utils.HTTP;
import org.junit.Test;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * Description: 验证dubbo-service-template几种类型请求调用，此处都没有传入aid,实际生产必须设置aid
 * User: lingminjun
 * Date: 2018-09-26
 * Time: 下午2:17
 */
public class DemoApiTest {


    public enum APP_ID {
        // 通用调用
        mymm(0,"mm.static.key!"),
        mymm_pc(1,"mm.static.key!"),
        mymm_iOS(2,"mm.static.key!"),
        mymm_android(3,"mm.static.key!");

        private APP_ID(int id, String key) {
            this.aid = id;
            this.static_key = key;
        }

        public static APP_ID valueOf(int id) {
            if (id == mymm_pc.aid) {
                return mymm_pc;
            } else if (id == mymm_iOS.aid) {
                return mymm_iOS;
            } else if (id == mymm_android.aid) {
                return mymm_android;
            }
            return mymm;
        }

        public final int aid;
        public final String static_key;
    }


    String static_key = "mm.static.key!";
    String sm = "md5";

    public static class Resp<T> implements Serializable {
        public Response stat;
//        public List<T> content;
        public T[] content;
    }

    public static class TokenResp extends Resp<ESBToken> {

    }

    public static class DeviceTokenResp extends Resp<ESBDeviceToken> {

    }

    //加签参数
    public void addMD5Sign(String key,Map<String,String> params) throws UnsupportedEncodingException {
        params.put(ESBSTDKeys.SIGNATURE_METHOD_KEY,sm);

        StringBuilder builder = ESBAPISignature.getSortedParameters(params);
        Signable signature = ESBAPISignature.getSignable(sm, (key == null || key.length() == 0) ? static_key : key ,null);

        String sign = signature.sign(builder.toString().getBytes(ESBConsts.UTF8));
        params.put(ESBSTDKeys.SIGN_KEY,sign);
    }

    public StringBuilder getSortedParameters(Map<String, String> params) throws UnsupportedEncodingException {
        HashMap<String,String> map = new HashMap<>();
        for (Map.Entry<String,String> entry : params.entrySet()) {
            map.put(URLEncoder.encode(entry.getKey(),ESBConsts.UTF8_STR),URLEncoder.encode(entry.getValue(),ESBConsts.UTF8_STR));
        }
        return ESBAPISignature.getSortedParameters(map);
    }


    @Test
    public void testSayHello() throws Exception {
        Map<String,String> params = new HashMap<>();
        params.put(ESBSTDKeys.SELECTOR_KEY,"demo.sayHello");
        params.put("name","abc");
        params.put(ESBSTDKeys.AID_KEY,"" + APP_ID.mymm_pc.aid);
        addMD5Sign(null,params);

        String json = HTTP.get("http://localhost:8080/m.api", params);
        System.out.println("【"+json+"】");

    }

    @Test
    public void testWebLogin() throws Exception {
        Map<String,String> params = new HashMap<>();
        params.put(ESBSTDKeys.SELECTOR_KEY,"demo.webLogin");
        params.put("name","中文试试");
        params.put("pswd","123456");
        params.put(ESBSTDKeys.AID_KEY,"" + APP_ID.mymm_pc.aid);
        addMD5Sign(null,params);

        String json = HTTP.get("http://localhost:8080/m.api", params);
        System.out.println("【"+json+"】");
        Resp<ESBToken> resp = JSON.parseObject(json, TokenResp.class);

    }


    @Test
    public void testMobileLogin() throws Exception {
        //登录获取
        String dtoken;
        String dcsr_key;
        {
            Map<String,String> params = new HashMap<>();
            params.put(ESBSTDKeys.SELECTOR_KEY,"demo.logDevice");
            params.put("name","abc");
            params.put(ESBSTDKeys.AID_KEY,"" + APP_ID.mymm_iOS.aid);
            addMD5Sign(null,params);

            String json = HTTP.get("http://localhost:8080/m.api", params);
            System.out.println("【"+json+"】");
            Resp<ESBDeviceToken> resp = JSON.parseObject(json, DeviceTokenResp.class);
            dtoken = resp.content[0].token;
            dcsr_key = resp.content[0].key;
        }


        Map<String,String> params = new HashMap<>();
        params.put(ESBSTDKeys.SELECTOR_KEY,"demo.mobileLogin");
        params.put("name","中文试试");
        params.put("pswd","123456");
        params.put(ESBSTDKeys.DEVICE_TOKEN_KEY,dtoken);
        addMD5Sign(dcsr_key,params);

        String json = HTTP.get("http://localhost:8080/m.api", params);
        System.out.println("【"+json+"】");
//        Resp<ESBToken> resp = JSON.parseObject(json, TokenResp.class);

    }


    @Test
    public void testSayFriend() throws Exception {

        //登录获取
        String token;
        String stoken;
        String rtoken;
        String csrf_key; //秘钥

        {
            Map<String,String> params = new HashMap<>();
            params.put(ESBSTDKeys.SELECTOR_KEY,"demo.webLogin");
            params.put("name","MJ");
            params.put("pswd","123456");
            params.put(ESBSTDKeys.AID_KEY,"" + APP_ID.mymm_pc.aid);
            addMD5Sign(null,params);

            String json = HTTP.get("http://localhost:8080/m.api", params);
            System.out.println("【"+json+"】");
            Resp<ESBToken> resp = JSON.parseObject(json, TokenResp.class);
            csrf_key = resp.content[0].key;
            token = resp.content[0].token;
            stoken = resp.content[0].stoken;
            rtoken = resp.content[0].refresh;
        }

        Map<String,String> params = new HashMap<>();
        params.put(ESBSTDKeys.SELECTOR_KEY,"demo.sayFriend");
        params.put("name","MJ");
        params.put(ESBSTDKeys.TOKEN_KEY,token);
        addMD5Sign(csrf_key,params);

        String json = HTTP.get("http://localhost:8080/m.api", params);
        System.out.println("【"+json+"】");
    }

    @Test
    public void testArray() throws Exception {
        Map<String,String> params = new HashMap<>();
        params.put(ESBSTDKeys.SELECTOR_KEY,"demo.testArray");
        params.put("param","[1,2,3,4]");
        params.put("strs","[\"abc\",\"sssss\"]");
        params.put(ESBSTDKeys.AID_KEY,"" + APP_ID.mymm_pc.aid);
        addMD5Sign(null,params);

        String json = HTTP.get("http://localhost:8080/m.api", params);
        System.out.println("【"+json+"】");

    }

    @Test
    public void testEntity() throws Exception {
        Map<String,String> params = new HashMap<>();
        params.put(ESBSTDKeys.SELECTOR_KEY,"demo.testEntity");
        {
            Map<String,String> obj = new HashMap<>();
            obj.put("name","中文");
            obj.put("age","121");

            params.put("param", JSON.toJSONString(obj,ESBConsts.FASTJSON_SERIALIZER_FEATURES));
        }
        params.put(ESBSTDKeys.AID_KEY,"" + APP_ID.mymm_pc.aid);
        addMD5Sign(null,params);

        String json = HTTP.get("http://localhost:8080/m.api", params);
        System.out.println("【"+json+"】");

    }


    @Test
    public void testWebLoginAddDemo() throws Exception {
        //登录获取
        String token;
        String stoken;
        String rtoken;
        String csrf_key; //秘钥

        {
            Map<String,String> params = new HashMap<>();
            params.put(ESBSTDKeys.SELECTOR_KEY,"demo.webLogin");
            params.put("name","MJ");
            params.put("pswd","123456");
            params.put(ESBSTDKeys.AID_KEY,"" + APP_ID.mymm_pc.aid);
            addMD5Sign(null,params);

            String json = HTTP.get("http://localhost:8080/m.api", params);
            System.out.println("【"+json+"】");
            Resp<ESBToken> resp = JSON.parseObject(json, TokenResp.class);
            csrf_key = resp.content[0].key;
            token = resp.content[0].token;
            stoken = resp.content[0].stoken;
            rtoken = resp.content[0].refresh;
        }

        // 单个增加
        {
            Map<String, String> params = new HashMap<>();
            params.put(ESBSTDKeys.SELECTOR_KEY, "demo.addDemo");

            Map<String, String> obj = new HashMap<>();
            obj.put("username", "随便测试");
            obj.put("createAt", "" + System.currentTimeMillis());
            params.put("demo", JSON.toJSONString(obj, ESBConsts.FASTJSON_SERIALIZER_FEATURES));

            params.put(ESBSTDKeys.TOKEN_KEY, token);
            addMD5Sign(csrf_key, params);

            String json = HTTP.get("http://localhost:8080/m.api", params);
            System.out.println("【" + json + "】");
        }

        // 批量增加
        {
            Map<String, String> params = new HashMap<>();
            params.put(ESBSTDKeys.SELECTOR_KEY, "demo.batchAddDemo");

            List<Map<String, String>> lst = new ArrayList<>();
            {
                Map<String, String> obj = new HashMap<>();
                obj.put("username", "aaa");
                obj.put("createAt", "" + System.currentTimeMillis());
                lst.add(obj);
            }
            {
                Map<String, String> obj = new HashMap<>();
                obj.put("username", "bbb");
                obj.put("createAt", "" + System.currentTimeMillis());
                lst.add(obj);
            }
            params.put("models", JSON.toJSONString(lst, ESBConsts.FASTJSON_SERIALIZER_FEATURES));
            params.put("ignoreError", "false");

            params.put(ESBSTDKeys.TOKEN_KEY, token);
            addMD5Sign(csrf_key, params);

            String json = HTTP.get("http://localhost:8080/m.api", params);
            System.out.println("【" + json + "】");
        }

    }
}
