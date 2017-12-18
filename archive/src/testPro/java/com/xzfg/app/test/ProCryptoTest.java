package com.xzfg.app.test;

import com.xzfg.app.security.Crypto;

/**
 * This tests the basic 3des encryption.
 */
public class ProCryptoTest extends CryptoTestAbstract {
    private static final String KEY_SPEC = "AES";
    private static final int KEY_LENGTH = 256;
    private static final Crypto crypto = new Crypto(KEY_SPEC + "/CTR/NoPadding",16,KEY_LENGTH);

    @Override
    public String getKeySpec() {
        return KEY_SPEC;
    }
    @Override
    public int getKeyLength() {
        return KEY_LENGTH;
    }

    @Override
    public Crypto getCrypto() {
        return crypto;
    }


}
