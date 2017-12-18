package com.xzfg.net.test;

import com.xzfg.app.codec.Hex;
import com.xzfg.app.security.Crypto;

/**
 */
public class BasicNetTest extends NetTestAbstract {
    public static final Crypto crypto = new Crypto("DESede/CTR/NoPadding",8,192);

    @Override
    public long getPort() {
        return 4502l;
    }

    @Override
    public Crypto getCrypto() throws Exception {
        if (!crypto.isEncrypting()) {
            crypto.setKey(Hex.decodeHex("2d6f6fd46b8aa56e3b36b39ab30cda13450ea0c8091a2458".toCharArray()));
        }
        return crypto;
    }


}
