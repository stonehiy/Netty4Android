package com.james.nettylib.netty;

public interface ResponseListener {
    public void onSuccess(String data);

    public void onFail(int errCode);
}
