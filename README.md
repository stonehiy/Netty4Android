# Netty4Android
基于Netty的Android长连接实现

### 1. 协议的确定
-    协议头——4字节，标识协议体的长度
-    协议体——包括4字节扩展信息+传参，其中扩展信息里包括协议加密类型，传参为key，value键值对类型的数据，数据格式为4字节key长度+key+4字节value长度+value

### 2.协议加解密
- 客户端随机生成16位密钥，通过RSA非对称加密方式将密钥同步给服务器
- 密钥同步后，使用该密钥通过AES对称加密的方式对协议进行加密，然后进行数据传输

### 3.协议封装和解封装
```
package com.**.nettylib.netty.codec;

import com.**.nettylib.constant.NetworkConfig;
import com.**.nettylib.netty.Request;
import com.**.nettylib.netty.util.AESUtils;
import com.**.nettylib.netty.util.ExtendInfo;
import com.**.nettylib.netty.util.GZIPUtils;
import com.**.nettylib.netty.util.KeyManager;
import com.**.nettylib.netty.util.MsgEncryptType;
import com.**.nettylib.netty.util.ProtocolUtil;
import com.**.nettylib.netty.util.RSAUtils;

import java.io.ByteArrayOutputStream;
import java.util.Map;
import java.util.Set;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

/**
 * Created by james on 2018/7/30.
 * 加密协议组包,支持处理粘包和半包
 */
public class MessageEncryptEncoder extends MessageToByteEncoder<Request> {

    @Override
    protected void encode(ChannelHandlerContext channelHandlerContext, Request request, ByteBuf byteBuf) throws Exception {
        byteBuf.writeBytes(encodeResponse(request));
    }

    public byte[] encodeResponse(Request request) {
        try {
            Map<String, String> properties = request.toMap();
            Set<Map.Entry<String, String>> propertiesSet = properties.entrySet();
            ByteArrayOutputStream bodyStream = new ByteArrayOutputStream();
            for (Map.Entry<String, String> property : propertiesSet) {
                String propertyName = property.getKey();
                String propertyValue = property.getValue();
                bodyStream.write(ProtocolUtil.stringToByteArray(propertyName, "utf-8"));
                bodyStream.write(ProtocolUtil.stringToByteArray(propertyValue, "utf-8"));
            }
            byte[] bodyBytes = bodyStream.toByteArray();// 数据部分
            bodyStream.close();

            ByteArrayOutputStream responseStream = new ByteArrayOutputStream();
            ExtendInfo ext = request.getExt();
            byte[] extBytes = ext.toExtBytes();
            int msgEncryptType = ext.getMsgEncryptType();
            if (MsgEncryptType.NONO == msgEncryptType) {
                // do noting
            } else if (MsgEncryptType.RSA == msgEncryptType) {
                bodyBytes = doEncodeOfRSA(bodyBytes);
            } else if (MsgEncryptType.AES == msgEncryptType) {
                bodyBytes = doEncodeOfAES(bodyBytes);
            }
            responseStream.write(ProtocolUtil.intToByteArray(bodyBytes.length + extBytes.length));
            responseStream.write(extBytes);
            responseStream.write(bodyBytes);
            byte[] responseBytes = responseStream.toByteArray();// 整个响应包
            responseStream.close();
            return responseBytes;
        } catch (Exception e) {
            return null;
        }
    }

    private byte[] doEncodeOfRSA(byte[] bodyBytes) {
        // 先压缩，再加密
        return RSAUtils.publicEncrypt(GZIPUtils.compress(bodyBytes), NetworkConfig.RSA_PUBLIC_KEY);
    }

    private byte[] doEncodeOfAES(byte[] bodyBytes) {
        String password = KeyManager.aesKey;
        // 先压缩，再加密
        return AESUtils.encrypt(GZIPUtils.compress(bodyBytes), password);
    }
}

package com.**.nettylib.netty.codec;

import com.**.nettylib.netty.util.AESUtils;
import com.**.nettylib.netty.util.ExtendInfo;
import com.**.nettylib.netty.util.GZIPUtils;
import com.**.nettylib.netty.util.KeyManager;
import com.**.nettylib.netty.util.MsgEncryptType;
import com.**.nettylib.netty.util.ProtocolUtil;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

/**
 * Created by james on 2018/7/30.
 * 加密协议拆包,支持处理粘包和半包
 */
public class MessageEncryptDecoder extends ByteToMessageDecoder {
    public static String skey;

    private byte[] remainingBytes;

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf msg, List<Object> out) throws Exception {
        ByteBuf currBB = null;
        if(remainingBytes == null) {
            currBB = msg;
        }else {
            byte[] tb = new byte[remainingBytes.length + msg.readableBytes()];
            System.arraycopy(remainingBytes, 0, tb, 0, remainingBytes.length);
            byte[] vb = new byte[msg.readableBytes()];
            msg.readBytes(vb);
            System.arraycopy(vb, 0, tb, remainingBytes.length, vb.length);
            currBB = Unpooled.copiedBuffer(tb);
        }
        while(currBB.readableBytes() > 0) {
            if(!doDecode(ctx, currBB, out)) {
                break;
            }
        }
        if(currBB.readableBytes() > 0) {
            remainingBytes = new byte[currBB.readableBytes()];
            currBB.readBytes(remainingBytes);
        }else {
            remainingBytes = null;
        }
    }

    /**
     * @Title:doDecode
     * @param ctx
     * @param msg
     * @param out
     * @return boolean
     */
    private boolean doDecode(ChannelHandlerContext ctx, ByteBuf msg, List<Object> out)  throws Exception{
        if(msg.readableBytes() < 4)
            return false;
        msg.markReaderIndex();
        int len = msg.readInt();
        if(msg.readableBytes() < len) {
            msg.resetReaderIndex();
            return false;
        }
        byte[] extBytes = new byte[4];
        msg.readBytes(extBytes);
        ExtendInfo ext = new ExtendInfo(extBytes);
        int msgEncryptType = ext.getMsgEncryptType();

        byte[] body = new byte[len-4];
        msg.readBytes(body);
        Map<String, String> properties = decodeProperties(body,msgEncryptType);
        out.add(properties);
        if(msg.readableBytes() > 0)
            return true;
        return false;
    }

    private Map<String, String> decodeProperties(byte[] bodyBytes,int msgEncryptType) throws Exception {
        if (MsgEncryptType.NONO == msgEncryptType) {
            // do noting
        } else if (MsgEncryptType.AES == msgEncryptType) {
            bodyBytes = doDecodeOfAES(bodyBytes);
        }

        StringBuilder builder = new StringBuilder();
        for (byte b : bodyBytes) {
            builder.append(b).append(',');
        }
        try {
            Map<String, String> properties = new HashMap<String, String>();
            ByteArrayInputStream in = new ByteArrayInputStream(bodyBytes);
            in.skip(0);
            while (in.available() > 0) {
                String propertyName = ProtocolUtil.readString(in, "utf-8");
                String propertyValue = ProtocolUtil.readString(in, "utf-8");
                properties.put(propertyName, propertyValue);
            }
            return properties;
        } catch (Exception e) {
            throw new Exception("DoDecode exception, Received:byte[" + bodyBytes.length + "]{" + builder + "}");
        }
    }

    private byte[] doDecodeOfAES(byte[] bodyBytes) {
        String password = KeyManager.aesKey;
        return GZIPUtils.uncompress(AESUtils.decrypt(bodyBytes, password));
    }
}

```


### 4.发包和收包的对应关系确定
  在协议体的传参部分，参数中加入自增长的sn，即sequence num，表示消息的序列号，服务器回包的时候也带上该sn，之后客户端可以通过sn来对应发包和回包
  
```
package com.**.nettylib.netty;

import android.content.Context;
import android.content.SharedPreferences;

import com.**.nettylib.netty.codec.MessageDecoder;
import com.**.nettylib.netty.util.SecurityUtil;
import com.**.nettylib.netty.util.ExtendInfo;
import com.**.nettylib.netty.util.KeyManager;
import com.**.nettylib.netty.util.MsgEncryptType;

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
            params += entry.getKey() + "=" + entry.getValue();
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

```


### 5.心跳机制
通过IdleStateHandler设置写空闲时间，间隔一定时间（15s）给服务器发送心跳包来维持连接
```
package com.**.nettylib.netty;


import com.**.nettylib.constant.NetworkConfig;
import com.**.nettylib.netty.codec.MessageDecoder;
import com.**.nettylib.netty.codec.MessageEncoder;
import com.**.nettylib.netty.codec.MessageEncryptDecoder;
import com.**.nettylib.netty.codec.MessageEncryptEncoder;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;

/**
 * Created by james on 2018/6/20.
 */

public class NettyClientInitializer extends ChannelInitializer<SocketChannel> {

    private NettyListener listener;

    public NettyClientInitializer(NettyListener listener) {
        if(listener == null){
            throw new IllegalArgumentException("listener == null ");
        }
        this.listener = listener;
    }

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
//        SslContext sslCtx = SSLContext.getDefault()
//                .createSSLEngine(InsecureTrustManagerFactory.INSTANCE).build();

        ChannelPipeline pipeline = ch.pipeline();
//        pipeline.addLast(sslCtx.newHandler(ch.alloc()));    // 开启SSL
        pipeline.addLast(new LoggingHandler(LogLevel.INFO));    // 开启日志，可以设置日志等级
        pipeline.addLast("IdleStateHandler", new IdleStateHandler(NetworkConfig.READ_IDLE_TIME_SECOND, NetworkConfig.HEART_BEAT_TIME, 0));
        pipeline.addLast("StringDecoder", new MessageEncryptDecoder());//解码器 这里要与服务器保持一致
        pipeline.addLast("StringEncoder", new MessageEncryptEncoder());//编码器 这里要与服务器保持一致
        pipeline.addLast(new NettyClientHandler(listener));
    }
}
```


```
package com.**.nettylib.netty;

import android.util.Log;

import com.**.nettylib.netty.util.LogUtils;

import java.util.HashMap;
import java.util.Map;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;

/**
 * Created by james on 2018/6/20.
 */

public class NettyClientHandler extends SimpleChannelInboundHandler<Map> {
    private static final String TAG = NettyClientHandler.class.getName();
    private NettyListener listener;

    public NettyClientHandler(NettyListener listener) {
        this.listener = listener;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        NettyClient.getInstance().setConnectStatus(true);
        listener.onServiceStatusConnectChanged(NettyListener.STATUS_CONNECT_SUCCESS);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        NettyClient.getInstance().setConnectStatus(false);
        listener.onServiceStatusConnectChanged(NettyListener.STATUS_CONNECT_CLOSED);
        NettyClient.getInstance().reconnect();
    }


    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, Map map) throws Exception {
        LogUtils.logError(TAG, "thread == " + Thread.currentThread().getName());
        listener.onMessageResponse(map);
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent event = (IdleStateEvent) evt;
            if (event.state() == IdleState.WRITER_IDLE){
                // 发送密钥成功后才开始心跳
                if(NettyClient.getInstance().isSendAesKeyFinish()){
                    try{
                        Request request = new Request("heartbeat", new HashMap<String, String>(), new ResponseListener() {
                            @Override
                            public void onSuccess(String data) {
                            }

                            @Override
                            public void onFail(int errcode) {
                            }

                        });
                        ctx.channel().writeAndFlush(request);
                    } catch (Exception e){
                    }
                }
            }else if(event.state() == IdleState.READER_IDLE){
                // 响应超时进行网络重连
                NettyClient.getInstance().setConnectStatus(false);
                NettyClient.getInstance().reconnect();
            }
        }
    }
}

```


### 6.网络重连机制
- 服务器主动断开后重连
- 监听网络断开恢复可用后进行重连
- 服务器响应超时后重连
- sendMsg时客户端连接状态为断开且手机网络连接正常时重连

### 7.超时重发/重复消息过滤机制
维护一个请求缓存列表，当发送消息的时候将消息暂存在消息列表中，如果消息发送成功则移除，如果失败则待网络重连成功后再发送

```
package com.**.nettylib.netty;

import com.**.nettylib.netty.util.LogUtils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;


/**
 * 管理请求
 */
public class RequestManager {
    private static RequestManager sRequestManager = new RequestManager();
    private CopyOnWriteArrayList<Request> mCachedRequestList = new CopyOnWriteArrayList<>();
    private List<String> mNoCacheServiceList = new ArrayList<>();

    private RequestManager() {
        mNoCacheServiceList.add("syncAesKey");
        mNoCacheServiceList.add("sessionlogin");
    }

    public static RequestManager getInstance() {
        return sRequestManager;
    }

    /**
     * 缓存需要重发的请求
     *
     * @param request
     */
    public void add(Request request) {
        if (request != null) {
            if (isCacheRequest(request)) {
                // 判断sn是否已经存在，过滤重复请求
                for (Request r : mCachedRequestList) {
                    if (r.getSN() == request.getSN()) {
                        return;
                    }
                }
                mCachedRequestList.add(request);
            }
        }
    }

    /**
     * 是否重发
     *
     * @param request
     * @return
     */
    private boolean isCacheRequest(Request request) {
        String serviceName = request.getServiceName();
        return serviceName.startsWith("query") && !mNoCacheServiceList.contains(serviceName);
    }

    public void removeBySN(int sn) {
        Iterator<Request> it = mCachedRequestList.iterator();
        while (it.hasNext()) {
            Request request = it.next();
            if (request.getSN() == sn) {
                LogUtils.logError("NettyClient", "移除缓存请求：" + request.getServiceName());
                mCachedRequestList.remove(request);
                break;
            }
        }
    }


    /**
     * 通过tag移除请求
     */
    public void removeByTag(String tag) {
        Iterator<Request> it = mCachedRequestList.iterator();
        while (it.hasNext()) {
            Request request = it.next();
            if (request.getTag().equals(tag)) {
                LogUtils.logError("NettyClient", "通过Tag:" + tag + "移除缓存请求：" + request.getServiceName());
                mCachedRequestList.remove(request);
            }
        }
    }


    /**
     * 请求重发
     */
    public void resend() {
        for (Request request : mCachedRequestList) {
            LogUtils.logError("NettyClient", "重发缓存请求：" + request.getServiceName());
            NettyClient.getInstance().sendMessage(request);
        }
    }

    /**
     * 是否重复的查询请求
     */
    public boolean isRepeatRequest(Request request) {
        if (request.getServiceName().startsWith("query") && !request.isDefaultTag()) {
            for (Request req : mCachedRequestList) {
                if (req.getTag().equals(request.getTag())
                        && req.getServiceName().equals(request.getServiceName())
                        && req.getReqInfoMd5().equals(request.getReqInfoMd5())) {
                    LogUtils.logError("NettyClient", "重复的查询请求：tag = " + request.getTag() + ",serviceName = " + request.getServiceName());
                    return true;
                }
            }
        }
        return false;
    }
}


```




       
   

   
   

