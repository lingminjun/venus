package com.venus.apigw.document.entities;

import com.venus.esb.annotation.ESBDesc;

/**
 * Created by MJ Ling on 18-9-20
 */
@ESBDesc("接口实体字段信息")
public class FieldInfo {
    @ESBDesc("字段类型")
    public String  type;
    @ESBDesc("字段简单类型(用于显示)")
    public String  display;
    @ESBDesc("字段名")
    public String  name;
    @ESBDesc("是否为集合类型")
    public boolean isList;
    @ESBDesc("注释")
    public String  desc;
}
