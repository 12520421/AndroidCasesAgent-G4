package com.xzfg.net.test;

import com.xzfg.app.codec.Hex;
import com.xzfg.app.security.Crypto;

/**
 */
public class EliteNetTest extends NetTestAbstract {
    public static final Crypto crypto = new Crypto("AES/CTR/NoPadding",16,256);

    @Override
    public long getPort() {
        return 4504l;
    }

    @Override
    public Crypto getCrypto() throws Exception {
        if (!crypto.isEncrypting()) {
            crypto.setKey(Hex.decodeHex("EDD642EDCACECA49C8353A97AF7B235F770CBE0C1AC4FE73C551DAA0E5C74537".toCharArray()));
        }
        return crypto;
    }


}
