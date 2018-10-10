package com.venus.apigw.document.entities;

import com.venus.esb.annotation.ESBDesc;

import java.util.List;

/**
 * Created by MJ Ling on 18-9-20
 */
@ESBDesc("类型结构描述")
public class TypeStruct {
    @ESBDesc("结构名(用于显示)")
    public String          name;
    @ESBDesc("结构类型名")
    public String          type;
    @ESBDesc("分组名")
    public String          groupName;
    @ESBDesc("成员")
    public List<FieldInfo> fieldList;
}
