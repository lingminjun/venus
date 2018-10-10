package com.venus.apigw.document.entities;

import com.venus.esb.ESBAPIInfo;
import com.venus.esb.ESBSecurityLevel;
import com.venus.esb.idl.ESBAPICode;
import com.venus.esb.idl.ESBAPIDef;
import com.venus.esb.idl.ESBAPIParam;
import com.venus.esb.idl.ESBAPIStruct;
import com.venus.esb.lang.ESBT;

import java.util.*;

/**
 * 接口信息
 * 复用老的文档显示功能
 */
public final class ApiMethodInfo {
    /**
     * 未知资源
     */
    public static final ApiMethodInfo UnknownMethod = new ApiMethodInfo();

    static {
        UnknownMethod.methodName = "Unknown";
        UnknownMethod.apiInfo = new ESBAPIInfo();
        UnknownMethod.apiInfo.api = new ESBAPIDef();
        UnknownMethod.apiInfo.api.desc = "未知资源";
        UnknownMethod.apiInfo.api.detail = "未知资源";
//        UnknownMethod.errorCodes = new AbstractReturnCode[]{};
//        UnknownMethod.displayErrorCodes = new HashMap<String, AbstractReturnCode>();
//        UnknownMethod.parameterInfos = null;
//        UnknownMethod.proxyMethodInfo = null;
//        UnknownMethod.securityLevel = SecurityType.None;
    }

    /**
     * 实际接口描述
     */
    public ESBAPIInfo apiInfo = null;

    /**
     * 被代理的方法所属的接口,dubbo interface
     */
    public Class<?> dubboInterface;

    /**
     * 提供被代理方法的实例
     */
    public Object serviceInstance;

    private String methodName = null;
    public final String getMethodName() {
        if (apiInfo == null || apiInfo.api == null) {
            return "";
        }
        if (methodName != null && methodName.length() > 0) {
            return methodName;
        }
        methodName = apiInfo.api.getAPISelector();
        return methodName;
    }

    private String methodDisplayName = null;
    public final String getMethodDisplayName() {
        if (apiInfo == null || apiInfo.api == null) {
            return "";
        }
        if (methodDisplayName != null && methodDisplayName.length() > 0) {
            return methodDisplayName;
        }
        methodDisplayName = apiInfo.api.getDisplayAPISelector();
        return methodDisplayName;
    }

    public final String getDescription() {
        if (apiInfo == null || apiInfo.api == null) {
            return "";
        }

        return ESBT.string(apiInfo.api.desc);
    }

    public final String getDetail() {
        if (apiInfo == null || apiInfo.api == null) {
            return "";
        }

        return ESBT.string(apiInfo.api.detail);
    }

    public final String getGroupName() {
        if (apiInfo == null || apiInfo.api == null) {
            return "";
        }

        return ESBT.string(apiInfo.api.domain);
    }

    public final String getSecurityLevel() {
        if (apiInfo == null || apiInfo.api == null) {
            return "";
        }

        return ESBSecurityLevel.valueOf(apiInfo.api.security).name();
    }

    public final String getOwner() {
        if (apiInfo == null || apiInfo.api == null) {
            return "";
        }

        return ESBT.string(apiInfo.api.owner);
    }

    public final String getReturnType() {
        if (apiInfo == null || apiInfo.api == null) {
            return "";
        }

        return apiInfo.api.returned.getDisplayType();
    }

    public final ESBAPIStruct getReturnTypeStruct() {
        if (apiInfo == null || apiInfo.api == null) {
            return null;
        }

        return apiInfo.api.structs.get(apiInfo.api.returned.type);
    }

    public final Map<String,ESBAPIStruct> getAllStruct() {
        if (apiInfo == null || apiInfo.api == null) {
            return new HashMap<>();
        }

        if (apiInfo.api.structs == null) {return new HashMap<>();}
        return apiInfo.api.structs;
    }

    public final ESBAPIParam[] getParams() {
        if (apiInfo == null || apiInfo.api == null || apiInfo.api.params == null) {
            return new ESBAPIParam[0];
        }

        return apiInfo.api.params;
    }

    /**
     * 获取对外展示的所有错误码归类
     */
    public ESBAPICode[] getAllDisplayErrorCodes() {
        if (apiInfo == null || apiInfo.api == null || apiInfo.api.codes == null) {
            return new ESBAPICode[0];
        }

        List<ESBAPICode> list = new ArrayList<ESBAPICode>(apiInfo.api.codes.values());
        Collections.sort(list, new Comparator<ESBAPICode>() {
            @Override
            public int compare(ESBAPICode o1, ESBAPICode o2) {
                //负数全部放在上面
                int compare =  o1.code > o2.code ? 1 : o1.code < o2.code ? -1 : 0;
                if (compare == 0) {
                    String domain1 = o1.domain;
                    if (domain1 == null || domain1.length() == 0) {
                        domain1 = "0";
                    }
                    String domain2 = o2.domain;
                    if (domain2 == null || domain2.length() == 0) {
                        domain2 = "0";
                    }
                    return domain1.compareTo(domain2);
                }
                return compare;
            }
        });

        return list.toArray(new ESBAPICode[0]);
    }
}