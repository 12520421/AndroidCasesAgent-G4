package com.xzfg.app.util;

import android.content.Context;
import android.content.Intent;

import com.xzfg.app.Application;
import com.xzfg.app.Givens;
import com.xzfg.app.model.url.SetSetupFieldUrl;
import com.xzfg.app.services.SetSetupFieldService;

import java.util.HashMap;

/**
 */
public class SetSetupFieldUtil {


    public static SetSetupFieldUrl getUrl(Application application, String key, String value) {
        if (application.isSetupComplete()) {
            SetSetupFieldUrl url = new SetSetupFieldUrl(application, key, value);
            if (key != null && value != null) {
                HashMap<String,String> pairs = new HashMap<>();
                pairs.put(key,value);
                url.setPairs(pairs);
            }
            return url;
        }
        return null;
    }

    public static SetSetupFieldUrl getUrl(Application application) {
        return getUrl(application, null, null);
    }

    public static SetSetupFieldUrl getUrl(Application application, HashMap<String,String> pairs) {
        if (application.isSetupComplete()) {
            SetSetupFieldUrl url = new SetSetupFieldUrl(application);
            if (pairs != null && !pairs.isEmpty()) {
                url.setPairs(pairs);
            }
            return url;
        }
        return null;

    }

    public static void send(Context context, SetSetupFieldUrl setSetupFieldUrl) {
        if (setSetupFieldUrl != null && setSetupFieldUrl.getPairs() != null && !setSetupFieldUrl.getPairs().isEmpty()) {
            Intent i = new Intent(context, SetSetupFieldService.class);
            i.putExtra(Givens.MESSAGE, setSetupFieldUrl);
            context.startService(i);
        }
    }

}
