package com.james.nettylib.netty;

import android.util.Log;

import com.james.nettylib.netty.util.LogUtils;

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
