package com.venus.apigw.servlet;

import com.venus.apigw.common.BaseServlet;
import com.venus.esb.ESB;
import com.venus.esb.ESBResponse;
import com.venus.esb.lang.ESBCookie;
import com.venus.esb.lang.ESBException;

import javax.servlet.annotation.WebServlet;
import java.util.List;
import java.util.Map;

/**
 * 请求转发，esb完成请求分解
 * @author MJ
 */
@WebServlet("/m.api")
public class DispatchServlet extends BaseServlet {

    private static final long   serialVersionUID = 1L;


    public DispatchServlet() {
        super();
    }

    // 初始化特殊参数
    {
        this.notFeedContext = true;
    }

    @Override
    protected List<ESBResponse> dispatchedCall(Map<String,String> params, Map<String,String> header, Map<String,ESBCookie> cookies, String body) throws ESBException {
            return ESB.bus().call(params,header,cookies,body);
    }

}
