package com.xzfg.app.managers;

import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;

import com.crashlytics.android.Crashlytics;
import com.xzfg.app.Application;
import com.xzfg.app.BuildConfig;
import com.xzfg.app.Events;
import com.xzfg.app.R;
import com.xzfg.app.model.url.LoginUrl;
import com.xzfg.app.security.Crypto;
import com.xzfg.app.util.Network;

import javax.inject.Inject;

import de.greenrobot.event.EventBus;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import timber.log.Timber;


public class SessionManager {

    private final Object sessionIdLock = new Object();
    @Inject
    Crypto crypto;
    @Inject
    Application application;
    @Inject
    ConnectivityManager connectivityManager;
    @Inject
    OkHttpClient httpClient;
    private volatile SignInHandlerThread signInThread;
    private volatile String mSessionId;
    private volatile Long lastSet;
    private volatile boolean inProgress = false;
    private volatile long delay = 100;

    public SessionManager(Application application) {
        application.inject(this);
        EventBus.getDefault().register(this);
        //Timber.d("SESSION MANAGER STARTED.");
    }

    public String getSessionId() {
        synchronized (sessionIdLock) {
            if (mSessionId == null && signInThread == null) {
                //Timber.d("GETTING NEW SESSION ID.");
                signInThread = new SignInHandlerThread();
            }
            if (mSessionId != null && signInThread != null) {
                signInThread.kill();
                signInThread = null;
            }
            return mSessionId;
        }
    }

    private void setSessionId(String sessionId) {
        synchronized (sessionIdLock) {
            this.mSessionId = sessionId;
            if (sessionId != null) {
                this.lastSet = System.currentTimeMillis();
                signInThread.kill();
                signInThread = null;
            } else {
                if (signInThread == null) {
                    signInThread = new SignInHandlerThread();
                }
            }
        }
        EventBus.getDefault().postSticky(new Events.Session(this.mSessionId));
    }

    public void onEventMainThread(Events.InvalidSession event) {
        synchronized (sessionIdLock) {
            setSessionId(null);
        }
    }

    public void onEventMainThread(Events.NetworkAvailable event) {
        // if the network becomes available, and we haven't gotten a new session id in the last 30
        // seconds, clear the existing session id, and get a new one.
        synchronized (sessionIdLock) {
            if (mSessionId == null || (lastSet != null && System.currentTimeMillis() - lastSet >= 30000)) {
                setSessionId(null);
            }
        }
    }


    public void onEventMainThread(Events.NetworkStatus event) {
        if (event.isUp()) {
            synchronized (sessionIdLock) {
                if (mSessionId == null || (lastSet != null && System.currentTimeMillis() - lastSet >= 30000)) {
                    setSessionId(null);
                }
            }
        } else {
            if (signInThread != null) {
                signInThread.kill();
            }
        }
    }

    private boolean isConnected() {
        NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isAvailable() && activeNetwork.isConnected();
    }

    private class SignInHandlerThread extends HandlerThread {
        private final Handler mHandler;
        private final Runnable runnable;
        private volatile boolean alive = true;

        public SignInHandlerThread() {
            // use THREAD_PRIORITY_LESS_FAVORABLE to decrease the thread priority by 2 steps.
            // this abouve THREAD_PRIORITY_BACKGROUND, as that results in too many skipped attempts,
            // yet low enough that it should never impact the UI.
            super(SignInHandlerThread.class.getName(), android.os.Process.THREAD_PRIORITY_BACKGROUND + android.os.Process.THREAD_PRIORITY_MORE_FAVORABLE);
            start();
            mHandler = new Handler(getLooper());
            runnable = new SignInRunnable();
            //Timber.d("SIGN IN HANDLER INITIALIZED");
            mHandler.post(runnable);
        }

        public void kill() {
            alive = false;
            mHandler.removeCallbacks(runnable);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                quitSafely();
            } else {
                quit();
            }
        }

        public class SignInRunnable implements Runnable {
            public void run() {
                //Timber.d("RUNNING...");
                synchronized (sessionIdLock) {
                    if (mSessionId != null) {
                        kill();
                        return;
                    }

                    if (alive && application.getScannedSettings() != null && isConnected() && mSessionId == null) {
                        try {
                            //Timber.d("GOT SCANNED SETTINGS!");
                            LoginUrl loginUrl = new LoginUrl(application, application.getString(R.string.login_endpoint));
                            //Timber.d("LOGIN Calling (Unencrypted)" + loginUrl.toString());
                            //Timber.d("LOGIN Calling (Encrypted)" + loginUrl.toString(crypto));
                            Response response = httpClient.newCall(new Request.Builder().url(loginUrl.toString(crypto)).build()).execute();
                            String responseBody = response.body().string();
                            response.body().close();

                            //Timber.d("LOGIN Response Body (encrypted): " + responseBody);

                            // if we don't receive a 200 ok, it's a network error.
                            if (response == null || response.code() != 200 || responseBody == null || responseBody.contains("HTTP/1.0 404 File not found") || responseBody.startsWith("ok")) {
                                if (BuildConfig.DEBUG) {
                                    Crashlytics.setString("Url", loginUrl.toString());
                                    Crashlytics.setLong("Response Code", response.code());
                                    Crashlytics.setString("Response", responseBody);
                                }
                                throw new Exception("Server did not respond appropriately.");
                            }

                            if (responseBody.startsWith("Error:") || responseBody.startsWith("ERROR:")) {
                                if (BuildConfig.DEBUG) {
                                    Crashlytics.setString("Url", loginUrl.toString());
                                    Crashlytics.setLong("Response Code", response.code());
                                    Crashlytics.setString("Response", responseBody);
                                }
                                throw new Exception("Server returned us an error.");
                            }

                            responseBody = crypto.decryptFromHexToString(responseBody).trim();


                            if (responseBody.startsWith("ok") || responseBody.length() != 36) {
                                if (BuildConfig.DEBUG) {
                                    Crashlytics.setString("Url", loginUrl.toString());
                                    Crashlytics.setLong("Response Code", response.code());
                                    Crashlytics.setString("Response", responseBody);
                                }
                                throw new Exception("Server did not respond appropriately.");
                            }

                            if (responseBody.startsWith("Error:") || responseBody.startsWith("ERROR:")) {
                                if (BuildConfig.DEBUG) {
                                    Crashlytics.setString("Url", loginUrl.toString());
                                    Crashlytics.setLong("Response Code", response.code());
                                    Crashlytics.setString("Response", responseBody);
                                }
                                throw new Exception("Server returned us an error.");
                            }

                            //Timber.d("LOGIN Response Body (unencrypted): " + responseBody);
                            //Timber.d("LOGIN Setting sessionId to: " + responseBody.trim());
                            setSessionId(responseBody.trim());
                            // Send notification
                            EventBus.getDefault().post(new Events.SessionAcquired(mSessionId));

                        } catch (Exception e) {
                            if (!Network.isNetworkException(e)) {
                                Timber.w(e, "An error occurred attempting to sign-in for SessionId.");
                            }
                            delay = Math.min(60000, (delay * 2));
                        }

                        if (alive) {
                            if (mSessionId == null) {
                                mHandler.postDelayed(this, delay);
                                return;
                            } else {
                                delay = 1000;
                                return;
                            }
                        }
                    } else {
                        if (alive) {
                            mHandler.postDelayed(this, 3000);
                        }
                    }
                }

            }
        }
    }

}
