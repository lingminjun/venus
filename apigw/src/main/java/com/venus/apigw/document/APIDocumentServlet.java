package com.venus.apigw.document;

import com.venus.apigw.config.GWConfig;
import com.venus.apigw.consts.ConstField;
import com.venus.esb.lang.ESBConsts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Created by lingminjun on 17/2/22.APIDocumentServlet
 * 控制是否需要开放api 文档
 */
public class APIDocumentServlet extends InfoServlet {
    private static final Logger logger = LoggerFactory.getLogger(APIDocumentServlet.class);
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        logger.info("get api document begin");
        if (GWConfig.getInstance().isOpenAPIDocument()) {
            logger.info("get api url {}", req.getPathInfo());
            super.doGet(req, resp);
        } else {
            OutputStream out = resp.getOutputStream();
            resp.setContentType("plain/text");
            resp.setCharacterEncoding(ESBConsts.UTF8_STR);
            out.write("API-GW 2.0".getBytes(ConstField.UTF8));
        }
        logger.info("get api document end");
    }
}
