package com.venus.apigw.document.entities;

import com.venus.esb.annotation.ESBDesc;

import java.util.List;

/**
 * Created by MJ Ling on 18-9-20
 */
@ESBDesc("接口文档")
public class Document {
    @ESBDesc("应用接口信息")
    public List<MethodInfo>          apiList;
    @ESBDesc("通用异常信息")
    public List<CodeInfo>            codeList;
    @ESBDesc("通用返回值结构描述")
    public List<TypeStruct>          respStructList;
    @ESBDesc("系统级参数列表描述")
    public List<SystemParameterInfo> systemParameterInfoList;
}
