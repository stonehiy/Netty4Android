package com.james.nettylib.netty;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;

/**
 * Created by james on 2018/6/20.
 */

public abstract class FutureListener implements ChannelFutureListener {
    @Override
    public void operationComplete(ChannelFuture future) throws Exception {
        if (future.isSuccess()) {
            success();
        } else {
            error();
        }
    }

    public abstract void success();

    public abstract void error();
}
