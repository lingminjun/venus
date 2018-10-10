package com.venus.apigw.document.entities;

import com.venus.esb.annotation.ESBDesc;

/**
 * Created by MJ Ling on 18-9-20
 */
@ESBDesc("参数信息")
public class ParameterInfo {
    @ESBDesc("参数类型")
    public String       type;
    @ESBDesc("参数简单类型(用于显示)")
    public String       display;
    @ESBDesc("默认值(非必选参数)")
    public String       defaultValue;
    @ESBDesc("验证规则(正则表达式)")
    public String       verifyRegex;
    @ESBDesc("验证失败提示")
    public String       verifyMsg;
    @ESBDesc("是否必选参数")
    public boolean      isRequired;
    @ESBDesc("参数名")
    public String       name;
    @ESBDesc("参数描述")
    public String       description;
    @ESBDesc("是否是集合或数组类型")
    public boolean      isList;
    @ESBDesc("是否需要rsa加密")
    public boolean      isRsaEncrypt;
    @ESBDesc("参数在接口中的次序, 当前可能的取值有 int0, int1...int9 str0, str1...str9")
    public String       sequence;
}
