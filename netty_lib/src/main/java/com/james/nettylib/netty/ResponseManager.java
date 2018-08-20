package com.james.nettylib.netty;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;


/**
 *
 */
public class ResponseManager {
    private static ResponseManager sResponseManager = new ResponseManager();
    private Map<Integer, Map<String, Object>> requestMap = new HashMap<Integer, Map<String, Object>>();

    private ResponseManager() {

    }

    public static ResponseManager getInstance() {
        return sResponseManager;
    }

    public void addListener(Request request) {
        // 避免请求重发的时候重复添加回调
        if(request != null){
            if (requestMap.get(request.getSN()) == null) {
                Map<String, Object> info = new HashMap<String, Object>();
                info.put("tag", request.getTag());
                info.put("callback", request.getCallback());
                requestMap.put(request.getSN(), info);
            }
        }
    }

    public ResponseListener getListener(int sn) {
        if (requestMap.get(sn) != null) {
            return (ResponseListener) requestMap.get(sn).get("callback");
        }
        return null;
    }

    /**
     * 通过sn移除
     */
    public void removeListener(int sn) {
        requestMap.remove(sn);
    }

    public void removeListenerByTag(String tag) {
        Iterator<Map.Entry<Integer, Map<String, Object>>> it = requestMap.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, Map<String, Object>> entry = it.next();
            if (entry.getValue() != null && entry.getValue().get("tag").equals(tag)) {
                it.remove();
            }
        }
    }
}

