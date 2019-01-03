package com.venus.apigw.manager;

import com.alibaba.fastjson.JSON;
import com.venus.apigw.common.BaseServlet;
import com.venus.apigw.db.APIPojo;
import com.venus.apigw.db.DBUtil;
import com.venus.apigw.db.StampPojo;
import com.venus.esb.ESB;
import com.venus.esb.ESBAPIContext;
import com.venus.esb.ESBAPIInfo;
import com.venus.esb.ESBResponse;
import com.venus.esb.config.ESBConfigCenter;
import com.venus.esb.lang.*;
import com.venus.esb.sign.ESBAPIAlgorithm;
import com.venus.esb.sign.ESBAPISignature;
import com.venus.esb.sign.utils.Signable;
import com.venus.esb.utils.HTTP;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * Description: 以下接口支持：
 *              GET 1.1 gw.api 查询最近更新记录，并带出明细
 *              GET 1.2 gw.api?API={API} 查询某个API更新记录
 *
 *                  //get接口返回stamps
 *
 *
 *
 *              POST 2.1 gw.api?ROLLBACK_STAMP={ROLLBACK_STAMP} 批量回滚节点
 *              POST 2.2 gw.api?ROLLBACK_STAMP={ROLLBACK_STAMP}&API={API} 单个api回滚
 *              POST 2.3 gw.api?API_INFO={API_INFO} 更新接口 API_INFO为json_array
 *              POST 2.4 gw.api?REFRESH_API={REFRESH_API}&API={API} 批量回滚节点
 *                  //如果你配置了GW_IP参数（多个ip逗号分开,最好配置端口，如127.0.0.1:8080,127.0.0.2:8089），则会更新所有网关的接口
 *                  //post接口返回更新的apis
 * User: lingminjun
 * Date: 2018-10-07
 * Time: 上午9:21
 */
@WebServlet("/gw.do") //方便线上路径禁用，/gw.*不支持访问(gw.do、gw.html)
public class APIManagerServlet extends BaseServlet {
    public static final String API_KEY = "API";
    public static final String API_INFO_KEY = "API_INFO";
    public static final String REFRESH_API_KEY = "REFRESH_API";
    public static final String ROLLBACK_STAMP = "ROLLBACK_STAMP";
//    public static final String GW_IPS_KEY = "GW_IP";

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

    public void addRSASignature(Map<String,String> params) {
        // 写死
        String sm = ESBAPIAlgorithm.RSA.name();

        params.put(ESBSTDKeys.SIGNATURE_METHOD_KEY,sm);
        StringBuilder builder = ESBAPISignature.getSortedParameters(params);
        Signable signature = ESBAPISignature.getSignable(sm, ESBConfigCenter.instance().getPubRSAKey(),ESBConfigCenter.instance().getPriRSAKey());
        String sign = signature.sign(builder.toString().getBytes(ESBConsts.UTF8));
        params.put(ESBSTDKeys.SIGN_KEY,sign);
    }

    // 更新与回滚，必须验证RSA私钥签名
    //
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        ESBAPIContext context = ESBAPIContext.context();
        context.contentType = ESBAPIContext.JSON_CONTENT;
        context.arithmetic = ESBAPIAlgorithm.RSA.name();

        Map<String,String> params = parseRequestParams(req);

        if (!checkRSASignature(params)) {
            try {
                out(context, ESBExceptionCodes.SIGNATURE_ERROR("私钥验证不通过"), null, resp);
            } catch (ESBException e) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Bad Request");
            }
            return;
        }

        String thestamp = params.get(ROLLBACK_STAMP);
        String refresh = params.get(REFRESH_API_KEY);
//        String ips = params.get(GW_IPS_KEY);
        List<String> sels = new ArrayList<>();
        boolean isRollback = false;
        boolean isRefresh = false;
        if (thestamp != null && thestamp.length() > 0) {
            isRollback = true;
            String api = params.get(API_KEY);
            sels.addAll(doRollback(thestamp,api));
        } else if (refresh != null && refresh.length() > 0) {
            isRefresh = true;
            sels.addAll(doRefreshAPI(refresh));
        } else {
            //更新接口
            String apiInfo = params.get(API_INFO_KEY);
            if (apiInfo == null) {
                apiInfo = params.get(API_KEY);
            }

            sels.addAll(doUpdateAPIInfos(apiInfo));
        }

        List<ESBResponse> results = new ArrayList<>();
        ESBResponse success = new ESBResponse();
        String json = JSON.toJSONString(sels);
        success.result = "{\"success\":true,\"apis\":" + json + "}";
        results.add(success);

        if (sels.size() > 0) {
            if (isRollback) {
                logger.info("注意:回滚接口" + json);
            } else if (isRefresh) {
                logger.info("注意:刷新接口" + json);
            } else {
                logger.info("注意:更新接口" + json);
            }

            //通知其他接口更新
            List<String> ips = APIManager_New.shared().getHosts();
            if (ips != null && ips.size() > 0) {
                //int port = req.getRemotePort();
                //int port = req.getServerPort(); // 客户端请求端口，可能经过代理（顾先采用这个）
                String ip = APIManager_New.shared().currentClient();
                if (ip == null) {
                    int port = req.getLocalPort();    //不经过代理，直接请求服务（网段配置必须合理）
                    ip = req.getLocalAddr() + ":" + port;
                }
                noticeOtherGW(ips,sels,ip);
            }
        }

        try {
            out(context,null,results,resp);
        } catch (ESBException e) {
            logger.error("未知错误：", e);
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Bad Request");
            return;
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        ESBAPIContext context = ESBAPIContext.context();
        context.contentType = ESBAPIContext.JSON_CONTENT;
        context.arithmetic = ESBAPIAlgorithm.MD5.name();

        Map<String,String> params = parseRequestParams(request);

        String api = params.get(API_KEY);
        List<StampPojo> stamps = queryLatestVersion(api);

        List<ESBResponse> results = new ArrayList<>();
        ESBResponse success = new ESBResponse();
        String json = JSON.toJSONString(stamps,ESBConsts.FASTJSON_SERIALIZER_FEATURES);
        success.result = "{\"success\":true,\"stamps\":" + json + "}";
        results.add(success);

        try {
            out(context,null,results,response);
        } catch (ESBException e) {
            logger.error("未知错误：", e);
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Bad Request");
            return;
        }
    }

    @Override
    protected List<ESBResponse> dispatchedCall(Map<String, String> params, Map<String, String> header, Map<String, ESBCookie> cookies, String body) throws ESBException {
        return null;
    }



    // 通知其他网关更新
    private void noticeOtherGW(List<String> ips,List<String> apis, String tip) {
        if (apis == null || apis.size() == 0) {
            return;
        }

        StringBuilder sels = new StringBuilder();
        for (String api : apis) {
            if (sels.length() > 0) {
                sels.append(",");
            }
            sels.append(api);
        }

        Map<String,String> params = new HashMap<>();
        params.put(REFRESH_API_KEY,sels.toString());
        addRSASignature(params);

        // 遍历更新网关
        for (String ip : apis) {
            if (ip == null || ip.length() == 0) {
                continue;
            }
            //排除当前主机
            if (ip.equals(tip)) {
                continue;
            }

            String url = "http://" + ip + "/gw.do";

            try {
                String result = HTTP.post(url,params);
                System.out.println("刷新网关(" + ip + ")更新返回【"+result+"】");
            } catch (Throwable e) {
                logger.error("刷新网关(" + ip + ")接口[" + sels.toString() + "]报错", e);
            }
        }
    }

    //查找最近的更新版本
    private List<StampPojo> queryLatestVersion(String api) {
        if (api == null || api.length() == 0) {
            return DBUtil.latestUpdateStamp();
        } else {
            return DBUtil.latestUpdateStamp(api);
        }
    }

    // 批量更新
    private List<String> doRefreshAPI(String api) {
        System.out.println("注意，有更新动作！！！！");
        String[] ss = api.split(",");
        List<String> sels = new ArrayList<>();
        for (String sel : ss) {
            ESB.bus().refresh(sel);
            sels.add(sel);
        }
        return sels;
    }

    // 回滚（单个或者批量）
    private List<String> doRollback(String thestamp, String api) {
        List<String> sels = new ArrayList<>();
        System.out.println("注意，有回滚动作！！！！");
        if (api == null || api.length() == 0) {
            List<String> selectors = DBUtil.rollback(thestamp);
            for (String sel : selectors) {
                ESB.bus().refresh(sel);
            }
            sels.addAll(selectors);
        } else {
            String selector = DBUtil.rollback(thestamp,api);
            if (selector != null) {
                ESB.bus().refresh(selector);
                sels.add(selector);
            }

        }
        return sels;
    }

    // 批量更新
    private List<String> doUpdateAPIInfos(String apiInfo) {
        List<String> sels = new ArrayList<>();
        System.out.println("注意，有发布动作！！！！");
        //
        if (apiInfo != null && apiInfo.length() > 0) {

            // 批量更新
            List<ESBAPIInfo> list = JSON.parseArray(apiInfo, ESBAPIInfo.class);

            if (list.size() > 0) {
                List<String> selectors = DBUtil.upapis(list);
                for (String sel : selectors) {
                    ESB.bus().refresh(sel);
                }
                sels.addAll(selectors);
            }
        }
        return sels;
    }

}
