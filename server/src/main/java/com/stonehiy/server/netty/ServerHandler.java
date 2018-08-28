package com.stonehiy.server.netty;

import java.util.Map;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.GlobalEventExecutor;

public class ServerHandler extends SimpleChannelInboundHandler<Map> {
    /**
     * 定义一个集合来存储连上来的客户端
     */
    private static ChannelGroup group = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

    /**
     * 读取客户端的数据，然后广播给其他客户端，如果是自己给自己发消息则显示: 自己对自己说:xxx 如果是别人的消息则显示:XXX对我说:xxx
     */
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Map msg) throws Exception {
        System.out.println("Map msg : " + msg);
        Channel channel = ctx.channel();
        group.forEach(en -> {
            if (en.equals(channel)) {
                en.writeAndFlush("自己对自己说:" + msg + "\n");
            } else {
                en.writeAndFlush(channel.remoteAddress() + "对我说: " + msg + "\n");
            }
        });
    }

    /**
     * 客户端连上服务器的时候通知其他的客户端，XXX连上服务器了
     */
    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        Channel channel = ctx.channel();
        group.writeAndFlush("【服务器端消息】:" + channel.remoteAddress() + "加入了\n"); //调用集合的写方法会通知到所有在集合中的客户端
        //先调用group的写方法在加入，则可以避免对自己写入
        group.add(channel);
    }

    /**
     * 当客户端断开的时候通知其他的客户端XXX失去服务器端的连接了
     */
    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        Channel channel = ctx.channel();
        //group.remove(channel); //删除某一个客户端，但是没有必要显示的调用，ChannelGroup会自动检查是否在线
        group.writeAndFlush("【服务器端消息】:" + channel.remoteAddress() + "离开了\n");
    }

    /**
     * 客户端上线的时候，通知其他客户端,XXX上线了
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        Channel channel = ctx.channel();
        group.writeAndFlush("【服务器端消息】:" + channel.remoteAddress() + "上线了");
    }

    /**
     * 当客户端下线的时候，通知其他的客户端,XXX下线了
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        Channel channel = ctx.channel();
        group.writeAndFlush("【服务器端消息】:" + channel.remoteAddress() + "下线了");
    }

    /**
     * 出现异常的时候关闭客户端
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}
