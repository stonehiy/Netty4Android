package com.james.nettylib.netty;


import com.james.nettylib.constant.NetworkConfig;
import com.james.nettylib.netty.codec.MessageDecoder;
import com.james.nettylib.netty.codec.MessageEncoder;
import com.james.nettylib.netty.codec.MessageEncryptDecoder;
import com.james.nettylib.netty.codec.MessageEncryptEncoder;

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
        if(NetworkConfig.DEV){
            pipeline.addLast(new LoggingHandler(LogLevel.INFO));    // 开启日志，可以设置日志等级
        }
        pipeline.addLast("IdleStateHandler", new IdleStateHandler(NetworkConfig.READ_IDLE_TIME_SECOND, NetworkConfig.HEART_BEAT_TIME, 0));
        pipeline.addLast("StringDecoder", new MessageEncryptDecoder());//解码器 这里要与服务器保持一致
        pipeline.addLast("StringEncoder", new MessageEncryptEncoder());//编码器 这里要与服务器保持一致
        pipeline.addLast(new NettyClientHandler(listener));
    }
}