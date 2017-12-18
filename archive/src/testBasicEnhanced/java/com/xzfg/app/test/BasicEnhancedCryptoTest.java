package com.xzfg.app.test;

import com.xzfg.app.security.Crypto;
import com.xzfg.app.test.CryptoTestAbstract;

import org.junit.AfterClass;
import org.junit.BeforeClass;

import java.security.Security;

/**
 * This tests the basic 3des encryption.
 */
public class BasicEnhancedCryptoTest extends CryptoTestAbstract {
    private static final String KEY_SPEC = "DESede";
    private static final int KEY_LENGTH = 192;
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
        crypto = new Crypto(KEY_SPEC + "/CTR/NoPadding",8,KEY_LENGTH);
    }


    @AfterClass
    public static void tearDown() {
        Security.removeProvider("SC");
    }
}
