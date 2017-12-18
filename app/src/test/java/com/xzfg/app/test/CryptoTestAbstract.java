package com.xzfg.app.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.xzfg.app.codec.Base64;
import com.xzfg.app.security.Crypto;

import org.junit.Test;

import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Arrays;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * This provides most of what is needed to test the crypto across all the flavors.
 * Each flavor needs a test class that extends this, and at a minumum provides the
 * getKeyLength, getKeySpec, and getCrypto implementations.
 */
public abstract class CryptoTestAbstract {
    public static final String TEST_STRING = "Hello World! - $¢€𤭢";

    public abstract int getKeyLength();

    public abstract String getKeySpec();

    public abstract Crypto getCrypto();

    @Test
    public void cryptoTest() throws Exception {
        Crypto crypto = getCrypto();

        // generate a new key for testing purposes.
        byte[] key = generateKey(getKeySpec(), getKeyLength());
        assertNotNull("The key should not be null.", key);
        assertEquals("Invalid key length detected.", key.length * 8, getKeyLength());
        crypto.setKey(key);

        assertTrue("The crypto instance is not properly configured.", crypto.isEncrypting());

        // encrypt the test string.
        byte[] encryptedValue = crypto.encrypt(TEST_STRING);
        assertNotNull("The encrypted value should not be null.", encryptedValue);

        // converting the encrypted string back to text should not give the same value.
        assertNotEquals("The encrypted value is the same as the test string.", TEST_STRING, new String(encryptedValue, "UTF-8"));

        // decrypting the encrypted string back to text should have the same value.
        assertEquals("The decrypted value doesn't match the test string.", TEST_STRING, new String(crypto.decrypt(encryptedValue), "UTF-8"));

        // a second trip of the same string through the encryption, should have a different value.
        byte[] reencryptedValue = crypto.encrypt(TEST_STRING);
        assertNotNull("The second encrypted value is null.", reencryptedValue);

        // compare the raw binary arrays, should not be equal.
        assertFalse("The two encrypted values are identical, this should not be the case if each uses a unique IV.", Arrays.equals(encryptedValue, reencryptedValue));

        // compare the base64 string representations, should not be equal.
        assertNotEquals("The base64 representations are identical, this should not be the case if each uses a unique IV.", Base64.encodeBase64URLSafeString(encryptedValue), Base64.encodeBase64URLSafe(reencryptedValue));

        // but when decrypted, they both should have the same value:
        assertEquals("The decrypted values do not match.", new String(crypto.decrypt(encryptedValue), "UTF-8"), new String(crypto.decrypt(reencryptedValue), "UTF-8"));

    }

    /**
     * Generates a key for testing purposes. Note: testing the 256bit keys requires the Java
     * Unlimited Strength Jurisdiction Policy Files be installed in the JVM where the test is
     * running.
     *
     * @param keySpecName Should be AES or DESede
     * @param keyLength   should be 192,128, or 256.
     * @return byte array
     * @throws NoSuchProviderException exception
     * @throws InvalidKeySpecException exception
     * @throws NoSuchAlgorithmException exception
     */
    public byte[] generateKey(String keySpecName, int keyLength) throws NoSuchProviderException, InvalidKeySpecException, NoSuchAlgorithmException {
        byte[] trash = new byte[16];
        byte[] salt = new byte[8];
        SecureRandom secureRandom = SecureRandom.getInstance("SHA1PRNG");
        secureRandom.nextBytes(trash);
        Arrays.fill(trash, (byte) 0);
        secureRandom.nextBytes(salt);
        SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
        KeySpec keySpec = new PBEKeySpec("SOME PASSWORD.".toCharArray(), salt, 10000, keyLength);
        SecretKey pbkdf2Key = secretKeyFactory.generateSecret(keySpec);
        SecretKey secretKey = new SecretKeySpec(pbkdf2Key.getEncoded(), keySpecName);
        return secretKey.getEncoded();
    }
}
