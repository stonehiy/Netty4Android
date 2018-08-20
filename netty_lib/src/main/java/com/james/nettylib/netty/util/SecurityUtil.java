package com.james.nettylib.netty.util;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class SecurityUtil {

    public static final String DIGEST_MD5 = "MD5";

    /**
     * 生成摘要
     */
    public static byte[] digest(byte[] srcBytes, String digestName) {

        MessageDigest md = null;
        byte[] result = null;
        try {
            md = MessageDigest.getInstance(digestName);
            md.update(srcBytes);
            result = md.digest();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return result;
    }

    public static byte[] digest(byte[] srcBytes) {
        return digest(srcBytes, DIGEST_MD5);
    }

    /**
     * 生成摘要,并转换为16进度字符串
     */
    public static String digest(String src, String encode, String spitChar, String digestName) {
        String result = null;
        try {
            byte[] srcBytes = src.getBytes(encode);
            byte[] digestBytes = digest(srcBytes, digestName);
            if (digestBytes != null) {
                result = toHexString(digestBytes, spitChar);
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return result;
    }

    public static String digest(String src, String spitChar, String digestName) {
        return digest(src, "UTF-8", spitChar, digestName);
    }

    public static String digest(String src, String spitChar) {
        return digest(src, spitChar, DIGEST_MD5);
    }

    public static String digest(String src) {
        return digest(src, "", DIGEST_MD5);
    }


    public static String toHexString(byte[] data, String spitchar) {
        StringBuffer buffer = new StringBuffer();
        for (int i = 0; i < data.length; i++) {
            if (i > 0) {
                buffer.append(spitchar);
            }
            buffer.append(toHexString(data[i]));
        }
        return buffer.toString();
    }

    public static String toHexString(byte data) {
        return Integer.toHexString((data >>> 4) & 0X0F) + Integer.toHexString(data & 0X0F);
    }

    public static String toHexString(byte[] data) {
        return toHexString(data, "");
    }


    /**
     * 两次 md5加密
     *
     * @param text
     * @return
     */
    public static String md5Two(String text) {
        return digest(digest(text));
    }


    public static String md5(String text) {
        return digest(text);
    }
}
