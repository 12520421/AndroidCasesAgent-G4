package com.xzfg.app.reporting;

import android.content.Intent;

import com.xzfg.app.Application;
import com.xzfg.app.R;
import com.xzfg.app.model.url.MessageUrl;
import com.xzfg.app.services.MessageService;

import java.io.PrintWriter;
import java.io.StringWriter;

import timber.log.Timber;

/**
 */
public class AgentReportingTree extends Timber.DebugTree {

    Application application;

    public AgentReportingTree(Application application) {
        this.application = application;
    }

    /**
     * We can override this message to send our report to the server.
     */
    @Override
    public void e(String message, Object... args) {
        if (application.getScannedSettings() != null && application.getDeviceIdentifier() != null) {
            MessageUrl messageUrl = new MessageUrl(application.getScannedSettings(), application.getString(R.string.message_endpoint), application.getDeviceIdentifier());
            messageUrl.setMessage("Exception");
            messageUrl.getMetaData().put("info", message);
            Intent messageServiceIntent = new Intent(application, MessageService.class);
            messageServiceIntent.putExtra("message", messageUrl);
            application.startService(messageServiceIntent);
        }
    }

    /**
     * Takes the exception, builds a url, and sends it off to the service.
     */
    @Override
    public void e(Throwable t, String message, Object... args) {
        if (application.getScannedSettings() != null && application.getDeviceIdentifier() != null) {
            StringWriter trace = new StringWriter();
            t.printStackTrace(new PrintWriter(trace));
            MessageUrl messageUrl = new MessageUrl(application.getScannedSettings(), application.getString(R.string.message_endpoint), application.getDeviceIdentifier());
            messageUrl.setMessage("Exception");
            messageUrl.getMetaData().put("info", message);
            messageUrl.getMetaData().put("exceptionMessage", t.getMessage());
            messageUrl.getMetaData().put("stackTrace", trace.toString());
            Intent messageServiceIntent = new Intent(application, MessageService.class);
            messageServiceIntent.putExtra("message", messageUrl);
            application.startService(messageServiceIntent);
        }
    }
}
