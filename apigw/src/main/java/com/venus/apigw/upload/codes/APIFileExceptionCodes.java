package com.venus.apigw.upload.codes;

import com.venus.esb.lang.ESBException;

/**
 * Created by lingminjun on 17/4/22.
 */
public final class APIFileExceptionCodes {

    public final static String EXCEPTION_DOMAIN = "GW";

    public final static int UPLOAD_FILE_TOO_LARGE_CODE = -380;
    public static ESBException UPLOAD_FILE_TOO_LARGE(String reason) {
        return new ESBException("上传文件过大",EXCEPTION_DOMAIN,UPLOAD_FILE_TOO_LARGE_CODE,reason);
    }

    public final static int UPLOAD_FILE_NAME_ERROR_CODE = -390;
    public static ESBException UPLOAD_FILE_NAME_ERROR(String reason) {
        return new ESBException("上传文件名错误",EXCEPTION_DOMAIN,UPLOAD_FILE_NAME_ERROR_CODE,reason);
    }

    public final static int UPLOAD_FILE_LOSE_ERROR_CODE = -391;
    public static ESBException UPLOAD_FILE_LOSE_ERROR(String reason) {
        return new ESBException("上传文件丢失",EXCEPTION_DOMAIN,UPLOAD_FILE_LOSE_ERROR_CODE,reason);
    }
}
