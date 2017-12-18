package com.xzfg.net.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.xzfg.app.codec.Hex;
import com.xzfg.app.model.User;
import com.xzfg.app.model.Users;
import com.xzfg.app.security.Crypto;

import org.junit.Before;
import org.junit.Test;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.OkHttpClient.Builder;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 */
public abstract class NetTestAbstract {
    public static OkHttpClient client;

    public abstract long getPort();
    public abstract Crypto getCrypto() throws Exception;

    public String getIpAddress() {
        return "162.209.98.160";
    }

    public long getOrganizationId() {
        return 2L;
    }

    public String getUserName() {
        return "seguetech";
    }

    public String getPassword() {
        return "stargate5";
    }

    public String getUserId() {
        return "pBlrIIXOh/4=";
    }

    @Before
    public void setup() {
        OkHttpClient.Builder builder = new Builder();
        builder.connectTimeout(5L,TimeUnit.SECONDS);
        builder.readTimeout(5L, TimeUnit.SECONDS);
        builder.writeTimeout(5L, TimeUnit.SECONDS);
        client = builder.build();
    }


    public void appendParam(StringBuilder stringBuilder, String param, String value) throws UnsupportedEncodingException {
        if (stringBuilder.length() > 0) {
            stringBuilder.append("&");
        }
        stringBuilder.append(URLEncoder.encode(param, "UTF-8"));
        stringBuilder.append("=");
        stringBuilder.append(URLEncoder.encode(value,"UTF-8"));
    }

    public String getSerial() throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA1");
        String hashBytes = Hex.encodeHexString(md.digest("TEST_DEVICE".getBytes("UTF-8")));
        return hashBytes;
    }

    @Test
    public void getTimeTest() throws Exception {
        System.out.println("\nRunning getTimeTest.");
        Crypto crypto = getCrypto();

        String url = "https://"+getIpAddress()+":"+getPort()+"/bluebird/Message.aspx?message=GetTime";

        if (crypto.isEncrypting()) {
            String[] parts = url.split("\\?");
            url = parts[0] + "?" + Hex.encodeHexString(crypto.encrypt(parts[1]));
        }

        System.out.println("Requesting: " + url);

        Response response = client.newCall(new Request.Builder().url(url).build()).execute();
        assertNotNull("No response received from server.", response);
        System.out.println("Response Status: " + response.code() + " - " + response.message());
        assertEquals("A non-200 response was received.", response.code(),200);

        ResponseBody body = response.body();
        assertNotNull(body);
        String bodyString = body.string().trim();
        System.out.println("Server Returned: " + bodyString);
        if (crypto.isEncrypting()) {
            System.out.println("Decrypted URL Parameters: " + new String(crypto.decrypt(Hex.decodeHex(url.split("\\?")[1].toCharArray())),"UTF-8"));
            bodyString = new String(crypto.decrypt(Hex.decodeHex(bodyString.toCharArray())));
            System.out.println("Decrypted Response: " + bodyString);
        }

        assertFalse("A 404 error was found in the response body.", bodyString.contains("404"));
    }

    @Test
    public void registrationTest() throws Exception {
        System.out.println("\nRunning registration test.");
        Crypto crypto = getCrypto();

        String url = "https://"+getIpAddress()+":"+getPort()+"/bluebird/Register.aspx?";
        StringBuilder sb = new StringBuilder();
        appendParam(sb, "model", "CASESAgent");
        appendParam(sb, "password", getPassword());
        appendParam(sb, "serial", getSerial());
        appendParam(sb, "userId", getUserId());

        if (crypto.isEncrypting()) {
            url += Hex.encodeHexString(crypto.encrypt(sb.toString()));
        }
        else {
            url += sb.toString();
        }

        System.out.println("Requesting: " + url);

        Response response = client.newCall(new Request.Builder().url(url).build()).execute();
        assertNotNull("No response received from server.", response);
        System.out.println("Response Status: " + response.code() + " - " + response.message());
        assertEquals("A non-200 response was received.", response.code(),200);

        ResponseBody body = response.body();
        assertNotNull(body);
        String bodyString = body.string().trim();
        System.out.println("Server Returned: " + bodyString);

        assertFalse("A 404 error was found in the response body.", bodyString.contains("404"));

        if (crypto.isEncrypting()) {
            System.out.println("Decrypted URL Parameters: " + new String(crypto.decrypt(Hex.decodeHex(url.split("\\?")[1].toCharArray())),"UTF-8"));
            bodyString = new String(crypto.decrypt(Hex.decodeHex(bodyString.toCharArray())));
            System.out.println("Decrypted Response: " + bodyString);
        }

        assertTrue(bodyString.contains("OK"));
    }

    @Test
    public void chatRegistrationTest() throws Exception {
        System.out.println("\nRunning chat registration test.");
        Crypto crypto = getCrypto();

        String url = "https://"+getIpAddress()+":"+getPort()+"/bluebird/LogInUserByKey.aspx?";
        StringBuilder sb = new StringBuilder();
        appendParam(sb, "encKey", getUserId());

        if (crypto.isEncrypting()) {
            url += Hex.encodeHexString(crypto.encrypt(sb.toString()));
        }
        else {
            url += sb.toString();
        }

        System.out.println("Requesting: " + url);

        Response response = client.newCall(new Request.Builder().url(url).build()).execute();
        assertNotNull("No response received from server.", response);
        System.out.println("Response Status: " + response.code() + " - " + response.message());
        assertEquals("A non-200 response was received.", response.code(),200);

        ResponseBody body = response.body();
        assertNotNull(body);
        String bodyString = body.string().trim();
        System.out.println("Server Returned: " + bodyString);

        assertFalse("A 404 error was found in the response body.", bodyString.contains("404"));

        if (crypto.isEncrypting()) {
            System.out.println("Decrypted URL Parameters: " + new String(crypto.decrypt(Hex.decodeHex(url.split("\\?")[1].toCharArray())),"UTF-8"));
            bodyString = new String(crypto.decrypt(Hex.decodeHex(bodyString.toCharArray())));
            System.out.println("Decrypted Response: " + bodyString);
        }

        assertTrue(bodyString.length() == 36);

    }

    private String getSessionId() {
        try {
            System.out.println("\nRunning getMessengerUsers test.");
            Crypto crypto = getCrypto();
            String url = "https://" + getIpAddress() + ":" + getPort() + "/bluebird/LogInUserByKey.aspx?";
            StringBuilder sb = new StringBuilder();
            appendParam(sb, "encKey", getUserId());

            if (crypto.isEncrypting()) {
                url += Hex.encodeHexString(crypto.encrypt(sb.toString()));
            } else {
                url += sb.toString();
            }

            System.out.println("Requesting: " + url);

            Response response = client.newCall(new Request.Builder().url(url).build()).execute();
            System.out.println("Response Status: " + response.code() + " - " + response.message());

            ResponseBody body = response.body();
            String bodyString = body.string().trim();


            if (crypto.isEncrypting()) {
                System.out.println("Decrypted URL Parameters: " + new String(crypto.decrypt(Hex.decodeHex(url.split("\\?")[1].toCharArray())), "UTF-8"));
                bodyString = new String(crypto.decrypt(Hex.decodeHex(bodyString.toCharArray())));
                System.out.println("Decrypted Response: " + bodyString);
            }
            return bodyString;
        }
        catch (Exception e) {
            return null;
        }

    }

    @Test
    public void getMessageUsersTest() throws Exception {
        System.out.println("\nRunning chat getMessageUsers test.");
        String sessionId = getSessionId();
        assertNotNull(sessionId);

        Crypto crypto = getCrypto();

        String url = "https://"+getIpAddress()+":"+getPort()+"/bluebird/MessengerGetUsers.aspx?";
        StringBuilder sb = new StringBuilder();
        appendParam(sb, "sessionId", sessionId);

        if (crypto.isEncrypting()) {
            url += Hex.encodeHexString(crypto.encrypt(sb.toString()));
        }
        else {
            url += sb.toString();
        }

        System.out.println("Requesting: " + url);

        Response response = client.newCall(new Request.Builder().url(url).build()).execute();
        assertNotNull("No response received from server.", response);
        System.out.println("Response Status: " + response.code() + " - " + response.message());
        assertEquals("A non-200 response was received.", response.code(),200);

        ResponseBody body = response.body();
        assertNotNull(body);
        String bodyString = body.string().trim();
        System.out.println("Server Returned: " + bodyString);

        assertFalse("A 404 error was found in the response body.", bodyString.contains("404"));

        if (crypto.isEncrypting()) {
            System.out.println("Decrypted URL Parameters: " + new String(crypto.decrypt(Hex.decodeHex(url.split("\\?")[1].toCharArray())),"UTF-8"));
            bodyString = new String(crypto.decrypt(Hex.decodeHex(bodyString.toCharArray())));
            System.out.println("Decrypted Response: " + bodyString);
        }

        Serializer serializer = new Persister();
        Users users = serializer.read(Users.class,bodyString);
        assertNotNull(users);
        //assertNull(users.getErrors());
        assertNotNull(users.getUsers());
        assertTrue(users.getUsers().size() > 0);
        Collections.sort(users.getUsers());
        for ( User user : users.getUsers()) {
            System.out.println(user.toString());
        }
    }

    @Test
    public void getMessagesTest() throws Exception {
        System.out.println("\nRunning chat getUsers test.");
        String sessionId = getSessionId();
        assertNotNull(sessionId);

        Crypto crypto = getCrypto();

        String url = "https://"+getIpAddress()+":"+getPort()+"/bluebird/MessengerGetMessages.aspx?";
        StringBuilder sb = new StringBuilder();
        appendParam(sb, "sessionId", sessionId);

        if (crypto.isEncrypting()) {
            url += Hex.encodeHexString(crypto.encrypt(sb.toString()));
        }
        else {
            url += sb.toString();
        }

        System.out.println("Requesting: " + url);

        Response response = client.newCall(new Request.Builder().url(url).build()).execute();
        assertNotNull("No response received from server.", response);
        System.out.println("Response Status: " + response.code() + " - " + response.message());
        assertEquals("A non-200 response was received.", response.code(),200);

        ResponseBody body = response.body();
        assertNotNull(body);
        String bodyString = body.string().trim();
        System.out.println("Server Returned: " + bodyString);

        assertFalse("A 404 error was found in the response body.", bodyString.contains("404"));

        if (crypto.isEncrypting()) {
            System.out.println("Decrypted URL Parameters: " + new String(crypto.decrypt(Hex.decodeHex(url.split("\\?")[1].toCharArray())),"UTF-8"));
            bodyString = new String(crypto.decrypt(Hex.decodeHex(bodyString.toCharArray())));
            System.out.println("Decrypted Response: " + bodyString);
        }

        //assertTrue(bodyString.length() == 36);
    }

}
