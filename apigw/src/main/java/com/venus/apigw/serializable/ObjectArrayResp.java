package com.venus.apigw.serializable;

import com.venus.esb.annotation.ESBDesc;

import java.io.Serializable;
import java.util.Collection;

//POJOSerializer不能提供ObjectArrayResp的序列化支持,不支持动态类型的序列化
@ESBDesc("对象数组返回值")
public class ObjectArrayResp implements Serializable {
    private static final long serialVersionUID = 1L;

    @ESBDesc("对象数组返回值")
    public Object[] value;

    public static ObjectArrayResp convert(Object[] array) {
        ObjectArrayResp arrayResp = new ObjectArrayResp();
        arrayResp.value = array;
        return arrayResp;
    }

    public static ObjectArrayResp convert(Collection collection) {
        ObjectArrayResp arrayResp = new ObjectArrayResp();
        if (collection != null && collection.size() > 0) {
            arrayResp.value = collection.toArray();
        }
        return arrayResp;
    }
}
