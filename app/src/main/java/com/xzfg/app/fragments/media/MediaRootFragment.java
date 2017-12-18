package com.xzfg.app.fragments.media;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.xzfg.app.R;

/**
 */
public class MediaRootFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup viewGroup, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.media_root, viewGroup, false);
    }

    @Override
    public void onViewCreated(View v, Bundle savedInstanceState) {
        FragmentTransaction transaction = getChildFragmentManager().beginTransaction();
        transaction.replace(R.id.media_root_frame, new MediaHeaderListFragment(), "media_header_list");
        transaction.commitAllowingStateLoss();
    }
}
