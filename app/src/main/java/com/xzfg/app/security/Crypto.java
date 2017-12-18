package com.xzfg.app.security;

import com.xzfg.app.codec.Base64;
import com.xzfg.app.codec.Hex;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import timber.log.Timber;

/**
 * Handles all encryption/decryption steps.
 */
@SuppressWarnings("unused")
public class Crypto {

    private static final String prngName = "SHA1PRNG";
    private static final int ITERATIONS = 2000;
    private final String keySpecName;
    private final String cipherName;
    private final int ivSize;
    private final int keyLength;
    private boolean encrypting;
    private SecretKeySpec secretKeySpec = null;

    public Crypto() {
        this.keySpecName = null;
        this.cipherName = null;
        this.ivSize = -1;
        this.keyLength = -1;
        setEncrypting();
    }


    public Crypto(String cipher, int ivSize, int keyLength) {
        this.keySpecName = cipher.split("/")[0];
        this.cipherName = cipher;
        this.ivSize = ivSize;
        this.keyLength = keyLength;
        setEncrypting();
    }

    public static String encode(byte[] data) {
        return Base64.encodeBase64URLSafeString(data);
    }

    public static String decode(byte[] data) throws UnsupportedEncodingException {
        return new String(Base64.decodeBase64(data));
    }

    public int getIvSize() {
        return ivSize;
    }

    public int getExpectedKeyLength() {
        return keyLength;
    }

    public String getExpectedFormat() {
        if (this.cipherName == null) {
            return null;
        }
        if (this.cipherName.contains("DES")) {
            return "TripleDES";
        }
        if (this.cipherName.contains("AES")) {
            return "AES" + String.valueOf(keyLength);
        }
        return "HUH?";
    }

    public boolean isEncrypting() {
        return encrypting;
    }

    /*
    public void setKey(SecretKeySpec secretKeySpec) {
        if (secretKeySpec.getAlgorithm() == this.keySpecName) {
            this.secretKeySpec = secretKeySpec;
        }
        setEncrypting();
    }
    */

    private void setEncrypting() {
        encrypting = this.keySpecName != null && this.cipherName != null && ivSize != -1 && keyLength != -1 && secretKeySpec != null;
    }

    public void setKey(byte[] bytes) {
        if (bytes != null) {
            secretKeySpec = new SecretKeySpec(bytes, keySpecName);
        }
        setEncrypting();
    }

    /**
     * Gets a secure random instance. Gets and discards a chunk of randomness each time it's called.
     *
     * @return SecureRandom instance.
     * @throws NoSuchAlgorithmException exception
     */
    protected SecureRandom getSecureRandom() throws NoSuchAlgorithmException {
        byte[] trash = new byte[16];
        SecureRandom secureRandom = SecureRandom.getInstance(prngName);
        secureRandom.nextBytes(trash);
        Arrays.fill(trash, (byte) 0);
        return secureRandom;
    }

    public OutputStream getOutputStream(OutputStream outputStream) {
        BufferedOutputStream bos = new BufferedOutputStream(outputStream);

        if (!isEncrypting()) {
            return bos;
        }

        try {

            // Prep the initialization vector.
            byte iv[] = new byte[ivSize];
            getSecureRandom().nextBytes(iv);

            bos.write(iv);
            bos.flush();

            IvParameterSpec ivSpec = new IvParameterSpec(iv);

            // get the cipher.
            Cipher cipher = Cipher.getInstance(cipherName);

            // initialize the cipher.
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, ivSpec, getSecureRandom());

            // write the initialization vector to the head of the stream, so that we can retrieve it.
            //outputStream.write();

            return new CipherOutputStream(bos, cipher);
        } catch (Exception e) {
            Timber.e(e, "An error occurred attempting to encrypt an InputStream.");
        }

        return bos;
    }

    public InputStream getInputStream(InputStream inputStream) {
        BufferedInputStream bis = new BufferedInputStream(inputStream);

        if (!isEncrypting()) {
            return bis;
        }

        try {
            byte[] iv = new byte[ivSize];
            //noinspection ResultOfMethodCallIgnored
            bis.read(iv);
            IvParameterSpec ivSpec = new IvParameterSpec(iv);
            Cipher cipher = Cipher.getInstance(cipherName);
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, ivSpec);
            return new CipherInputStream(bis, cipher);
        } catch (Exception e) {
            e.printStackTrace();
            //Timber.e(e, "An error occurred attempting to decrypt an OutputStream");
        }

        return bis;
    }

    public String encryptToHex(String data) {
        if (!isEncrypting()) {
            return data;
        } else {
            return Hex.encodeHexString(encrypt(data));
        }
    }

    public byte[] encrypt(byte[] data) {
        try {
            if (!isEncrypting()) {
                return data;
            }

            // Prep the initialization vector.
            byte iv[] = new byte[ivSize];
            getSecureRandom().nextBytes(iv);
            IvParameterSpec ivSpec = new IvParameterSpec(iv);

            // get the cipher.
            Cipher cipher = Cipher.getInstance(cipherName);

            // initialize the cipher.
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, ivSpec, getSecureRandom());
            byte[] encrypted = cipher.doFinal(data);

            // write the initialization vector to the head of the stream, so that we can retrieve it.
            byte[] result = new byte[ivSize + encrypted.length];
            System.arraycopy(iv, 0, result, 0, iv.length);
            System.arraycopy(encrypted, 0, result, iv.length, encrypted.length);
            return result;
        } catch (Exception e) {
            Timber.e(e, "An error occurred attempting to decrypt an OutputStream");
        }

        return null;
    }

    public byte[] encrypt(String data) {
        try {
            return data.getBytes();
        } catch (Exception e) {
            Timber.e(e, "An error occurred attempting to decrypt an OutputStream");
        }

        return null;
    }

    public byte[] decryptFromHex(String data) {
        try {
            if (!isEncrypting()) {
                return data.getBytes();
            } else {
                return decrypt(Hex.decodeHex(data.toCharArray()));
            }
        } catch (Exception e) {
            Timber.e(e, "An error occurred attempting to decrypt from hex.");
        }
        return null;
    }

    public String decryptFromHexToString(String data) {
        try {
            if (!isEncrypting()) {
                return data;
            } else {
                return new String(decrypt(Hex.decodeHex(data.trim().toCharArray())));
            }
        } catch (Exception e) {
            Timber.e(e, "An error occurred attempting to decrypt from hex.");
        }
        return null;
    }

    public byte[] decrypt(byte[] data) {
        try {
            if (!isEncrypting()) {
                return data;
            }

            // Prep the initialization vector.
            // According to the spec, the Initialization Vector shouldn't be over 96 bits (12 bytes)
            byte iv[] = new byte[ivSize];

            // get the iv from the head of data;
            System.arraycopy(data, 0, iv, 0, iv.length);
            IvParameterSpec ivSpec = new IvParameterSpec(iv);

            // get the encrypted bytes at the tail
            byte[] encrypted = new byte[data.length - iv.length];
            System.arraycopy(data, iv.length, encrypted, 0, encrypted.length);


            // get the cipher.
            Cipher cipher = Cipher.getInstance(cipherName);

            // initialize the cipher.
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, ivSpec, getSecureRandom());

            return cipher.doFinal(encrypted);
        } catch (Exception e) {
            Timber.e(e, "An error occurred attempting to decrypt.");
        }

        return null;
    }

    public byte[] decryptBase64String(String data) throws UnsupportedEncodingException {
        return decrypt(Base64.decodeBase64(data.getBytes()));
    }

}
