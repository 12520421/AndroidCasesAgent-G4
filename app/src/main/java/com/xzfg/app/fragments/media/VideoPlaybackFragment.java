package com.xzfg.app.fragments.media;

import android.app.Activity;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.SeekBar;

import com.xzfg.app.Application;
import com.xzfg.app.R;
import com.xzfg.app.model.Media;

import javax.inject.Inject;

import timber.log.Timber;

/**
 *
 */
public class VideoPlaybackFragment extends Fragment implements View.OnClickListener, View.OnTouchListener, MediaPlayer.OnCompletionListener, MediaPlayer.OnBufferingUpdateListener, MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener, TextureView.SurfaceTextureListener, MediaPlayer.OnVideoSizeChangedListener {

    private final Handler handler = new Handler();
    @Inject
    Application application;
    MediaPlayer mediaPlayer = null;
    SeekBar seekBar;
    private Media.Record record;
    private int lengthOfAudio;
    private final Runnable r = new Runnable() {
        @Override
        public void run() {
            updateSeekProgress();
        }
    };
    private ImageButton playButton;
    private ImageButton stopButton;
    private ImageButton pauseButton;
    private volatile boolean prepared = false;
    private final Object mediaLock = new Object();
    private TextureView mTextureView;

    public static VideoPlaybackFragment newInstance(Media.Record record) {
        VideoPlaybackFragment f = new VideoPlaybackFragment();
        Bundle args = new Bundle();
        args.putParcelable("record", record);
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        record = getArguments().getParcelable("record");
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        ((Application) activity.getApplication()).inject(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup viewGroup, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_video, viewGroup, false);
        seekBar = (SeekBar) v.findViewById(R.id.seekBar);
        seekBar.setOnTouchListener(this);
        playButton = (ImageButton) v.findViewById(R.id.btn_play);
        playButton.setOnClickListener(this);
        pauseButton = (ImageButton) v.findViewById(R.id.btn_pause);
        pauseButton.setOnClickListener(this);
        stopButton = (ImageButton) v.findViewById(R.id.btn_stop);
        stopButton.setOnClickListener(this);
        playButton.setEnabled(false);
        pauseButton.setEnabled(false);
        stopButton.setEnabled(false);
        v.findViewById(R.id.back_button).setOnClickListener(this);
        mTextureView = (TextureView) v.findViewById(R.id.texture);
        mTextureView.setSurfaceTextureListener(this);
        return v;
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        synchronized (mediaLock) {
            playButton.setEnabled(true);
            pauseButton.setEnabled(false);
            stopButton.setEnabled(false);
        }
    }

    @Override
    public void onBufferingUpdate(MediaPlayer mp, int percent) {
        synchronized (mediaLock) {
            seekBar.setSecondaryProgress(percent);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        synchronized (mediaLock) {
            if (mediaPlayer == null) {
                prepared = false;
                mediaPlayer = new MediaPlayer();
            }

            mediaPlayer.setOnVideoSizeChangedListener(this);
            mediaPlayer.setOnBufferingUpdateListener(this);
            mediaPlayer.setOnCompletionListener(this);
            mediaPlayer.setOnPreparedListener(this);
            mediaPlayer.setOnErrorListener(this);
        }
    }


    @Override
    public void onPause() {
        release();
        super.onPause();
    }

    public synchronized void release() {
        try {
            synchronized (mediaLock) {
                if (mediaPlayer != null) {
                    if (mediaPlayer.isPlaying()) {
                        mediaPlayer.stop();
                    }
                    mediaPlayer.reset();
                    mediaPlayer.release();
                    mediaPlayer = null;
                }
            }
        } catch (Exception e) {
            Timber.d(e, "error releasing media player.");
        }
    }

    @Override
    public void onClick(View v) {
        synchronized (mediaLock) {
            int id = v.getId();
            switch (id) {
                case R.id.back_button: {
                    if (prepared && mediaPlayer.isPlaying()) {
                        try {
                            if (mediaPlayer != null) {
                                if (mediaPlayer.isPlaying()) {
                                    mediaPlayer.stop();
                                }
                                mediaPlayer.reset();
                                mediaPlayer.release();
                                mediaPlayer = null;
                            }
                        } catch (Exception e) {
                            Timber.d(e, "error releasing media player.");
                        }
                    }
                    getActivity().onBackPressed();
                    break;
                }
                case R.id.btn_play:
                    //Timber.d("PLAY BUTTON CLICKED.");
                    changeState(0);
                    break;
                case R.id.btn_pause:
                    //Timber.d("PAUSE BUTTON CLICKED.");
                    changeState(1);
                    break;
                case R.id.btn_stop:
                    //Timber.d("STOP BUTTON CLICKED.");
                    changeState(2);
                    break;
            }
        }
    }

    public synchronized void changeState(int toState) {
        synchronized (mediaLock) {

            if (toState == 0) {
                if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
                    playButton.setEnabled(false);
                    pauseButton.setEnabled(true);
                    stopButton.setEnabled(true);
                    mediaPlayer.start();
                    updateSeekProgress();
                }
            }
            if (toState == 1) {
                if (mediaPlayer.isPlaying()) {
                    handler.removeCallbacks(r);
                    mediaPlayer.pause();
                }
                playButton.setEnabled(true);
                pauseButton.setEnabled(false);
                stopButton.setEnabled(false);

            }
            if (toState == 2) {
                if (mediaPlayer.isPlaying()) {
                    handler.removeCallbacks(r);
                    mediaPlayer.pause();
                    mediaPlayer.seekTo(0);
                }
                seekBar.setProgress(0);
                playButton.setEnabled(true);
                pauseButton.setEnabled(false);
                stopButton.setEnabled(false);
            }
        }
    }


    private void updateSeekProgress() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            seekBar.setProgress((int) (((float) mediaPlayer.getCurrentPosition() / lengthOfAudio) * 100));
            handler.postDelayed(r, 32);
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            SeekBar tmpSeekBar = (SeekBar) v;
            mediaPlayer.seekTo((lengthOfAudio / 100) * tmpSeekBar.getProgress());
        }
        return false;
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        synchronized (mediaLock) {
            //Timber.d("PLAYBACK PREPARED.");
            adjustAspect(mediaPlayer.getVideoWidth(), mediaPlayer.getVideoHeight());
            playButton.setEnabled(true);
            pauseButton.setEnabled(false);
            stopButton.setEnabled(false);
            lengthOfAudio = mediaPlayer.getDuration();
            prepared = true;
        }
    }

    public void adjustAspect(int videoWidth, int videoHeight) {
        int viewWidth = mTextureView.getWidth();
        int viewHeight = mTextureView.getHeight();
        double aspectRatio = (double) videoHeight / videoWidth;

        int newWidth, newHeight;
        if (viewHeight > (int) (viewWidth * aspectRatio)) {
            // limited by narrow width; restrict height
            newWidth = viewWidth;
            newHeight = (int) (viewWidth * aspectRatio);
        } else {
            // limited by short height; restrict width
            newWidth = (int) (viewHeight / aspectRatio);
            newHeight = viewHeight;
        }
        int xoff = (viewWidth - newWidth) / 2;
        int yoff = (viewHeight - newHeight) / 2;
        Matrix txform = new Matrix();
        mTextureView.getTransform(txform);
        txform.setScale((float) newWidth / viewWidth, (float) newHeight / viewHeight);
        //txform.postRotate(10);          // just for fun
        txform.postTranslate(xoff, yoff);
        mTextureView.setTransform(txform);
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        switch (what) {
            case MediaPlayer.MEDIA_ERROR_IO:
                Timber.w("Media playback stopped. Io error.");
                break;
            case MediaPlayer.MEDIA_ERROR_MALFORMED:
                Timber.w("Media playback stopped. Malformed.");
                break;
            case MediaPlayer.MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK:
                Timber.w("Media playback stopped. Not valid for progressive.");
                break;
            case MediaPlayer.MEDIA_ERROR_SERVER_DIED:
                Timber.w("Media playback stopped. Server is dead.");
                break;
            case MediaPlayer.MEDIA_ERROR_TIMED_OUT:
                Timber.w("Media playback stopped. Timed out.");
                break;
            case MediaPlayer.MEDIA_ERROR_UNSUPPORTED:
                Timber.w("Media playback stopped. unsupported.");
                break;
            case MediaPlayer.MEDIA_ERROR_UNKNOWN:
                Timber.w("Media playback stopped. unknown.");
                break;
        }

        getActivity().onBackPressed();

        return false;
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        Surface s = new Surface(surface);
        mediaPlayer.setSurface(s);
        try {
            //Timber.d("PREPARING TO PLAY URL: " + record.getMediaUrl());
            mediaPlayer.setDataSource(record.getMediaUrl());
            //mediaPlayer.setVideoScalingMode(MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING);
            mediaPlayer.prepareAsync();
        } catch (Exception e) {
            Timber.e(e, "Error preparing for playback.");
        }

    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        playButton.setEnabled(false);
        release();
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {

    }

    @Override
    public void onVideoSizeChanged(MediaPlayer mp, int width, int height) {

    }
}