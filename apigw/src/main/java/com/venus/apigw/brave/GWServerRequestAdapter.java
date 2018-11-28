package com.venus.apigw.brave;

import com.github.kristofa.brave.*;
import com.venus.esb.ESBAPIContext;
import com.venus.esb.lang.ESBSTDKeys;
import com.venus.esb.lang.ESBT;
import com.venus.esb.sign.ESBUUID;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

/**
 * Created by lmj on 17/9/1.
 * 服务端收到请求(收到就汇报)
 */
public class GWServerRequestAdapter implements ServerRequestAdapter {

    private final ESBAPIContext context;
    private final Map<String, String> header;
    private final String spanName;
    private final Brave brave;


    public GWServerRequestAdapter(Brave brave, ESBAPIContext context, Map<String, String> header) {
        this.brave = brave;
        this.context = context;
        this.header = header;
        this.spanName = context.selector;
    }

    @Override
    public TraceData getTraceData() {
        //需要构造spanId,因为没有客户端部分，会自动写到localThread中---(自定义cid/tid/parent机制后续支持)
        SpanId spanId = this.brave.clientTracer().startNewSpan(getSpanName());
        if (spanId != null) {
            return TraceData.builder().spanId(spanId).sample(true).build();
        } else {
            return TraceData.builder().sample(true).build();//自行生产新的traceId
        }
    }

    @Override
    public String getSpanName() {
        if (spanName != null) {
            return spanName;
        }
        return context.getTempExt("the_request_url");
    }

    @Override
    public Collection<KeyValueAnnotation> requestAnnotations() {

        //收集参数
        Collection<KeyValueAnnotation> annotations = new ArrayList<KeyValueAnnotation>();

        //客户端信息
        String cip = context.getCip();
        annotations.add(KeyValueAnnotation.create("Client Address", cip+":0"));

//        context.putTempExt("the_request_url", getRequestUrl(request));
//        context.putTempExt("the_request_protocol", request.getScheme());
//        context.putTempExt("the_request_address", request.getLocalAddr()+":"+request.getLocalPort());
//        context.putTempExt("the_request_method", request.getMethod());
        //服务端信息
        annotations.add(KeyValueAnnotation.create("protocol", context.getTempExt("the_request_protocol")));//http还是https,版本暂时保留
        annotations.add(KeyValueAnnotation.create("Server Address", context.getTempExt("the_request_address")+"("+ESBT.getServiceName()+")"));
        annotations.add(KeyValueAnnotation.create("Server PId", ESBUUID.getProcessID()));
        annotations.add(KeyValueAnnotation.create("HTTP Method", context.getTempExt("the_request_method")));

        //取标准参数
        addStdParameters(annotations,context);

        //取参数,不做拆分,统一放在一起比较简单
        if (context.body != null) {
            annotations.add(KeyValueAnnotation.create("Request Body", context.body));
        }

        return annotations;
    }

    //主要header中的参数和一些通用参数记录下,其他不需要
    private static void addStdParameters(Collection<KeyValueAnnotation> annotations, ESBAPIContext context) {

        annotations.add(KeyValueAnnotation.create(ESBSTDKeys.AID_KEY, "" + context.aid));
        annotations.add(KeyValueAnnotation.create(ESBSTDKeys.DID_KEY, context.did));
        if (context.uid != null && context.uid.length() > 0) {
            annotations.add(KeyValueAnnotation.create(ESBSTDKeys.UID_KEY, context.uid));
        }
        if (context.acct != null && context.acct.length() > 0) {
            annotations.add(KeyValueAnnotation.create(ESBSTDKeys.ACCT_KEY, context.acct));
        }
        if (context.pid != null && context.pid.length() > 0) {
            annotations.add(KeyValueAnnotation.create(ESBSTDKeys.PID_KEY, context.pid));
        }
        if (context.l10n != null && context.l10n.length() > 0) {
            annotations.add(KeyValueAnnotation.create(ESBSTDKeys.L10N_KEY, context.l10n));
        }

        if (context.cid != null && context.cid.length() > 0) {
            annotations.add(KeyValueAnnotation.create(ESBSTDKeys.CID_KEY, context.cid));
        }
        if (context.cvc > 0) {
            annotations.add(KeyValueAnnotation.create(ESBSTDKeys.CVC_KEY, "" + context.cvc));
        }
        if (context.cvn != null && context.cvn.length() > 0) {
            annotations.add(KeyValueAnnotation.create(ESBSTDKeys.CVN_KEY, context.cvn));
        }

        if (context.ua != null && context.ua.length() > 0) {
            annotations.add(KeyValueAnnotation.create(ESBSTDKeys.UA_KEY, context.ua));
        }

        if (context.referer != null && context.referer.length() > 0) {
            annotations.add(KeyValueAnnotation.create(ESBSTDKeys.REFERER_KEY, context.referer));
        }

        if (context.host != null && context.host.length() > 0) {
            annotations.add(KeyValueAnnotation.create(ESBSTDKeys.HOST_KEY, context.host));
        }
    }

}
