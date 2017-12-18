package com.xzfg.app.exceptions;


public class CameraOpenException extends Exception {
    public CameraOpenException(String msg) {
        super(msg);
    }

    public CameraOpenException(String msg, Throwable e) {
        super(msg, e);
    }
}
