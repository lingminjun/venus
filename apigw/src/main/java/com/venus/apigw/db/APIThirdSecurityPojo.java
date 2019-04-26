package com.venus.apigw.db;

import com.venus.esb.ESBAPISecurity;
import com.venus.esb.lang.ESBT;
import com.venus.esb.sign.ESBAPIAlgorithm;
import com.venus.esb.sign.utils.*;

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

    public static ESBAPIAlgorithm getAlgorithm(String algorithm) {
        if (ESBAPIAlgorithm.CRC16.hit(algorithm)) {
            return ESBAPIAlgorithm.CRC16;
        } else if (ESBAPIAlgorithm.CRC32.hit(algorithm)) {
            return ESBAPIAlgorithm.CRC32;
        } else if (ESBAPIAlgorithm.MD5.hit(algorithm)) {
            return ESBAPIAlgorithm.MD5;
        } else if (ESBAPIAlgorithm.SHA1.hit(algorithm)) {
            return ESBAPIAlgorithm.SHA1;
        } else if (ESBAPIAlgorithm.HMAC.hit(algorithm)) {
            return ESBAPIAlgorithm.HMAC;
        } else if (ESBAPIAlgorithm.RSA.hit(algorithm)) {
            return ESBAPIAlgorithm.RSA;
        } else if (ESBAPIAlgorithm.ECC.hit(algorithm)) {
            return ESBAPIAlgorithm.ECC;
        }
        //默认sha1
        return ESBAPIAlgorithm.SHA1;
    }

    public ESBAPISecurity getAPISecurity() {
        ESBAPISecurity security = new ESBAPISecurity();
        security.id = ESBT.longInteger(this.id);
        security.algorithm = getAlgorithm(this.algo);
        security.prikey = this.prikey;
        security.pubkey = this.pubkey;
        return security;
    }
}
