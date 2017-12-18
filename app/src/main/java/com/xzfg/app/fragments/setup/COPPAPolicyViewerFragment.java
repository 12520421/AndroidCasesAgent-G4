package com.xzfg.app.fragments.setup;

import com.xzfg.app.R;
import com.xzfg.app.fragments.BaseWebViewFragment;

public class COPPAPolicyViewerFragment extends BaseWebViewFragment {
    @Override
    protected String getTitle() {
        return getString(R.string.coppa_policy);
    }

    @Override
    protected String getUrl() {
        return "file:///android_asset/policies/COPPA-Policy.html";
    }
}
