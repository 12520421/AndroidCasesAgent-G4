package com.xzfg.app.fragments.media;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.webkit.WebView;
import android.widget.ImageView;

import com.squareup.picasso.Picasso;
import com.xzfg.app.Application;
import com.xzfg.app.BuildConfig;
import com.xzfg.app.R;
import com.xzfg.app.model.Media;

import javax.inject.Inject;

/**
 *
 */
public class ImageViewerFragment extends Fragment implements View.OnClickListener {

    @Inject
    Application application;

    @Inject
    Picasso picasso;

    ImageView imageView;
    boolean alreadyMeasured = false;
    int frameWidth = 0;
    int frameHeight;
    private Media.Record record;

    public static ImageViewerFragment newInstance(Media.Record record) {
        ImageViewerFragment f = new ImageViewerFragment();
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

    public void measureFrame() {
        alreadyMeasured = true;
        frameWidth = imageView.getMeasuredWidth();
        frameHeight = imageView.getMeasuredHeight();
        load();
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup viewGroup, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_media, viewGroup, false);
        imageView = (ImageView) v.findViewById(R.id.img);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG);
        }
        imageView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            public void onGlobalLayout() {
                if (alreadyMeasured)
                    //noinspection deprecation
                    imageView.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                else
                    measureFrame();
            }
        });
        v.findViewById(R.id.back_button).setOnClickListener(this);
        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    public void load() {
        picasso.load(record.getMediaUrl() + "&w=" + frameWidth + "&h=" + frameHeight).into(imageView);
    }

    @Override
    public void onPause() {
        super.onPause();
        picasso.cancelRequest(imageView);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        switch (id) {
            case R.id.back_button:
                getActivity().onBackPressed();
                break;
        }
    }

}
