package com.venus.apigw.manager;

import com.venus.apigw.bi.ESBBILogger;
import com.venus.esb.*;
import com.venus.esb.lang.ESBCookie;
import com.venus.esb.lang.ESBException;
import com.venus.esb.ESB;
import com.venus.esb.ESBAPIContext;
import com.venus.esb.ESBResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * Description:
 * User: lingminjun
 * Date: 2018-09-21
 * Time: 下午9:22
 */
public class ESBLogger extends ESBBILogger {
    private static Logger accessFileLogger  = LoggerFactory.getLogger("net.pocrd.api.access");
    private static Logger requestFileLogger = LoggerFactory.getLogger("net.pocrd.api.request");
    public static final  String       ACCESS_SPLITTER   = new String(new char[] { ' ', 1 });

    @Override
    public void request(ESB esb, ESBAPIContext context, Map<String, String> params, Map<String, String> header, Map<String, ESBCookie> cookies, List<ESBResponse> result, ESBException e) {
        //埋点需要，记录请求URL
        String requestUrl = context.getTempExt("the_request_url").replace("\n", "");
        long now = System.currentTimeMillis();
        long cost = now - context.at;
        if (e == null) {
            request(context,requestUrl,cost);
        } else {
            request(context,e,requestUrl,cost);
        }
    }


    private void request(ESBAPIContext context, String url,  long cost) {
        requestFileLogger.info(
                url
                        + ACCESS_SPLITTER + context.utoken
                        + ACCESS_SPLITTER
                        + ACCESS_SPLITTER + context.at + ":" + cost);

    }


    private void request(ESBAPIContext context, ESBException e, String url,  long cost) {
        requestFileLogger.info(
                url
                        + ACCESS_SPLITTER + context.utoken
                        + ACCESS_SPLITTER + e.getReason()
                        + ACCESS_SPLITTER + context.at + ":" + cost);
    }


    @Override
    public void access(ESB esb, ESBAPIInfo info, ESBAPIContext context, Map<String, String> params, Map<String, String> header, Map<String, ESBCookie> cookies, ESBInvocation invocation, Object result, ESBException e) {
        super.access(esb,info,context,params,header,cookies,invocation,result,e);
        long now = System.currentTimeMillis();
        long cost = now - context.at;
        accessFileLogger.info(info.api.getAPISelector()
                        + ACCESS_SPLITTER + invocation.getURI()
                        + ACCESS_SPLITTER + (e != null ? e.getCode() : 0)
                        + ACCESS_SPLITTER + (e != null ? e.getDomain() : "")
                        + ACCESS_SPLITTER + (e != null ? e.getReason() : "SUCCESS")
                        + ACCESS_SPLITTER + context.at + ":" + cost);
    }
}
