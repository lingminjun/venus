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

    public Long created;
    public Long modified;

    public Long upstamp; //时间戳
    public String thestamp; //时间戳,非数字形式
    public Long prestamp;  //前一个发布的时间戳
    public String md5;     //json的md5,查看是否更新

    public Integer rollback;//回滚删除

    //    public Integer deleted;     //是否删除

    public String getAPISelector() {
        if (module == null) {
            return "" + domain + "." + domain + "." + method;
        } else {
            return "" + domain + "." + module + "." + method;
        }
    }

    private ESBAPIInfo info;
    public ESBAPIInfo getInfo() {
        if (info != null) {
            return info;
        }
        info = JSON.parseObject(json, ESBAPIInfo.class);
        return info;
    }
}
