package com.xzfg.app.util;

import android.content.Context;
import android.content.Intent;

import com.xzfg.app.Application;
import com.xzfg.app.Givens;
import com.xzfg.app.R;
import com.xzfg.app.model.url.MessageUrl;
import com.xzfg.app.services.MessageService;

/**
 */
public class MessageUtil {


    public static MessageUrl getMessageUrl(Application application, String message) {
        if (application.isSetupComplete()) {
            MessageUrl messageUrl = new MessageUrl(application.getScannedSettings(), application.getString(R.string.message_endpoint), application.getDeviceIdentifier());
            if (message != null) {
                messageUrl.setMessage(message);
            }
            return messageUrl;
        }
        return null;
    }

    public static MessageUrl getMessageUrl(Context context, String message) {
        return getMessageUrl((Application) context.getApplicationContext(), message);
    }

    public static MessageUrl getMessageUrl(Application application) {
        return getMessageUrl(application, null);
    }

    public static MessageUrl getMessageUrl(Context context) {
        return getMessageUrl(context, null);
    }


    public static void sendMessage(Context context, MessageUrl messageUrl) {
        if (messageUrl != null) {
            Intent i = new Intent(context, MessageService.class);
            i.putExtra(Givens.MESSAGE, messageUrl);
            context.startService(i);
        } else {
            //Timber.d("Message Url is null");
        }
    }

}
