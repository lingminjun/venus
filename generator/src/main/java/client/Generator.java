package client;

import com.alibaba.fastjson.JSON;
import com.venus.apigw.document.entities.*;
import com.venus.esb.lang.ESBConsts;
import com.venus.esb.lang.ESBT;
import com.venus.esb.utils.DateUtils;
import com.venus.esb.utils.FileUtils;
import com.venus.esb.utils.HTTP;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * Description:
 * User: lingminjun
 * Date: 2018-10-16
 * Time: 上午10:17
 */
public abstract class Generator {

    public static final String VENUS_APIGW_CODES_FILE_NAME = "Venus_APIGW_Codes";

    // 基础信息
    public final String outDir;
    public final String robotName;
    public final String robotVersion;
    public final String gwHost;
    public final int gwPort;
    public final String gwVersion;
    public final String programmingLanguage;
    public final String programmingLanguageVersion;

    //容器不允许构造后修改
    private final Set<String> groups;
    private final Set<String> selectors;

    public Generator(/*String robotName, String robotVersion,*/ String outDir, String gwHost, int gwPort, String gwVersion, String programmingLanguage, String programmingLanguageVersion) {
        this(outDir, gwHost,gwPort,gwVersion,programmingLanguage,programmingLanguageVersion,null,null);
    }
    public Generator(/*String robotName, String robotVersion,*/ String outDir, String gwHost, int gwPort, String gwVersion, String programmingLanguage, String programmingLanguageVersion,String[] filterGroups, String[] filterSelectors) {
        this.outDir = outDir;
        this.robotName = this.getClass().getSimpleName();
        this.robotVersion = "1.0.0";
        this.gwHost = gwHost;
        this.gwPort = gwPort;
        this.gwVersion = gwVersion;
        this.programmingLanguage = programmingLanguage;
        this.programmingLanguageVersion = programmingLanguageVersion;
        if (filterGroups != null && filterGroups.length > 0) {
            this.groups = new HashSet<String>();
            for (String str : filterGroups) {
                this.groups.add(str);
            }
        } else {
            this.groups = null;
        }
        if (filterSelectors != null && filterSelectors.length > 0) {
            this.selectors = new HashSet<String>();
            for (String str : filterSelectors) {
                this.selectors.add(str);
            }
        } else {
            this.selectors = null;
        }
    }

    public final String getNowDay() {
        return DateUtils.toShortYYYY_MM_DD(new Date());
    }

    public final String getFileHeaderDetail(String fileName, String fileExt) {
        return "//\n" +
                "//  " + fileName + fileExt + "\n" +
                "//  " + this.programmingLanguage + " " + this.programmingLanguageVersion + "\n" +
                "//\n" +
                "//  Created by Venus-API-GW(" + this.gwVersion + ") " + this.robotName + "(" + this.robotVersion + ") on " + getNowDay() + ".\n" +
                "//  Copyright © 2018年 MJ Ling. All rights reserved.\n" +
                "//  GitHub: https://github.com/lingminjun/venus .\n" +
                "//\n" +
                "\n\n";
    }

    public final String buildCallName(MethodInfo methodInfo, String prefix) {

        String[] ss = methodInfo.methodName.split("\\.");
        StringBuilder builder = new StringBuilder();
        if (prefix != null && prefix.length() > 0) {
            builder.append(prefix);
            builder.append("_");
        }

        for (int i = 0; i < ss.length; i++) {
            if (i > 0) {
                builder.append("_");
            }
            String str = ss[i];
            builder.append(str.substring(0,1).toUpperCase());
            builder.append(str.substring(1));
        }
        return builder.toString();
    }

    public final void gen() throws Exception {
        String url = "";
        if (gwPort == 443) {
            url = "https://" + this.gwHost + "/info.api?json";
        } else if (gwPort == 80 || gwPort == 0){
            url = "http://" + this.gwHost + "/info.api?json";
        } else {
            url = "http://" + this.gwHost + ":" + this.gwPort + "/info.api?json";
        }

        String json = HTTP.get(url,null);
        Document document = JSON.parseObject(json, Document.class);

        if (document == null || document.apiList == null) {
            System.out.println("nothing..");
            return;
        }

        HashSet<String> calls = new HashSet<String>();//记录已经写入的请求
        HashSet<String> entities = new HashSet<String>();//记录已经写入的entity
        HashSet<String> codes = new HashSet<String>();//记录已经写入的codes
        List<CodeInfo> codeList = new ArrayList<CodeInfo>();
        String filePrefix = theFilePrefix();
        String fileExt = theFileExt();
        for (MethodInfo methodInfo : document.apiList) {
            String groupName = methodInfo.groupName;
            String methodName = methodInfo.methodName;
            //filter过滤
            if (this.groups != null && !groups.contains(groupName)) {
                continue;
            }
            if (this.selectors != null && !selectors.contains(methodName)) {
                continue;
            }

            //已经写入过
            if (calls.contains(methodName)) {
                continue;
            }
            calls.add(methodName);

            StringBuilder builder = new StringBuilder();
            String callName = buildCallName(methodInfo,filePrefix);
            builder.append(getFileHeaderDetail(callName,fileExt));

            // 子类完成代码
            genAPICall(builder,methodInfo);

            String filePath = this.outDir.endsWith(File.separator) ? this.outDir + callName + fileExt : this.outDir + File.separator + callName + fileExt;

            FileUtils.writeFile(filePath,builder.toString(),ESBConsts.UTF8);

            //遍历Struct
            if (methodInfo.reqStructList != null) {
                for (TypeStruct struct : methodInfo.reqStructList) {
                    genStruct(methodInfo,struct,entities,fileExt);
                }
            }

            if (methodInfo.respStructList != null) {
                for (TypeStruct struct : methodInfo.respStructList) {
                    genStruct(methodInfo,struct,entities,fileExt);
                }
            }

            //遍历Code
            if (methodInfo.errorCodeList != null) {
                for (CodeInfo codeInfo : methodInfo.errorCodeList) {
                    if (codes.contains(codeInfo.service + "_" + codeInfo.code)) {
                        return;
                    }
                    codes.add(codeInfo.service + "_" + codeInfo.code);
                    codeList.add(codeInfo);
                }
            }
        }

        //生成codes
    }

    private void genStruct(MethodInfo methodInfo, TypeStruct struct, HashSet<String> entities, String fileExt) throws IOException {
        if (entities.contains(struct.type)) {
            return;
        }
        entities.add(struct.type);

        String entityName = struct.name;
        String filePath = this.outDir.endsWith(File.separator) ? this.outDir + entityName + fileExt : this.outDir + File.separator + entityName + fileExt;
        StringBuilder builder = new StringBuilder();
        builder.append(getFileHeaderDetail(entityName,fileExt));

        // 子类完成代码
        genAPIEntity(builder,struct);

        FileUtils.writeFile(filePath,builder.toString(),ESBConsts.UTF8);

        if (struct.fieldList != null) {
            for (FieldInfo fieldInfo : struct.fieldList) {
                //基础类型忽略
                if (ESBT.isBaseType(fieldInfo.type)) {
                    continue;
                }

                TypeStruct fstruct = findStructBy(methodInfo,fieldInfo.type);
                if (fieldInfo != null) {
                    genStruct(methodInfo,fstruct,entities,fileExt);
                }
            }
        }
    }

    private void genCode(MethodInfo methodInfo, List<CodeInfo> codeList, HashSet<String> codes, String fileExt) throws IOException {
        if (codeList == null || codeList.size() == 0) {
            return;
        }

        String fileName = VENUS_APIGW_CODES_FILE_NAME;
        String filePath = this.outDir.endsWith(File.separator) ? this.outDir + fileName + fileExt : this.outDir + File.separator + fileName + fileExt;
        StringBuilder builder = new StringBuilder();
        builder.append(getFileHeaderDetail(fileName,fileExt));
        genAPICodes(builder,codeList);
        FileUtils.writeFile(filePath,builder.toString(),ESBConsts.UTF8);
    }

    private final TypeStruct findStructBy(MethodInfo methodInfo, String type) {
        if (methodInfo.reqStructList != null) {
            for (TypeStruct struct : methodInfo.reqStructList) {
                if (type.equals(struct.type)) {
                    return struct;
                }
            }
        }

        if (methodInfo.respStructList != null) {
            for (TypeStruct struct : methodInfo.respStructList) {
                if (type.equals(struct.type)) {
                    return struct;
                }
            }
        }

        return null;
    }

    protected String theFilePrefix() {
        return null;
    }

    protected String theFileExt() {
        return ".java";
    }

//    public abstract void genAPICall(StringBuilder builder, MethodInfo methodInfo);

    // 子类重载并实现
    public abstract void genAPICall(StringBuilder builder, MethodInfo methodInfo);
    public abstract void genAPIEntity(StringBuilder builder, TypeStruct typeStruct);
    public abstract void genAPICodes(StringBuilder builder, List<CodeInfo> codeList);
}
