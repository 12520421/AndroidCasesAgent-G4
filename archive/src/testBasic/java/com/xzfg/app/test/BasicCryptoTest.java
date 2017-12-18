package com.xzfg.app.test;

import com.xzfg.app.security.Crypto;

/**
 * This tests the basic 3des encryption.
 */
public class BasicCryptoTest extends CryptoTestAbstract {
    private static final String KEY_SPEC = "DESede";
    private static final int KEY_LENGTH = 192;
    private static final Crypto crypto = new Crypto(KEY_SPEC + "/CTR/NoPadding",8,KEY_LENGTH);

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
