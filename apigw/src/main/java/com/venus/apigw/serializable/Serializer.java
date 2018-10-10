package com.venus.apigw.serializable;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.venus.apigw.consts.ConstField;
import com.venus.esb.lang.ESBConsts;
import com.venus.esb.lang.ESBException;
import com.venus.esb.lang.ESBExceptionCodes;
import com.venus.esb.lang.ESBRawString;

import java.io.IOException;
import java.io.OutputStream;

public interface Serializer<T> {
    /**
     * ApiSerializerFeatures
     */
    public static class ApiSerializerFeature {
        /**
         * 使用fastjson作为json序列化实现，序列化的特性定义
         */
        public static final SerializerFeature[] SERIALIZER_FEATURES = ESBConsts.FASTJSON_SERIALIZER_FEATURES;
    }

    /**
     * jsonString的序列化
     */
    public static final Serializer<JSONString> jsonStringSerializer = new Serializer<JSONString>() {

        @Override
        public void toXml(JSONString instance, OutputStream out, boolean isRoot) throws ESBException {
            try {
                if (instance.value != null) {
                    out.write(instance.value.getBytes(ConstField.UTF8));
                }
            } catch (Exception e) {
                throw ESBExceptionCodes.UNKNOWN_ERROR("").setCoreCause(e);
            }
        }

        @Override
        public void toJson(JSONString instance, OutputStream out, boolean isRoot) throws ESBException {
            try {
                if (instance.value != null) {
                    out.write(instance.value.getBytes(ConstField.UTF8));
                }
            } catch (Exception e) {
                throw ESBExceptionCodes.UNKNOWN_ERROR("").setCoreCause(e);
            }
        }
    };

    public static final Serializer<ESBRawString> rawStringSerializer = new Serializer<ESBRawString>() {

        @Override
        public void toXml(ESBRawString instance, OutputStream out, boolean isRoot) throws ESBException {
            try {
                if (instance.value != null) {
                    out.write(instance.value.getBytes(ConstField.UTF8));
                }
            } catch (Exception e) {
                throw ESBExceptionCodes.UNKNOWN_ERROR("").setCoreCause(e);
            }
        }

        @Override
        public void toJson(ESBRawString instance, OutputStream out, boolean isRoot) throws ESBException {
            try {
                if (instance.value != null) {
                    out.write(instance.value.getBytes(ConstField.UTF8));
                }
            } catch (Exception e) {
                throw ESBExceptionCodes.UNKNOWN_ERROR("").setCoreCause(e);
            }
        }
    };


    /**
     * note:PojoSerializer不支持动态类型，要让SerializerProvider支持要写很多恶心的代码，还是直接写java代码了
     */
    public static final Serializer<ObjectArrayResp> objectArrayRespSerializer = new Serializer<ObjectArrayResp>() {
        byte[][] bs = new byte[8][];

        {
            bs[0] = "<ObjectArrayResp>".getBytes(ConstField.UTF8);
            bs[1] = "<value>".getBytes(ConstField.UTF8);
            bs[2] = "<item>".getBytes(ConstField.UTF8);
            bs[3] = "</item>".getBytes(ConstField.UTF8);
            bs[4] = "</value>".getBytes(ConstField.UTF8);
            bs[5] = "</ObjectArrayResp>".getBytes(ConstField.UTF8);
            bs[6] = "<![CDATA[".getBytes(ConstField.UTF8);
            bs[7] = "]]>".getBytes(ConstField.UTF8);
        }

        @Override
        public void toXml(ObjectArrayResp instance, OutputStream out, boolean isRoot) throws ESBException {
            if (instance == null) {
                return;
            }
            try {
                if (isRoot) {
                    out.write(bs[0]);
                }
                if (instance.value != null) {
                    out.write(bs[1]);
                    for (Object obj : instance.value) {
                        out.write(bs[2]);
                        if (obj != null) {
                            if (obj.getClass() == String.class) {
                                out.write(bs[6]);
                                out.write(obj.toString().getBytes(ConstField.UTF8));
                                out.write(bs[7]);
                            } else if (obj.getClass().isEnum()) {
                                out.write(bs[6]);
                                out.write(((Enum) obj).name().getBytes(ConstField.UTF8));
                                out.write(bs[7]);
                            } else {
                                Serializer localSerializer = POJOSerializerProvider.getSerializer(obj.getClass());
                                localSerializer.toXml(obj, out, false);
                            }
                        }
                        out.write(bs[3]);
                    }
                    out.write(bs[4]);
                }
                if (isRoot) {
                    out.write(bs[5]);
                }
            } catch (IOException localIOException) {
                throw ESBExceptionCodes.UNKNOWN_ERROR("").setCoreCause(localIOException);
            }
        }

        @Override
        public void toJson(ObjectArrayResp instance, OutputStream out, boolean isRoot) throws ESBException {
            try {
                out.write(JSON.toJSONBytes(instance, ApiSerializerFeature.SERIALIZER_FEATURES));
            } catch (IOException localIOException) {
                throw ESBExceptionCodes.UNKNOWN_ERROR("").setCoreCause(localIOException);
            }
        }
    };

    void toXml(T instance, OutputStream out, boolean isRoot) throws ESBException;

    void toJson(T instance, OutputStream out, boolean isRoot) throws ESBException;

}
