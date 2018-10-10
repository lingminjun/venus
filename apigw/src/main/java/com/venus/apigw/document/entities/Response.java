package com.venus.apigw.document.entities;

import com.venus.esb.annotation.ESBDesc;

import java.util.List;

/**
 * Created by MJ Ling on 18-9-20
 */
@ESBDesc("接口返回值状态节点")
public class Response {
    @ESBDesc("当前服务端时间")
    public long systime;

    @ESBDesc("调用标识符")
    public String cid;

    @ESBDesc("返回码所在服务")
    public String domain;

    @ESBDesc("调用返回值")
    public int code;

    @ESBDesc("返回信息")
    public String msg;

    @ESBDesc("API调用状态，code的信息请参考ApiCode定义文件")
    public List<CallState> stateList;
}
