package com.stonehiy.server.netty;

import com.stonehiy.server.netty.codec.MessageDecoder;
import com.stonehiy.server.netty.codec.MessageEncoder;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

public class ServerInitializer extends ChannelInitializer<SocketChannel> {
    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();
        pipeline.addLast(new LoggingHandler(LogLevel.INFO));    // 开启日志，可以设置日志等级
        //pipeline.addLast("IdleStateHandler", new IdleStateHandler(NetworkConfig.READ_IDLE_TIME_SECOND, NetworkConfig.HEART_BEAT_TIME, 0));
        pipeline.addLast("StringDecoder", new MessageDecoder());//解码器 这里要与服务器保持一致
        pipeline.addLast("StringEncoder", new MessageEncoder());//编码器 这里要与服务器保持一致
//        pipeline.addLast("StringDecoder", new MessageEncryptDecoder());//解码器 这里要与服务器保持一致
//        pipeline.addLast("StringEncoder", new MessageEncryptEncoder());//编码器 这里要与服务器保持一致
//        pipeline.addLast(new StringEncoder(CharsetUtil.UTF_8));
//        pipeline.addLast(new StringDecoder(CharsetUtil.UTF_8));
        pipeline.addLast(new ServerHandler());


    }
}
