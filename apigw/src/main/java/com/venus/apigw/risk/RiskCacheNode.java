package com.venus.apigw.risk;

/**
 * Created by lingminjun on 17/6/15.
 * 容易变化的缓存数据节点
 */
public class RiskCacheNode<T extends Object> {
    private long at;
    private T obj;

    public long getAt() {
        return at;
    }

    public void setAt(long at) {
        this.at = at;
    }

    public T getObj() {
        return obj;
    }

    public void setObj(T obj) {
        this.obj = obj;
    }
}
