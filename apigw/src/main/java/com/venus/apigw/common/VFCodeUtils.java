package com.venus.apigw.common;

import com.alibaba.dubbo.common.json.JSON;
import com.alibaba.dubbo.common.json.JSONObject;
import com.venus.apigw.config.GWConfig;
import com.venus.esb.lang.ESBConsts;
import com.venus.esb.utils.HTTP;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 * Created by lingminjun on 17/6/15.
 */
public final class VFCodeUtils {
    private static final Logger logger = LoggerFactory.getLogger(VFCodeUtils.class);
    /**
     * 校验验证码
     * https://checkcode1.venus.com/get.do?type=reg&sessionID=f65cc1b992882954aeb2c11b9863dc73
     * http://checkcode1.venus.com/check.do?type=reg&sessionID=f65cc1b992882954aeb2c11b9863dc73&code=efedd
     * https://www.venus.com/m.api?_mt=user.webLogin&accountId=15673886363&type=MOBILE&password=18e47d0d649b11dc6ee6c49da5ee9154&vfCode=type%3Dreg%26sessionID%3D82582cd599c3cfed503c7d95c555906d%26code%3D24fg&_sm=md5&_aid=1&_sig=816a681d4cd7bfb69cf45714375c074d
     * @param vfcode
     * @return
     */
    public static boolean checkVfCode( String vfcode, boolean isNew) {

        //验证码的验证
        if (vfcode == null || vfcode.isEmpty()) {
            return false;
        }

        if (isNew) {
            int index = vfcode.indexOf("code=");
            if (index >= 0) {
                try {
                    vfcode = vfcode.substring(0, index) + "code=" + URLEncoder.encode(vfcode.substring(index + 5), ESBConsts.UTF8_STR);
                } catch (UnsupportedEncodingException e) {
                    return false;
                }
            } else {
                logger.error("[验证码]输入有误，new。vfcode={}", vfcode);
                return false;
            }
            String checkResp = "";
            try {
                checkResp = HTTP.get(GWConfig.getInstance().getVfCodeCheckUrl() + "?" + vfcode, null);
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (checkResp.equals("SUCCESS.\n")) {
                return true;
            }
        } else {
            String requestBody = "action=check&key=&" + vfcode;
            try {
                String checkResp = HTTP.get(GWConfig.getInstance().getVfCodeCheckUrl() + "?" + requestBody, null);
                JSONObject ob = (JSONObject) JSON.parse(checkResp);
                String resp = ob.getString("succeeded");
                if (resp.equals("true")) {
                    return true;
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        return false;
    }
}
