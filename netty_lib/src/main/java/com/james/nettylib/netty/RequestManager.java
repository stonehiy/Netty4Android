package com.james.nettylib.netty;

import com.james.nettylib.netty.util.LogUtils;

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

