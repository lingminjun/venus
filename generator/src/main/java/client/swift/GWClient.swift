//
//  GWClient.swift
//  Venus-API-GW(1.0.0) SwiftGenerator
//  swift 4.0
//
//  Created by lingminjun on 2018/10/13.
//  Copyright © 2018年 MJ Ling. All rights reserved.
//

import Foundation

// 防止反射无法生产数据
public protocol GWClientEntityInitialize {
    init(fill jsonObject: [String:Any]?)  // NS_DESIGNATED_INITIALIZER
}

// swift版请求端 venus-api-gw:https://github.com/lingminjun/venus
open class GWClient {
    public final class Consts {
        // application id 应用编号, 以XXX应用为例：pc端1, h5端2, iOS客户端3, android客户端4, 微信小程序5, 等等
        public static let AID_KEY = "_aid"
        
        // device id 设备标示符. 支持通过Cookie注入获取url中的值, cookie中存储负值
        public static let DID_KEY = "_did"
        
        // 第三方集成的身份标识(第三方集成情景下使用)
        public static let PID_KEY = "_pid"
        
        // user id 用户标示符
        public static let UID_KEY = "_uid"
        
        // 客户端应用安装渠道, 支持通过Cookie注入获取url中的值
        public static let CH_KEY = "_ch"
        
        // 来源,用于追踪引流拉新路径
        public static let SRC_KEY = "_src"
        
        // 自媒体营销平台 推广追踪
        public static let SMP_KEY = "_smp"
        
        // 设备指纹信息,可用于风控
        public static let DNA_KEY = "_dna"
        
        // user agent注入 不支持在url中使用该参数
//        public static let UA_KEY = "_ua"
        
        
        // client version name; version 客户端版本 : 1.0.0
        public static let CVN_KEY = "_cvn"
        
        // version 客户端版本号 : 10
        public static let CVC_KEY = "_cvc"
        
        // 调用时刻,客户端请求时间
        public static let MOENT_KEY = "_at"
        
        // format 返回值格式,取值为枚举SerializeType中的定义,取值范围JSON/XML
        public static let CONTENT_TYPE_KEY = "_ft"
        
        // Localization[l10n] 用于返回信息国际化. 兼容HTTP Header 'Accept-Language'. 可支持通过Cookie注入获取url中的值")
        public static let L10N_KEY = "_l10n"
        
        // user token 代表访问者身份,完成用户登入流程后获取
        public static let TOKEN_KEY = "_tk"
        
        // user token 中的exts数据,将会被在必要的场景传输,oss时参数传递
        public static let TOKEN_EXTS_KEY = "_tk_exts"
        
        // user secret token 只存放于web/h5站点的secret cookie中，用于在不同domain间传递csrfToken
        public static let SECRET_TOKEN_KEY = "_stk"
        
        // user info
        public static let USER_INFO_KEY = "_uinfo"
        
        // refresh token 刷新token需要，不能放入cookie
        public static let REFRESH_TOKEN_KEY = "_rtk"
        
        // device token 代表访问设备的身份,完成设备注册流程后获取
        public static let DEVICE_TOKEN_KEY = "_dtk"
        
        // temp token 临时验证权token
        public static let TEMP_TOKEN_KEY = "_ttk"
        
        // method 请求的方法名: domain.module.methodName 或者 module.methodName
        public static let SELECTOR_KEY = "_mt"
        
        // signature 参数字符串签名
        public static let SIGN_KEY = "_sig"
        
        // CAPTCHA:人机是被参数,需要以此来界定是否问人操作,而非机器
        public static let CAPTCHA_KEY = "_captcha"
        
        
        // signature method 签名算法 hmac,md5,sha1,rsa,ecc
        public static let SIGNATURE_METHOD_KEY = "_sm"
        
        // jsonp callback名 名字字母开头任意一个字母或数字或下划线,5到64位:^[A-Za-z]\\w{5,64} 【用于sso过程:必传参数】
        public static let JSONP_CALLBACK_KEY = "_cb"
        
        // sso token【仅仅用于sso过程--目标站请求:必传参数】
        public static let SSO_TOKEN_KEY = "_sso_tk"
        
        // sso to domain【仅仅用于sso过程--起始站请求:必传参数】
        public static let SSO_TO_DOMAIN_KEY = "_sso_domain"
        
        // sso to did【仅仅用于sso过程--起始站请求:必传参数】
        public static let SSO_TO_DID_KEY = "_sso_did"
        
        // sso to aid【仅仅用于sso过程--起始站请求:必传参数】
        public static let SSO_TO_AID_KEY = "_sso_aid"
        
        // 两种特殊的请求ESB验权支持
        public static let SSO_SPECIFIC_SELECTOR            = "esb.sso.ESBSpecial"
        public static let REFRESH_TOKEN_SPECIFIC_SELECTOR  = "esb.auth.ESBSpecial"
        
        // token过期标识
        public static let TOKEN_EXPIRED_CODE = -300
        public static let SUCCESS_CODE = 0
    }
    
    public final class Context {
        fileprivate static let security_l10n_key = "security.l10n.key"
        
        fileprivate static let security_device_token_key = "security.device.token.key"
        fileprivate static let security_device_id_key = "security.device.id.key"
        fileprivate static let security_device_key_key = "security.device.key.key"
        
        fileprivate static let security_user_token_key = "security.user.token.key"
        fileprivate static let security_user_refresh_key = "security.user.refresh.key"
        fileprivate static let security_user_expire_key = "security.user.expire.key"
        fileprivate static let security_user_id_key = "security.user.id.key"
        fileprivate static let security_user_key_key = "security.user.key.key"
        
        fileprivate static let security_secret_token_key = "security.secret.token.key"
        
        //当前登录是account环境
        fileprivate static let security_account_id_key = "security.account.id.key"
        
        // app信息
        public let aid:Int = 1 //application id
        public let cvn:String = Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? "0.0.1"
        public let cvc:Int = Bundle.main.infoDictionary?["CFBundleVersion"] as? Int ?? 1
        
        // base gw url
//        public let gwscheme:String = "https"
//        public let gwhost:String = "m.mymm.com"//参考venus-api-gw配置
//        public let gwport:Int = 0
        public let gwscheme:String = "http"
        public let gwhost:String = "127.0.0.1"//参考venus-api-gw配置
        public let gwport:Int = 8080
        public let gwpath:String = "m.api"
        
        // 中文(简体)：zh_CN；中文(繁体)：zh_TW；英文：en
        fileprivate var l10n:String = "zh_CN"
        
        // static key
        fileprivate var skey:String = "mm.static.key!" //参考venus-api-gw配置
        fileprivate var salgo:String = "md5" //统一md5
        
        // device id
        fileprivate var did:String = ""
        
        // device token
        fileprivate var dtoken:String = "" //device token
        fileprivate var dkey:String = ""
        
        // user info
        fileprivate var account_id:String = ""
        fileprivate var uid:String = ""
        
        // user token
        fileprivate var utoken:String = ""
        fileprivate var stoken:String = ""
        fileprivate var refresh:String = ""
        fileprivate var ukey:String = ""
        fileprivate var uexpire:Int = 0
        
        
        
        // singleton
        public static let shared = Context()
        
        private init() {
            l10n = UserDefaults.standard.string(forKey: Context.security_l10n_key) ?? "zh_CN"
                
            did = UserDefaults.standard.string(forKey: Context.security_device_id_key) ?? ""
            dtoken = UserDefaults.standard.string(forKey: Context.security_device_token_key) ?? ""
            dkey = UserDefaults.standard.string(forKey: Context.security_device_key_key) ?? ""
            
            uid = UserDefaults.standard.string(forKey: Context.security_user_id_key) ?? ""
            utoken = UserDefaults.standard.string(forKey: Context.security_user_token_key) ?? ""
            stoken = UserDefaults.standard.string(forKey: Context.security_secret_token_key) ?? ""
            refresh = UserDefaults.standard.string(forKey: Context.security_user_refresh_key) ?? ""
            ukey = UserDefaults.standard.string(forKey: Context.security_user_key_key) ?? ""
            uexpire = UserDefaults.standard.integer(forKey: Context.security_user_expire_key)
            
            account_id = UserDefaults.standard.string(forKey: Context.security_account_id_key) ?? ""
        }
        
        public func setL10n(l10n:String) {
            self.l10n = l10n.isEmpty ? "zh_CN" : l10n
            UserDefaults.standard.set(self.l10n, forKey: Context.security_l10n_key)
            UserDefaults.standard.synchronize()
        }
        
        public func setDevice(did:String, token:String, key:String) {
            self.did = did
            self.dtoken = token
            self.dkey = key
            
            UserDefaults.standard.set(did, forKey: Context.security_device_id_key)
            UserDefaults.standard.set(token, forKey: Context.security_device_token_key)
            UserDefaults.standard.set(key, forKey: Context.security_device_key_key)
            UserDefaults.standard.synchronize()
        }
        
        // 登录account情况（非user认证）
        public func setAccountToken(accountId:String, token:String, stoken:String, refresh:String, key:String, expire:Int = 0) {
            self.account_id = accountId
            UserDefaults.standard.set(accountId, forKey: Context.security_account_id_key)
            if self.uid.isEmpty && self.utoken.isEmpty {
                self.utoken = token
                self.stoken = stoken
                self.refresh = refresh
                self.ukey = key
                self.uexpire = expire
                
                UserDefaults.standard.set(token, forKey: Context.security_user_token_key)
                UserDefaults.standard.set(stoken, forKey: Context.security_secret_token_key)
                UserDefaults.standard.set(refresh, forKey: Context.security_user_refresh_key)
                UserDefaults.standard.set(key, forKey: Context.security_user_key_key)
                UserDefaults.standard.set(expire, forKey: Context.security_user_expire_key)
            }
            UserDefaults.standard.synchronize()
        }
        
        public func setToken(uid:String, token:String, stoken:String, refresh:String, key:String, expire:Int = 0) {
            self.uid = uid
            self.utoken = token
            self.stoken = stoken
            self.refresh = refresh
            self.ukey = key
            self.uexpire = expire
            
            UserDefaults.standard.set(uid, forKey: Context.security_user_id_key)
            UserDefaults.standard.set(token, forKey: Context.security_user_token_key)
            UserDefaults.standard.set(stoken, forKey: Context.security_secret_token_key)
            UserDefaults.standard.set(refresh, forKey: Context.security_user_refresh_key)
            UserDefaults.standard.set(key, forKey: Context.security_user_key_key)
            UserDefaults.standard.set(expire, forKey: Context.security_user_expire_key)
            UserDefaults.standard.synchronize()
        }
        
        public func getL10n() -> String {
            return l10n
        }
        
        public func getDid() -> String {
            return did
        }
        
        public func getUid() -> String {
            return uid
        }
        
        public func isLogin() -> Bool {
            return (!uid.isEmpty || !account_id.isEmpty) && !utoken.isEmpty
        }
    }
    
    // 认证权限
    public enum SecurityLevel: Int {
        //无认证
        case none = 0x00000000
        //设备认证
        case deviceAuth = 0x00000001
        //账号认证
        case accountAuth = 0x00000010
        //用户认证
        case userAuth = 0x00000100
        // 保密认证
        case secretAuth = 0x00001000
        // 复杂认证，三方自定义验证方式pid+秘钥
        case integrated = 0x00010000
        
        // 一次性认证使用
        case once = 0x00100000
        
        // 延长认证，仅仅用于refresh，请使用前面token的定义
        case extend = 0x01000000
        
        // 其他用途认证
        case other = 0x10000000
        
        //包含此权限
        func check(code:Int) -> Bool {
            return (self.rawValue & code) == self.rawValue
        }
    }
    
    // 实体基类
    
    public class Entity : GWClientEntityInitialize {
        public required init(fill jsonObject: [String : Any]?) {
            //
        }
    }
    
    public final class ESBToken : Entity {
        public var success:Bool = false
        public var token:String = ""
        public var stoken:String = ""
        public var refresh:String = ""
        public var key:String = ""
        public var expire:Int = 0
        public var scope:String = ""
        public var did:String = ""
        public var uid:String = ""
        public var user:String = ""
        
        public required init(fill jsonObject: [String : Any]?) {
            super.init(fill: jsonObject)
            if let json = jsonObject {
                success = (json["success"] as? Bool) ?? false
                token = (json["token"] as? String) ?? ""
                stoken = (json["stoken"] as? String) ?? ""
                refresh = (json["refresh"] as? String) ?? ""
                key = (json["key"] as? String) ?? ""
                expire = (json["expire"] as? Int) ?? 0
                scope = (json["scope"] as? String) ?? ""
                did = (json["did"] as? String) ?? ""
                uid = (json["uid"] as? String) ?? ""
                user = (json["user"] as? String) ?? ""
            }
        }
    }
    
    // 接口调用实体 此处因swift泛型类型限制，故没使用泛型，其他语言请使用泛型：class APICall<T extends Entity> { ... }
    open class APICall {
        fileprivate var selector:String = ""
        fileprivate var level:SecurityLevel = SecurityLevel.none
        
        // 实体结果
        fileprivate var result:Entity? = nil
        
        // 异常结果
        fileprivate var statusCode:Int = 0
        fileprivate var statusCodeDomain:String? = nil
        fileprivate var statusMessage:String? = nil
        
        public final func getSelector() -> String {
            return self.selector
        }
        
        public final func getLevel() -> SecurityLevel {
            return self.level
        }
        
        public final func getStatusCode() -> Int {
            return self.statusCode
        }
        
        public final func getStatusCodeDomain() -> String {
            return self.statusCodeDomain ?? ""
        }
        
        public final func getStatusMessage() -> String {
            return self.statusMessage ?? ""
        }
        
        public final func getResult<T: Entity>() -> T? {
            return result as? T ?? nil
        }
        
        // 子类重载（java直接使用泛型采用擦拭法将类型还原出来）
        public func getResultType() -> Entity.Type /*Swift.AnyClass*/ {
            return Entity.self
        }
        
        // 子类重载
        public func getQuery() -> [String:HTTP.Value] {
            let query:[String:HTTP.Value] = [:]
            return query
        }
        
        // 子类重载
        public func getHeaders() -> [String:String] {
            let headers:[String:String] = [:]
            return headers
        }
        
        // 子类重载
        public func getCookies() -> [String:String] {
            let cookie:[String:String] = [:]
            return cookie
        }
        
        //
        public init(selector:String, level:SecurityLevel) {
            self.selector = selector
            self.level = level
        }
    }
    
    public static func buildHTTPRequest<C: Sequence>(_ calls:C) -> HTTP.ERequest where C.Iterator.Element == APICall {
        var sel = ""
        var query:[String:HTTP.Value] = [:]
        var query1:[String:HTTP.Value] = [:]
        var headers:[String:String] = [:]
        var cookies:[String:String] = [:]
        var idx = 0
        var maxLevel = SecurityLevel.none
        for call in calls {
            if call.selector.isEmpty {
                continue
            }
            
            if !sel.isEmpty {
                sel += ","
            }
            sel += call.getSelector()
            
            // 取最大的权限
            if call.getLevel().rawValue > maxLevel.rawValue {
                maxLevel = call.getLevel()
            }
            
            //参数
            let qv = call.getQuery()
            for e in qv {
                query1[e.key] = e.value
                query["\(idx)_" + e.key] = e.value
            }
            
            //header
            let hd = call.getHeaders()
            for e in hd {
                headers[e.key] = e.value
            }
            
            //cookie
            let ck = call.getCookies()
            for e in ck {
                cookies[e.key] = e.value
            }
            
            idx += 1
        }
        
        // idx多个selector
        if idx <= 0 {
            query = query1
        }
        
        //添加selector参数
        query[Consts.SELECTOR_KEY] = HTTP.Value(sel)
        
        //根据maxLevel添加必要参数
        let key = GWClient.decorateContextInfo(query:&query, level:maxLevel)
        GWClient.signQuery(query: &query, key: key)
        
        let req = HTTP.ERequest()
        req.scheme = Context.shared.gwscheme
        req.host = Context.shared.gwhost
        req.port = Context.shared.gwport
        req.path = Context.shared.gwpath
        req.query = query
        req.headers = headers
        req.cookies = cookies
        return req
    }
    
    // 构建refresh请求
    public final class RefreshCall : APICall {
        init() {
            super.init(selector: Consts.REFRESH_TOKEN_SPECIFIC_SELECTOR, level: .extend)
        }
        public override func getResultType() -> GWClient.Entity.Type {
            return ESBToken.self
        }
    }
    
    public static func buildRefreshRequest() -> HTTP.ERequest {
        var query:[String:HTTP.Value] = [:]
        
        //添加selector参数
        query[Consts.SELECTOR_KEY] = HTTP.Value(Consts.REFRESH_TOKEN_SPECIFIC_SELECTOR)
        
        //根据maxLevel添加必要参数
        let key = GWClient.decorateContextInfo(query:&query, level:.extend)
        GWClient.signQuery(query: &query, key: key)
        
        
        let req = HTTP.ERequest()
        req.scheme = Context.shared.gwscheme
        req.host = Context.shared.gwhost
        req.port = Context.shared.gwport
        req.path = Context.shared.gwpath
        req.query = query
        req.exclusive = true
        return req
    }
    
    // 构建refresh请求
    //请求方信息:to domain
    //    var _sso_aid = GetQueryString('_sso_aid');
    //    var _sso_did = GetQueryString('_sso_did');
    //    var _sso_domain = GetQueryString('_sso_domain');
    public static func buildSSORequest(to aid:Int, _ did:String, _ domain:String) -> HTTP.ERequest {
        var query:[String:HTTP.Value] = [:]
        
        //sso信息
        query[Consts.SSO_TO_AID_KEY] = HTTP.Value(aid)
        query[Consts.SSO_TO_DID_KEY] = HTTP.Value(did)
        query[Consts.SSO_TO_DOMAIN_KEY] = HTTP.Value(domain)
        
        //添加selector参数
        query[Consts.SELECTOR_KEY] = HTTP.Value(Consts.SSO_SPECIFIC_SELECTOR)
        
        //根据maxLevel添加必要参数
        let key = GWClient.decorateContextInfo(query:&query, level:.once)
        GWClient.signQuery(query: &query, key: key)
        
        
        let req = HTTP.ERequest()
        req.scheme = Context.shared.gwscheme
        req.host = Context.shared.gwhost
        req.port = Context.shared.gwport
        req.path = Context.shared.gwpath
        req.query = query
        return req
    }
    
    // 添加环境信息
    private static func decorateContextInfo(query:inout [String:HTTP.Value], level:SecurityLevel) -> String {
        
        var key = Context.shared.skey
        if level == .extend {// 刷新utoken 需要utoken,stoken,refresh
            query[Consts.TOKEN_KEY] = HTTP.Value(Context.shared.utoken)
            query[Consts.SECRET_TOKEN_KEY] = HTTP.Value(Context.shared.stoken)
            query[Consts.REFRESH_TOKEN_KEY] = HTTP.Value(Context.shared.refresh)
            key = Context.shared.ukey
        } else if level == .once {//sso
            query[Consts.TOKEN_KEY] = HTTP.Value(Context.shared.utoken)
            query[Consts.SECRET_TOKEN_KEY] = HTTP.Value(Context.shared.stoken)
            key = Context.shared.ukey
        } else if level == .userAuth {//用户认证
            query[Consts.TOKEN_KEY] = HTTP.Value(Context.shared.utoken)
            key = Context.shared.ukey
        } else if level == .accountAuth {//账号认证
            query[Consts.TOKEN_KEY] = HTTP.Value(Context.shared.utoken)
            key = Context.shared.ukey
        } else if level == .deviceAuth {//设备认证
            key = Context.shared.dkey
        }
        
        //设备信息
        if !Context.shared.did.isEmpty {
            query[Consts.DID_KEY] = HTTP.Value(Context.shared.did)
        }
        if !Context.shared.dtoken.isEmpty {
            query[Consts.DEVICE_TOKEN_KEY] = HTTP.Value(Context.shared.dtoken)
        }
        
        //语言环境
        query[Consts.L10N_KEY] = HTTP.Value(Context.shared.l10n)
        
        // app info
        query[Consts.AID_KEY] = HTTP.Value(Context.shared.aid)
        query[Consts.CVN_KEY] = HTTP.Value(Context.shared.cvn)
        query[Consts.CVC_KEY] = HTTP.Value(Context.shared.cvc)
        
        return key
    }
    
    // 采用md5加签数据
    public static func signQuery(query:inout [String:HTTP.Value], key:String) {
        query[Consts.SIGNATURE_METHOD_KEY] = HTTP.Value(Context.shared.salgo)
        
        let str = HTTP.queryJoin(query: query, join: "", encode: false) + key
        let md5 = GWClient.md5(string: str)
        query[Consts.SIGN_KEY] = HTTP.Value(md5)
    }
    
    public static func md5(string:String) -> String {
        let context = UnsafeMutablePointer<CC_MD5_CTX>.allocate(capacity: 1)
        var digest = Array<UInt8>(repeating:0, count:Int(CC_MD5_DIGEST_LENGTH))
        CC_MD5_Init(context)
        CC_MD5_Update(context, string, CC_LONG(string.lengthOfBytes(using: String.Encoding.utf8)))
        CC_MD5_Final(&digest, context)
        context.deallocate()
        var hexString = ""
        for byte in digest {
            hexString += String(format:"%02x", byte)
        }
        return hexString
    }
    
    public final class CallState : SerializableEntity {
        public static func json_serialization(_ entity: GWClient.CallState) -> String {
            return ""
        }
        
        public static func json_deserialization(_ string: String) -> CallState {
            return CallState()
        }
        
        public var domain:String = ""     //返回码所在服务
        public var code:Int = 0           //返回值
        public var msg:String = ""        //返回信息
        public var length:Int = 0         //数据长度
    }
    
    public final class Stat : SerializableEntity {
        public static func json_serialization(_ entity: GWClient.Stat) -> String {
            return ""
        }
        
        public static func json_deserialization(_ string: String) -> Stat {
            return Stat()
        }
        
        public var systime:Int64 = 0      //当前服务端时间
//        public var cid:String = ""        //调用标识符
        public var domain:String = ""     //返回码所在服务
        public var code:Int = 0           //调用返回值
        public var msg:String = ""        //返回信息
        public var stateList:[CallState] = [] //API调用状态，code的信息请参考ApiCode定义文件
    }
    
    public final class ResultWrap : SerializableEntity {
        public static func json_serialization(_ entity: GWClient.ResultWrap) -> String {
            return ""
        }
        
        public static func json_deserialization(_ string: String) -> ResultWrap {
            let rt = ResultWrap()
            if let data = string.data(using: String.Encoding.utf8),
                let obj = try? JSONSerialization.jsonObject(with: data, options: JSONSerialization.ReadingOptions.mutableContainers),
                let json = obj as? [String:Any] {
                //解析数据,一定是一个array
                if let ct = json["content"] as? [[String:Any]] {
                    rt.content = ct
                }
                
                //解析状态
                if let ct = json["stat"] as? [String:Any] {
//                    rt.stat = Stat()
//                    rt.stat.cid
                    rt.stat.code = (ct["code"] as? Int) ?? 0
                    rt.stat.domain = (ct["domain"] as? String) ?? ""
                    rt.stat.msg = (ct["msg"] as? String) ?? ""
                    rt.stat.systime = (ct["systime"] as? Int64) ?? 0
                    
                    //解析stateList
                    if let ay = ct["stateList"] as? [[String:Any]] {
                        for st in ay {
                            let cs = CallState()
                            cs.code = (st["code"] as? Int) ?? 0
                            cs.domain = (st["domain"] as? String) ?? ""
                            cs.msg = (st["msg"] as? String) ?? ""
                            cs.length = (st["length"] as? Int) ?? 0
                            rt.stat.stateList.append(cs)
                        }
                    }
                }
            }
            return rt
        }
        
        public var stat:Stat = Stat()
        public var content:[[String:Any]] = []
    }
    
    //执行远程调用
    public static func en(_ call:APICall) {
        en([call])
    }
    
    //执行远程调用
    public static func en<C: Sequence>(_ calls:C) where C.Iterator.Element == APICall {
        let req:HTTP.Request<HTTP.Empty> = buildHTTPRequest(calls)
        
        let block:() -> Void = {
            var res:HTTP.Response<GWClient.ResultWrap> = HTTP.accesser().post(req)
            //填充数据
            if (fill(calls, res: res)) {//try again，必须重新buildRequest
                res = HTTP.accesser().post(buildHTTPRequest(calls))
                fill(calls, res: res)
            }
        }
        
        // 线程安全，但存在脏数据可能（此处只读，故不严格要求在锁内get，其他语言请使用volatile修饰exclusive_flag）
        let flag = req.exclusive || exclusive_flag
        if flag {
            synchronized(set: req.exclusive, block)
        } else {
            block()
        }
    }
    
    //填充数据(对特殊返回值需要处理，如登录；对特殊错误需要处理，如token过期)
    @discardableResult
    private static func fill<C: Sequence>(_ calls:C, res:HTTP.Response<GWClient.ResultWrap>, checkExpired:Bool = true) -> Bool where C.Iterator.Element == APICall {
        if let rt = res.result {
            
            //检查token过期并refresh存在
            if checkExpired && rt.stat.code == Consts.TOKEN_EXPIRED_CODE && !Context.shared.refresh.isEmpty {
                let refresh:HTTP.Response<GWClient.ResultWrap> = HTTP.accesser().post(buildRefreshRequest())
                //展现结果
                let refreshCall = RefreshCall()
                fill([refreshCall], res: refresh, checkExpired: false)
                if let token:ESBToken = refreshCall.getResult() {
                    if !Context.shared.account_id.isEmpty && Context.shared.uid.isEmpty {
                        Context.shared.setAccountToken(accountId: token.uid, token: token.token, stoken: token.stoken, refresh: token.refresh, key: token.key, expire: token.expire)
                    } else {
                        Context.shared.setToken(uid: token.uid, token: token.token, stoken: token.stoken, refresh: token.refresh, key: token.key, expire: token.expire)
                    }
                    return true
                }
            }
            
            //填充数据
            if rt.stat.code != Consts.SUCCESS_CODE {//不成功，则直接设置错误信息
                for call in calls {
                    if call.selector.isEmpty {
                        continue
                    }
                    //设置错误信息
                    call.statusCode = rt.stat.code
                    call.statusMessage = rt.stat.msg
                    call.statusCodeDomain = rt.stat.domain
                    break
                }
            } else {
                
                var idx = 0
                for call in calls {
                    if call.selector.isEmpty {
                        continue
                    }
                    
                    //防止越界
                    var success = rt.stat.code == Consts.SUCCESS_CODE
                    if !success && idx < rt.stat.stateList.count {//取错误信息
                        let cs = rt.stat.stateList[idx]
                        success = cs.code == Consts.SUCCESS_CODE
                        if cs.code != Consts.SUCCESS_CODE {
                            call.statusCode = cs.code
                            call.statusMessage = cs.msg
                            call.statusCodeDomain = cs.domain
                        }
                    }
                    
                    // 成功，则将数据转出
                    if success {
                        if idx < rt.content.count {
                            let json = rt.content[idx]
                            call.result = call.getResultType().init(fill: json)
                        }
                    }
                    
                    idx += 1
                }
            }
        }
        return false
    }
    
    private static var exclusive_flag:Bool = false
    private static func synchronized(set:Bool = false,_ body: () -> Void) {
        objc_sync_enter(self)
        if set { exclusive_flag = true }
        defer {
            if set { exclusive_flag = false }
            objc_sync_exit(self)
        }
        return body()
    }
}










