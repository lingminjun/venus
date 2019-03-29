package com.venus.apigw.common;

import com.venus.apigw.config.GWConfig;
import com.venus.apigw.consts.ConstField;
import com.venus.apigw.document.entities.CallState;
import com.venus.apigw.document.entities.Response;
import com.venus.esb.*;
import com.venus.esb.lang.*;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.*;
import java.util.Map.Entry;

/**
 * 一律同化get和post请求
 */
public abstract class BaseServlet extends HttpServlet {

    private static final   long                 serialVersionUID         = 1L;
    private static final   Logger               logger                   = LoggerFactory.getLogger(BaseServlet.class);

    public static final   String               HEADER_ORGIN             = "Access-Control-Allow-Origin";
    public static final   String               HEADER_METHOD            = "Access-Control-Allow-Method";
    public static final   String               HEADER_CREDENTIALS       = "Access-Control-Allow-Credentials";
    public static final   String               HEADER_METHOD_VALUE      = "POST, GET, OPTIONS, PUT, DELETE, HEAD";
    public static final   String               HEADER_CREDENTIALS_VALUE = "true";

    public static final   String               CONTENT_TYPE_XML         = "application/xml; charset=utf-8";
    public static final   String               CONTENT_TYPE_JSON        = "application/json; charset=utf-8";
    public static final   String               CONTENT_TYPE_JAVASCRIPT  = "application/javascript; charset=utf-8";
    public static final   String               CONTENT_TYPE_PLAINTEXT   = "text/plain";


    //说明下，因为ESB完整性考虑，延后context初始化，但是其他情况，都提前初始化
    protected boolean notFeedContext = false;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        //get方法乱码问题解决：
        //修改tomcat服务器的server.xml文件，在Connector节点加入URIEncoding=”UTF-8”
        //如下：
        processRequest(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        processRequest(request, response);
    }


    protected final boolean isAddressIP(String host) {
        if (host == null) {
            return false;
        }
        String[] ss = host.toLowerCase().split("\\.");
        if (ss.length == 4
                && ESBT.isDigit(ss[0])
                && ESBT.isDigit(ss[1])
                && ESBT.isDigit(ss[2])
                && ESBT.isDigit(ss[3])) {
            return true;
        }
        return false;
    }

    // 获取主域名，二级域名，如www.baidu.com，主域名则为 baidu.com
    protected final String getMainHost(HttpServletRequest request) {
        // www.baidu.com
        String host = request.getHeader("Host");
        if (host != null) {
            // 转小写
            host = host.toLowerCase();

            // 截取端口
            {
                int idx = host.lastIndexOf(":");
                if (idx > 0 && idx < host.length()) {
                    host = host.substring(0,idx);
                }
            }

            // 判断是纯ip
            if (!isAddressIP(host)) {
                String[] ss = host.split("\\.", Integer.MAX_VALUE);
                if (ss.length >= 2) {
                    host = ss[ss.length - 2] + "." + ss[ss.length - 1];
                }
            }
        }
        return host;
    }

    // 获取Origin主域名，二级域名，如www.baidu.com，主域名则为 baidu.com
    protected final String getMainOrigin(HttpServletRequest request) {
        // https://www.baidu.com
        String origin = request.getHeader("Origin");
        String host = null;
        if (origin != null) {
            // 转小写
            origin = origin.toLowerCase();

            // 截取头
            {
                int idx = origin.indexOf("//");
                if (idx > 0 && idx + 2 < origin.length()) {
                    origin = origin.substring(idx + 2);
                }
            }
            // 截取端口
            {
                int idx = origin.lastIndexOf(":");
                if (idx > 0 && idx < origin.length()) {
                    origin = origin.substring(0,idx);
                }
            }

            // 判断是纯ip
            if (!isAddressIP(origin)) {
                String[] ss = origin.split("\\.", Integer.MAX_VALUE);
                if (ss.length >= 2) {
                    host = ss[ss.length - 2] + "." + ss[ss.length - 1];
                } else {
                    host = origin;
                }
            } else {
                host = origin;
            }

        }
        return host;
    }

    protected final void setResponseCrossHeader(HttpServletRequest request, HttpServletResponse response) {
        String origin = getMainOrigin(request);
        String host = getMainHost(request);
        //默认支持三级域名跨域 如www.baidu.com，则支持.baidu.com方式
        if (origin != null && origin.equals(host)) {
            response.setHeader(HEADER_ORGIN, request.getHeader("Origin"));//
            response.addHeader(HEADER_METHOD, HEADER_METHOD_VALUE);
            response.setHeader(HEADER_CREDENTIALS, HEADER_CREDENTIALS_VALUE);
        }
        // 安全性控制 （防止免登进入其他网站域名，拥有用户权限）
        else if (origin != null && GWConfig.getInstance().getOriginWhiteList().containsKey(origin)) {
            response.setHeader(HEADER_ORGIN, request.getHeader("Origin"));//
            response.addHeader(HEADER_METHOD, HEADER_METHOD_VALUE);
            response.setHeader(HEADER_CREDENTIALS, HEADER_CREDENTIALS_VALUE);
        }
    }

    protected final void out(ESBAPIContext context, ESBException e, List<ESBResponse> result, HttpServletResponse response) throws IOException, ESBException {

        // 转发服务，直接返回
        if (context.isTransmit()) {//转发服务，直接设置，一般会配合ESBRawString使用
            if (result != null && result.size() > 1) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Bad Request! Transmit request can’t combination");
            }
            String type = context.getExt(ESBAPIContext.CUSTOM_CONTENT_TYPE_KEY);
            if (type == null || type.length() == 0) {
                type = CONTENT_TYPE_JSON;
            }
            response.setContentType(type);

            if (e != null) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Bad Request");
            } else {//直接返回数据
                ESBResponse res = result.get(0);
                if (res.exception != null) {
                    response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Bad Request");
                } else {
                    byte[] buf = (res.result == null ? "" : res.result).getBytes(ESBConsts.UTF8);
                    response.getOutputStream().write(buf, 0, buf.length);
                }
            }
            return;
        }

        Response apiResponse = new Response();
        apiResponse.systime = System.currentTimeMillis();
        if (e != null) {
            apiResponse.code = e.getCode();
            apiResponse.msg = e.getLocalizedMessage();
            apiResponse.domain = e.getDomain();
        } else {
            apiResponse.code = 0;
        }

        if (context.cid != null) {
            apiResponse.cid = context.cid;
        }

        apiResponse.stateList = new ArrayList<>();
        if (result != null && result.size() > 0) {
            for (int i = 0; i < result.size(); i++) {
                ESBResponse res = result.get(i);
                CallState state = new CallState();
                if (res.exception != null) {
                    state.code = res.exception.getCode();
                    state.msg = res.exception.getLocalizedMessage();
                    state.domain = res.exception.getDomain();
                } else {
                    state.code = 0;
                    state.length = (res.result == null || res.result.length() == 0) ? 2 : res.result.length();
                }
                apiResponse.stateList.add(state);
            }
        }


        ServletOutputStream output = response.getOutputStream();

        //暂时还未支持
        if (ESBAPIContext.XML_CONTENT.equals(context.contentType)) {

//            output.write(ConstField.XML_START);
//            apiResponseSerializer.toXml(apiResponse, output, true);
//            apiContext.outputStream.writeTo(output);
//            output.write(ConstField.XML_END);

        } else {// 默认json，text同样转json
            ESBAPISerializer serializer = new ESBAPIJsonSerializer();

            // ajax请求
            if (context.jsonpCallback != null) {
                output.write(context.jsonpCallback.getBytes(ESBConsts.UTF8));
                output.write(ConstField.JSONP_START);
            }

            // 状态数据
            output.write(ConstField.JSON_START);
            output.write(serializer.serialized(apiResponse).getBytes(ESBConsts.UTF8));

            //填充数据
            output.write(ConstField.JSON_CONTENT);
            if (result != null && result.size() > 0) {
                for (int i = 0; i < result.size(); i++) {
                    if (i > 0) {
                        output.write(ConstField.JSON_SPLIT);
                    }
                    ESBResponse res = result.get(i);
                    if (res.result == null || res.result.length() == 0) {
                        output.write(ConstField.JSON_EMPTY);
                    } else {
                        output.write(res.result.getBytes(ESBConsts.UTF8));
                    }
                }
            }
            output.write(ConstField.JSON_END);

            // ajax请求
            if (context.jsonpCallback != null) {
                output.write(ConstField.JSONP_END);
            }

        }
    }

    public static String readPostBody(HttpServletRequest request) {
        String body = "";
        try {
            InputStream bd = request.getInputStream();

            String encoding = request.getCharacterEncoding();
            if (ESBT.isEmpty(encoding)) {
                encoding = ESBConsts.UTF8_STR;
            }
            body = IOUtils.toString(bd, encoding);
        } catch (Exception e) {
            e.printStackTrace();
            if (GWConfig.isDebug) {
                logger.error("read post body failed.", e);
            }
        }
        return body;
    }

    protected final void setResponseContentType(HttpServletRequest request,HttpServletResponse response, ESBAPIContext context) {
        // 设置response 的content type
        if (context.transmit) {//转发服务，直接设置，一般会配合ESBRawString使用
            String type = context.getExt(ESBAPIContext.CUSTOM_CONTENT_TYPE_KEY);
            if (type == null || type.length() == 0) {
                type = CONTENT_TYPE_JSON;
            }
            response.setContentType(type);
        } else if (ESBAPIContext.XML_CONTENT.equals(context.contentType)) {
            response.setContentType(CONTENT_TYPE_XML);
        } else if (ESBAPIContext.TEXT_CONTENT.equals(context.contentType)) {
            response.setContentType(CONTENT_TYPE_PLAINTEXT);
        } else {// 默认json
            if (context.jsonpCallback == null) {
                response.setContentType(CONTENT_TYPE_JSON);
            } else {//ajax请求支持
                response.setContentType(CONTENT_TYPE_JAVASCRIPT);
            }
        }
    }


    /**
     * 执行web请求
     */
    private final void processRequest(HttpServletRequest request, HttpServletResponse response) throws IOException {
        ESBAPIContext.remove();// 防止取脏数据
        ESBAPIContext context = ESBAPIContext.context();//走新的流程
        try {
            try {
                request.setCharacterEncoding(ESBConsts.UTF8_STR);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }

            //取所有参数
            Map<String,String> params = parseRequestParams(request);
            Map<String,String> header = parseRequestHeaders(request);
            Map<String,ESBCookie> cookies = parseRequestCookies(request);

            //将数据填充到context中
            if (!notFeedContext) {
                ESBAPIContext.fill(context, params, header, cookies, null);
                //自动注入did
                context.autoInjectDeviceToken();
            }

            //埋点需要，记录请求URL
            context.putTempExt("the_request_url", getRequestUrl(request));
            context.putTempExt("the_request_protocol", request.getScheme());
            context.putTempExt("the_request_address", request.getLocalAddr()+":"+request.getLocalPort());
            context.putTempExt("the_request_method", request.getMethod());

            //解决H5跨域问题
            setResponseCrossHeader(request,response);

            //设置响应头
            setResponseContentType(request, response, context);

            //执行调用
            processCall(context,request,response,params,header,cookies);
        } catch (Throwable e) {
            logger.error("请求异常",e);
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "GW Bad Request");
        }
    }

    // 参数都处理好了
    protected void processCall(ESBAPIContext context, HttpServletRequest request, HttpServletResponse response, Map<String,String> params, Map<String,String> header, Map<String,ESBCookie> cookies) throws IOException {
        try {

            // 转发调用
            List<ESBResponse> result = dispatchedCall(params, header, cookies, readPostBody(request));//

            // 返回前，设置cookie (必须在 getOutputStream 前设置)
            ESBCookieExts setc = context.cookies;
            if (setc != null && setc.size() > 0) {
                setResponseCookies(setc.map(),response);
            }

            //填充内容
            out(context,null, result, response);

        } catch (ESBException e) {
            // 访问被拒绝(如签名验证失败)
            try {
                // 服务一次，若设置cookie，同样修改
                ESBCookieExts setc = context.cookies;
                if (setc != null && setc.size() > 0) {
                    setResponseCookies(setc.map(),response);
                }

                out(context, e, null, response);
            } catch (ESBException e1) {
                logger.error("写请求异常",e1);
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Bad Request");
            }
        } catch (Throwable e) {
            logger.error("请求异常",e);
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Bad Request");
        }
    }

    abstract protected List<ESBResponse> dispatchedCall(Map<String,String> params, Map<String,String> header, Map<String,ESBCookie> cookies, String body) throws ESBException;

    /**
     * 根据客户端在Header或者Cookie中设定的目标dubbo服务的版本号或者url，绕过注册中心调用对应的dubbo服务，仅在DEBUG模式下允许使用
     */

    /**
     * 请求参数获取
     */
    protected final Map<String, String> parseRequestParams(HttpServletRequest request) {
        Map<String, String> params = new HashMap<String, String>();
        Enumeration<String> names = request.getParameterNames();
        if (names != null) {
            while (names.hasMoreElements()) {
                String name = names.nextElement();
                String value = request.getParameter(name);
                params.put(name, (value == null || value.length() == 0) ? "" : value);
            }
        }

        return params;
    }

    /**
     * 请求header获取
     */
    protected final Map<String, String> parseRequestHeaders(HttpServletRequest request) {
        //get request headers
        Map<String, String> map = new HashMap<String, String>();

        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String key = headerNames.nextElement();
            //过滤掉cookie
            if ("Cookie".equalsIgnoreCase(key)) {
                continue;
            }
            String value = request.getHeader(key);
            map.put(key, value);
        }

        return map;
    }

    /**
     * 请求cookie获取
     */
    protected final Map<String, ESBCookie> parseRequestCookies(HttpServletRequest request) {
        //get request headers
        Map<String, ESBCookie> map = new HashMap<String, ESBCookie>();

        // 优先使用 url 中的 userToken 和 deviceId
        Cookie[] cs = request.getCookies();
        if (cs != null) {
            for (Cookie c : cs) {
                ESBCookie ck = new ESBCookie();
                ck.domain = c.getDomain();
                ck.name = c.getName();
                try {
                    ck.value = URLDecoder.decode(c.getValue(), ESBConsts.UTF8_STR);
                } catch (UnsupportedEncodingException e) {
                    ck.value = c.getValue();
                }
                ck.path = c.getPath();
                ck.maxAge = c.getMaxAge();
                ck.secure = c.getSecure();
                ck.version = c.getVersion();
                ck.httpOnly = c.isHttpOnly();
                map.put(c.getName(), ck);
            }
        }

        return map;
    }

    public final String getRequestUrl(HttpServletRequest request) {
        StringBuilder sb = new StringBuilder(300);
        sb.append(request.getScheme());
        sb.append(request.getHeader("Host"));
        sb.append(request.getRequestURI());
        Map<String, String[]> params = request.getParameterMap();
        if (params != null) {
            sb.append("?");
            if (GWConfig.isDebug) {  // 开发环境下用于将打印到日志的url还原成能够直接放到浏览器请求的编码格式。
                try {
                    for (String key : params.keySet()) {
                        if (key != null) {
                            String[] vs = params.get(key);
                            sb.append(key);
                            sb.append("=");
                            sb.append(URLEncoder.encode(vs.length > 0 ? vs[0] : "", ESBConsts.UTF8_STR));
                            sb.append("&");
                        }
                    }
                } catch (UnsupportedEncodingException e) {
                    logger.error("URLEncoder encode the post data failad", e);
                }
            } else {
                for (String key : params.keySet()) {
                    if (key != null) {
                        String[] vs = params.get(key);
                        sb.append(key);
                        sb.append("=");
                        sb.append(vs.length > 0 ? vs[0] : "");
                        sb.append("&");
                    }
                }
            }
        }
        return sb.toString();
    }

    /**
     * 请求cookie获取
     */
    protected final void setResponseCookies(Map<String,ESBCookie> cookies, HttpServletResponse response) {
        if (cookies == null) {
            return;
        }
        for (Entry<String,ESBCookie> entry : cookies.entrySet()) {
            setResponseCookie(entry.getValue(),response);
        }
    }

    /**
     * 请求cookie获取
     */
    protected final void setResponseCookie(ESBCookie c, HttpServletResponse response) {
        try {
            Cookie cookie = new Cookie(c.name, (c.value == null || c.value.length() == 0)? "" : URLEncoder.encode(c.value, ESBConsts.UTF8_STR));
            cookie.setMaxAge(c.maxAge);
            cookie.setHttpOnly(c.httpOnly);
            cookie.setSecure(c.secure);
            if (!ESBT.isEmpty(c.path)) {
                cookie.setPath(c.path);
            }
            //tomcat8遇到问题 在tomcat_home/conf/context.xml添加配置：
            // <CookieProcessor className="org.apache.tomcat.util.http.LegacyCookieProcessor" />
            cookie.setDomain((c.domain == null || c.domain.length() == 0)? "" : c.domain);
            response.addCookie(cookie);
        } catch (Throwable e) {e.printStackTrace();}
    }
}
