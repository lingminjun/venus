package com.venus.apigw.db;

import com.venus.esb.ESBAPISecurity;
import com.venus.esb.lang.ESBT;
import com.venus.esb.sign.ESBAPIAlgorithm;

/**
 * Created with IntelliJ IDEA.
 * Description:
 * User: lingminjun
 * Date: 2018-10-08
 * Time: 下午8:48
 */
public final class APIThirdSecurityPojo {
    public Long id;
    public String platform;
    public String info;

    public String prikey;
    public String pubkey;
    public String algo;

    public Long create;
    public Long modified;

    public Boolean delete;

    public ESBAPISecurity getAPISecurity() {
        ESBAPISecurity security = new ESBAPISecurity();
        security.id = ESBT.longInteger(this.id);
        security.algorithm = ESBAPIAlgorithm.valueOf(this.algo);
        security.prikey = this.prikey;
        security.pubkey = this.pubkey;
        return security;
    }
}
