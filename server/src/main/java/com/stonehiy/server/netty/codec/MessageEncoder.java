package com.stonehiy.server.netty.codec;

import com.stonehiy.server.netty.Request;
import com.stonehiy.server.netty.util.ProtocolUtil;

import java.io.ByteArrayOutputStream;
import java.util.Map;
import java.util.Set;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

/**
 * Created by james on 2018/6/30.
 * 组包
 */
public class MessageEncoder extends MessageToByteEncoder<Request> {

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
            responseStream.write(ProtocolUtil.intToByteArray(bodyBytes.length));
            responseStream.write(bodyBytes);
            byte[] responseBytes = responseStream.toByteArray();// 整个响应包
            responseStream.close();
            return responseBytes;
        } catch (Exception e) {
            return null;
        }
    }
}
