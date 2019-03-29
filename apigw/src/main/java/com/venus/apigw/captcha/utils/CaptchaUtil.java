package com.venus.apigw.captcha.utils;

import com.venus.apigw.captcha.Captcha;
import com.venus.apigw.captcha.GifCaptcha;
import com.venus.apigw.captcha.SpecCaptcha;
import com.venus.esb.ESBAPIContext;
import com.venus.esb.annotation.ESBAPI;
import com.venus.esb.config.ESBConfigCenter;
import com.venus.esb.lang.ESBConsts;
import com.venus.esb.lang.ESBException;
import com.venus.esb.lang.ESBT;
import com.venus.esb.utils.MD5;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.awt.*;
import java.io.IOException;

/**
 * 图形验证码工具类
 * Created by 王帆 on 2018-07-27 上午 10:08.
 */
public class CaptchaUtil {
    private static final String DEFAULT_SALT    = "m.captcha.123";
    private static final int DEFAULT_IMAGE_TYPE = 1; //gif
    private static final int DEFAULT_CODE_TYPE  = 2; //纯数字
    private static final int DEFAULT_LEN        = 4; //4个字符
    private static final int DEFAULT_WIDTH      = 100; //宽
    private static final int DEFAULT_HEIGHT     = 40; //高

    /**
     * 验证验证码
     *
     * @param salt    加盐
     * @param code    用户输入的验证码
     * @param session tupiansessuib
     * @return 是否正确
     */
    public static boolean ver(String salt, String code, String session) {
        try {
            return ESBAPIContext.verifyCaptcha(salt,code,session);
        } catch (ESBException e) {
            return false;
        }
    }

    /**
     * 输出验证码
     *
     * @param salt     加盐 （session中加盐，与业务参数一起验证）
     * @throws IOException IO异常
     */
    public static Captcha out(String salt)
            throws IOException {
        return out(salt, DEFAULT_LEN);
    }

    /**
     * 输出验证码
     *
     * @param salt     加盐 （session中加盐，与业务参数一起验证）
     * @param len      长度
     * @throws IOException IO异常
     */
    public static Captcha out(String salt, int len)
            throws IOException {
        return out(salt, DEFAULT_WIDTH, DEFAULT_HEIGHT, len);
    }

    /**
     * 输出验证码
     *
     * @param salt     加盐 （session中加盐，与业务参数一起验证）
     * @param len      长度
     * @param font     字体
     * @throws IOException IO异常
     */
    public static Captcha out(String salt, int len, Font font)
            throws IOException {
        return out(salt, DEFAULT_WIDTH, DEFAULT_HEIGHT, len, font);
    }

    /**
     * 输出验证码
     *
     * @param salt     加盐 （session中加盐，与业务参数一起验证）
     * @param width    宽度
     * @param height   高度
     * @param len      长度
     * @throws IOException IO异常
     */
    public static Captcha out(String salt, int width, int height, int len)
            throws IOException {
        return out(salt, width, height, len, null);
    }

    /**
     * 输出验证码
     *
     * @param salt     加盐 （session中加盐，与业务参数一起验证）
     * @param width    宽度
     * @param height   高度
     * @param len      长度
     * @param font     字体
     * @throws IOException IO异常
     */
    public static Captcha out(String salt, int width, int height, int len, Font font)
            throws IOException {
        return outCaptcha(salt, width, height, len, font, DEFAULT_IMAGE_TYPE, DEFAULT_CODE_TYPE);
    }

    /**
     * 输出验证码
     *
     * @param salt     加盐 （session中加盐，与业务参数一起验证）
     * @param width    宽度
     * @param height   高度
     * @param len      长度
     * @param font     字体
     * @param mType    图片类型 0：普通图片；1：gif图
     * @param cType    验证码类型 1：数字字母；2：数字；3：字母
     * @throws IOException IO异常
     */
    private static Captcha out(String salt, int width, int height, int len, Font font, int mType, int cType, HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        return outCaptcha(salt,width,height,len,font,mType,cType);
    }

    /**
     * 输出验证码
     *
     * @param salt     加盐 （session中加盐，与业务参数一起验证）
     * @param width    宽度
     * @param height   高度
     * @param len      长度
     * @param font     字体
     * @param mType    图片类型 0：普通图片；1：gif图
     * @param cType    验证码类型 1：数字字母；2：数字；3：字母
     * @throws IOException IO异常
     */
    private static Captcha outCaptcha(String salt, int width, int height, int len, Font font, int mType, int cType)
            throws IOException {
        Captcha captcha = null;
        if (mType == 0) {
            captcha = new SpecCaptcha(width, height, len);
        } else if (mType == 1) {
            captcha = new GifCaptcha(width, height, len);
        }
        if (font != null) {
            captcha.setFont(font);
        }
        captcha.setCharType(cType);

        // 处理算法 验证码 与 session关系
        if (ESBT.isEmpty(salt)) {
            salt = DEFAULT_SALT;
        }
        captcha.setSalt(salt);

        String session = ESBAPIContext.captchaSession(salt,captcha.text());

        captcha.setSession(session);

        return captcha;
    }

}
