package com.venus.apigw.bi;

import java.io.Serializable;

/**
 * Created with IntelliJ IDEA.
 * Description:
 *     记录access相关信息
 *     x:(_via=192.168.0.104, _mt=demo.webLogin, _ua=Java/1.8.0_152, _did=-59933928954159001, _host=localhost:8080, _ft=JSON, _aid=1, _cid=c0a80068296300290000000df4572de4, _at=1543133928931, _sm=md5, _tid=c0a80068296300290000000df4572de4, _cvc=0, _sig=259dc7e78d010e795f3868c5f3ffdadb) msg:demo.demo.webLogin dubbo://com.sfebiz.demo.api.demoservice:20880/webLogin?var0=java.lang.String&var1=java.lang.String 0  SUCCESS 1543133928931:3569
 * User: lingminjun
 * Date: 2019-04-05
 * Time: 11:33 PM
 */
public final class PVPojo implements Serializable {
    public Long id;

    public Integer aid;
    public Integer app;
    public Integer tml; //terminal: pc端1, h5端2, iOS客户端3, android客户端4, 微信小程序5, 支付宝小程序6, 等等后续定义

    public Long did;
    public Long uid;
    public Long acct;

    public String tid; // trace id

    public String mthd; // domain.module.method

    public String md5; // referer md5
    public String ref; // referer
    public String qry; // 参数部分
    public String vid; // 特殊参数记录，vid
    public String src; // 特殊参数记录，vid
    public String spm; // 特殊参数记录，vid

    public String cip; // client ip
    public Integer cvc; // 客户端版本num 1000
    public String cvn;  // 客户端版本号1.0.0

    public Integer scs; // success

    public Long cost;   // 调用耗时
//    public Long htm; // call at hour time
    public Long at;  // call at

}
