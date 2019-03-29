package com.venus.apigw.servlet;

import com.venus.apigw.captcha.Captcha;
import com.venus.apigw.captcha.utils.CaptchaUtil;
import com.venus.apigw.common.BaseServlet;
import com.venus.esb.ESBAPIContext;
import com.venus.esb.ESBResponse;
import com.venus.esb.lang.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * 验证码servlet, 算法验证session, 加点注释
 */
public class CaptchaServlet extends BaseServlet {
    private static final long serialVersionUID = -90304944339413093L;

    @Override
    protected void processCall(ESBAPIContext context, HttpServletRequest request, HttpServletResponse response, Map<String, String> params, Map<String, String> header, Map<String, ESBCookie> cookies) throws IOException {
        String salt = context.dtoken;
        if (ESBT.isEmpty(salt)) {
            salt = context.did + context.ua;
        }

        //生成
        Captcha info = CaptchaUtil.out(salt);

        // 设置cookie
        ESBCookie cookie = new ESBCookie();
        cookie.name = ESBSTDKeys.CAPTCHA_SESSION_KEY;
        cookie.value = info.getSession();
        cookie.maxAge = 15 * 60; //15分钟，主要是给网页长时间操作
        cookie.domain = getMainHost(request);// + ":8080";
        context.putCookie(ESBSTDKeys.CAPTCHA_SESSION_KEY,cookie);

        // 设置头部
        setImageHeader(response);

        // 返回前，设置cookie (必须在 getOutputStream 前设置)
        ESBCookieExts setc = context.cookies;
        if (setc != null && setc.size() > 0) {
            setResponseCookies(setc.map(),response);
        }

        // 输出内容
        info.out(response.getOutputStream());

        // 验证码
//        System.out.println(info.text());
    }

    @Override
    protected List<ESBResponse> dispatchedCall(Map<String, String> params, Map<String, String> header, Map<String, ESBCookie> cookies, String body) throws ESBException {
        return null;
    }

    /**
     * 设置相应头
     *
     * @param response HttpServletResponse
     */
    public static void setImageHeader(HttpServletResponse response) {
        response.setContentType("image/gif");
        response.setHeader("Pragma", "No-cache");
        response.setHeader("Cache-Control", "no-cache");
        response.setDateHeader("Expires", 0);
    }

}
