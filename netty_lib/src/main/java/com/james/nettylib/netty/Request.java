package com.james.nettylib.netty;

import android.content.Context;
import android.content.SharedPreferences;

import com.james.nettylib.netty.codec.MessageDecoder;
import com.james.nettylib.netty.util.SecurityUtil;
import com.james.nettylib.netty.util.ExtendInfo;
import com.james.nettylib.netty.util.KeyManager;
import com.james.nettylib.netty.util.MsgEncryptType;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import io.netty.util.internal.StringUtil;

public class Request {
    private String tag = "netty";
    private String serviceName;
    private Map<String, String> reqInfo;
    private String cs;
    private int sn;
    private ExtendInfo ext;
    private ResponseListener callback;

    public Request(String serviceName, Map<String, String> reqInfo, ResponseListener callback, String tag) {
        this(serviceName, reqInfo, callback);
        if (tag != null && !(tag.trim().equals(""))) {
            this.tag = tag;
        }
    }

    public Request(String serviceName, Map<String, String> reqInfo, ResponseListener callback) {
        this.serviceName = serviceName;
        ext = new ExtendInfo();
        if (serviceName.equals("syncAesKey")) {
            this.reqInfo = new HashMap<>();
            String key = createKey(16);
            this.reqInfo.put("aesKey", key);
            KeyManager.aesKey = key;
            ext.setMsgEncryptType(MsgEncryptType.RSA);
        } else {
            this.reqInfo = reqInfo;
            //公共参数
            this.reqInfo.put("version", "1.01");
            this.reqInfo.put("userid", getUserIDFromSDCard());
//            this.reqInfo.put("userid","yc1000066");
            this.reqInfo.put("sessionid", getSessionIDFromSDCard());
            ext.setMsgEncryptType(MsgEncryptType.AES);
        }
        this.cs = getSign(serviceName, reqInfo);
        this.sn = NettyClient.getInstance().getSN();
        this.callback = callback;
    }

    public int getSN() {
        return this.sn;
    }

    public String getServiceName() {
        return this.serviceName;
    }

    public String getTag() {
        return this.tag;
    }

    public boolean isDefaultTag(){
        return this.tag.equals("netty");
    }

    public String getReqInfoMd5() {
        return md5(map2String(this.reqInfo));
    }

    public ExtendInfo getExt() {
        return this.ext;
    }

    public ResponseListener getCallback() {
        return this.callback;
    }

    private String getSign(String method, Map<String, String> reqInfo) {
        if (StringUtil.isNullOrEmpty(MessageDecoder.skey)) {
            return "noskey";
        } else {
            String params = map2String(reqInfo);
            String str = method + params + "0x4Plfd8b65O3Lkm" + MessageDecoder.skey;
            if (method.equals("iappay")) {
                str = method + params + "0x4Plfd8b65O3Lkm" + MessageDecoder.skey;
            }
            return md5(str);
        }
    }

    private String map2String(Map<String, String> reqInfo) {
        String params = "";
        for (Map.Entry<String, String> entry : reqInfo.entrySet()) {
            if (!StringUtil.isNullOrEmpty(params)) {
                params += "&";
            }
            params += entry.getKey() + "=" + String.valueOf(entry.getValue());
        }
        return params;
    }

    private String md5(String str) {
        if (StringUtil.isNullOrEmpty(str)) {
            return "";
        }

        return SecurityUtil.md5(str);
    }

    //转成Map，用于组包
    public Map<String, String> toMap() {
        Map<String, String> map = new HashMap<String, String>();
        map.put("serviceName", this.serviceName);
        map.put("reqInfo", map2String(this.reqInfo));
        map.put("cs", this.cs);
        map.put("sn", String.valueOf(this.sn));

        return map;
    }

    private static String getUserIDFromSDCard() {
        String result = null;
        SharedPreferences preference = NettyClient.getInstance().getContext().getSharedPreferences("USERINFO", Context.MODE_PRIVATE);
        result = preference.getString("USERID", "");
        return result;
    }

    private static String getSessionIDFromSDCard() {
        String result = null;
        SharedPreferences preference = NettyClient.getInstance().getContext().getSharedPreferences("USERINFO", Context.MODE_PRIVATE);
        result = preference.getString("SESSIONID", "");
        return result;
    }


    /**
     * 随机生成指定位数秘钥
     *
     * @param KeyLength
     * @return
     */
    public String createKey(int KeyLength) {
        String base = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        Random random = new Random();
        StringBuffer Keysb = new StringBuffer();
        for (int i = 0; i < KeyLength; i++)    //生成指定位数的随机秘钥字符串
        {
            int number = random.nextInt(base.length());
            Keysb.append(base.charAt(number));
        }
        return Keysb.toString();
    }
}
