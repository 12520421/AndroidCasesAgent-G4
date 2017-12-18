package com.xzfg.app.managers;

import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.StatFs;

import com.xzfg.app.Application;
import com.xzfg.app.Events;
import com.xzfg.app.Givens;

import java.io.File;
import java.util.ArrayList;

import javax.inject.Inject;

import de.greenrobot.event.EventBus;
import timber.log.Timber;

/**
 *
 */
public class FreeSpaceManager {
    private static final long factor = 1024 * 1024;
    private static final long LONG_INTERVAL = 30000;
    private static final long SHORT_INTERVAL = 1000;
    @Inject
    Application application;
    private volatile long freeMb = Givens.FREE_SPACE_LIMIT + 1L;
    private volatile long interval = LONG_INTERVAL;
    private FreeSpaceHandlerThread freeSpaceHandlerThread;

    public FreeSpaceManager(Application application) {
        application.inject(this);
        freeSpaceHandlerThread = new FreeSpaceHandlerThread();
        EventBus.getDefault().registerSticky(this);
    }

    public long getFreeMb() {
        return freeMb;
    }

    public long getInterval() {
        return interval;
    }

    public void onEventMainThread(Events.VideoRecording videoRecordingEvent) {
        if (videoRecordingEvent.isRecording()) {
            interval = SHORT_INTERVAL;
        } else {
            interval = LONG_INTERVAL;
        }
        freeSpaceHandlerThread.force();
    }


    private class FreeSpaceHandlerThread extends HandlerThread {

        Handler mHandler;
        FreeSpaceRunnable runnable;

        public FreeSpaceHandlerThread() {
            super(FreeSpaceHandlerThread.class.getName(), Givens.THREAD_PRIORITY + (2 * android.os.Process.THREAD_PRIORITY_LESS_FAVORABLE));
            start();
            mHandler = new Handler(getLooper());
            runnable = new FreeSpaceRunnable();
            mHandler.post(runnable);
        }

        public void force() {
            mHandler.removeCallbacks(runnable);
            mHandler.post(runnable);
        }

        private class FreeSpaceRunnable implements Runnable {
            @SuppressWarnings("ResultOfMethodCallIgnored")
            public void run() {
                try {
                    //Timber.d("checking free space");

                    if (!application.isSetupComplete()) {
                        mHandler.postDelayed(this, getInterval());
                        return;
                    }


                    ArrayList<File> storageDirs = new ArrayList<>();
                    File internal = application.getFilesDir();
                    if (internal != null) {
                        if (!internal.exists()) {
                            internal.mkdirs();
                        }
                        if (internal.exists()) {
                            //Timber.d("Examining internal storage for free space.");
                            storageDirs.add(internal);
                        }
                    }


                    if (application.getAgentSettings().getSecurity() == 0) {
                        //Timber.d("Medium Security");
                        if (application.getAgentSettings().getExternalStorage() == 1) {
                            //Timber.d("External Storage Enabled.");
                            if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                                //Timber.d("External Storage Mounted.");
                                File external = application.getExternalFilesDir(null);
                                if (external != null) {
                                    if (!external.exists()) {
                                        external.mkdirs();
                                    }
                                    if (external.exists()) {
                                        //Timber.d("Examining external storage for free space.");
                                        storageDirs.add(external);
                                    }
                                }
                            }
                        }
                    }


                    long newMb = 0;
                    for (File storageDir : storageDirs) {
                        double free;

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                            StatFs statFs = new StatFs(storageDir.getAbsolutePath());
                            long bytesPerBlock = statFs.getBlockSizeLong();
                            free = statFs.getFreeBlocksLong() * bytesPerBlock;
                            if (free >= factor) {
                                free = free / factor;
                            } else {
                                free = 0L;
                            }
                        } else {
                            // using the standard java getFreeSpace includes overhead blocks
                            // that shouldn't usually be accounted for as part of storage.
                            free = Math.max(storageDir.getFreeSpace(), 0);

                            // if we have less than factor available, report it as 0.
                            // close enough for horseshoes, handgrenades, and determining if there's
                            // sufficient space to store a video. And prevents us from having
                            if (free >= factor) {
                                free = free / factor;
                                // so we subtract 10% to account for that.
                                free = Math.max(0d, free - free / 10);
                            } else {
                                free = 0;
                            }

                        }
                        if (newMb == 0)
                            newMb = (long) free;
                        else
                            newMb = Math.min(newMb, (long) free);
                        //Timber.d(String.valueOf(newMb) + " MB free space remaing on " + storageDir);

                    }
                    freeMb = newMb;

                    if (freeMb < Givens.FREE_SPACE_LIMIT) {
                        Timber.w("Insufficient free space remaining.! Only + " + freeMb + " left.");
                        EventBus.getDefault().post(new Events.FreeSpaceLowEvent());
                    } else {
                        //Timber.d("Sufficient free space remaining. " + freeMb + " remaining.");
                    }

                } catch (Exception e) {
                    Timber.e(e, "An error occurred attempting to watch for free space.");
                }

                mHandler.postDelayed(this, getInterval());

            }

        }
    }


}
