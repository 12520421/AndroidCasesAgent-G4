package com.xzfg.app.managers;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.PowerManager;

import com.crashlytics.android.Crashlytics;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.xzfg.app.Application;
import com.xzfg.app.BuildConfig;
import com.xzfg.app.Events;
import com.xzfg.app.Givens;
import com.xzfg.app.R;
import com.xzfg.app.model.Chat;
import com.xzfg.app.model.Message;
import com.xzfg.app.model.Messages;
import com.xzfg.app.model.ScannedSettings;
import com.xzfg.app.model.SendableMessage;
import com.xzfg.app.model.User;
import com.xzfg.app.model.Users;
import com.xzfg.app.model.url.ChatBaseUrl;
import com.xzfg.app.model.url.ChatMessagesUrl;
import com.xzfg.app.security.Crypto;
import com.xzfg.app.security.Fuzz;
import com.xzfg.app.services.ChatService;
import com.xzfg.app.util.DateTransformer;
import com.xzfg.app.util.Network;

import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;
import org.simpleframework.xml.transform.RegistryMatcher;

import java.io.InterruptedIOException;
import java.lang.reflect.Type;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Inject;

import de.greenrobot.event.EventBus;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import timber.log.Timber;

/**
 * Because the server doesn't keep track of anything by itself, we have to keep track of things here.
 * This necessitates either saving content to disk, or reloading everything upon relaunch.  Taking
 * a somewhat balanced approach here by only writing ids or groups of ids to disk, rather than
 * actual messages - it means that on a relaunch, the entire chat history will need to be reloaded.
 * <p/>
 * It also means we have to hold the whole of the chat history in memory, which may not work out
 * very well in the long run. The server side would need to have a more... comprehensive API in
 * order to overcome these obstacles, or we would need to be able to store a sqlite database with
 * the chat history locally - which is apparently problematic, even using sqlcipher, due to bundling
 * of encryption algorithms.
 */
public class ChatManager {

    // if we're in the chat tab, check every 1.5 seconds.
    private static final long SHORT_INTERVAL = (int) (1.5d * 1000);

    // if we're not in the chat tab, fall back to every 25 seconds.
    // this should be sufficient to keep our session alive while the
    // app is in the foreground and not paused.
    private static final long LONG_INTERVAL = 25 * 1000;
    private static final Object threadLock = new Object();
    private static final Object contactsLock = new Object();
    private final List<User> contacts = Collections.synchronizedList(new LinkedList<User>());
    private final List<Chat> chats = Collections.synchronizedList(new LinkedList<Chat>());
    private final ConcurrentHashMap<String, Chat> chatUserMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, User> contactNameMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, User> contactIdMap = new ConcurrentHashMap<>();
    private final String contactEntry;
    @Inject
    Application application;
    @Inject
    Crypto crypto;
    @Inject
    OkHttpClient httpClient;
    @Inject
    Gson gson;
    @Inject
    SharedPreferences sharedPreferences;
    @Inject
    ConnectivityManager connectivityManager;
    @Inject
    PowerManager powerManager;
    @Inject
    FixManager fixManager;
    @Inject
    SessionManager sessionManager;
    private volatile Long lastMessageId = 0L;
    private volatile boolean paused = false;
    private volatile boolean wasDown = false;
    private volatile boolean started = false;
    private MessageListHandlerThread listHandlerThread;
    private ContactHandlerThread contactHandlerThread;
    private volatile boolean chatTabSelected = false;

    public ChatManager(Application application) {
        application.inject(this);
        contactEntry = Fuzz.en("contacts", application.getDeviceIdentifier());
        EventBus.getDefault().registerSticky(this);
    }

    public Long getLastMessageId() {
        return lastMessageId;
    }

    public void setLastMessageId(Long lastMessageId) {
        this.lastMessageId = lastMessageId;
    }

    public boolean isPaused() {
        return paused;
    }

    public void setPaused(boolean paused) {
        this.paused = paused;
    }

    public ConcurrentHashMap<String, Chat> getChatUserMap() {
        return chatUserMap;
    }

    public ConcurrentHashMap<String, User> getContactNameMap() {
        return contactNameMap;
    }

    public List<Chat> getChats() {
        return chats;
    }

    public boolean isChatTabSelected() {

        return chatTabSelected;
    }

    public void setChatTabSelected(boolean selected) {
        chatTabSelected = selected;
        if (selected) {
            onPause();
            onResume();
            //EventBus.getDefault().postSticky(new Events.ChatStatus(false));
            EventBus.getDefault().postSticky(new Events.DisplayChanged(application.getString(R.string.tab_chat), R.id.chat));
        }
    }

    // how often?
    private Long getInterval() {
        return chatTabSelected ? SHORT_INTERVAL : LONG_INTERVAL;
    }

    public void onResumeFromUI() {
        started = true;
        onResume();
    }

    public void onPauseFromUI() {
        started = false;
        onPause();
    }

    public void onResume() {
        //Timber.d("onResume Called.");
        if (!started) {
            //Timber.d("UI not initialized, returning.");
            return;
        }
        if (isConnected()) {
            synchronized (threadLock) {
                if (contactHandlerThread == null) {
                    //Timber.d("Creating contact handler thread.");
                    contactHandlerThread = new ContactHandlerThread();
                }
                if (listHandlerThread == null) {
                    //Timber.d("Creating list handler thread.");
                    listHandlerThread = new MessageListHandlerThread();
                }
            }
            if (application.isSetupComplete() && application.getAgentSettings().getSecurity() == 0) {
                Intent i = new Intent(application, ChatService.class).setAction(Givens.ACTION_CHAT_PROCESS_QUEUE);
                application.startService(i);
            }
        }
    }

    // stop looking for new messages
    public void onPause() {
        //Timber.d("onPause Called.");
        synchronized (threadLock) {
            if (listHandlerThread != null) {
                //Timber.d("Deconstructing list handler.");
                MessageListHandlerThread deadHandlerThread = listHandlerThread;
                deadHandlerThread.kill();
                listHandlerThread = null;
            }

            if (contactHandlerThread != null) {
                //Timber.d("Deconstructing contact handler.");
                ContactHandlerThread deadHandlerThread = contactHandlerThread;
                deadHandlerThread.kill();
                contactHandlerThread = null;
            }
        }
    }

    public Chat getChat(User user) {
        if (chatUserMap.containsKey(user.getUsername())) {
            return chatUserMap.get(user.getUsername());
        }
        return null;
    }

    public void clearUserId(String id) {
        //listHandlerThread.onPause();

        User user = findUserById(id);
        Chat c = chatUserMap.get(user.username);

        if (c != null) {
            c.getMessages().clear();

            if (chats.contains(c)) {
                chats.remove(c);
            } else {
                Timber.d("Chats doesn't contain chat.");
            }
        }

        if (chatUserMap.containsKey(user.getUsername())) {
            chatUserMap.remove(user.getUsername());
        }

        //if (!paused) {
        //  listHandlerThread.onResume();
        //}

        EventBus.getDefault().post(new Events.ChatsCleared(id));
        notifyChatDataChanged();
    }

    public void clearAll() {
        //Timber.d("clearing all");
        onPause();
        chats.clear();
        chatUserMap.clear();
        onResume();
        notifyChatDataChanged();
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(Events.NetworkStatus networkStatus) {
        if (networkStatus.isUp() && application.isSetupComplete()) {
            onResume();
        } else {
            onPause();
        }
    }

    public void onEventMainThread(Events.Session event) {
        //Timber.d("Session Event - Session Id: " + event.getSessionId());
        if (event.getSessionId() == null) {
            onPause();
        }
        if (event.getSessionId() != null) {
            onResume();
        }
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(Events.ChatMessageSent event) {
        // get the sent message.
        SendableMessage sm = event.getSendableMessage();

        // find the user the message was sent to.
        User toUser = findUserById(sm.getToUserId());

        // get the chat associated with that user.
        Chat c = getChat(toUser);

        // no chat? Create chat.
        if (c == null) {
            c = new Chat(toUser, sm);
            chats.add(c);
            Collections.sort(chats);
            chatUserMap.put(toUser.getUsername(), c);
            return;
        } else {
            List<Message> messages = c.getMessages();
            // we have chat. update the message
            int x = messages.indexOf(sm);
            if (x > -1) {
                messages.set(x, sm);
            } else {
                messages.add(sm);
            }
        }
    }

    public synchronized void updateUsers(List<User> updatedContacts) {
        synchronized (contactsLock) {
            contacts.clear();
            contactNameMap.clear();
            contactIdMap.clear();
            contacts.addAll(updatedContacts);

            // keep a list of contacts by both name and id.
            for (User user : updatedContacts) {
                contactNameMap.put(user.getUsername(), user);
                contactIdMap.put(user.getUserId(), user);

                // update the user object inside the chat, which will allow for refreshing online status.
                if (chatUserMap.containsKey(user)) {
                    chatUserMap.get(user).setUser(user);
                }

            }

            // if the user is no longer in our contacts list, remove them from the chat list.
            //Timber.d("Checking chats for removed users.");
            for (Map.Entry<String, Chat> currentChat : chatUserMap.entrySet()) {
                if (!contactNameMap.containsKey(currentChat.getKey())) {
                    //Timber.d("Removing chat - " + currentChat.getKey());
                    chats.remove(currentChat.getValue());
                    chatUserMap.remove(currentChat.getKey());
                }
            }

            notifyChatDataChanged();
            EventBus.getDefault().postSticky(new Events.ContactsLoaded(contacts));
        }
    }

    public User findUserById(String userId) {
        //synchronized (contactsLock) {
        if (contactIdMap.containsKey(userId)) {
            return contactIdMap.get(userId);
        }
        //}
        return null;
    }

    public User findUserByName(String name) {
        //synchronized (contactsLock) {
        if (contactNameMap.containsKey(name)) {
            return contactNameMap.get(name);
        }
        //}
        return null;
    }

    public synchronized void addMessages(Messages messages) {
        if (messages == null || messages.getLastMessageId() == null) {
            return;
        }

        // we don't take messages from the past.
        if (messages.getLastMessageId().getMessageId() != null && messages.getLastMessageId().getMessageId() > getLastMessageId()) {
            setLastMessageId(messages.getLastMessageId().getMessageId());
        } else {
            return;
        }

        Map<String, List<Message>> newMap = new ConcurrentHashMap<>();


        if (messages.getMessages() != null && !messages.getMessages().isEmpty()) {
            updateStatus(messages.getMessages());


            // messages should already by sorted by date, and should not go back in time!
            List<Message> messageList = messages.getMessages();

            for (Message message : messageList) {
                User user = null;

                if (message.getType() != null && !message.getType().isEmpty()) {
                    user = findUserById(message.getType());
                }

                if (user == null) {
                    user = findUserByName(message.getUsername());
                }

                if (user == null) {
                    user = findUserByName(message.getToUser());
                }

                if (user != null) {
                    Chat chat;
                    List<Message> mapList = null;

                    if (!newMap.containsKey(user.getUsername())) {
                        mapList = Collections.synchronizedList(new LinkedList<Message>());
                        newMap.put(user.getUsername(), mapList);
                    } else {
                        mapList = newMap.get(user.getUsername());
                    }
                    mapList.add(message);

                    if (!chatUserMap.containsKey(user.getUsername())) {
                        chat = new Chat(user, message);
                        chats.add(chat);
                        chatUserMap.put(user.getUsername(), chat);
                    } else {
                        chat = chatUserMap.get(user.getUsername());
                        chat.addMessage(message);
                    }
                } else {
                    Timber.d("Couldn't find user in user list!");
                }
            }
            Collections.sort(chats);
            for (Chat chat : chats) {
                if (chat.getMessages() == null || chat.getMessages().isEmpty()) {
                    chats.remove(chat);
                } else {
                    LinkedList<Message> cleared = new LinkedList<>();
                    for (Message m : chat.getMessages()) {
                        if (m instanceof SendableMessage) {
                            SendableMessage sm = (SendableMessage) m;
                            if (sm.getCreated() == null || sm.getStatus() != null) {
                                cleared.add(sm);
                            }
                        } else {
                            cleared.add(m);
                        }
                    }
                    chat.getMessages().clear();
                    chat.getMessages().addAll(cleared);
                    Collections.sort(chat.getMessages());
                }
            }

            notifyChatDataChanged();
        }

        EventBus.getDefault().post(new Events.NewChatMessages(newMap));
    }

    private void updateStatus(List<Message> messages) {
      /*  boolean enableNotification = false;
        if (messages != null) {
            for (Message m : messages) {
                if (!m.getUsername().equals(application.getScannedSettings().getUserName())) {
                    enableNotification = true;
                    break;
                }
            }

            if (enableNotification) {
                EventBus.getDefault().postSticky(new Events.ChatStatus(true));
            }
        }*/
    }

    public void notifyChatDataChanged() {
        if (chats.isEmpty()) {
            //EventBus.getDefault().postSticky(new Events.ChatStatus(false));
        }
        EventBus.getDefault().postSticky(new Events.ChatDataUpdated());
    }

    // load contacts from storage cache.
    @SuppressLint("ApplySharedPref")
    private synchronized void loadContacts() {
        synchronized (contactsLock) {
            if (sharedPreferences.contains(contactEntry)) {
                Type type = new TypeToken<LinkedList<User>>() {
                }.getType();
                try {
                    LinkedList<User> contacts =
                            gson.fromJson(
                                    crypto.decryptFromHexToString(
                                            Fuzz.de(
                                                    sharedPreferences.getString(contactEntry, null),
                                                    application.getDeviceIdentifier()
                                            )
                                    ),
                                    type
                            );
                    updateUsers(contacts);
                    return;
                } catch (Exception e) {
                    Timber.e(e, "Couldn't read contacts, removing.");
                    sharedPreferences.edit().remove(contactEntry).commit();
                }
            }
        }
    }

    // get contacts from the server
    private void retrieveContacts() throws Exception {
        synchronized (contactsLock) {

            // if we're in medium security, and offline
            if (application.isSetupComplete() && application.getAgentSettings().getSecurity() == 0 && (!isConnected() || sessionManager.getSessionId() == null)) {
                loadContacts();
                return;
            }

            try {
                ChatBaseUrl contactsUrl = new ChatBaseUrl(application.getScannedSettings(), application.getString(R.string.getusers_endpoint), sessionManager.getSessionId());
                Location lastLocation = fixManager.getLastLocation();
                if (lastLocation != null) {
                    contactsUrl.setLocation(lastLocation);
                }
                Response response = httpClient.newCall(new Request.Builder().url(contactsUrl.toString(crypto)).build()).execute();
                String responseBody = response.body().string().trim();
                response.body().close();

                // if we don't receive a 200 ok, it's a network error.
                if (response.code() != 200 || responseBody.contains("HTTP/1.0 404 File not found")) {
                    if (BuildConfig.DEBUG) {
                        Crashlytics.setString("Url", contactsUrl.toString());
                        Crashlytics.setLong("Response Code", response.code());
                        Crashlytics.setString("Response", responseBody);
                    }
                    throw new Exception("Server did not respond appropriately.");
                }

                responseBody = crypto.decryptFromHexToString(responseBody);
                //Timber.d("Response Body: " + responseBody);

                if (responseBody.startsWith("Invalid SessionId")) {
                    EventBus.getDefault().post(new Events.InvalidSession());
                    return;
                }

                Serializer serializer = new Persister();

                if (responseBody.startsWith("<?xml version='1.0' encoding='utf-8' ?><error>")) {
                    com.xzfg.app.model.Error error = serializer.read(com.xzfg.app.model.Error.class, responseBody);
                    if (error != null && error.getMessage() != null && !error.getMessage().isEmpty() && error.getMessage().equalsIgnoreCase("Invalid SessionId")) {
                        EventBus.getDefault().post(new Events.InvalidSession());
                    } else {
                        if (BuildConfig.DEBUG) {
                            Crashlytics.setString("Url", contactsUrl.toString());
                            Crashlytics.setLong("Response Code", response.code());
                            Crashlytics.setString("Response", responseBody);
                        }
                        Timber.e("The server returned an error: " + error.getMessage());
                    }
                } else {
                    Users users = serializer.read(Users.class, responseBody);

                    if (users != null && users.getUsers() != null) {
                        User publicBroadcastUser = new User("b", application.getString(R.string.public_broadcast), true);
                        publicBroadcastUser.setSystem(true);
                        users.getUsers().add(publicBroadcastUser);

                        User generalChatUser = new User("x", application.getString(R.string.general_chat), true);
                        generalChatUser.setSystem(true);
                        users.getUsers().add(generalChatUser);

                        Collections.sort(users.getUsers());
                        if (application.isSetupComplete() && application.getAgentSettings().getSecurity() == 0) {
                            saveContacts(users.getUsers());
                        }
                        updateUsers(users.getUsers());
                    } else {
                        //Timber.d("No users found.");
                    }
                }

            } catch (Exception e) {
                //Timber.d(e, "Couldn't get contacts, trying cache.");
                if (application.isSetupComplete() && application.getAgentSettings().getSecurity() == 0 && sharedPreferences.contains(contactEntry)) {
                    loadContacts();
                } else {
                    throw e;
                }
            }
        }
    }

    private void saveContacts(LinkedList<User> users) {
        synchronized (contactsLock) {
            // cache to disk
            //Timber.d("Caching contacts.");
            boolean committed = sharedPreferences.edit().putString(contactEntry,
                    Fuzz.en(
                            crypto.encryptToHex(
                                    gson.toJson(users)
                            ),
                            application.getDeviceIdentifier()
                    )
            ).commit();
            if (!committed) {
                Timber.d("Failed to cache contacts to sharedPreferences for medium security.");
            }
        }
    }

    // expose contacts.
    public List<User> getContacts() {
        synchronized (contactsLock) {
            return Collections.unmodifiableList(contacts);
        }
    }

    private boolean isConnected() {
        NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isAvailable() && activeNetwork.isConnected();
    }

    @Override
    public String toString() {
        return "ChatManager{" +
                "lastMessageId=" + lastMessageId +
                ", paused=" + paused +
                ", wasDown=" + wasDown +
                ", started=" + started +
                ", listHandlerThread=" + listHandlerThread +
                ", contactHandlerThread=" + contactHandlerThread +
                ", contacts=" + contacts +
                ", chatTabSelected=" + chatTabSelected +
                ", chats=" + chats +
                ", chatUserMap=" + chatUserMap +
                ", contactNameMap=" + contactNameMap +
                ", contactIdMap=" + contactIdMap +
                ", application=" + application +
                ", crypto=" + crypto +
                ", httpClient=" + httpClient +
                ", gson=" + gson +
                ", sharedPreferences=" + sharedPreferences +
                ", connectivityManager=" + connectivityManager +
                ", powerManager=" + powerManager +
                ", fixManager=" + fixManager +
                ", sessionManager=" + sessionManager +
                ", contactEntry='" + contactEntry + '\'' +
                '}';
    }

    private class ContactHandlerThread extends HandlerThread {
        private final Handler mHandler;
        private final ContactRunnable runnable;
        private volatile boolean alive = true;

        public ContactHandlerThread() {
            // use THREAD_PRIORITY_LESS_FAVORABLE to decrease the thread priority by 2 steps.
            // this abouve THREAD_PRIORITY_BACKGROUND, as that results in too many skipped attempts,
            // yet low enough that it should never impact the UI.
            super(ContactHandlerThread.class.getName(), Givens.THREAD_PRIORITY);
            start();
            mHandler = new Handler(getLooper());
            runnable = new ContactRunnable();
            mHandler.post(runnable);
        }

        public void kill() {
            alive = false;
            mHandler.removeCallbacks(runnable);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                quitSafely();
            } else {
                quit();
            }
        }

        private final class ContactRunnable implements Runnable {
            @Override
            public void run() {
                //Timber.d("Getting contacts.");
                try {
                    if (application.isSetupComplete() && application.getAgentSettings().getAgentRoles().chat()) {
                        if (sessionManager.getSessionId() == null) {
                            while (isConnected() && sessionManager.getSessionId() == null) {
                                try {
                                    //Timber.d("Waiting for session id.");
                                    Thread.sleep(200);
                                } catch (Exception e) {
                                    Timber.w(e, "Waiting for session id interrupted.");
                                }
                            }
                        }

                        try {
                            ChatManager.this.retrieveContacts();
                        } catch (InterruptedException e) {
                            Timber.d(e, "Thread interrupted.");
                        } catch (Exception e) {
                            if (!Network.isNetworkException(e)) {
                                Timber.w(e, "An error occurred attempting to retrieve contacts.");
                            }
                        }
                    }
                } catch (Exception e) {
                    if (!Network.isNetworkException(e)) {
                        Timber.d(e, "Error retrieving contacts.");
                    }
                }

                if (alive) {
                    //Timber.d("Chat user retrieval complete, next run in " + LONG_INTERVAL + "ms.");
                    mHandler.postDelayed(runnable, LONG_INTERVAL);
                } else {
                    //Timber.d("Chat user retrieval complete.");
                }
            }
        }

    }

    private class MessageListHandlerThread extends HandlerThread {
        private final Handler mHandler;
        private final MessageListRunnable runnable;
        private volatile boolean alive = true;

        public MessageListHandlerThread() {
            super(MessageListHandlerThread.class.getName(), Givens.THREAD_PRIORITY);
            start();
            mHandler = new Handler(getLooper());
            runnable = new MessageListRunnable();

            // we start with the short interval to allow contact list time to process.
            mHandler.postDelayed(runnable, SHORT_INTERVAL);
            //Timber.d("Message List Handler Thread Created.");
        }

        public void kill() {
            alive = false;
            mHandler.removeCallbacks(runnable);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                quitSafely();
            } else {
                quit();
            }
        }

        public Handler getHandler() {
            return mHandler;
        }

        /**
         * This provides the runnable that does the work of polling for messages. Why do this here?
         * By performing the work in the runable on this thread, we wait until the work is done
         * before queuing up the next polling event.  So if we have 1.5 second window between polling
         * attempts, we complete one before the next one is polled. That way if the server takes 6
         * seconds to respond to us, we haven't already queued up 4 future events.
         */
        private class MessageListRunnable implements Runnable {

            public void retrieveMessages(String sessionId) throws Exception {
                ScannedSettings scannedSettings = application.getScannedSettings();
                if (scannedSettings == null) {
                    throw new Exception("Scanned settings shouldn't be null!");
                }


                ChatMessagesUrl messagesUrl = new ChatMessagesUrl(scannedSettings, application.getString(R.string.chatmessages_endpoint), sessionId);
                messagesUrl.setLastMessageId(getLastMessageId());
                //Timber.d("Calling: " + messagesUrl);
                //Timber.d("Calling (encrypted):" + messagesUrl.toString(crypto));

                Response response = httpClient.newCall(new Request.Builder().url(messagesUrl.toString(crypto)).build()).execute();
                String responseBody = response.body().string().trim();
                //Timber.d("Response Body: " + responseBody);
                response.body().close();

                // if we don't receive a 200 ok, it's a network error.
                if (response.code() != 200 || responseBody.isEmpty() || responseBody.contains("HTTP/1.0 404 File not found") || responseBody.startsWith("ok")) {
                    if (BuildConfig.DEBUG) {
                        Crashlytics.setString("Url", messagesUrl.toString());
                        Crashlytics.setLong("Response Code", response.code());
                        Crashlytics.setString("Response", responseBody);
                    }
                    throw new Exception("Server did not respond appropriately.");
                }

                responseBody = crypto.decryptFromHexToString(responseBody).trim();


                //Timber.d("Response Body (decrypted): " + responseBody);

                // if we have an invalid sessionid, send the broadcast.
                if (responseBody.startsWith("Invalid SessionId")) {
                    EventBus.getDefault().post(new Events.InvalidSession());
                    return;
                }

                if (responseBody.startsWith("ok") || !responseBody.startsWith("<?xml version='1.0' encoding='utf-8' ?>")) {
                    if (BuildConfig.DEBUG) {
                        Crashlytics.setString("Url", messagesUrl.toString());
                        Crashlytics.setLong("Response Code", response.code());
                        Crashlytics.setString("Response", responseBody);
                    }
                    throw new Exception("Server did not send a valid response.");
                }

                RegistryMatcher matcher = new RegistryMatcher();
                matcher.bind(Date.class, new DateTransformer());
                Serializer serializer = new Persister(matcher);

                if (responseBody.startsWith("<?xml version='1.0' encoding='utf-8' ?><error>")) {
                    com.xzfg.app.model.Error error = serializer.read(com.xzfg.app.model.Error.class, responseBody);
                    if (error != null && error.getMessage() != null && !error.getMessage().isEmpty() && error.getMessage().equalsIgnoreCase("Invalid SessionId")) {
                        EventBus.getDefault().post(new Events.InvalidSession());
                    } else {
                        Timber.e("The server returned an error: " + error.getMessage());
                    }
                } else {
                    Messages messages = serializer.read(Messages.class, responseBody);
                    if (messages.getMessages() != null && !messages.getMessages().isEmpty()) {
                        addMessages(messages);
                    } else {
                        //Timber.d("No messages to process.");
                    }
                }
            }

            @Override
            public void run() {
                //Timber.d("Looking for messages.");
                try {
                    if (!isConnected()) {
                        throw new Exception("Not connected.");
                    }

                    if (application.isSetupComplete() && application.getAgentSettings().getAgentRoles().chat()) {

                        // keep trying for a session id
                        if (sessionManager.getSessionId() == null) {
                            while (isConnected() && (sessionManager.getSessionId() == null || (getContacts() != null && getContacts().isEmpty()))) {
                                try {
                                    //Timber.d("Looping until we have our values.");
                                    Thread.sleep(250);
                                } catch (Exception e) {
                                    Timber.e(e, "Waiting for session id interrupted.");
                                }
                            }
                        }

                        if (!isConnected() || sessionManager.getSessionId() == null || getContacts() == null || getContacts().isEmpty()) {
                            throw new Exception("Network/Session/Contacts unavailable.");
                        }

                        try {
                            if (sessionManager.getSessionId() != null) {
                                retrieveMessages(sessionManager.getSessionId());
                            }
                        } catch (InterruptedException e) {
                            Timber.d(e, "Thread interrupted.");
                        } catch (SocketTimeoutException e) {
                            Timber.d(e, "Timeout Exception: Stupid Network.");
                        } catch (InterruptedIOException e) {
                            Timber.d(e, "Interupted IO Exception: Stupid Network.");
                        } catch (SocketException e) {
                            Timber.d(e, "Socket Exception: Stupid Network.");
                        } catch (Exception e) {
                            Timber.w(e, "An error occurred attempting to retrieve messages.");
                        }
                    } else {
                        //Timber.d(MessageListRunnable.class.getName() + " NEEDS SETTINGS (OR ROLE)");
                    }
                } catch (Exception e) {
                    Timber.d(e, "Couldn't get messages.");
                }

                if (alive) {
                    //Timber.d("Chat message retrieval complete, next run in " + getInterval() + "ms.");
                    mHandler.postDelayed(runnable, getInterval());
                } else {
                    //Timber.d("Chat message retrieval complete.");
                }
            }
        }


    }
}
