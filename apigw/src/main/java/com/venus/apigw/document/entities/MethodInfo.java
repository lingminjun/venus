package com.venus.apigw.document.entities;

import com.venus.esb.annotation.ESBDesc;

import java.util.List;

/**
 * Created by MJ Ling on 18-9-20
 */
@ESBDesc("接口信息")
public class MethodInfo {
    @ESBDesc("返回值类型")
    public String              returnType;
    @ESBDesc("返回值简单类型(用于显示)")
    public String              returnDisplay;
    @ESBDesc("接口名")
    public String              methodName;
    @ESBDesc("接口名(用于显示)")
    public String              methodDisplayName;
    @ESBDesc("接口简介")
    public String              description;
    @ESBDesc("接口详细信息")
    public String              detail;
    @ESBDesc("调用接口所需安全级别")
    public String              securityLevel;
    @ESBDesc("接口分组名")
    public String              groupName;
    @ESBDesc("接口可用状态")
    public String              state;
    @ESBDesc("接口返回值类型结构描述")
    public List<TypeStruct>    respStructList;
    @ESBDesc("接口参数列表信息")
    public List<ParameterInfo> parameterInfoList;
    @ESBDesc("接口返回值类型结构描述")
    public List<TypeStruct>    reqStructList;
    @ESBDesc("接口返回业务异常列表")
    public List<CodeInfo>      errorCodeList;
    @ESBDesc("接口组负责人")
    public String              groupOwner;
    @ESBDesc("接口负责人")
    public String              methodOwner;
    @Deprecated
    @ESBDesc("允许访问的第三方编号列表")
    public int[]               allowThirdPartyIds;
    @ESBDesc("是否只允许通过加密通道访问")
    public boolean             encryptionOnly;
    @ESBDesc("Integared级别接口是否需要网关对请求进行签名验证")
    public boolean             needVerify;
}
