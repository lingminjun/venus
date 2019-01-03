package com.venus.apigw.db;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * Description: 更新节点
 * User: lingminjun
 * Date: 2018-10-07
 * Time: 下午10:20
 */
public final class StampPojo implements Serializable {
    public String thestamp;
    public List<String> apis;
}
