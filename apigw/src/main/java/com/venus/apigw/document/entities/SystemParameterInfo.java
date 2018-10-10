package com.venus.apigw.document.entities;

import com.venus.esb.annotation.ESBDesc;

/**
 * Created by MJ Ling on 18-9-20
 */
@ESBDesc("系统级参数")
public class SystemParameterInfo {
    @ESBDesc("参数名称")
    public String name;
    @ESBDesc("描述")
    public String desc;
}
