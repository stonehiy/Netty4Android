package com.stonehiy.server.netty.util;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

/**
 * Created by james on 2018/7/30.
 */
public class AESUtils {
    private static final String KEY_ALGORITHM = "AES";
    private static final String DEFAULT_CIPHER_ALGORITHM = "AES/ECB/PKCS5Padding";//默认的加密算法

    /**
     * AES加密
     * @param origin   明文字节数组
     * @param password  为了与iOS统一，password必须是16字节
     * @return
     */
    public static byte[] encrypt(byte[] origin, String password) {
        try {
            byte[] enCodeFormat = password.getBytes();
            SecretKeySpec key = new SecretKeySpec(enCodeFormat, KEY_ALGORITHM);// 转换为AES专用密钥
            Cipher cipher = Cipher.getInstance(DEFAULT_CIPHER_ALGORITHM);// 创建密码器
            cipher.init(Cipher.ENCRYPT_MODE, key);// 初始化为加密模式的密码器
            byte[] decrypted = cipher.doFinal(origin);
            return decrypted;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * AES解密
     * @param encrypt    密文字节数组
     * @param password  为了与iOS统一，password必须是16字节
     * @return
     */
    public static byte[] decrypt(byte[] encrypt, String password) {
        try {
            byte[] enCodeFormat = password.getBytes();
            SecretKeySpec key = new SecretKeySpec(enCodeFormat, KEY_ALGORITHM);// 转换为AES专用密钥
            Cipher cipher = Cipher.getInstance(DEFAULT_CIPHER_ALGORITHM);// 创建密码器
            cipher.init(Cipher.DECRYPT_MODE, key);// 初始化为解密模式的密码器
             byte[] origin = cipher.doFinal(encrypt);
            return origin;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

}
