package com.venus.apigw.document;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.venus.apigw.consts.ConstField;
import com.venus.apigw.db.APIPojo;
import com.venus.apigw.db.DB;
import com.venus.apigw.document.entities.ApiMethodInfo;
import com.venus.apigw.document.entities.Document;
import com.venus.apigw.manager.APIManager_New;
import com.venus.apigw.serializable.POJOSerializerProvider;
import com.venus.apigw.serializable.Serializer;
import com.venus.esb.lang.ESBConsts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * 获取接口信息,不做权限控制
 * @author guankaiqiang
 * modify lingminjun
 */
public class InfoServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(InfoServlet.class);
    private static final Serializer<Document> docs = POJOSerializerProvider.getSerializer(Document.class);
    private static final String XML_RESP_CONTENT_TYPE = "application/xml";
    private static final String JSON_RESP_CONTENT_TYPE = "application/json";
    private static final String RESP_CHARSET = ESBConsts.UTF8_STR;
    private static byte[] XML_HEAD = ("<?xml version='1.0' encoding='utf-8'?><?xml-stylesheet type='text/xsl' href='/xslt/apiInfo.xsl'?>").getBytes(
            ConstField.UTF8);
    private static ApiMethodInfo[] apiMethodInfos;
    private static Document document;
    private static Object lock = new Object();

    public static void setApiMethodInfos(final ApiMethodInfo[]... infos) {
        synchronized (lock) {
            if (infos != null && infos.length > 0) {
                logger.info("api document info size {}",infos.length);
                List infoList = new LinkedList<ApiMethodInfo>();
                for (ApiMethodInfo[] infoArray : infos) {
                    infoList.addAll(Arrays.asList(infoArray));
                }
                apiMethodInfos = new ApiMethodInfo[infoList.size()];
                infoList.toArray(apiMethodInfos);
            }
            document = new ApiDocumentationHelper().getDocument(apiMethodInfos);
            logger.info("api method list size {}",(document.apiList != null ? document.apiList.size() : 0));
        }
    }

    public static Document convertApiMethodInfos(List<APIPojo> apiPojos) {

        List<ApiMethodInfo> apis = new ArrayList<>();
        for (APIPojo pojo : apiPojos) {
            apis.add(APIManager_New.convertToMethodInfo(pojo.getInfo()));
        }

            return new ApiDocumentationHelper().getDocument(apis.toArray(new ApiMethodInfo[0]));
//            logger.info("api method list size {}",(document.apiList != null ? document.apiList.size() : 0));

    }

    private static void reloadDocument() {
        synchronized (lock) {
            document = new ApiDocumentationHelper().getDocument(apiMethodInfos);
        }
    }

    public static Document getDocument() {
        synchronized (lock) {
            return document;
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {

            //修改成数据库查询接口
            List<APIPojo> results = DB.query("apigw_api","`status` = ?", new Object[]{0}, APIPojo.class);
            Document document = convertApiMethodInfos(results);

            logger.info("api method count {}",(document.apiList != null ? document.apiList.size() : 0));
            OutputStream out = resp.getOutputStream();
            resp.setCharacterEncoding(RESP_CHARSET);
            String queryString = req.getQueryString();
            if (queryString == null || queryString.isEmpty()) {
                resp.setContentType(XML_RESP_CONTENT_TYPE);
                out.write(XML_HEAD);//链xslt
                docs.toXml(document, out, true);
            } else if (queryString.contains("json")) {
                resp.setContentType(JSON_RESP_CONTENT_TYPE);
//                    docs.toJson(document, out, true);
                //文档解析避免出现$ref
                out.write(JSON.toJSONBytes(document, SerializerFeature.DisableCircularReferenceDetect));
            } else if (queryString.contains("raw")) {
                resp.setContentType(XML_RESP_CONTENT_TYPE);
                docs.toXml(document, out, true);
            } else {
                resp.setContentType(XML_RESP_CONTENT_TYPE);
                out.write(XML_HEAD);//链xslt
                docs.toXml(document, out, true);
            }
        } catch (Throwable t) {
            logger.error("parse xml for api info failed.", t);
            resp.getWriter().write(t.getMessage());
            t.printStackTrace(resp.getWriter());
        }
    }
}

