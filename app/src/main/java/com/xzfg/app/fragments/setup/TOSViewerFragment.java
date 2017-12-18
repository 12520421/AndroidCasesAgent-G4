package com.xzfg.app.fragments.setup;

import com.xzfg.app.R;
import com.xzfg.app.fragments.BaseWebViewFragment;

public class TOSViewerFragment extends BaseWebViewFragment {
    @Override
    protected String getTitle() {
        return getString(R.string.terms_of_use);
    }

    @Override
    protected String getUrl() {
        return "file:///android_asset/policies/TOS.html";
    }
}
