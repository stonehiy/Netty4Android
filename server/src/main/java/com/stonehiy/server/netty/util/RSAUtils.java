package com.stonehiy.server.netty.util;


import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import javax.crypto.Cipher;


/**
 * Created by james on 2018/7/30.
 */
public class RSAUtils {

    //将Base64编码后的公钥转换成PublicKey对象
    private static PublicKey string2PublicKey(String pubStr) {
        byte[] keyBytes = base642Byte(pubStr);
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
        KeyFactory keyFactory;
        PublicKey publicKey = null;
        try {
            keyFactory = KeyFactory.getInstance("RSA");
            publicKey = keyFactory.generatePublic(keySpec);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return publicKey;
    }

    //公钥加密
    public static byte[] publicEncrypt(byte[] content, String publicKeyStr) {
        byte[] bytes = null;
        try {
            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(Cipher.ENCRYPT_MODE, string2PublicKey(publicKeyStr));
            bytes = cipher.doFinal(content);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return bytes;
    }

    //Base64编码转字节数组
    private static byte[] base642Byte(String base64Key) {
        Base64.Decoder decoder = Base64.getDecoder();
        return decoder.decode(base64Key);
    }
}
