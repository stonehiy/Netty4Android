package com.james.nettylib.netty.codec;

import com.james.nettylib.constant.NetworkConfig;
import com.james.nettylib.netty.Request;
import com.james.nettylib.netty.util.AESUtils;
import com.james.nettylib.netty.util.ExtendInfo;
import com.james.nettylib.netty.util.GZIPUtils;
import com.james.nettylib.netty.util.KeyManager;
import com.james.nettylib.netty.util.MsgEncryptType;
import com.james.nettylib.netty.util.ProtocolUtil;
import com.james.nettylib.netty.util.RSAUtils;

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
