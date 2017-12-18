package com.xzfg.app.services;

import android.content.Context;
import android.content.Intent;

import com.xzfg.app.Application;
import com.xzfg.app.Events;
import com.xzfg.app.R;
import com.xzfg.app.model.ScannedSettings;
import com.xzfg.app.util.Network;

import java.net.URLEncoder;

import javax.inject.Inject;
import javax.net.ssl.SSLHandshakeException;

import de.greenrobot.event.EventBus;
import me.dm7.barcodescanner.zbar.BarcodeFormat;
import me.dm7.barcodescanner.zbar.Result;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import timber.log.Timber;

public class LoginService extends BackWakeIntentService {
    public static final String URL_KEY = LoginService.class.getName() + "_" + "url";
    public static final String PORT_KEY = LoginService.class.getName() + "_" + "port";
    public static final String USERNAME_KEY = LoginService.class.getName() + "_" + "username";
    public static final String PASSWORD_KEY = LoginService.class.getName() + "_" + "password";

    @Inject
    Application application;

    @Inject
    OkHttpClient httpClient;


    public LoginService() {
        super(LoginService.class.getName());
    }

    public void onCreate() {
        super.onCreate();
        ((Application)getApplication()).inject(this);
    }

    public static void login(Context context, String url, String port, String username, String password) {
        Intent intent = new Intent(context,LoginService.class);
        intent.putExtra(URL_KEY,url);
        intent.putExtra(PORT_KEY,port);
        intent.putExtra(USERNAME_KEY,username);
        intent.putExtra(PASSWORD_KEY,password);
        context.startService(intent);
    }

    @Override
    protected void doWork(Intent intent) {
        try {
            String url = intent.getStringExtra(URL_KEY);
            String port = intent.getStringExtra(PORT_KEY);
            String username = intent.getStringExtra(USERNAME_KEY);
            String password = intent.getStringExtra(PASSWORD_KEY);

            if (url.contains("://")) {
                url = url.substring(url.indexOf("://") + 3);
            }
            if (url.endsWith("/")) {
                url = url.substring(0, url.length() - 1);
            }

            StringBuilder urlString = new StringBuilder(192);

            urlString.append(url);
            urlString.append(":");
            urlString.append(String.valueOf(port));
            urlString.append(getString(R.string.registration_login_endpoint));
            urlString.append("?username=").append(URLEncoder.encode(username, "UTF-8"));
            urlString.append("&password=").append(URLEncoder.encode(password, "UTF-8"));

            String callString = urlString.toString();
            //Timber.d("Create Account Url: " + callString + " Length: " + callString.length());

            Response httpResponse = null;
            String responseBody = null;

            try {
                httpResponse = httpClient.newCall(new Request.Builder().url("https://" + callString).build()).execute();
                responseBody = httpResponse.body().string().trim();

                // if we don't receive a 200 ok, it's a network error.
                if (httpResponse.code() != 200 || responseBody.contains("HTTP/1.0 404 File not found")) {
                    throw new Exception("The server returned an error.");
                }
            }
            catch (Exception a) {
                Timber.e(a,"First Call Failed.");
                try {
                    httpResponse = httpClient.newCall(new Request.Builder().url("https://" + callString).build()).execute();
                    responseBody = httpResponse.body().string().trim();
                    if (httpResponse.code() != 200 || responseBody.contains("HTTP/1.0 404 File not found")) {
                        throw new Exception("The server returned an error.",a);
                    }
                }
                catch (Exception b) {
                    Timber.w(b, "Second Call Failed.");
                    if (b instanceof SSLHandshakeException)
                        throw new SSLHandshakeException(b.getMessage());
                    else
                        throw new Exception("The server returned an error.",b);
                }
            }

            if (responseBody.startsWith("Error:")) {
                EventBus.getDefault().postSticky(new Events.Registration(false, responseBody.substring(responseBody.indexOf(":")+1).trim()));
                return;
            }

            Result result = new Result();
            result.setBarcodeFormat(BarcodeFormat.QRCODE);
            result.setContents(responseBody);
            final ScannedSettings settings = ScannedSettings.parse(application, result);

            Intent registrationIntent = new Intent(application, RegistrationService.class);
            registrationIntent.putExtra(ScannedSettings.class.getName(), settings);
            application.startService(registrationIntent);


        }
        catch (Exception e) {
            if (!Network.isNetworkException(e)) {
                Timber.e(e, "Login registration failed.");
            }
            String msg = getString(R.string.registration_failed);
            if (e instanceof SSLHandshakeException) {
                msg += (" " + getString(R.string.ssl_error));
            }
            EventBus.getDefault().postSticky(new Events.Registration(false, msg));
        }

    }
}
