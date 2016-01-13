package com.zhaoxiaodan.miband;

public class MiBandNotConnectException extends RuntimeException{
    public MiBandNotConnectException(String detailMessage) {
        super(detailMessage);
    }
}