package com.xzfg.app.managers;

import android.media.AudioManager;

import com.xzfg.app.Application;

import javax.inject.Inject;


/**
 *
 */
public class VolumeManager {

    private final Object volumeLock = new Object();
    @Inject
    AudioManager audioManager;
    private volatile boolean muted = false;

    private volatile int alarmVolume;
    private volatile int dtmfVolume;
    private volatile int notificationVolume;
    private volatile int ringVolume;

    public VolumeManager(Application application) {
        application.inject(this);
    }

    public boolean isMuted() {
        return muted;
    }

    public void mute() {
        synchronized (volumeLock) {
            if (!muted) {
                muted = true;

                alarmVolume = audioManager.getStreamVolume(AudioManager.STREAM_ALARM);
                audioManager.setStreamVolume(AudioManager.STREAM_ALARM, 0, 0);

                dtmfVolume = audioManager.getStreamVolume(AudioManager.STREAM_DTMF);
                audioManager.setStreamVolume(AudioManager.STREAM_DTMF, 0, 0);

                notificationVolume = audioManager.getStreamVolume(AudioManager.STREAM_NOTIFICATION);
                if (notificationVolume == 0) {
                    notificationVolume = -1;
                } else {
                    audioManager.setStreamVolume(AudioManager.STREAM_NOTIFICATION, 0, 0);
                }

                ringVolume = audioManager.getStreamVolume(AudioManager.STREAM_RING);
                if (ringVolume == 0) {
                    ringVolume = -1;
                } else {
                    audioManager.setStreamVolume(AudioManager.STREAM_RING, 0, 0);
                }

                audioManager.setStreamMute(AudioManager.STREAM_SYSTEM, true);
                audioManager.setStreamMute(AudioManager.STREAM_MUSIC, true);
            }
        }
    }

    public void restore() {
        synchronized (volumeLock) {
            if (muted) {
                muted = false;
                audioManager.setStreamMute(AudioManager.STREAM_SYSTEM, false);
                audioManager.setStreamMute(AudioManager.STREAM_MUSIC, false);
                audioManager.setStreamVolume(AudioManager.STREAM_ALARM, alarmVolume, 0);
                audioManager.setStreamVolume(AudioManager.STREAM_DTMF, dtmfVolume, 0);
                if (notificationVolume >= 0) {
                    audioManager.setStreamVolume(AudioManager.STREAM_NOTIFICATION, notificationVolume, 0);
                }
                if (ringVolume >= 0) {
                    audioManager.setStreamVolume(AudioManager.STREAM_RING, ringVolume, 0);
                }
            }
        }

    }
}
