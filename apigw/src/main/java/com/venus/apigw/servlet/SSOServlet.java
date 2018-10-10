package com.venus.apigw.servlet;

import com.venus.apigw.common.BaseServlet;
import com.venus.esb.ESB;
import com.venus.esb.ESBResponse;
import com.venus.esb.lang.ESBCookie;
import com.venus.esb.lang.ESBException;
import com.venus.esb.lang.ESBSTDKeys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.annotation.WebServlet;
import java.util.List;
import java.util.Map;

/**
 * Created by lingminjun on 17/3/22.
 *
 *
 * 分浏览器免登和客户端免登：
 *
 * 浏览器免登陆(redirect_url1重定向iframe跳转，redirect借助中间也wsso.html完成)
 *
 * A(domain)                            B(domain)                                  Auth GW
 * ————————                            ——————————                                 ——————————
 *    |                                    |                                          |
 *    |redirect                            |                                          |
 *    |  redirect_url1:to(domain)+to_aid   |                                          |
 *    |----------------------------------->|                                          |
 *    |                                    |                                          |
 *    |                                    |  req_sso1:to(domain)+to_aid+sign+cookie  |
 *    |                                    |----------------------------------------->|
 *    |                                    |                                          |验证权限
 *    |                                    |             res_sso1:sso_tk              |并授予sso_tk
 *    |                                    |<-----------------------------------------|
 *    |                                    |redirect                                  |
 *    |        redirect_url2:sso_tk        |                                          |
 *    |<-----------------------------------|                                          |
 *    |                                    |                                          |
 *    |                              req_sso2:sso_tk                                  |
 *    |------------------------------------------------------------------------------>|
 *    |                                    |                                          |验证sso_tk与域名
 *    |                            res_sso2:u_tk+cookie                               |授予u_tk和种cookie
 *    |<------------------------------------------------------------------------------|
 *    |                                    |                                          |
 *    |                                    |                                          |
 *    |                                    |                                          |
 *    |                                    |                                          |
 *
 *    注意客户端免登类似流程，A应用通知B应用请求sso_tk（一次性token），并传回给A应用，A应用就可以拿sso_tk换认证信息了
 *
 *
 */
@WebServlet("/sso.api")
public class SSOServlet extends BaseServlet {
    private static final Logger logger           = LoggerFactory.getLogger(SSOServlet.class);


    @Override
    protected List<ESBResponse> dispatchedCall(Map<String,String> params, Map<String,String> header, Map<String,ESBCookie> cookies, String body) throws ESBException {
        String ssoToken = params.get(ESBSTDKeys.SSO_TOKEN_KEY);
        // 请求授予认证方域名请求
        if (ssoToken != null && ssoToken.length() > 0) {
//            params.put(ESBSTDKeys.SSO_TOKEN_KEY)
//            logger.info("");
        } else {// 被请求授予认证方域名请求

        }

        return ESB.bus().call(params,header,cookies,body);

    }
}
