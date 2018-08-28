package com.stonehiy.server.netty.util;

/**
 * Created by james on 2018/7/30.
 */
public class ExtendInfo {
    private int msgEncryptType = MsgEncryptType.NONO; // 参见MsgEncryptType

    /**
     *
     * @param extBytes 扩展字节总共四个字节。目前只有第四个字节后4位，表示加密类型。
     */
    public ExtendInfo(byte[] extBytes) {
        msgEncryptType = extBytes[3] & 0x0F; // 第四个字节后4位，表示加密类型
    }

    public ExtendInfo() {

    }

    public int getMsgEncryptType() {
        return msgEncryptType;
    }

    public void setMsgEncryptType(int msgEncryptType) {
        this.msgEncryptType = msgEncryptType;
    }

    public byte[] toExtBytes() {
        byte[] extBytes = {0, 0 , 0 ,0};
        extBytes[3] = (byte)(msgEncryptType & 0x0F);
        return extBytes;
    }

}
