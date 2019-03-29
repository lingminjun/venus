package com.venus.apigw.servlet;

import com.venus.apigw.captcha.utils.CaptchaUtil;
import com.venus.apigw.common.BaseServlet;
import com.venus.esb.ESBAPIContext;
import com.venus.esb.ESBResponse;
import com.venus.esb.lang.ESBCookie;
import com.venus.esb.lang.ESBException;
import com.venus.esb.lang.ESBSTDKeys;
import com.venus.esb.lang.ESBT;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * 验证码,
 *      单独验证接口: /captcha/check.api
 *      默认风控验证:
 */
public class CaptchaCheckServlet extends BaseServlet {
    private static final long serialVersionUID = -90304944339413093L;

    @Override
    protected void processCall(ESBAPIContext context, HttpServletRequest request, HttpServletResponse response, Map<String, String> params, Map<String, String> header, Map<String, ESBCookie> cookies) throws IOException {
        String salt = context.dtoken;
        if (ESBT.isEmpty(salt)) {
            salt = context.did + context.ua;
        }
        String code = context.getRightValue(ESBSTDKeys.CAPTCHA_KEY,params,null,-1);
        String session = context.getRightValue(ESBSTDKeys.CAPTCHA_SESSION_KEY,params,cookies,-1);

        // 算法与esb内部验证保持一致
        boolean check = CaptchaUtil.ver(salt,code,session);
        response.setContentType(CONTENT_TYPE_JSON);
        response.getOutputStream().write(("{\"success\":"+check+"}").getBytes());
    }

    @Override
    protected List<ESBResponse> dispatchedCall(Map<String, String> params, Map<String, String> header, Map<String, ESBCookie> cookies, String body) throws ESBException {
        return null;
    }

}
