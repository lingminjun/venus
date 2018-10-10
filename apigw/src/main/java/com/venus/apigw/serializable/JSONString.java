package com.venus.apigw.serializable;

import com.venus.esb.annotation.ESBDesc;

import java.io.Serializable;

@ESBDesc("返回json格式的string")
public class JSONString implements Serializable {
    @ESBDesc("json string")
    public String value;
}