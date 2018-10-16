//
//  HTTP.swift
//  Venus-API-GW(1.0.0) SwiftGenerator
//  swift 4.0
//
//  Created by lingminjun on 2018/10/13.
//  Copyright © 2018年 MJ Ling. All rights reserved.
//

import Foundation

// 定义可序列化对象
public protocol SerializableEntity {
    // 暂时仅仅定义json格式的
    static func json_serialization(_ entity:Self) -> String;
    static func json_deserialization(_ string:String) -> Self;
    
    // 以后扩展xml
//    static func xml_serialization(_ entity:Self) -> String;
//    static func xml_deserialization(_ string:String) -> Self;
}

// 定义http请求模板
open class HTTP {
    
    /// query value defined
    public enum Value {
        case raw(String)
        
        // value支持 String
        public init<S: StringProtocol>(_ string: S) {self = .raw("\(string)")}
        
        // support Int,Float,Double,Bool,Character
        public init(_ int: Int) {self = .raw("\(int)")}
        public init(_ int8: Int8) {self = .raw("\(int8)")}
        public init(_ int16: Int16) {self = .raw("\(int16)")}
        public init(_ int32: Int32) {self = .raw("\(int32)")}
        public init(_ int64: Int64) {self = .raw("\(int64)")}
        public init(_ uint: UInt) {self = .raw("\(uint)")}
        public init(_ uint8: UInt8) {self = .raw("\(uint8)")}
        public init(_ uint16: UInt16) {self = .raw("\(uint16)")}
        public init(_ uint32: UInt32) {self = .raw("\(uint32)")}
        public init(_ uint64: UInt64) {self = .raw("\(uint64)")}
        public init(_ float: Float) {self = .raw("\(float)")}
        public init(_ double: Double) {self = .raw("\(double)")}
        public init(_ bool: Bool) {self = .raw("\(bool)")}
        public init(_ char: Character) {self = .raw("\(char)")}
        
        // get string
        public var string: String? { get{ switch self { case Value.raw(let value): return value; } } }
        
        // get bool
        public var bool: Bool? {
            get{
                switch self {
                case Value.raw(let value):
                    let v = value.lowercased()
                    if v == "true" || v == "yes" || v == "on" || v == "1" || v == "t" || v == "y" {
                        return true
                    } else if v == "false" || v == "no" || v == "off" || v == "0" || v == "f" || v == "n" {
                        return false
                    }
                    break
                }
                return nil
            }
        }
        
        // get char
        public var char: Character? {
            get{
                switch self {
                case Value.raw(let value):
                    if value.count == 1 {
                        return value[value.startIndex]
                    }
                    break
                }
                return nil
            }
        }
        
        // get double
        public var double: Double? { get{ switch self { case Value.raw(let value): return Double(value); } } }
        
        // get float
        public var float: Float? { get{ switch self { case Value.raw(let value): return Float(value); } } }
        
        // get int
        public var int: Int? { get{ switch self { case Value.raw(let value): return Int(value); } } }
        public var int8: Int8? { get{ switch self { case Value.raw(let value): return Int8(value); } } }
        public var int16: Int16? { get{ switch self { case Value.raw(let value): return Int16(value); } } }
        public var int32: Int32? { get{ switch self { case Value.raw(let value): return Int32(value); } } }
        public var int64: Int64? { get{ switch self { case Value.raw(let value): return Int64(value); } } }
        public var uint: UInt? { get{ switch self { case Value.raw(let value): return UInt(value); } } }
        public var uint8: UInt8? { get{ switch self { case Value.raw(let value): return UInt8(value); } } }
        public var uint16: UInt16? { get{ switch self { case Value.raw(let value): return UInt16(value); } } }
        public var uint32: UInt32? { get{ switch self { case Value.raw(let value): return UInt32(value); } } }
        public var uint64: UInt64? { get{ switch self { case Value.raw(let value): return UInt64(value); } } }
        
        
        // support SerializableEntity
        public init<E: SerializableEntity>(_ entity: E) { self = .raw("\(E.json_serialization(entity))") }
        public func json<E: SerializableEntity>() -> E? {
            switch self {
            case Value.raw(let value):
                return E.json_deserialization(value)
            }
        }
    }
    
    // Request
    open class Request<T: SerializableEntity> {
        public var scheme:String = "https"
        public var host:String = ""
        public var port:Int = 443
        public var path:String = ""
        public var query:[String:Value] = [:]    //非url encode
        public var headers:[String:String] = [:] //非url encode
        public var cookies:[String:String] = [:] //非url encode
        public var body:T? = nil
        public var exclusive:Bool = false        //排他调用
        
        public final var url: String {
            if host.isEmpty {
                return ""
            }
            
            var scm = scheme.lowercased()
            if scm.isEmpty {
                scm = "https"
            }
            
            if (scm == "https" && (port == 0 || port == 443))
                || (scm == "http" && (port == 0 || port == 80)) {
                return scm + "://" + host.lowercased() + (path.starts(with: "/") ? path : ("/" + path) )
            } else {
                return scm + "://" + host.lowercased() + ":\(port)" + (path.starts(with: "/") ? path : ("/" + path) )
            }
        }
        
        public final var queryString:String {
            return HTTP.queryJoin(query: query)
        }
        
        public final var cookieString:String {
            var str = ""
            for e in cookies {
                if !e.value.isEmpty {
                    if !str.isEmpty {
                        str += "; "
                    }
                    
                    str += HTTP.urlEncoded(str:e.key) + "=" + HTTP.urlEncoded(str:e.value)
                }
            }
            return str
        }
        
        public final var getRequest: URLRequest? {
            var urlString = self.url
            let queryString = self.queryString
            if !queryString.isEmpty {
                urlString += "&" + queryString
            }
            
            guard let url = URL(string: urlString) else {
                return nil
            }

            var req = URLRequest(url: url, cachePolicy: .useProtocolCachePolicy, timeoutInterval: 30)
            req.httpMethod = "GET"
            
            for e in headers {
                if !e.value.isEmpty {
                    req.addValue(e.key, forHTTPHeaderField: HTTP.urlEncoded(str: e.value))
                }
            }
            
            let cookies = self.cookieString
            if !cookies.isEmpty {
                req.addValue("Cookie", forHTTPHeaderField: cookies)
            }
            
            return req
        }
        
        public final var postRequest: URLRequest? {
            var urlString = self.url
            let queryString = self.queryString
            var bodyData:Data? = nil
            var contentType:String = "application/x-www-form-urlurlEncoded; charset=utf-8"
            if let body = self.body {// body存在
                if !queryString.isEmpty {
                    urlString += "&" + queryString
                }
                contentType = "application/json; charset=utf-8"
                bodyData = T.json_serialization(body).data(using: String.Encoding.utf8)
            } else if !queryString.isEmpty {
                bodyData = queryString.data(using: String.Encoding.utf8)
            }
            
            guard let url = URL(string: urlString) else {
                return nil
            }
            
            var req = URLRequest.init(url: url, cachePolicy: .useProtocolCachePolicy, timeoutInterval: 30)
            req.httpMethod = "POST"
            
            for e in headers {
                if !e.value.isEmpty {
                    req.addValue(e.key, forHTTPHeaderField: HTTP.urlEncoded(str: e.value))
                }
            }
            
            let cookies = self.cookieString
            if !cookies.isEmpty {
                req.addValue("Cookie", forHTTPHeaderField: cookies)
            }
            
            req.setValue("Content-Type", forHTTPHeaderField: contentType)
            req.httpBody = bodyData
            
            return req
        }
    }
    
    public final class Empty : SerializableEntity {
        public static func json_serialization(_ entity: HTTP.Empty) -> String {
            return ""
        }
        
        public static func json_deserialization(_ string: String) -> HTTP.Empty {
            return Empty()
        }
    }
    
    // 常用空body请求
    open class ERequest: Request<Empty> {}
    
    open class Response<T: SerializableEntity> {
        public var code:Int = 0
        public var message:String? = nil
        public var result:T? = nil
    }
    
    //http请求 (default 实现，可继承重载实现)
    open class Accesser {
        
        open func get<R: SerializableEntity>(_ request: ERequest) -> Response<R> {
            
            let response = Response<R>()
            
            // 创建Request对象
            // url: 请求路径
            // cachePolicy: 缓存协议
            // timeoutInterval: 网络请求超时时间(单位：秒)
            guard let req = request.getRequest else {
                response.code = -100
                return response
            }
            
            let configuration:URLSessionConfiguration = URLSessionConfiguration.default
            let session = URLSession(configuration: configuration)
            
            let semaphore = DispatchSemaphore(value: 0)
//            let timeout = DispatchTime.init(uptimeNanoseconds: 60 * 1000 * 1000 * 1000 )
            
            let task = session.dataTask(with:req) { (data, res, err) in
                if let err = err {
                    response.code = -100
                    response.message = err.localizedDescription
                } else if let res = res as? HTTPURLResponse {
                    response.code = res.statusCode
                    response.message = HTTPURLResponse.localizedString(forStatusCode:res.statusCode)
                }
                
                //不管成功还是失败，都取数据
                if let data = data,let str = String(data: data, encoding: String.Encoding.utf8) {
                    response.result = R.json_deserialization(str)
//                    let json = try!JSONSerialization.jsonObject(with: data! as Data, options: .mutableContainers)
                }
                
                semaphore.signal()
            }
            task.resume();
            
            // 等待结束
            let r = semaphore.wait(timeout: .distantFuture) //永不超时
            if r == .timedOut {
                response.code = -100
                response.message = "timeout"
                return response
            }

            return response
        }
        
        open func post<T: SerializableEntity, R: SerializableEntity>(_ request: Request<T>) -> Response<R> {
            let response = Response<R>()
            
            // 创建Request对象
            guard let req = request.postRequest else {
                response.code = -100
                return response
            }
            
            let configuration:URLSessionConfiguration = URLSessionConfiguration.default
            let session = URLSession(configuration: configuration)
            
            let semaphore = DispatchSemaphore(value: 0)
//            let timeout = DispatchTime.init(uptimeNanoseconds: 60 * 1000 * 1000 * 1000 )
            
            let task = session.dataTask(with:req) { (data, res, err) in
                if let err = err {
                    response.code = -100
                    response.message = err.localizedDescription
                } else if let res = res as? HTTPURLResponse {
                    response.code = res.statusCode
                    response.message = HTTPURLResponse.localizedString(forStatusCode:res.statusCode)
                }
                
                //不管成功还是失败，都取数据
                if let data = data,let str = String(data: data, encoding: String.Encoding.utf8) {
                    response.result = R.json_deserialization(str)
                }
                
                semaphore.signal()
            }
            task.resume();
            
            // 等待结束
            let r = semaphore.wait(timeout: .distantFuture) //永不超时
            if r == .timedOut {
                response.code = -100
                return response
            }
            
            return response
        }
    }
    
    // 自行重写，修改你的HTTPAccesser
    open class func accesser() -> Accesser {
        return Accesser()
    }
    
    ///
    open static func queryJoin(query: [String:Value], join:String = "&", encode:Bool = true) ->String {
        
        if query.isEmpty {
            return ""
        }
        
        let keys = query.keys.sorted()
        let valuse = query.values
        if valuse.count == 1 {
            let str = valuse.first?.string ?? ""
            if str.isEmpty {
                return keys[0]
            }
            return keys[0] + "=" + (encode ? HTTP.urlEncoded(str: str) : str)
        }
        
        var result = ""
        for key in keys {
            if let qvalue = query[key] {
                let str = qvalue.string ?? ""
                let value = (encode ? HTTP.urlEncoded(str: str) : str)
                if !join.isEmpty && !result.isEmpty {
                    result += join
                }
                result += key + "=" + value
            }
        }
        
        return result
    }
    
    /// query url encode
    open static func urlEncoded(str:String) -> String {
        guard let value = str.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed) else {
            return str
        }
        return value
    }
    
    /// query url decode
    open static func urlDecoded(str:String) -> String {
        guard let value = str.removingPercentEncoding else {
            return str
        }
        return value
    }
}


