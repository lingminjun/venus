package com.venus.apigw.document.entities;

import com.venus.esb.annotation.ESBDesc;

/**
 * Created by MJ Ling on 18-9-20
 */
@ESBDesc("调用状态")
public class CallState {
    @ESBDesc("返回值")
    public int code;

    @ESBDesc("数据长度")
    public int length;

    @ESBDesc("返回信息")
    public String msg;

    @ESBDesc("返回码所在服务")
    public String domain;
}
