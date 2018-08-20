package com.james.nettylib.netty.util;


import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

import com.james.nettylib.constant.NetworkConfig;


/**
 * 网络层日志管理
 * Create by james on 2018/8/3
 */
public class LogUtils
{

    private static final String TAG = LogUtils.class.getSimpleName();
    private static final boolean PRINT_LOG_SWITCH = true;
    private static final boolean PRINT_LOG = NetworkConfig.DEV && PRINT_LOG_SWITCH;
    /**
     * 一般log
     * @param tag
     * @param content
     */
    public static void log(String tag, String content) {
        if (tag == null || content == null) {
            return;
        }

        if (PRINT_LOG) {
            Log.d(tag, content);
        }
    }

    /**
     * 错误log
     * @param tag
     * @param content
     */
    public static void logError(String tag, String content) {
        if (tag == null || content == null) {
            return;
        }

        if (PRINT_LOG) {
            Log.e(tag, content);
        }
    }


    /**
     * 辅助函数：获取当前时间
     * @return
     */
    public static String getMillTimeEx() {
        SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss.SSS");
        return format.format(new Date());
    }

    /**
     * 辅助函数：获取当前时间
     * @return
     */
    public static String getDateTime() {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH-mm-ss");
        return format.format(new Date());
    }

    /**
     * 打印JSON 对象
     * @param tag  log 标签
     * @param msg   JSON对象
     */
    public static void printJson(String tag, String msg) {
        if (!PRINT_LOG) {
            return;
        }
        String message;

        try {
            if (msg.startsWith("{")) {
                JSONObject jsonObject = new JSONObject(msg);
                message = jsonObject.toString(4);
            } else if (msg.startsWith("[")) {
                JSONArray jsonArray = new JSONArray(msg);
                message = jsonArray.toString(4);
            } else {
                message = msg;
            }
        } catch (JSONException e) {
            message = msg;
        }

        printLine(tag, true);

        String[] lines = message.split(System.getProperty("line.separator"));
        for (String line : lines) {
            Log.d(tag, "║ " + line);
        }
        printLine(tag, false);
    }

    private static void printLine(String tag, boolean isTop) {
        if (isTop) {
            Log.d(tag, "╔═══════════════════════════════════════════════════════════════════════════════════════");
        } else {
            Log.d(tag, "╚═══════════════════════════════════════════════════════════════════════════════════════");
        }
    }

}
