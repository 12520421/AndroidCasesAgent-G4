package com.xzfg.app.test;

import com.xzfg.app.security.Crypto;

import org.junit.AfterClass;
import org.junit.BeforeClass;

import java.security.Security;

/**
 * This tests the basic 3des encryption.
 */
public class ProEnhancedCryptoTest extends CryptoTestAbstract {
    private static final String KEY_SPEC = "AES";
    private static final int KEY_LENGTH = 128;
    private static Crypto crypto;

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

    @BeforeClass
    public static void setUp() {
        Security.insertProviderAt(new org.spongycastle.jce.provider.BouncyCastleProvider(), 1);
        crypto = new Crypto(KEY_SPEC + "/CTR/NoPadding",16,KEY_LENGTH);
    }


    @AfterClass
    public static void tearDown() {
        Security.removeProvider("SC");
    }
}
