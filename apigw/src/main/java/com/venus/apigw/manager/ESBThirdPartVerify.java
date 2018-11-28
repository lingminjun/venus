package com.venus.apigw.manager;

import com.alibaba.dubbo.common.utils.LRUCache;
import com.venus.apigw.db.APIThirdSecurityPojo;
import com.venus.apigw.db.DBUtil;
import com.venus.esb.ESBAPISecurity;
import com.venus.esb.ESBAPIVerify;
import com.venus.esb.lang.ESBT;

import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * Description: 加载第三方接口秘钥对
 * User: lingminjun
 * Date: 2018-09-21
 * Time: 下午4:24
 */
public class ESBThirdPartVerify extends ESBAPIVerify {

    private Map<String,ESBAPISecurity> cache = new LRUCache<String, ESBAPISecurity>(1000);

    @Override
    protected ESBAPISecurity loadThirdPartySecurity(String tid) {
        ESBAPISecurity security = cache.get(tid);
        if (security != null) {
            return security;
        }


        APIThirdSecurityPojo pojo = DBUtil.getThirdPartySecurityInfo(ESBT.longInteger(tid));
        if (pojo != null) {
            security = pojo.getAPISecurity();
        }

        if (security != null) {
            cache.put(tid,security);
        }

        //最好是做缓存
        return security;//super.loadThirdPartySecurity(tid);
    }
}
