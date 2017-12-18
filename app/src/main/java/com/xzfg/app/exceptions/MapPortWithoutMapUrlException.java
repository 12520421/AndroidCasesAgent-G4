package com.xzfg.app.exceptions;

@SuppressWarnings("unused")
public class MapPortWithoutMapUrlException extends Exception {
    public MapPortWithoutMapUrlException(String msg) {
        super(msg);
    }

    public MapPortWithoutMapUrlException(String msg, Throwable e) {
        super(msg, e);
    }
}
