package com.venus.apigw.manager;

import com.alibaba.fastjson.JSON;
import com.venus.apigw.common.BaseServlet;
import com.venus.apigw.db.APIPojo;
import com.venus.apigw.db.DB;
import com.venus.esb.ESB;
import com.venus.esb.ESBAPIContext;
import com.venus.esb.ESBAPIInfo;
import com.venus.esb.ESBResponse;
import com.venus.esb.config.ESBConfigCenter;
import com.venus.esb.lang.*;
import com.venus.esb.sign.ESBAPIAlgorithm;
import com.venus.esb.sign.ESBAPISignature;
import com.venus.esb.sign.utils.Signable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * Description: 更新api
 * User: lingminjun
 * Date: 2018-10-07
 * Time: 上午9:21
 */
@WebServlet("/gw.api")
public class APIManagerServlet extends BaseServlet {

    private static final Logger logger                   = LoggerFactory.getLogger(APIManagerServlet.class);

    protected boolean checkRSASignature(Map<String,String> params) {
//        Map<String,String> params = parseRequestParams(req);
        // 拼装被签名参数列表
        StringBuilder sb = ESBAPISignature.getSortedParameters(params);

        // 验证签名
        String sig = params.get(ESBSTDKeys.SIGN_KEY);
        if (sig == null || sig.length() == 0) {
            return false;
        }

        // 写死
        String sm = ESBAPIAlgorithm.RSA.name();

        //为none的情况
        Signable signature = ESBAPISignature.getSignable(sm, ESBConfigCenter.instance().getPubRSAKey(),null);
        return signature.verify(sig,sb.toString().getBytes(ESBConsts.UTF8));
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        ESBAPIContext context = ESBAPIContext.context();
        context.contentType = ESBAPIContext.JSON_CONTENT;
        context.arithmetic = ESBAPIAlgorithm.RSA.name();
//        super.doPost(req, resp);

        Map<String,String> params = parseRequestParams(req);

        if (!checkRSASignature(params)) {
            try {
                out(context, ESBExceptionCodes.SIGNATURE_ERROR("私钥验证不通过"), null, resp);
            } catch (ESBException e) {
                e.printStackTrace();
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Bad Request");
            }
            return;
        }

        //更新接口 (TODO: 支持批量)
        String selector = params.get("APISelector");
        if (selector == null || selector.length() == 0) {
            try {
                out(context, ESBExceptionCodes.PARAMETER_ERROR("参数设置错误，必填APISelector参数"), null, resp);
            } catch (ESBException e) {
                e.printStackTrace();
                logger.error("参数设置错误，必填APISelector参数", e);
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Bad Request");
            }
            return;
        }

        String apiInfo = params.get("API");

        //暂时不支持更新
        if (apiInfo != null && apiInfo.length() > 0) {
            // 插入数据库。暂时不实现
            List<ESBAPIInfo> list = JSON.parseArray(apiInfo,ESBAPIInfo.class);
            for (ESBAPIInfo api : list) {
                APIPojo pojo = new APIPojo();
                pojo.modified = System.currentTimeMillis();
                pojo.domain = api.api.domain;
                pojo.module = api.api.module;
                pojo.method = api.api.methodName;
                pojo.detail = api.api.desc;
                pojo.owner = api.api.owner;
                pojo.security = api.api.security;
                pojo.status = 0;
                pojo.json = JSON.toJSONString(api,ESBConsts.FASTJSON_SERIALIZER_FEATURES);
                DB.upinsert("apigw_api",new String[]{"domain","module","method","json","detail","owner","security","status","modified"},pojo);
            }
        }

        String[] ss = selector.split(",");
        if (ss != null) {
            for (String sel : ss) {
                ESB.bus().refresh(sel);
            }
        } else {
            ESB.bus().refresh(selector);
        }

        List<ESBResponse> results = new ArrayList<>();
        ESBResponse success = new ESBResponse();
        success.result = "{\"success\":true}";
        results.add(success);

        try {
            out(context,null,results,resp);
        } catch (ESBException e) {
            e.printStackTrace();
            logger.error("未知错误：", e);
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Bad Request");
            return;
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
//        super.doGet(request, response);
        response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Bad Request");
    }

    @Override
    protected List<ESBResponse> dispatchedCall(Map<String, String> params, Map<String, String> header, Map<String, ESBCookie> cookies, String body) throws ESBException {
        return null;
    }

}
