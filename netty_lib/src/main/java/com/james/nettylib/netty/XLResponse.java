package com.james.nettylib.netty;

import java.util.HashMap;
import java.util.Map;


/**
 * 响应数据
 */

/**
 * 通用协议介绍
 * <p>
 * 通用报文格式：无论是请求还是响应，报文都由一个通用报文头和实际数据组成。报文头在前，数据在后
 * （1）报文头:由数据解析类型，数据解析方法，编码，扩展字节，包长度组成,共16个字节：
 * 编码方式（1byte）、加密（1byte）、扩展1（1byte）、扩展2（1byte）、会话ID（4byte）、命令或者结果码（4byte）、包长（4byte）
 * （2）数据:由包长指定。请求或回复数据。类型对应为JAVA的Map<String,String>
 * 数据格式定义：
 * 字段1键名长度    字段1键名 字段1值长度    字段1值
 * 字段2键名长度    字段2键名 字段2值长度    字段2值
 * 字段3键名长度    字段3键名 字段3值长度    字段3值
 * …    …    …    …
 * 长度为整型，占4个字节
 */
public class XLResponse {
    private byte encode;// 数据编码格式。已定义：0：UTF-8，1：GBK，2：GB2312，3：ISO8859-1
    private byte encrypt;// 加密类型。0表示不加密
    private byte extend1;// 用于扩展协议。暂未定义任何值
    private byte extend2;// 用于扩展协议。暂未定义任何值
    private int sessionid;// 会话ID
    private int result;// 结果码
    private int length;// 数据包长

    private Map<String, String> values = new HashMap<String, String>();

    private String ip;

    public void setValue(String key, String value) {
        values.put(key, value);
    }

    public String getValue(String key) {
        if (key == null) {
            return null;
        }
        return values.get(key);
    }

    public byte getEncode() {
        return encode;
    }

    public void setEncode(byte encode) {
        this.encode = encode;
    }

    public byte getEncrypt() {
        return encrypt;
    }

    public void setEncrypt(byte encrypt) {
        this.encrypt = encrypt;
    }

    public byte getExtend1() {
        return extend1;
    }

    public void setExtend1(byte extend1) {
        this.extend1 = extend1;
    }

    public byte getExtend2() {
        return extend2;
    }

    public void setExtend2(byte extend2) {
        this.extend2 = extend2;
    }

    public int getSessionid() {
        return sessionid;
    }

    public void setSessionid(int sessionid) {
        this.sessionid = sessionid;
    }

    public int getResult() {
        return result;
    }

    public void setResult(int result) {
        this.result = result;
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public Map<String, String> getValues() {
        return values;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public void setValues(Map<String, String> values) {
        this.values = values;
    }

    @Override
    public String toString() {
        return "XLResponse [encode=" + encode + ", encrypt=" + encrypt + ", extend1=" + extend1 + ", extend2=" + extend2
                + ", sessionid=" + sessionid + ", result=" + result + ", length=" + length + ", values=" + values + ", ip=" + ip + "]";
    }
}