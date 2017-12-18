package com.xzfg.app.fragments.setup;

import com.xzfg.app.R;
import com.xzfg.app.fragments.BaseWebViewFragment;

public class PrivacyPolicyViewerFragment extends BaseWebViewFragment {
    @Override
    protected String getTitle() {
        return getString(R.string.privacy_policy);
    }

    @Override
    protected String getUrl() {
        return "file:///android_asset/policies/Privacy-Policy.html";
    }
}
