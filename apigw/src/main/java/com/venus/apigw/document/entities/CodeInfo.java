package com.venus.apigw.document.entities;

import com.venus.esb.annotation.ESBDesc;

/**
 * Created by MJ Ling on 18-9-20
 */
@ESBDesc("编码信息")
public class CodeInfo {
    @ESBDesc("编码值")
    public int          code;
    @ESBDesc("编码名称")
    public String       name;
    @ESBDesc("编码描述")
    public String       desc;
    @ESBDesc("编码所属服务")
    public String       service;
    @ESBDesc("是否显示给客户端")
    public boolean      isDesign;
}
