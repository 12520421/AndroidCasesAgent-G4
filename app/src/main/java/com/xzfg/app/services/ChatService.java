package com.xzfg.app.services;

import android.content.Intent;
import android.content.SharedPreferences;

import com.crashlytics.android.Crashlytics;
import com.google.gson.Gson;
import com.xzfg.app.Application;
import com.xzfg.app.BuildConfig;
import com.xzfg.app.Events;
import com.xzfg.app.Givens;
import com.xzfg.app.R;
import com.xzfg.app.managers.ChatManager;
import com.xzfg.app.managers.SessionManager;
import com.xzfg.app.model.ScannedSettings;
import com.xzfg.app.model.SendableMessage;
import com.xzfg.app.model.url.ChatDeleteUrl;
import com.xzfg.app.model.url.SendChatUrl;
import com.xzfg.app.security.Crypto;
import com.xzfg.app.util.Network;

import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;

import javax.inject.Inject;

import de.greenrobot.event.EventBus;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import timber.log.Timber;

/**
 * This service handles all communications with the Chat API endpoints.
 */
public class ChatService extends BackWakeIntentService {

    @Inject
    Application application;
    @Inject
    Crypto crypto;
    @Inject
    OkHttpClient httpClient;
    @Inject
    SharedPreferences sharedPreferences;
    @Inject
    ChatManager chatManager;
    @Inject
    SessionManager sessionManager;
    @Inject
    Gson gson;

    private ScannedSettings scannedSettings;


    public ChatService() {
        super(ChatService.class.getName());
    }

    @Override
    public void onCreate() {
        super.onCreate();
        ((Application) getApplication()).inject(this);
        scannedSettings = application.getScannedSettings();
    }


    @Override
    protected void doWork(Intent intent) {
        if (!application.isSetupComplete()) {
            return;
        }

        try {
            switch (intent.getAction()) {
                case Givens.ACTION_CHAT_SEND_MESSAGE:
                    sendMessage(intent);
                    break;
                case Givens.ACTION_CHAT_PROCESS_QUEUE:
                    processQueue();
                    break;
                case Givens.ACTION_CHAT_DELETE_MESSAGES:
                    delete(intent);
                    break;
                case Givens.ACTION_CHAT_EMAIL_SEND_MESSAGE:
                    email(intent);
                    break;
                case Givens.ACTION_CHAT_SMS_SEND_MESSAGE:
                    sms(intent);
                    break;
            }
        } catch (Exception e) {
            Timber.w(e, "Something seriously went wrong in the ChatService.");
        }

    }

    private void email(Intent intent) {
        SendableMessage message = intent.getParcelableExtra("chat_message");
        try {
            SendChatUrl url = new SendChatUrl(scannedSettings, getString(R.string.send_email_endpoint), sessionManager.getSessionId());
            url.setToUserId(message.getToUserId());
            url.setText(message.getMessage());
            url.setFromUsername(application.getScannedSettings().getUserName());

            //Timber.d("Sending message: " + message);
            Response response = httpClient.newCall(new Request.Builder().url(url.toString(crypto)).build()).execute();
            String responseBody = response.body().string().trim();
            response.body().close();

            // if we don't receive a 200 ok, it's a network error.
            if (response.code() != 200 || responseBody.contains("HTTP/1.0 404 File not found")) {
              if (BuildConfig.DEBUG) {
                Crashlytics.setString("Url", url.toString());
                Crashlytics.setLong("Response Code", response.code());
                Crashlytics.setString("Response", responseBody);
              }
                throw new Exception("Server did not respond appropriately.");
            }

            responseBody = crypto.decryptFromHexToString(responseBody).trim();
            if (responseBody.startsWith("Error")) {
              if (BuildConfig.DEBUG) {
                Crashlytics.setString("Url", url.toString());
                Crashlytics.setLong("Response Code", response.code());
                Crashlytics.setString("Response", responseBody);
              }
                throw new Exception("Server returned us an error.");
            }
            if (responseBody.contains("Invalid SessionId")) {
                EventBus.getDefault().post(new Events.InvalidSession());
                throw new Exception("Invalid SessionId!");
            }

            //Timber.d(responseBody);

        } catch (Exception e) {
            Timber.w(e, "Couldn't send Email.");
        }
    }

    private void sms(Intent intent) {
        SendableMessage message = intent.getParcelableExtra("chat_message");
        try {
            SendChatUrl url = new SendChatUrl(scannedSettings, getString(R.string.send_email_endpoint), sessionManager.getSessionId());
            url.setToUserId(message.getToUserId());
            url.setText(message.getMessage());
            url.setFromUsername(application.getScannedSettings().getUserName());

            //Timber.d("Sending message: " + message);
            Response response = httpClient.newCall(new Request.Builder().url(url.toString(crypto)).build()).execute();
            String responseBody = response.body().string().trim();
            response.body().close();

            // if we don't receive a 200 ok, it's a network error.
            if (response.code() != 200 || responseBody.contains("HTTP/1.0 404 File not found")) {
              if (BuildConfig.DEBUG) {
                Crashlytics.setString("Url", url.toString());
                Crashlytics.setLong("Response Code", response.code());
                Crashlytics.setString("Response", responseBody);
              }
                throw new Exception("Server did not respond appropriately.");
            }

            responseBody = crypto.decryptFromHexToString(responseBody).trim();
            if (responseBody.startsWith("Error")) {
              if (BuildConfig.DEBUG) {
                Crashlytics.setString("Url", url.toString());
                Crashlytics.setLong("Response Code", response.code());
                Crashlytics.setString("Response", responseBody);
              }
                throw new Exception("Server returned us an error.");
            }
            if (responseBody.contains("Invalid SessionId")) {
                EventBus.getDefault().post(new Events.InvalidSession());
                throw new Exception("Invalid SessionId!");
            }

            //Timber.d(responseBody);

        } catch (Exception e) {
            Timber.w(e, "Couldn't send Email.");
        }
    }


    private void delete(Intent intent) {
        String userId = null;
        if (intent.hasExtra("userId")) {
            userId = intent.getStringExtra("userId");
        }

        if (userId == null) {
            deleteAll(null);
        } else {
            if (userId.equals("x") || userId.equals("b")) {
                deleteAll(userId);
            } else {
                deleteFromUser(userId);
            }
        }
    }

    private void deleteAll(String type) {
        try {
            ChatDeleteUrl url = new ChatDeleteUrl(scannedSettings, getString(R.string.chat_clear_all_endpoint), sessionManager.getSessionId());
            url.setMessageId(String.valueOf(chatManager.getLastMessageId()));
            if (type != null) {
                url.setType(type);
            }

            Response response = httpClient.newCall(new Request.Builder().url(url.toString(crypto)).build()).execute();
            String responseBody = response.body().string().trim();
            response.body().close();

            // if we don't receive a 200 ok, it's a network error.
            if (response.code() != 200 || responseBody.contains("HTTP/1.0 404 File not found")) {
              if (BuildConfig.DEBUG) {
                Crashlytics.setString("Url", url.toString());
                Crashlytics.setLong("Response Code", response.code());
                Crashlytics.setString("Response", responseBody);
              }
                throw new Exception("Server did not respond appropriately.");
            }

            responseBody = crypto.decryptFromHexToString(responseBody).trim();
            if (responseBody.startsWith("Error")) {
              if (BuildConfig.DEBUG) {
                Crashlytics.setString("Url", url.toString());
                Crashlytics.setLong("Response Code", response.code());
                Crashlytics.setString("Response", responseBody);
              }
                throw new Exception("Server returned us an error.");
            }

            if (responseBody.contains("Invalid SessionId")) {
                EventBus.getDefault().post(new Events.InvalidSession());
                throw new Exception("Invalid SessionId!");
            }

        } catch (Exception e) {
            Timber.w(e, "An error occurred attempting to delete messages.");
            return;
        }

        if (type == null) {
            chatManager.clearAll();
        } else {
            chatManager.clearUserId(type);
        }

    }

    private void deleteFromUser(String userId) {
        try {
            ChatDeleteUrl url = new ChatDeleteUrl(scannedSettings, getString(R.string.chat_clear_user_endpoint), sessionManager.getSessionId());
            url.setToUserId(userId);

            Response response = httpClient.newCall(new Request.Builder().url(url.toString(crypto)).build()).execute();
            String responseBody = response.body().string().trim();
            response.body().close();

            // if we don't receive a 200 ok, it's a network error.
            if (response.code() != 200 || responseBody.contains("HTTP/1.0 404 File not found")) {
              if (BuildConfig.DEBUG) {
                Crashlytics.setString("Url", url.toString());
                Crashlytics.setLong("Response Code", response.code());
                Crashlytics.setString("Response", responseBody);
              }
                throw new Exception("Server did not respond appropriately.");
            }

            responseBody = crypto.decryptFromHexToString(responseBody).trim();
            if (responseBody.startsWith("Error")) {
              if (BuildConfig.DEBUG) {
                Crashlytics.setString("Url", url.toString());
                Crashlytics.setLong("Response Code", response.code());
                Crashlytics.setString("Response", responseBody);
              }
                throw new Exception("Server returned us an error.");
            }
            if (responseBody.contains("Invalid SessionId")) {
                EventBus.getDefault().post(new Events.InvalidSession());
                throw new Exception("Invalid SessionId!");
            }
        } catch (Exception e) {
            Timber.w(e, "An error occurred attempting to delete messages.");
            return;
        }

        chatManager.clearUserId(userId);
    }

    /**
     * send a message.
     *
     * @param intent the intent used to fire the service, containing the message to be sent as a
     *               String extra.
     */
    private void sendMessage(Intent intent) {
        SendableMessage message = intent.getParcelableExtra("chat_message");
        boolean queued = false;

        if (application.getAgentSettings().getSecurity() == 0 && !isConnected()) {
            //Timber.d("Queueing message for later delivery.");
            queued = queMessage(message);
            return;
        }

        try {
            sendMessage(message);
            message.setCreated(new Date());
            message.setStatus(null);
            EventBus.getDefault().post(new Events.ChatMessageSent(message));
        } catch (Exception e) {

            if (application.getAgentSettings().getSecurity() == 0) {
                //Timber.d("An error was received from the server, queuing the message for later.");
                queMessage(message);
                return;
            } else {
                message.setStatus(getString(R.string.message_failed));
                EventBus.getDefault().post(new Events.ChatMessageSent(message));
            }
            if (!Network.isNetworkException(e)) {
                Timber.w(e, "A problem occurred attempting to send a message.");
            }
        }


    }

    private void sendMessage(SendableMessage message) throws Exception {
        SendChatUrl url = new SendChatUrl(scannedSettings, getString(R.string.sendmessage_endpoint), sessionManager.getSessionId());
        url.setToUserId(message.getToUserId());
        url.setMessage(message.getMessage());

        //Timber.d("Sending message: " + message);
        Response response = httpClient.newCall(new Request.Builder().url(url.toString(crypto)).build()).execute();
        String responseBody = response.body().string().trim();
        response.body().close();

        // if we don't receive a 200 ok, it's a network error.
        if (response.code() != 200 || responseBody.contains("HTTP/1.0 404 File not found")) {
          if (BuildConfig.DEBUG) {
            Crashlytics.setString("Url", url.toString());
            Crashlytics.setLong("Response Code", response.code());
            Crashlytics.setString("Response", responseBody);
          }
            throw new Exception("Server did not respond appropriately.");
        }

        responseBody = crypto.decryptFromHexToString(responseBody).trim();
        if (responseBody.startsWith("Error")) {
          if (BuildConfig.DEBUG) {
            Crashlytics.setString("Url", url.toString());
            Crashlytics.setLong("Response Code", response.code());
            Crashlytics.setString("Response", responseBody);
          }
            throw new Exception("Server returned us an error.");
        }
        if (responseBody.contains("Invalid SessionId")) {
            EventBus.getDefault().post(new Events.InvalidSession());
            throw new Exception("Invalid SessionId!");
        }

        //Timber.d(responseBody);
    }


    private boolean queMessage(SendableMessage message) {
        //Timber.d("Queuing has begun.");
        HashSet<String> messageQueue = (HashSet<String>) sharedPreferences.getStringSet("chat_messages", new LinkedHashSet<String>());
        SharedPreferences.Editor editor = sharedPreferences.edit();
        messageQueue.add(crypto.encryptToHex(gson.toJson(message)));
        //Timber.d("Message Added To Queue");
        boolean committed = editor.putStringSet("chat_messages", messageQueue).commit();

        if (committed) {
            //Timber.d("Queue committed to disk.");
            message.setStatus(getString(R.string.message_queued));
        } else {
            message.setStatus(getString(R.string.message_failed));
        }
        //Timber.d("Posting message sent event.");
        EventBus.getDefault().post(new Events.ChatMessageSent(message));

        return committed;
    }

    private void processQueue() {
        //Timber.d("Attempting to process the queue.");
        LinkedHashSet<String> failedMessages = new LinkedHashSet<>();
        HashSet<String> messageQueue = (HashSet<String>) sharedPreferences.getStringSet("chat_messages", new LinkedHashSet<String>());
        if (!messageQueue.isEmpty()) {
            //Timber.d("Found " + messageQueue.size() + " messages in queue.");
            boolean committed = sharedPreferences.edit().remove("chat_messages").commit();
            if (committed) {
               // Timber.d("Removed queued messages from disk.");
                for (String msgString : messageQueue) {
                   // Timber.d("Inflating message...");
                    SendableMessage message = gson.fromJson(crypto.decryptFromHexToString(msgString), SendableMessage.class);
                    //Timber.d(message.toString());
                    try {
                       // Timber.d("Sending message");
                        sendMessage(message);
                        message.setCreated(new Date());
                        message.setStatus(null);
                        EventBus.getDefault().post(new Events.ChatMessageSent(message));
                        //Timber.d("Message Sent.");
                    } catch (Exception e) {
                        if (!Network.isNetworkException(e)) {
                            Timber.d("Failed to resend message, adding back to failed message queue.");
                        }
                        failedMessages.add(msgString);
                    }
                }
            }
            if (!failedMessages.isEmpty()) {
                //Timber.d("whoops, we have some failed messages.");
                messageQueue = (LinkedHashSet<String>) sharedPreferences.getStringSet("chat_messages", new LinkedHashSet<String>());
                messageQueue.addAll(failedMessages);
                committed = sharedPreferences.edit().putStringSet("chat_messages", messageQueue).commit();
                for (String msgString : messageQueue) {
                    //Timber.d("inflating message so we can set it as queued.");
                    SendableMessage message = gson.fromJson(crypto.decryptFromHexToString(msgString), SendableMessage.class);
                    message.setStatus(getString(R.string.message_queued));
                    EventBus.getDefault().post(new Events.ChatMessageSent(message));
                }
            }
        }

    }

}
