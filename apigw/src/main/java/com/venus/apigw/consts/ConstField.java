package com.venus.apigw.consts;

import com.venus.esb.lang.ESBConsts;

import java.nio.charset.Charset;

public class ConstField {
    public static final Charset UTF8                 = ESBConsts.UTF8;
    public static final Charset ASCII                = ESBConsts.ASCII;
    public static final byte[]  XML_START            = "<xml>".getBytes(UTF8);
    public static final byte[]  XML_END              = "</xml>".getBytes(UTF8);
    public static final byte[]  JSON_START           = "{\"stat\":".getBytes(UTF8);
    public static final byte[]  JSON_CONTENT         = ",\"content\":[".getBytes(UTF8);
    public static final byte[]  XML_EMPTY            = "<empty/>".getBytes(ConstField.UTF8);
    public static final byte[]  JSON_SPLIT           = ",".getBytes(UTF8);
    public static final byte[]  JSON_END             = "]}".getBytes(UTF8);
//    public static final byte[]  JSON_CONTENT_V2         = ",\"content\":".getBytes(UTF8);
//    public static final byte[]  JSON_END_V2             = "}".getBytes(UTF8);
    public static final byte[]  JSON_EMPTY           = "{}".getBytes(UTF8);
    public static final byte[]  JSONP_START          = "(".getBytes(UTF8);
    public static final byte[]  JSONP_END            = ");".getBytes(UTF8);
    public static final String  ERROR_CODE_EXT       = "net.pocrd.ERROR_CODE_EXT";
    public static final String  SET_COOKIE_TOKEN     = "net.pocrd.SET_COOKIE_TOKEN";
    public static final String  SET_COOKIE_STOKEN    = "net.pocrd.SET_COOKIE_STOKEN";
    public static final String  SET_COOKIE_USER_INFO = "net.pocrd.SET_COOKIE_USER_INFO";
    public static final String  REDIRECT_TO          = "net.pocrd.REDIRECT_TO";
    public static final String  CREDIT               = "net.pocrd.CREDIT";
    public static final String  MSG                  = "net.pocrd.MSG";
    public static final String  SERVICE_LOG          = "net.pocrd.SERVICE_LOG";
}
