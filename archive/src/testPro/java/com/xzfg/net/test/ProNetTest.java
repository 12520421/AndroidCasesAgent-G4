package com.xzfg.net.test;

import com.xzfg.app.codec.Hex;
import com.xzfg.app.security.Crypto;

/**
 */
public class ProNetTest extends NetTestAbstract {
    public static final Crypto crypto = new Crypto("AES/CTR/NoPadding",16,256);

    @Override
    public long getPort() {
        return 4503l;
    }

    @Override
    public Crypto getCrypto() throws Exception {
        if (!crypto.isEncrypting()) {
            crypto.setKey(Hex.decodeHex("3463e006bc952e4696af0cd6c026bc21".toCharArray()));
        }
        return crypto;
    }


}
