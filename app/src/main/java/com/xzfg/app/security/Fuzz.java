package com.xzfg.app.security;


import com.xzfg.app.codec.Base64;
import com.xzfg.app.codec.Hex;

import timber.log.Timber;

/**
 * The fuzz class handles enfuzzing and defuzzing of a string.
 */
public class Fuzz {

    private Fuzz() {
    }

    /**
     * Takes a normal string, and fuzzes it.
     */
    public static String en(String data, String key) {
        try {
            // rot 13
            data = rotate(data);
            // base64 encode
            data = Base64.encodeBase64URLSafeString(data.getBytes("UTF-8"));
            // xor
            data = enxor(data, new StringBuilder(key).reverse().toString());
            data = Hex.encodeHexString(data.getBytes("UTF-8"));
        } catch (Exception e) {
            Timber.e(e, "Couldn't encode data.");
        }
        return data;
    }


    /**
     * Takes a fuzzed string, and de-fuzzes it.
     */
    public static String de(String data, String key) {
        try {
            data = new String(Hex.decodeHex(data.toCharArray()));
            data = dexor(data, new StringBuilder(key).reverse().toString());
            data = new String(Base64.decodeBase64(data.getBytes()));
            data = rotate(data);
        } catch (Exception e) {
            Timber.e(e, "Failled to defuzz.");
        }
        return data;
    }

    private static String rotate(String input) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (c >= 'a' && c <= 'm') c += 13;
            else if (c >= 'A' && c <= 'M') c += 13;
            else if (c >= 'n' && c <= 'z') c -= 13;
            else if (c >= 'N' && c <= 'Z') c -= 13;
            sb.append(c);
        }
        return sb.toString();
    }

    public static String enxor(String message, String key) {
        try {
            if (message == null || key == null) return null;

            char[] keys = key.toCharArray();
            char[] mesg = message.toCharArray();

            int ml = mesg.length;
            int kl = keys.length;
            char[] newmsg = new char[ml];

            for (int i = 0; i < ml; i++) {
                newmsg[i] = (char) (mesg[i] ^ keys[i % kl]);
            }
            mesg = null;
            keys = null;
            String temp = new String(newmsg);
            return temp;
        } catch (Exception e) {
            return null;
        }
    }


    public static String dexor(String message, String key) {
        try {
            if (message == null || key == null) return null;
            char[] keys = key.toCharArray();
            char[] mesg = message.toCharArray();

            int ml = mesg.length;
            int kl = keys.length;
            char[] newmsg = new char[ml];

            for (int i = 0; i < ml; i++) {
                newmsg[i] = (char) (mesg[i] ^ keys[i % kl]);
            }
            mesg = null;
            keys = null;
            return new String(newmsg);
        } catch (Exception e) {
            return null;
        }
    }
}
