package com.xzfg.app.test;

import com.xzfg.app.security.Crypto;

import org.junit.Test;

/**
 * This tests the null encryption.
 */
public class LiteCryptoTest extends CryptoTestAbstract {

    @Test
    @Override
    public void cryptoTest() throws Exception {
        Crypto crypto = new Crypto();
        byte[] encryptedValue = crypto.encrypt(TEST_STRING);
        assertEquals(TEST_STRING,new String(encryptedValue, "UTF-8"));
        assertEquals(TEST_STRING,new String(crypto.decrypt(encryptedValue),"UTF-8"));
    }

    // we don't need these!

    @Override
    public int getKeyLength() {
        return 0;
    }

    @Override
    public String getKeySpec() {
        return null;
    }

    @Override
    public Crypto getCrypto() {
        return null;
    }
}
