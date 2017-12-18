package com.xzfg.app.exceptions;

public class CameraParameterException extends Exception {

    public CameraParameterException(String msg) {
        super(msg);
    }

    public CameraParameterException(String msg, Throwable e) {
        super(msg, e);
    }
}
