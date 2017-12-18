package com.xzfg.app.fragments;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.FrameLayout;

import com.xzfg.app.Events;
import com.xzfg.app.R;
import com.xzfg.app.model.url.MessageUrl;
import com.xzfg.app.util.MessageUtil;

import de.greenrobot.event.EventBus;

public class HelpFragment extends Fragment {
    private WebView mWebView;

    @SuppressWarnings("deprecation")
    @SuppressLint("SetJavaScriptEnabled")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup viewGroup, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_help, viewGroup, false);

        FrameLayout frame = (FrameLayout) v.findViewById(R.id.frame);
        mWebView = BaseWebViewFragment.createBaseWebView(getActivity());
        frame.addView(mWebView);

        busRegister();
        EventBus.getDefault().post(new Events.DisplayChanged(getString(R.string.help), R.id.help));

        return v;
    }

    public void busRegister() {
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
        }
        MessageUtil.sendMessage(getActivity().getApplication(), MessageUtil.getMessageUrl(getActivity().getApplication(), MessageUrl.MESSAGE_GET_HELP));
    }

    @Override
    public void onResume() {
        super.onResume();
        mWebView.onResume();
        busRegister();
    }

    @Override
    public void onPause() {
        super.onPause();
        mWebView.stopLoading();
        mWebView.onPause();
        EventBus.getDefault().unregister(this);
    }

    public void onEventMainThread(Events.HelpReceived helpReceived) {
        if (mWebView != null) {
            mWebView.loadDataWithBaseURL(null, helpReceived.getHelp(), "text/html", "utf-8", null);
        }
    }

}
