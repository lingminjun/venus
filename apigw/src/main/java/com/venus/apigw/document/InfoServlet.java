package com.venus.apigw.document;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.venus.apigw.common.BaseServlet;
import com.venus.apigw.consts.ConstField;
import com.venus.apigw.db.DBUtil;
import com.venus.apigw.document.entities.ApiMethodInfo;
import com.venus.apigw.document.entities.Document;
import com.venus.apigw.manager.APIManager_New;
import com.venus.apigw.serializable.POJOSerializerProvider;
import com.venus.apigw.serializable.Serializer;
import com.venus.esb.ESBAPIInfo;
import com.venus.esb.ESBResponse;
import com.venus.esb.lang.ESBConsts;
import com.venus.esb.lang.ESBCookie;
import com.venus.esb.lang.ESBException;
import com.venus.esb.lang.ESBT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 获取接口信息,不做权限控制
 * @author guankaiqiang
 * modify lingminjun
 */
public class InfoServlet extends BaseServlet {

    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(InfoServlet.class);
    private static final Serializer<Document> docs = POJOSerializerProvider.getSerializer(Document.class);
    private static final String XML_RESP_CONTENT_TYPE = "application/xml";
    private static final String JSON_RESP_CONTENT_TYPE = "application/json";
    private static final String RESP_CHARSET = ESBConsts.UTF8_STR;
    private static byte[] XML_HEAD = ("<?xml version='1.0' encoding='utf-8'?><?xml-stylesheet type='text/xsl' href='/xslt/apiInfo.xsl'?>").getBytes(
            ConstField.UTF8);

    public static Document convertApiMethodInfos(List<ESBAPIInfo> infos) {

        List<ApiMethodInfo> apis = new ArrayList<>();
        for (ESBAPIInfo pojo : infos) {
            apis.add(APIManager_New.convertToMethodInfo(pojo));
        }

        return new ApiDocumentationHelper().getDocument(apis.toArray(new ApiMethodInfo[0]));
    }


    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        OutputStream out = null;
        try {
            Map<String,String> params = parseRequestParams(req);
            int status = ESBT.integer(params.get("kind"),0);
            String type = ESBT.string(params.get("type"),"xml");

            //修改成数据库查询接口
            List<ESBAPIInfo> results = DBUtil.allAPIsForStatus(status);
            Document document = convertApiMethodInfos(results);

            logger.info("api method count {}",(document.apiList != null ? document.apiList.size() : 0));
            out = resp.getOutputStream();
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
            if (out == null) {
                out = resp.getOutputStream();
            }
            out.write(t.getMessage().getBytes());
            t.printStackTrace(resp.getWriter());
        }
    }

    @Override
    protected List<ESBResponse> dispatchedCall(Map<String, String> params, Map<String, String> header, Map<String, ESBCookie> cookies, String body) throws ESBException {
        return null;
    }
}

