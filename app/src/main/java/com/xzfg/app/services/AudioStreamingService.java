package com.xzfg.app.services;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.NetworkOnMainThreadException;
import android.os.ParcelFileDescriptor;
import android.os.PowerManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;

import com.xzfg.app.Application;
import com.xzfg.app.BuildConfig;
import com.xzfg.app.Events;
import com.xzfg.app.Givens;
import com.xzfg.app.R;
import com.xzfg.app.activities.MainActivity;
import com.xzfg.app.managers.FixManager;
import com.xzfg.app.managers.MediaManager;
import com.xzfg.app.model.url.MessageUrl;
import com.xzfg.app.security.Crypto;
import com.xzfg.app.util.BatteryUtil;
import com.xzfg.app.util.MessageUtil;
import com.xzfg.app.util.Network;

import org.apache.commons.io.IOUtils;
import org.joda.time.DateTime;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Queue;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.inject.Inject;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import de.greenrobot.event.EventBus;
import timber.log.Timber;

/**
 *
 */
public class AudioStreamingService extends Service implements MediaRecorder.OnErrorListener {
    private static final int SOCKET_TIMEOUT = 20000;
    private static final int[] frameSizes = new int[8];
    private static final byte[] amrHeader = new byte[6];
    protected volatile boolean mStreaming = false;
    @Inject
    Application application;
    @Inject
    PowerManager powerManager;
    @Inject
    MediaManager mediaManager;
    @Inject
    Crypto crypto;
    @Inject
    FixManager fixManager;
    @Inject
    SSLSocketFactory socketFactory;
    boolean started = false;
    private PowerManager.WakeLock wakeLock;
    private MediaRecorder mediaRecorder;
    private AudioHandlerThread audioHandlerThread;
    private ParcelFileDescriptor[] pipe;
    private Queue<byte[]> frameQueue = new ConcurrentLinkedQueue<>();
    private AudioSenderHandlerThread senderHandlerThread;
    private long maxDurationTime = -1;

    private Notification notification;

    @Override
    public void onCreate() {
        super.onCreate();
        ((Application) getApplication()).inject(this);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, AudioStreamingService.class.getName());
        if (wakeLock.isHeld()) {
            wakeLock.release();
        }
        wakeLock.acquire();
        if (BuildConfig.AUDIO_NOTIFICATION) {
            Intent notificationIntent = new Intent(this, MainActivity.class);
            notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NO_ANIMATION);
            PendingIntent contentIntent = PendingIntent.getActivity(this,
                    0, notificationIntent,
                    PendingIntent.FLAG_CANCEL_CURRENT);
            notification = new NotificationCompat.Builder(this)
                    .setSmallIcon(R.drawable.ic_action_mic)
                    .setColor(ContextCompat.getColor(this,R.color.redorange))
                    .setContentTitle(getString(R.string.audio_recording))
                    .setWhen(System.currentTimeMillis())
                    .setShowWhen(true)
                    .setContentIntent(contentIntent)
                    .setOngoing(true)
                    .build();
        }

        frameSizes[0] = 13;
        frameSizes[1] = 14;
        frameSizes[2] = 16;
        frameSizes[3] = 18;
        frameSizes[4] = 20;
        frameSizes[5] = 21;
        frameSizes[6] = 27;
        frameSizes[7] = 32;
        amrHeader[0] = 35;
        amrHeader[1] = 33;
        amrHeader[2] = 65;
        amrHeader[3] = 77;
        amrHeader[4] = 82;
        amrHeader[5] = 10;

        try {
            senderHandlerThread = new AudioSenderHandlerThread();
            audioHandlerThread = new AudioHandlerThread();
        } catch (Exception e) {
            Timber.e(e, "client setup");
        }

        // register for events.
        //EventBus.getDefault().register(this);

        try {
            pipe = ParcelFileDescriptor.createPipe();
        } catch (IOException e) {
            Timber.e(e, "Couldn't create pipe.");
            return;
        }

        mediaRecorder = new MediaRecorder();
      //  mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.AMR_NB);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        mediaRecorder.setOutputFile(pipe[1].getFileDescriptor());
        //mediaRecorder.setAudioSamplingRate(44100);
        //mediaRecorder.setAudioEncodingBitRate(96000);
        mediaRecorder.setOnErrorListener(this);

    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent i, int flags, int startId) {
        if (mStreaming || started) {
            stopSelf();
            return START_NOT_STICKY;
        }

        started = true;
        mStreaming = true;
        if (BuildConfig.AUDIO_NOTIFICATION && notification != null) {
            startForeground(Givens.NOTIFICATION_AUDIO_STREAMING_ID, notification);
        }

        senderHandlerThread.begin();
        audioHandlerThread.begin();

        // Set time to stop streaming
        if (i.getExtras() != null) {
            int duration = i.getExtras().getInt(Givens.EXTRA_MAX_DURATION, -1);
            if (duration > 0) {
                maxDurationTime = DateTime.now().getMillis() + duration;
            }
        }

        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        if (EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this);
        }
        stopStreaming();
        stopForeground(true);
        if (wakeLock.isHeld()) {
            wakeLock.release();
        }
        super.onDestroy();
    }

    private synchronized void stopStreaming() {
        if (mStreaming) {
            mStreaming = false;

            if (audioHandlerThread != null) {
                AudioHandlerThread deadThread = audioHandlerThread;
                audioHandlerThread = null;
                deadThread.end();
            }
            if (senderHandlerThread != null) {
                AudioSenderHandlerThread deadThread = senderHandlerThread;
                senderHandlerThread = null;
                deadThread.end();
            }
            stopForeground(true);
        }
    }

    @Override
    public void onError(MediaRecorder mr, int what, int extra) {
        switch (what) {
            case MediaRecorder.MEDIA_ERROR_SERVER_DIED: {
                Timber.e("Media Recorder Stopped Unexpectedly. Server Died.");
                MessageUtil.sendMessage(application, MessageUtil.getMessageUrl(application, "Audio Streaming Stopped Unexpectedly. Server Died."));
                break;
            }
            case MediaRecorder.MEDIA_RECORDER_ERROR_UNKNOWN: {
                Timber.e("Media Recorder Stopped Unexpectedly. Cause unknown.");
                MessageUtil.sendMessage(application, MessageUtil.getMessageUrl(application, "Audio Streaming Stopped Unexpectedly. Cause Unknown."));
                break;
            }
        }
        stopForeground(true);
        stopSelf();
    }

    // Adds audio frame to the frame queue
    public void queueFrame(byte[] frame) {
        frameQueue.add(frame);

        // Stop streaming if max duration reached
        if (maxDurationTime > 0 && DateTime.now().getMillis() > maxDurationTime) {
            stopForeground(true);
            stopSelf();
        }
    }


    // This thread captures audio frames from MediaRecorder and adds them to the queue
    private final class AudioHandlerThread extends HandlerThread {
        private final Handler mHandler;
        private final AudioRunnable runnable;

        private volatile boolean alive = false;
        private volatile boolean stopping = false;
        private volatile boolean started = false;

        public AudioHandlerThread() {
            super(AudioHandlerThread.class.getName(), android.os.Process.THREAD_PRIORITY_DEFAULT + (2 * android.os.Process.THREAD_PRIORITY_LESS_FAVORABLE));
            start();
            mHandler = new Handler(getLooper());
            runnable = new AudioRunnable();
            //Timber.d("Audio Streaming Thread Created.");
        }

        public synchronized void end() {
            stopping = true;
            //Timber.d("Stopping Audio Streaming Thread.");

            mHandler.removeCallbacks(runnable);

            if (mediaRecorder != null) {
                try {
                    mediaRecorder.stop();
                } catch (Exception e) {
                    Timber.w(e, "Couldn't stop media recorder.");
                }
                try {
                    mediaRecorder.reset();
                } catch (Exception e) {
                    Timber.w(e, "Couldn't reset media recorder.");
                }
                try {
                    mediaRecorder.release();
                } catch (Exception e) {
                    Timber.w(e, "Couldn't release media recorder.");
                }
                mediaRecorder = null;

            }
            alive = false;

            if (pipe != null && pipe.length == 2) {
                try {
                    pipe[1].close();
                } catch (Exception e) {
                    Timber.e(e, "Couldn't close write pipe.");
                }

                try {
                    pipe[0].close();
                } catch (Exception e) {
                    Timber.e(e, "Couldn't close read pipe");
                }
            }
            interrupt();


            mediaManager.audioRecordingEnded();
            if (started) {
                MessageUtil.sendMessage(application, MessageUtil.getMessageUrl(application, MessageUrl.MESSAGE_LIVE_AUDIO_STOP));
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                quitSafely();
            } else {
                quit();
            }
        }

        public void begin() {
            if (!alive) {
                //Timber.d("Starting Audio Streaming Thread.");
                alive = true;
                mHandler.post(runnable);
            }
        }


        private class AudioRunnable implements Runnable {

            @Override
            public void run() {
                try {
                    StringBuilder headerBuffer = new StringBuilder(192);
                    headerBuffer.append("uid=").append(application.getScannedSettings().getOrganizationId()).append(application.getDeviceIdentifier());
                    headerBuffer.append("|format=amr");
                    Location lastLocation = fixManager.getLastLocation();
                    if (lastLocation != null) {
                        headerBuffer.append("|latitude=").append(String.valueOf(lastLocation.getLatitude()));
                        headerBuffer.append("|longitude=").append(String.valueOf(lastLocation.getLongitude()));
                        headerBuffer.append("|provider=").append(lastLocation.getProvider());
                        headerBuffer.append("|accuracy=").append(String.valueOf(lastLocation.getAccuracy()));
                        headerBuffer.append("|speed=").append(String.valueOf(lastLocation.getSpeed()));
                        headerBuffer.append("|bearing=").append(lastLocation.getBearing());
                        SimpleDateFormat f = new SimpleDateFormat("yyyy-MMM-dd HH:mm:ss");
                        f.setTimeZone(TimeZone.getTimeZone("UTC"));
                        headerBuffer.append("|dateUTC=").append(f.format(new Date(lastLocation.getTime())));
                    }
                    Integer batteryLevel = BatteryUtil.getBatteryLevel(application);
                    if (batteryLevel != null) {
                        headerBuffer.append("|battery=").append(String.valueOf(batteryLevel));
                    }
                    if (application.getAgentSettings() != null) {
                        if (application.getAgentSettings().getCaseNumber() != null && !application.getAgentSettings().getCaseNumber().trim().isEmpty()) {
                            headerBuffer.append("|caseNumber=").append(application.getAgentSettings().getCaseNumber());
                        }
                        if (application.getAgentSettings().getCaseDescription() != null && !application.getAgentSettings().getCaseDescription().trim().isEmpty()) {
                            headerBuffer.append("|msg=").append(application.getAgentSettings().getCaseDescription());
                        }
                    }

                    //Timber.d("Sending header: " + headerString);
                    byte[] header = crypto.encrypt(headerBuffer.toString());

                    // Add frame to the queue. AudioSenderHandlerThread will send to server
                    queueFrame(header);

                    if (alive) {
                        //Timber.d("Sending start message.");
                        MessageUtil.sendMessage(application, MessageUtil.getMessageUrl(application, MessageUrl.MESSAGE_LIVE_AUDIO_START));
                        mediaManager.audioRecordingStarted();
                        started = true;
                    }

                    mediaRecorder.prepare();
                    Thread.sleep(1000);
                    mediaRecorder.start();

                    int b = 0;
                    // skip first 6 bytes
                    int headerCount = 0;
                    byte[] frameBuffer = new byte[10240];
                    int frameBufferSize = 0;
                    int currentFrameSize = 0;
                    int currentFrameBytesRead = 0;
                    int frameCount = 0;
                    InputStream is = null;
                    try {

                        is = new ParcelFileDescriptor.AutoCloseInputStream(pipe[0]);
                        Timber.e("Available: " + is.available());
                    } catch (Exception e) {
                        Timber.e(e, "Couldn't get input stream.");
                    }
                    try {
                        while (alive && is != null && (b = is.read()) != -1) {
                            headerCount++;
                            if (headerCount < 7) {
                                continue;
                            }
                            currentFrameBytesRead++;
                            frameBuffer[frameBufferSize] = (byte) b;
                            if (currentFrameSize == 0) {
                                // Must be a header byte
                                try {
                                    int h = b << 1;
                                    int cmr = ((h & 0xF0) >> 4);
                                    currentFrameSize = frameSizes[cmr];
                                    frameCount++;
                                } catch (Exception e) {
                                    Timber.e(e, "AUDIO");
                                }
                            } else {
                                if (currentFrameBytesRead == currentFrameSize) {
                                    currentFrameBytesRead = 0;
                                    currentFrameSize = 0;
                                }
                            }
                            if (frameCount > 251) {
                                frameCount = 0;
                                // send 5 seconds of frames as one package and
                                // reset. One frame is roughly 20ms.
                                byte[] frameBufferCopy = new byte[frameBufferSize + amrHeader.length];
                                System.arraycopy(amrHeader, 0, frameBufferCopy, 0, amrHeader.length);
                                System.arraycopy(frameBuffer, 0, frameBufferCopy, amrHeader.length, frameBufferSize);
                                byte[] audioPackage;
                                if (crypto.isEncrypting()) {
                                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                                    OutputStream cryptoStream = crypto.getOutputStream(baos);
                                    IOUtils.copy(new ByteArrayInputStream(frameBufferCopy), cryptoStream);
                                    cryptoStream.flush();
                                    audioPackage = baos.toByteArray();
                                } else {
                                    audioPackage = frameBufferCopy;
                                }

                                if (alive) {
                                    // Add frame to the queue. AudioSenderHandlerThread will send to server
                                    queueFrame(audioPackage);
                                }
                                frameBufferSize = 0;
                            }
                            frameBufferSize++;
                        }
                    } catch (IOException ioe) {
                        Timber.w(ioe, "Error reading/writing audio.");
                    }

                    // send whatever we have.
                    frameCount = 0;
                    byte[] frameBufferCopy = new byte[frameBufferSize + amrHeader.length];
                    System.arraycopy(amrHeader, 0, frameBufferCopy, 0, amrHeader.length);
                    System.arraycopy(frameBuffer, 0, frameBufferCopy, amrHeader.length, frameBufferSize);
                    byte[] audioPackage;
                    if (crypto.isEncrypting()) {
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        OutputStream cryptoStream = crypto.getOutputStream(baos);
                        IOUtils.copy(new ByteArrayInputStream(frameBufferCopy), cryptoStream);
                        cryptoStream.flush();
                        audioPackage = baos.toByteArray();
                    } else {
                        audioPackage = frameBufferCopy;
                    }

                    if (alive) {
                        // Add frame to the queue. AudioSenderHandlerThread will send to server
                        queueFrame(audioPackage);
                    }
                } catch (InterruptedException ie) {
                    if (stopping) {
                        Timber.w("Interrupted, stopping.");
                    } else {
                        if (alive) {
                            MessageUtil.sendMessage(application, MessageUtil.getMessageUrl(application, "Audio Streaming Stopped Unexpectedly."));
                            Timber.e(ie, "Error streaming audio.");
                            stopForeground(true);
                            stopSelf();
                            return;
                        }
                    }
                } catch (Exception e) {
                    if (alive) {
                        MessageUtil.sendMessage(application, MessageUtil.getMessageUrl(application, "Audio Streaming Stopped Unexpectedly."));
                        if (!Network.isNetworkException(e)) {
                            Timber.e(e, "Error streaming audio.");
                        }
                        stopForeground(true);
                        stopSelf();
                        return;
                    }
                } finally {
                }
            }
        }
    }


    // This thread takes audio frames from the queue and uploads them to the server
    private final class AudioSenderHandlerThread extends HandlerThread {
        private final Handler mHandler;
        private final AudioSenderRunnable runnable;

        private volatile boolean alive = false;
        private volatile boolean stopping = false;
        private volatile boolean started = false;
        private volatile SSLSocket socket;

        public AudioSenderHandlerThread() {
            super(AudioHandlerThread.class.getName(), android.os.Process.THREAD_PRIORITY_DEFAULT/* + (2 * android.os.Process.THREAD_PRIORITY_LESS_FAVORABLE)*/);
            start();
            mHandler = new Handler(getLooper());
            runnable = new AudioSenderRunnable();
        }

        public synchronized void end() {
            stopping = true;
            mHandler.removeCallbacks(runnable);
            alive = false;

            if (socket != null) {
                try {
                    socket.close();
                } catch (NetworkOnMainThreadException e) {
                    Timber.e(e, "NetworkOnMainThreadException exception in audio streamer.");
                } catch (Exception e) {
                    Timber.e(e, "Error closing socket in audio streamer.");
                }
                socket = null;
            }
            interrupt();

            if (started) {
                //MessageUtil.sendMessage(application, MessageUtil.getMessageUrl(application, MessageUrl.MESSAGE_LIVE_AUDIO_STOP));
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                quitSafely();
            } else {
                quit();
            }
        }

        public void begin() {
            if (!alive) {
                alive = true;
                mHandler.post(runnable);
            }
        }

        private class AudioSenderRunnable implements Runnable {

            @Override
            public void run() {
                try {
                    ByteBuffer sizeInt = ByteBuffer.allocate(4);
                    socket = (SSLSocket) socketFactory.createSocket();
                    String ipAddress = application.getScannedSettings().getIpAddress();
                    if (ipAddress.contains("://")) {
                        ipAddress = ipAddress.split("://")[1];
                    }

                    socket.connect(new InetSocketAddress(ipAddress, application.getScannedSettings().getAudioPort().intValue()), SOCKET_TIMEOUT);
                    socket.setSoTimeout(SOCKET_TIMEOUT);
                    socket.setTcpNoDelay(true);
                    socket.setKeepAlive(true);

                    if (alive && socket != null && !socket.isClosed() && socket.isConnected()) {
                        started = true;
                    }

                    try {
                        // Keep running while thread is allowed to live or there are frames in the queue
                        while ((alive || !frameQueue.isEmpty()) && socket != null && !socket.isClosed() && socket.isConnected()) {
                            // if frame queue is empty then wait 1 sec and try again
                            if (frameQueue.isEmpty()) {
                                Thread.sleep(1000);
                                continue;
                            }
                            // retrieve audio frame from queue
                            byte[] audioPackage = frameQueue.poll();
                            if (audioPackage != null && audioPackage.length > 0) {
                                // package size
                                sizeInt.clear();
                                sizeInt.putInt(audioPackage.length);
                                if (alive && socket != null && !socket.isClosed() && socket.isConnected()) {
                                    socket.getOutputStream().write(sizeInt.array());
                                    // the package
                                    socket.getOutputStream().write(audioPackage);
                                    socket.getOutputStream().flush();
                                }
                            }
                        }
                    }
                    catch (SSLHandshakeException e) {
                        EventBus.getDefault().post(new Events.SSLHandshakeError(getString(R.string.ssl_error)));
                    }
                    catch (IOException ioe) {
                        Timber.w(ioe, "Error reading/writing audio (AS-Sender).");
                    }

                } catch (InterruptedException ie) {
                    if (stopping) {
                        Timber.w("Interrupted, stopping (AS-Sender).");
                    } else {
                        if (alive) {
                            MessageUtil.sendMessage(application, MessageUtil.getMessageUrl(application, "Audio Streaming Stopped Unexpectedly (AS-Sender)."));
                            Timber.e(ie, "Error streaming audio (AS-Sender).");
                            stopForeground(true);
                            stopSelf();
                            return;
                        }
                    }
                } catch (Exception e) {
                    if (alive) {
                        MessageUtil.sendMessage(application, MessageUtil.getMessageUrl(application, "Audio Streaming Sender Stopped Unexpectedly."));
                        if (!Network.isNetworkException(e)) {
                            Timber.e(e, "Error streaming audio (AS-Sender).");
                        }
                        stopForeground(true);
                        stopSelf();
                        return;
                    }
                } finally {
                    if (socket != null) {
                        try {
                            socket.close();
                        } catch (Exception e) {
                            Timber.e(e, "Error closing socket in audio streaming (AS-Sender).");
                        }
                        socket = null;
                    }
                }
            }
        }
    }

}
