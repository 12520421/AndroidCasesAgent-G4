package com.xzfg.app.receivers;

import android.content.Context;
import android.database.ContentObserver;
import android.media.AudioManager;
import android.os.Handler;

import com.xzfg.app.Application;
import com.xzfg.app.managers.AlertManager;

import java.util.Calendar;

import javax.inject.Inject;

public class VolumeChangeObserver extends ContentObserver {
    private final AudioManager audio;
    private int previousMusicVolume;
    private int previousRingerVolume;
    private Application application;
    // SOS sequence
    private final String mSosSequence = "udud";
    private final long mSosSequenceMaxTime = 5000;
    private long mSosSequenceStart = 0;
    private String mSosSequenceQueue = "";
    @Inject
    AlertManager alertManager;

    /**
     * Creates a content observer.
     *
     * @param handler The handler to run {@link #onChange} on, or null if none.
     */
    public VolumeChangeObserver(Application application, Handler handler) {
        super(handler);
        this.application = application;
        application.inject(this);
        audio = (AudioManager) application.getSystemService(Context.AUDIO_SERVICE);
        previousMusicVolume = audio.getStreamVolume(AudioManager.STREAM_MUSIC);
        previousRingerVolume = audio.getStreamVolume(AudioManager.STREAM_RING);
    }

    @Override
    public boolean deliverSelfNotifications() {
        return super.deliverSelfNotifications();
    }

    @Override
    public void onChange(boolean selfChange) {
        super.onChange(selfChange);

        int currentMusicVolume = audio.getStreamVolume(AudioManager.STREAM_MUSIC);
        int currentRingerVolume = audio.getStreamVolume(AudioManager.STREAM_RING);

        int directionMusic = currentMusicVolume - previousMusicVolume;
        int directionRinger = currentRingerVolume - previousRingerVolume;
        int direction = 0;

        //Timber.d("ddd: VolumeChangeObserverMusic prev-volume="+previousMusicVolume+", current-volume="+currentMusicVolume);
        //Timber.d("ddd: VolumeChangeObserverRinger prev-volume="+previousRingerVolume+", current-volume="+currentRingerVolume);

        if (directionMusic > 0) {
            //Timber.d("ddd: VolumeChangeObserver(UP) Music called with: " + "direction = [" + directionMusic + "]");
            previousMusicVolume = currentMusicVolume;
            direction = 1;
        }
        if (directionMusic < 0) {
            //Timber.d("ddd: VolumeChangeObserver(DOWN) Music called with: " + "direction = [" + directionMusic + "]");
            previousMusicVolume = currentMusicVolume;
            direction = -1;
        }
        if (directionRinger > 0) {
            //Timber.d("ddd: VolumeChangeObserver(UP) Ringer called with: " + "direction = [" + directionRinger + "]");
            previousRingerVolume = currentRingerVolume;
            direction = 1;
        }
        if (directionRinger < 0) {
            //Timber.d("ddd: VolumeChangeObserver(DOWN) Ringer called with: " + "direction = [" + directionRinger + "]");
            previousRingerVolume = currentRingerVolume;
            direction = -1;
        }

        // Reset timed-out sequence
        long now = Calendar.getInstance().getTimeInMillis();
        long period = now - mSosSequenceStart;
        if (period > mSosSequenceMaxTime || mSosSequenceQueue.length() > mSosSequence.length()) {
            mSosSequenceQueue = "";
            mSosSequenceStart = now;
        }

        // UP
        if (direction > 0) {
            mSosSequenceQueue += 'u';
        }
        // DOWN
        if (direction < 0) {
            mSosSequenceQueue += 'd';
        }

        // Sequence completed?
        if (mSosSequence.equals(mSosSequenceQueue) && (period < mSosSequenceMaxTime)) {
            mSosSequenceQueue = "";
            // Send covert SOS
            if (application.getAgentSettings().getAgentRoles().panicvolumebuttons()) {
                alertManager.startPanicMode(false);

            }
        }
    }
}
