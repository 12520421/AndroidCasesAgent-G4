package com.xzfg.net.test;

import com.xzfg.app.security.Crypto;

/**
 */
public class LiteNetTest extends NetTestAbstract {

    public Crypto crypto = new Crypto();

    @Override
    public long getPort() {
        return 4500;
    }

    @Override
    public Crypto getCrypto() throws Exception {
        return crypto;
    }


}
