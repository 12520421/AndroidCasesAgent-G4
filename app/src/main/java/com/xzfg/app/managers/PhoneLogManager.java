package com.xzfg.app.managers;


import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.PowerManager;
import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.xzfg.app.Application;
import com.xzfg.app.BuildConfig;
import com.xzfg.app.Givens;
import com.xzfg.app.model.PhoneLogs;
import com.xzfg.app.security.Crypto;

import org.apache.commons.io.IOUtils;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;
import org.simpleframework.xml.stream.Format;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringWriter;
import java.net.InetSocketAddress;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.inject.Inject;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;


public class PhoneLogManager  {
    private static final String TAG = PhoneLogManager.class.getName();
    private static final ConcurrentLinkedQueue<String> phonelogQueue = new ConcurrentLinkedQueue<>();
    private static final int MAX_FIXES = 86400;

    private final ReportingHandlerThread reportingHandlerThread;
    private File logFile;
    private final PowerManager.WakeLock wakeLock;

    @Inject
    Application application;
    @Inject
    ConnectivityManager connectivityManager;

    @Inject
    PowerManager powerManager;
    @Inject
    Crypto crypto;
    @Inject
    SSLSocketFactory socketFactory;

    public PhoneLogManager(Application application) {
        application.inject(this);
        reportingHandlerThread = new ReportingHandlerThread();
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, PhoneLogManager.class.getSimpleName());
    }

    public void submitFile(File file) {
        try {
            if (application.getAgentSettings().getSecurity() == 0) {
                if (file.exists()) {
                    if (phonelogQueue != null) {
                        if (phonelogQueue.size() == MAX_FIXES) {
                            //Timber.d("Popping a log to make room for more. Max Size: " + MAX_FIXES);
                            phonelogQueue.poll();
                        }
                        phonelogQueue.add(readFile(file));
                    }
                    logFile = file;
                    //if (logFile.exists()){logFile.delete();}
                }
            }
        }
        catch (Exception ex) {
            Log.e(TAG, ex.getMessage());
        }
    }

    private String readFile(File file) {
        try {
            FileInputStream is = new FileInputStream(file);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            return new String(buffer);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return null;
        }
    }

    private boolean isConnected() {
        NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isAvailable() && activeNetwork.isConnected();
    }

    private class ReportingHandlerThread extends HandlerThread {
        private final Handler handler;
        private final Runnable runnable;

        public ReportingHandlerThread() {
            super(ReportingHandlerThread.class.getName(), Givens.THREAD_PRIORITY);
            start();
            handler = new Handler(getLooper());
            runnable = new Reporter();
        //    handler.postDelayed(runnable, getInterval());
        }

        public int getInterval() {
            int interval = 1;
            if (application.isSetupComplete() && application.getAgentSettings() != null) {
                interval = application.getAgentSettings().getReportInterval();
                if (interval == 0)
                    interval = application.getAgentSettings().getFixInterval();
            }
            return interval * 1000;
        }


        public class Reporter implements Runnable {

            private void sendLogs(PhoneLogs phoneLogs) throws Exception {

                if (isConnected() && phoneLogs != null && !phoneLogs.getPhoneLogs().isEmpty()) {
                    //Timber.d("Serializing fixes.");
                    final Format format = new Format(0);
                    Serializer serializer = new Persister(format);
                    StringWriter sw = new StringWriter();
                    serializer.write(phoneLogs, sw);

                    String responseBody = "";
                    String xml = "PostPhoneMessage=xml=" + sw.toString() + "\n";
                    xml = xml.replace("<string>", "").replace("</string>", "").replace("phoneLogs", "messages").replace("&lt;", "<").replace("&gt;", ">");

                    String content = crypto.encryptToHex(xml);
                    String ipAddress = application.getScannedSettings().getIpAddress();
                    if (ipAddress.contains("://")) {
                        ipAddress = ipAddress.split("://")[1];
                    }

                    SSLSocket socket = null;
                    OutputStream os = null;
                    InputStream is = null;

                    try {
                        socket = (SSLSocket) socketFactory.createSocket();
                        socket.connect(new InetSocketAddress(ipAddress, application.getScannedSettings().getTrackingPort().intValue()), 20000);
                        os = socket.getOutputStream();
                        os.write(content.getBytes("UTF-8"));
                        os.flush();

                        is = socket.getInputStream();
                        if (is != null) {
                            BufferedReader in = new BufferedReader(new InputStreamReader(is));
                            String line = null;
                            StringBuilder response = new StringBuilder();
                            while ((line = in.readLine()) != null) {
                                response.append(line);
                            }
                            responseBody = response.toString().trim();
                        }
                    }
                    catch (Exception ex) {
                        // TODO: Handle SSL errors here...
                        String msg = ex.getMessage();
                    }
                    finally {
                        if (socket != null && !socket.isClosed()) {
                            socket.close();
                        }
                        if (os != null) {
                            IOUtils.closeQuietly(os);
                        }
                        if (is != null) {
                            IOUtils.closeQuietly(is);
                        }
                    }

                    // if we don't receive a 200 ok, it's a network error.
                    if (responseBody.contains("Error") || responseBody.contains("ERROR") || responseBody.contains("HTTP/1.0 404 File not found") || responseBody.startsWith("ok")) {
                        if (BuildConfig.DEBUG) {
                            Crashlytics.setString("Url", ipAddress);
                            Crashlytics.setString("Content", xml);
                            Crashlytics.setString("Response", responseBody);
                        }
                        throw new Exception("Server did not respond appropriately.");
                    }

                    responseBody = crypto.decryptFromHexToString(responseBody).trim();

                    if (responseBody.contains("ERROR") || responseBody.contains("Error") || responseBody.contains("Invalid SessionId") || responseBody.contains("Invalid Session Id") || responseBody.startsWith("ok")) {
                        if (BuildConfig.DEBUG) {
                            Crashlytics.setString("Url", ipAddress);
                            Crashlytics.setString("Content", xml);
                            Crashlytics.setString("Response", responseBody);
                        }
                        throw new Exception("Server returned us an error.");
                    }

                    if (responseBody.startsWith("OK")) {
                        if (logFile.exists()){logFile.delete();}
                    }
                }
            }


            @Override
            public void run() {
                if (isConnected() && !phonelogQueue.isEmpty() && application.getAgentSettings() != null && application.getScannedSettings() != null) {
                    PhoneLogs logs = new PhoneLogs(application.getScannedSettings().getOrganizationId() + application.getDeviceIdentifier(), application.getScannedSettings().getPassword());
                    //Timber.d("New fix object instantiated. Removing fixes from global queue and adding to local queue.");
                    ConcurrentLinkedQueue<String> queue = new ConcurrentLinkedQueue<>();
                    queue.addAll(phonelogQueue);
                    phonelogQueue.clear();

                    logs.setPhoneLogs(queue);

                    try {
                        sendLogs(logs);
                    } catch (Exception e) {
                        phonelogQueue.addAll(queue);
                    }
                }
             //   handler.postDelayed(runnable, getInterval());
            }
        }
    }
}

