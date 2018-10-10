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
import java.util.HashMap;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * Description: 验证spring-boot-template几种类型请求调用，此处都没有传入aid,实际生产必须设置aid
 * User: lingminjun
 * Date: 2018-09-26
 * Time: 下午2:17
 */
public class ServletApiTest {


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
    public void test1() throws Exception {
        Map<String,String> params = new HashMap<>();
        params.put(ESBSTDKeys.SELECTOR_KEY,"sprmvc.test1");
        params.put("name","MJLing");
        params.put("cmm","试试中文是否ok");
        params.put(ESBSTDKeys.AID_KEY,"" + APP_ID.mymm_pc.aid);
        addMD5Sign(null,params);

        String json = HTTP.get("http://localhost:8080/m.api", params);
        System.out.println("【"+json+"】");

    }

    @Test
    public void test11() throws Exception {
        Map<String,String> params = new HashMap<>();
        params.put(ESBSTDKeys.SELECTOR_KEY,"sprmvc.test11");
        params.put("name","lingminjun");
        params.put("cmm","试试中文是否ok");
        params.put(ESBSTDKeys.AID_KEY,"" + APP_ID.mymm_pc.aid);
        addMD5Sign(null,params);

        String json = HTTP.get("http://localhost:8080/m.api", params);
        System.out.println("【"+json+"】");

    }

    @Test
    public void test2() throws Exception {// post
        Map<String,String> params = new HashMap<>();
        params.put(ESBSTDKeys.SELECTOR_KEY,"sprmvc.test2");
        {
            Map<String,String> obj = new HashMap<>();
            obj.put("name","lingminjun");
            obj.put("cmm","试试中文是否ok");

            params.put("item", JSON.toJSONString(obj,ESBConsts.FASTJSON_SERIALIZER_FEATURES));
        }

        params.put(ESBSTDKeys.AID_KEY,"" + APP_ID.mymm_pc.aid);
        addMD5Sign(null,params);

        String json = HTTP.get("http://localhost:8080/m.api", params);
        System.out.println("【"+json+"】");

    }

    @Test
    public void test6() throws Exception {
        Map<String,String> params = new HashMap<>();
        params.put(ESBSTDKeys.SELECTOR_KEY,"sprmvc.test6");
        params.put("name","lingminjun");
        params.put("age","11");
        {
            Map<String,String> obj = new HashMap<>();
            obj.put("name","lingminjun");
            obj.put("cmm","试试中文是否ok");

            params.put("item", JSON.toJSONString(obj,ESBConsts.FASTJSON_SERIALIZER_FEATURES));
        }
        params.put(ESBSTDKeys.AID_KEY,"" + APP_ID.mymm_pc.aid);
        addMD5Sign(null,params);

        String json = HTTP.get("http://localhost:8080/m.api", params);
        System.out.println("【"+json+"】");

    }

    @Test
    public void testCreateCity() throws Exception {
        Map<String,String> params = new HashMap<>();
        params.put(ESBSTDKeys.SELECTOR_KEY,"sprmvc.createCity");
        {
            Map<String,String> obj = new HashMap<>();
            obj.put("id", "" + ((System.currentTimeMillis() - ESBConsts.TIME_2018_01_01)/1000));
            obj.put("provinceId", "123");
            obj.put("cityName", "上海" + ((System.currentTimeMillis() - ESBConsts.TIME_2018_01_01)/1000));
            obj.put("description", "随便写点信息");

            params.put("city", JSON.toJSONString(obj,ESBConsts.FASTJSON_SERIALIZER_FEATURES));
        }
        params.put(ESBSTDKeys.AID_KEY,"" + APP_ID.mymm_pc.aid);
        addMD5Sign(null,params);

        String json = HTTP.get("http://localhost:8080/m.api", params);
        System.out.println("【"+json+"】");

    }

}
