package com.venus.apigw.document;

import com.venus.apigw.document.entities.*;
import com.venus.esb.annotation.ESBDesc;
import com.venus.esb.helper.ESBAPIHelper;
import com.venus.esb.idl.ESBAPICode;
import com.venus.esb.idl.ESBAPIParam;
import com.venus.esb.idl.ESBAPIStruct;
import com.venus.esb.lang.*;
import com.venus.apigw.document.entities.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;

public class ApiDocumentationHelper {
    private static final Logger logger = LoggerFactory.getLogger(ApiDocumentationHelper.class);
//
//    private static final ConcurrentHashMap<String, TypeStruct> reqStructs = new ConcurrentHashMap<String, TypeStruct>();
//    private static final ConcurrentHashMap<String, TypeStruct> respStructs = new ConcurrentHashMap<String, TypeStruct>();
//    private static final ConcurrentHashMap<String, TypeStruct> vitualListStructs = new ConcurrentHashMap<String, TypeStruct>();
//    private static final List<TypeStruct> emptyTypeStructList = new ArrayList<TypeStruct>(0);

    public Document getDocument(ApiMethodInfo[] apis) {
        try {
            Document document = new Document();
            document.apiList = new LinkedList<MethodInfo>();

            //加载通用异常
            document.codeList = getSysErrorCode();


            //通用返回值
            document.respStructList = getCommonRespTypeStruct();

            //系统参数
            document.systemParameterInfoList = getSysParamInfo();

//            HashSet<String> designCode = new HashSet<String>();

            Arrays.sort(apis, new Comparator<ApiMethodInfo>() {
                @Override
                public int compare(ApiMethodInfo o1, ApiMethodInfo o2) {
                    return o1.getMethodName().compareTo(o2.getMethodName());
                }
            });
            for (ApiMethodInfo info : apis) {

                MethodInfo methodInfo = new MethodInfo();
                methodInfo.description = info.getDescription();
                methodInfo.detail = info.getDetail();
                methodInfo.groupName = info.getGroupName();
                methodInfo.methodName = info.getMethodName();
                methodInfo.methodDisplayName = info.getMethodDisplayName();
                methodInfo.securityLevel = info.getSecurityLevel();
                methodInfo.groupOwner = info.getOwner();
                methodInfo.methodOwner = info.getOwner();
                methodInfo.encryptionOnly = false;
                methodInfo.needVerify = true;

                String groupName = info.getGroupName();

                //返回值处理
                methodInfo.returnType = info.getReturnType();
                methodInfo.returnDisplay = convertEntityName(info.getReturnType());
                ESBAPIStruct resStruct = info.getReturnTypeStruct();
                methodInfo.respStructList = new ArrayList<>();
                parseStruct(groupName,resStruct,info.getAllStruct(),methodInfo.respStructList, new HashSet<>());


                methodInfo.state = ApiOpenState.OPEN.name();

                //处理错误码
                ESBAPICode[] errorCodes = info.getAllDisplayErrorCodes();
                if (errorCodes != null) {
                    methodInfo.errorCodeList = new ArrayList<CodeInfo>(errorCodes.length);
                    for (ESBAPICode rc : errorCodes) {
                        CodeInfo c = new CodeInfo();
                        c.code = rc.code;
                        c.desc = rc.desc;
                        c.name = rc.name;
                        c.service = rc.domain;

                        //不包含则添加 通用异常
//                        if (!designCode.contains(rc.domain + "_" + rc.code)) {
//                            document.codeList.add(c);
//                        }

                        methodInfo.errorCodeList.add(c);
                    }
                }

                //参数处理
                ESBAPIParam[] params = info.getParams();
                if (params != null && params.length > 0) {
                    methodInfo.parameterInfoList = new ArrayList<>();
                    methodInfo.reqStructList = new ArrayList<>();
                    parseParams(groupName,params,info.getAllStruct(),methodInfo.parameterInfoList,methodInfo.reqStructList);
                }
                document.apiList.add(methodInfo);
            }

            return document;
        } catch (Exception e) {
            logger.error("parse xml for api info failed.", e);
            System.out.println(e.getMessage());
            e.printStackTrace(System.out);
        }
        return null;
    }

//    public static void parseStruct(String groupName, ESBAPIStruct struct, Map<String,ESBAPIStruct> structs, List<TypeStruct> list) {
//        parseStruct(groupName,struct,structs,list,new HashSet<>());
//    }

    public static void parseStruct(String groupName, ESBAPIStruct struct, Map<String,ESBAPIStruct> structs, List<TypeStruct> list, Set<String> sets) {
        if (struct == null) {
            return;
        }

        if (sets.contains(struct.type)) {
            return;
        }
        sets.add(struct.type);

        TypeStruct s = new TypeStruct();
        s.name = convertEntityName(struct.type);
        s.type = struct.type;
        s.groupName = groupName;
        list.add(s);

        if (struct.fields == null || struct.fields.length == 0) {
            return;
        }

        List<FieldInfo> fls = new ArrayList<>();
        for (ESBAPIParam param : struct.fields) { //属性列表
            FieldInfo f = new FieldInfo();
            f.type = param.getDisplayType();
            f.display = convertEntityName(param.getDisplayType());
            //因为display名字已经给出数组形式
            f.isList = false;//param.isArray;
            f.name = param.name;
            f.desc = param.desc;
            fls.add(f);
        }
        s.fieldList = fls;

        // param为符合类型时
        for (ESBAPIParam param : struct.fields) { //属性列表
            String coreType = ESBT.convertCoreType(param.type);
            parseStruct(groupName,structs.get(coreType),structs,list,sets);
        }
    }

    /**
     * 解析参数，从ESB描述转换
     * @param groupName
     * @param paramters
     * @param structs
     * @param params
     * @param list
     */
    public void parseParams(String groupName, ESBAPIParam[] paramters, Map<String,ESBAPIStruct> structs, List<ParameterInfo> params ,List<TypeStruct> list) {
        parseParams(groupName,paramters,structs,params,list,new HashSet<>());
    }
    private void parseParams(String groupName, ESBAPIParam[] paramters, Map<String,ESBAPIStruct> structs, List<ParameterInfo> params, List<TypeStruct> list, Set<String> sets) {

        for (ESBAPIParam p : paramters) {
            //标准参数，底层必须传输或者网关自动解除包
            if (!ESBSTDKeys.isSTDKey(p.name)) {
                ParameterInfo b = new ParameterInfo();
                if (p.defaultValue != null) {
                    b.defaultValue = p.defaultValue;
                }

                b.isRsaEncrypt = false;//p.isRsaEncrypted;
                b.isRequired = p.required;
                b.name = p.name;
                b.description = p.desc;
                // 由于下面已经转为数组了  Collection.class.isAssignableFrom(p.type) || p.type.isArray();
                b.isList = false;
                b.type = p.getDisplayType();
                b.display = convertEntityName(p.getDisplayType());
                params.add(b);
            }
        }

        int intIndex = 0;
        int strIndex = 0;
        // 为尚未初始化的sequence赋值
        HashSet<String> sequenceSet = new HashSet<>();
        for (ParameterInfo info : params) {
            if (info.sequence == null || info.sequence.length() == 0) {
                if (!info.isList && (int.class.getSimpleName().equals(info.type)
                        || long.class.getSimpleName().equals(info.type))) {
                    do {
                        info.sequence = "int" + intIndex++;
                    } while (sequenceSet.contains(info.sequence));
                } else {
                    do {
                        info.sequence = "str" + strIndex++;
                    } while (sequenceSet.contains(info.sequence));
                }
            }
        }

        //遍历下一级结构化数据
        for (ESBAPIParam p : paramters) {
            //标准参数，底层必须传输或者网关自动解除包
            if (!ESBSTDKeys.isSTDKey(p.name)) {
                // param为符合类型时
                String coreType = ESBT.convertCoreType(p.type);
                parseStruct(groupName,structs.get(coreType),structs,list,sets);
            }
        }
    }

    private List<TypeStruct> getCommonRespTypeStruct() {
        Map<String,ESBAPIStruct> strust = new HashMap<>();

        ESBAPIHelper.parseObjectType(Response.class, strust);
        ESBAPIHelper.parseObjectType(ESBBoolean.class,strust);
        ESBAPIHelper.parseObjectType(ESBBooleanArray.class,strust);
        ESBAPIHelper.parseObjectType(ESBDouble.class,strust);
        ESBAPIHelper.parseObjectType(ESBDoubleArray.class,strust);
        ESBAPIHelper.parseObjectType(ESBFloat.class,strust);
        ESBAPIHelper.parseObjectType(ESBFloatArray.class,strust);
        ESBAPIHelper.parseObjectType(ESBLong.class,strust);
        ESBAPIHelper.parseObjectType(ESBLongArray.class,strust);
        ESBAPIHelper.parseObjectType(ESBNumber.class,strust);
        ESBAPIHelper.parseObjectType(ESBNumberArray.class,strust);
        ESBAPIHelper.parseObjectType(ESBString.class,strust);
        ESBAPIHelper.parseObjectType(ESBStringArray.class,strust);
        ESBAPIHelper.parseObjectType(ESBRawString.class,strust);

        ESBAPIHelper.parseObjectType(ESBContext.class,strust);
        ESBAPIHelper.parseObjectType(ESBDeviceToken.class,strust);
        ESBAPIHelper.parseObjectType(ESBToken.class,strust);
        ESBAPIHelper.parseObjectType(ESBSSOToken.class,strust);


        List<TypeStruct> list = new ArrayList<>();
        HashSet<String> sets = new HashSet<>();
        for (ESBAPIStruct struct : strust.values()) {
            parseStruct("ESB", struct, strust, list, sets);
        }
        return list;
    }

    /**
     * 获取系统级参数
     */
    private List<CodeInfo> getSysErrorCode() {
        Map<Integer,ESBAPICode> codes = ESBAPIHelper.loadCodes(ESBExceptionCodes.class,null);
        if (codes == null || codes.size() == 0) {
            return new ArrayList<>();
        }

        List<CodeInfo> list = new LinkedList<CodeInfo>();
        for (ESBAPICode rc : codes.values()) {
            CodeInfo c = new CodeInfo();
            c.code = rc.code;
            c.desc = rc.desc;
            c.name = rc.name;
            c.service = rc.domain;

            list.add(c);
        }

        return list;
    }


    /**
     * 获取系统级参数
     */
    private List<SystemParameterInfo> getSysParamInfo() {
        Field[] fields = ESBSTDKeys.class.getDeclaredFields();
        List<SystemParameterInfo> systemParameterInfos = new LinkedList<SystemParameterInfo>();
        for (Field field : fields) {
            if (isConstField(field)) {
                ESBDesc desc = field.getAnnotation(ESBDesc.class);
                if (desc != null && !desc.ignore()) {
                    SystemParameterInfo systemParameterInfo = new SystemParameterInfo();
                    try {
                        systemParameterInfo.name = (String) field.get(ESBSTDKeys.class);
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException("get const field failed", e);
                    }
                    systemParameterInfo.desc = desc.value();
                    systemParameterInfos.add(systemParameterInfo);
                } else {
//                    throw new RuntimeException(String.format("miss description of field in CommonParameter, field name:%s", field.getName()));
                }
            }
        }
        return systemParameterInfos;
    }

    /**
     * 是否是常量
     */
    public static boolean isConstField(Field field) {
        int efm = field.getModifiers();
        if (Modifier.isPublic(efm) && Modifier.isStatic(efm) && Modifier.isFinal(efm)) {
            return true;
        }
        return false;
    }

    //命名方式，后续还需要继续支持
    private static String convertEntityName(String type) {
        String[] ss = type.split("\\.");
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < ss.length; i++) {
            if (i > 0) {
                builder.append("_");
            }
            String str = ss[i];
//            if (str.length() == 0) {
//                System.out.println("xxx");
//            }
            builder.append(str.substring(0,1).toUpperCase());
            builder.append(str.substring(1));
        }
        return builder.toString();
    }

    private String getEntityName(String group, Class<?> type) {
        return (group == null || group.length() == 0 || type.getName().startsWith(
                "net.pocrd.")) ? "Api_" + type.getSimpleName() : "Api_" + group.toUpperCase() + "_" + type.getSimpleName();
    }

    //命名方式，后续还需要继续支持
    private String getEntityName4CollectionAndArray(String group, Class<?> type) {
        return (group == null || group.length() == 0 || type.getName().startsWith(
                "net.pocrd.")) ?
                "Api_" + type.getSimpleName() + "_ArrayResp" :
                "Api_" + group.toUpperCase() + "_" + type.getSimpleName() + "_ArrayResp";
    }

    private boolean hasPublicFields(Class<?> clazz) {
        if (clazz == null) {
            throw new RuntimeException("get public fields num failed, clazz is null!");
        }
        Field[] fields = clazz.getDeclaredFields();
        for (Field field : fields) {
            int modifier = field.getModifiers();
            if (Modifier.isPublic(modifier)) {
                return true;
            }
        }
        return false;
    }
}
