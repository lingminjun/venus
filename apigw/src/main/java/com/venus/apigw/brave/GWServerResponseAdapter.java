package com.venus.apigw.brave;

import com.github.kristofa.brave.KeyValueAnnotation;
import com.github.kristofa.brave.ServerResponseAdapter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by lmj on 17/9/1.
 * 客户端/服务端响应
 */
public class GWServerResponseAdapter implements ServerResponseAdapter {

    private final boolean success;
    private final String result;
    private final String errorMessage;

    public GWServerResponseAdapter(boolean success, String var7, String errorMessage) {
        this.success = success;
        this.result = var7;
        this.errorMessage = errorMessage;
    }

    private String getPrefix() {
        return "Server ";

    }

    @Override
    public Collection<KeyValueAnnotation> responseAnnotations() {
        List<KeyValueAnnotation> annotations = new ArrayList<KeyValueAnnotation>();
        if(!success){
            annotations.add(KeyValueAnnotation.create(getPrefix() + "exception", errorMessage));
        } else {
            KeyValueAnnotation keyValueAnnotation =  KeyValueAnnotation.create(getPrefix() + "status","success");
            annotations.add(keyValueAnnotation);
            //返回值记录
            if (result != null) {
                annotations.add(KeyValueAnnotation.create("result", result));
            } else {
                annotations.add(KeyValueAnnotation.create("result", "null"));
            }
        }
        return annotations;
    }

}
