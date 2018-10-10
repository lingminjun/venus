package com.venus.apigw.manager;

import com.alibaba.fastjson.JSON;
import com.github.kristofa.brave.Brave;
import com.venus.apigw.brave.GWServerRequestAdapter;
import com.venus.apigw.brave.GWServerResponseAdapter;
import com.venus.esb.ESB;
import com.venus.esb.ESBAPIContext;
import com.venus.esb.ESBResponse;
import com.venus.esb.brave.ESBBraveFactory;
import com.venus.esb.lang.ESBConsts;
import com.venus.esb.lang.ESBCookie;
import com.venus.esb.lang.ESBException;

import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * Description:
 * User: lingminjun
 * Date: 2018-09-20
 * Time: 下午5:04
 */
public class ESBBraveObserver implements ESB.APIObserver {

    @Override
    public void before(ESB esb, ESBAPIContext context, Map<String, String> params, Map<String, String> header, Map<String, ESBCookie> cookies) {
        //因为客户端并没有正在传递span过来,此处要做的是伪造客户端传递过来的span,并作为服务端接受span
        Brave brave = ESBBraveFactory.getBrave();
        if (brave == null) {
            return;
        }

        brave.serverRequestInterceptor().handle(new GWServerRequestAdapter(brave,context,header));
    }

    @Override
    public void after(ESB esb, ESBAPIContext context, Map<String, String> params, Map<String, String> header, Map<String, ESBCookie> cookies, List<ESBResponse> result, ESBException e) {
        //作为服务端,告知已经正确返回结果
        Brave brave = ESBBraveFactory.getBrave();
        if (brave == null) {
            return;
        }

        boolean success = false;
        String error_message = null;
        String result_message = null;
        if (result != null && result.size() > 0 && result.get(0).exception == null) {
            success = true;
            result_message = JSON.toJSONString(result, ESBConsts.FASTJSON_SERIALIZER_FEATURES);
        } else if (e != null){
            error_message = e.getReason();
        } else if (result != null && result.size() > 0 && result.get(0).exception != null) {
            error_message = result.get(0).exception.getReason();
        }
        brave.serverResponseInterceptor().handle(new GWServerResponseAdapter(success,result_message,error_message));
    }

}
