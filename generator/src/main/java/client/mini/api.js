'use strict';

import Promise from '../libs/es6-promise.min'

const config = require('../config.js');
const MD5 = require('../libs/md5')
const _ = require('../libs/underscore')
const HOST_URI = config.HOST_URI;

var DEFAULT_REQUEST_HEADER = {
    _aid: config.FENGQU_APP_ID,
    _sm: 'md5',
    _ch: config.CHANNEL_ID
};

/**
 * @function sign
 * @param  {Map}      params           参与请求签名的实体
 * @param  {Boolean}  isForceUserLogin 是否需要强制登陆
 */
function sign(params, security_type, isForceUserLogin) {
    var map = {
        'None': function(data, force) {
            return _.extend(data, {
                _sig: encrypt(data, config.APPEND_WORD)
            });
        },
        'RegisteredDevice': function(data, force) {
            var deviceInfo = wx.getStorageSync('deviceInfo');
            var deviceSecret = deviceInfo.deviceSecret;
            return _.extend(data, {
                _sig: encrypt(data, deviceSecret)
            });
        },
        'UserLogin': function(data, force) {
            var deviceInfo = wx.getStorageSync('deviceInfo');
            var deviceSecret = deviceInfo.deviceSecret;
            return _.extend(data, {
                _sig: encrypt(data, deviceSecret)
            });
        }
    };
    if (_.isFunction(map[security_type])) {
        // 过滤所有的undefined和null的数据
        _.each(params, function(value, key, list) {
            if (_.isUndefined(value) || _.isNull(value)) {
                delete params[key];
            }
        });
        // 将参数附加上必须要传递的标记
        var required = _.extend(params, DEFAULT_REQUEST_HEADER);
        var deviceInfo = wx.getStorageSync('deviceInfo');
        if (deviceInfo && deviceInfo.deviceId) {
            required = _.extend(required, {
                _did: deviceInfo.deviceId,
                _dtk: deviceInfo.deviceToken
            });
        }
        var token = wx.getStorageSync('token');
        if (token) {
            required = _.extend(required, {
                _tk: token
            });
        }
        // 做加密生产_sig对象
        var _sig = map[security_type].call(this, required, isForceUserLogin);
        // 返回数据可以直接做请求
        return _.extend(required, _sig);
    } else {
        return params;
    }
}

/**
 * @function encrypt
 * @param  {Map}    params      需要加密的参数
 * @param  {String} appendWord  加言
 */
function encrypt(params, appendWord) {
    var arr = [];

    // 将Map变成Array，使用key=value的方式进行拼接
    _.each(params, function(value, key) {
        arr.push(key + '=' + value);
    });

    // 以ascii进行排序
    arr.sort();

    // 将队列拼接成String
    var str = arr.join('');
    str = str + appendWord;
    // 做md5加密
    return MD5.md5(str);
}

//URL参数编码
function obj2uri (obj) {
    return Object.keys(obj).map(function (k) {
        return k + '=' + encodeURIComponent(obj[k]);
    }).join('&');
}

//获取带验签的参数字符串
function getSignParams(method, params, security_type, isForceUserLogin) {
    var required = _.extend(params,  {
        _mt: method
    });
    return sign(params, security_type, isForceUserLogin);
}
/**
 * 发送get 请求
 * @param method 请求方法
 * @param param 参数，可选
 * @param showLog 是否打印日志
 * @param showLoading 是否显示加载框
 * @param showError 是否显示错误框
 * @returns {Promise}
 */
export function getRequest(host = HOST_URI, method, params = {}, security_type, isForceUserLogin, showLog = false, showLoading = true, showError = true) {
    return request('GET', host, method, params, security_type, isForceUserLogin, showLog, showLoading, showError);
}
/**
 * 发送POST请求
 * @param method 请求方法
 * @param param 参数，可选
 * @param showLog 是否打印日志
 * @param showLoading 是否显示加载框
 * @param showError 是否显示错误框
 * @returns {Promise}
 */
export function postRequest(host = HOST_URI, method, params = {}, security_type, isForceUserLogin, showLog = false, showLoading = true, showError = true) {
    return request('POST', host, method, params, security_type, isForceUserLogin, showLog, showLoading, showError);
}
/**
 * 接口请求基类方法
 * @param method 请求方法
 * @param relativeUrl 相对路径
 * @param param 参数，可选
 * @param showLog 是否打印日志
 * @param showLoading 是否显示加载框
 * @param showError 是否显示错误框
 * @returns {Promise}
 */
function request(requestMethod, host, method, params, security_type, isForceUserLogin, showLog, showLoading, showError) {
    var data = getSignParams(method, params, security_type, isForceUserLogin)
    var formData = obj2uri(data)

    if (showLoading) wx.showLoading({title: '加载中',});
    if (showLog) {
        console.log('方法:', requestMethod);
        console.log('接口:', method);
        console.log("参数:", params);
    }

    return new Promise((resolve, reject) => {
        wx.request({
            url: host,
            method: requestMethod,
            header: {
                'content-type': 'application/x-www-form-urlencoded;charset=utf-8'
            },
            data: formData,
            success: function (res) {
                console.log("服务器返回数据：", res.data);
                if (showLoading) wx.hideLoading();
                var content = res.data.content;
                var code = res.data.stat.code;
                if (code == 0) {
                    var stateCode = res.data.stat.stateList[0].code;
                    if (stateCode == 0) {
                        resolve(content);
                    } else {
                        var msg = res.data.stat.stateList[0].msg;
                        if (showError) wx.showToast({title: msg, duration: 2000, image: '/images/warn.png'});
                        reject(msg);
                    }
                } else if (code == -300 || code == -360) {
                    //登录接口中报 token 错误 或 token 失效
                    console.error('token 错误 或 token 失效')
                } else if (code == -180) {
                    //token错误
                    var msg = '错误：-180';
                    console.log("请求失败：", msg);
                    reject(msg);
                    if (showError) wx.showToast({title: msg, duration: 2000, image: '/images/warn.png'});
                } else {
                    var msg = '请求错误';
                    if (res.data.stat.stateList && res.data.stat.stateList.length > 0){
                        msg = res.data.stat.stateList[0].msg;
                    }
                    console.log("请求失败：", msg);
                    reject(msg);
                    if (showError) wx.showToast({title: msg, duration: 2000, image: '/images/warn.png'});
                }
            },
            fail: function(res) {
                if (showLoading) wx.hideLoading();
                reject(res.data);
                if (showError) wx.showToast({title: '连接服务器失败', duration: 2000, image: '/images/warn.png'});
                console.log("连接服务器失败：", res.data);
            },
            complete: function(res) {
                
            }
        })
    });
}

/**
 * 下载文件
 * @param url 远程文件地址
 * @param showLoading 是否显示加载框
 * @returns {Promise}
 */
export function download(url = "", showLoading = true) {
    if (showLoading) wx.showLoading({title: '加载中',});
    return new Promise((resolve, reject) => {
        wx.downloadFile({
            url: url,
            success(res) {
                resolve(res.tempFilePath);
            },
            fail(res){
                reject(res);
            },
            complete(){
                if (showLoading) wx.hideLoading();
            }
        })
    });
}

/**
 * 上传文件
 * todo【注意】请确认上传路径
 * @param resPath 文件路径
 * @param showLoading 是否显示加载框
 * @returns {Promise}
 */
export function upload(resPath, showLoading = true) {
    if (showLoading) wx.showLoading({title: '资源上传中',});
    return new Promise((resolve, reject) => {
        wx.uploadFile({
            url: rootUrl + "/user/upload",
            filePath: resPath,
            name: 'file',
            success(res){
                console.log(res);
                let data = JSON.parse(res.data);
                if (data.status && data.status.succeed == 1) {
                    let voiceUrl = data.data.link;
                    resolve(voiceUrl);
                } else {
                    reject(res.data);
                }
            },
            fail(error){
                reject(error);
            },
            complete(){
                if (showLoading) wx.hideLoading();
            }
        });
    });
}

module.exports = {
    //获取host
    getFengquHost: function() {
        return config.HOST_URI;
    },
    //发送GET请求
    getRequest: getRequest,
    //发送POST请求
    postRequest: postRequest
};