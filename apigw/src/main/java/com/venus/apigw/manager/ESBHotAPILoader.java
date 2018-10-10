package com.venus.apigw.manager;

import com.venus.apigw.db.APIPojo;
import com.venus.apigw.db.DB;
import com.venus.esb.ESB;
import com.venus.esb.ESBAPIInfo;
import com.venus.esb.ESBAPILoader;

import java.util.List;


/**
 * Created with IntelliJ IDEA.
 * Description: 用于热更新API
 * User: lingminjun
 * Date: 2018-09-18
 * Time: 下午8:56
 */
public class ESBHotAPILoader extends ESBAPILoader {

    @Override
    public ESBAPIInfo load(ESB esb, String selector) {
        System.out.println("尝试加载新的api=[" + selector + "]");

        //内存加载
        ESBAPIInfo info = super.load(esb, selector);
        if (info != null) {
            return info;
        }

        //从APIManager中加载，不记录缓存，此种api加载方式无法更新
        info = APIManager_New.shared().getAPI(selector);
        if (info != null) {
            return info;
        }

        //加载库
        info = getRemoteConfigAPI(esb, selector);
        if (info != null) {//本地缓存下
            saveMemoryCacheAPI(selector,info);
            saveFileCacheAPI(selector,info);
        }

        //最后加载文件（保底，如果提前依赖文件，则无法重启后更新最新的文档） 文件加载
        info = getFileCacheAPI(selector);
        if (info != null) {
            saveMemoryCacheAPI(selector,info);
            return info;
        }

        return info;
    }

    @Override
    public void refresh(ESB esb, String selector) {
        System.out.println("刷新api=[" + selector + "]");
//        removeFileCacheAPI(selector);//保留做备份
        removeMemoryCacheAPI(selector);
    }

    private ESBAPIInfo getRemoteConfigAPI(ESB esb, String selector) {
//        ESBBeanFactory factory = esb.beanFactory();
        String[] strs = selector.split("\\.");
        if (strs.length != 3) {
            return null;
        }
        List<APIPojo> results = DB.query("apigw_api","`domain` = ? and `module` = ? and `method` = ?", new Object[]{strs[0],strs[1],strs[2]}, APIPojo.class);
        if (results != null && results.size() > 0) {
            APIPojo pojo = results.get(0);
            return pojo.getInfo();
        }

        return null;
    }

}
