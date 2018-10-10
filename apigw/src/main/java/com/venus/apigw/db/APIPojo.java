package com.venus.apigw.db;

import com.alibaba.fastjson.JSON;
import com.venus.esb.ESBAPIInfo;
import com.venus.esb.annotation.ESBAPI;
import com.venus.esb.lang.ESBConsts;

import java.io.Serializable;

/**
 * Created with IntelliJ IDEA.
 * Description:
 * User: lingminjun
 * Date: 2018-10-07
 * Time: 下午10:20
 */
public final class APIPojo implements Serializable {
    public Long id;

    public String domain;
    public String module;
    public String method;

    public String owner;
    public String version;
    public String detail;

    public Integer security;

    public String json;

    public Integer status;//状态:0普通接口;1开放平台接口;-1禁用;
    public Integer mock;

    public Long create;
    public Long modified;


    private ESBAPIInfo info;
    public ESBAPIInfo getInfo() {
        if (info != null) {
            return info;
        }
        info = JSON.parseObject(json, ESBAPIInfo.class);
        return info;
    }
}
