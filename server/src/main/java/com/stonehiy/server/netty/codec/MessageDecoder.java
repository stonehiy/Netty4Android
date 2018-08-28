package com.stonehiy.server.netty.codec;


import com.stonehiy.server.netty.util.ProtocolUtil;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

/**
 * Created by james on 2018/6/30.
 * 拆包,支持处理粘包和半包
 */
public class MessageDecoder extends ByteToMessageDecoder {
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
        byte[] body = new byte[len];
        msg.readBytes(body);
        Map<String, String> properties = decodeProperties(body);
        out.add(properties);
        if(msg.readableBytes() > 0)
            return true;
        return false;
    }

    private Map<String, String> decodeProperties(byte[] bodyBytes) throws Exception {
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
}
