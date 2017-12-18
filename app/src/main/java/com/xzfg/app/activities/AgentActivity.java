package com.xzfg.app.activities;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.StrictMode;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.PopupWindow;
import android.widget.Toast;

import com.soundcloud.android.crop.Crop;
import com.xzfg.app.Application;
import com.xzfg.app.BaseFragment;
import com.xzfg.app.BuildConfig;
import com.xzfg.app.DebugLog;
import com.xzfg.app.Events;
import com.xzfg.app.Givens;
import com.xzfg.app.R;
import com.xzfg.app.fragments.ATCommandsFragment;
import com.xzfg.app.fragments.AppPrefsFragment;
import com.xzfg.app.fragments.CheckinFragment;
import com.xzfg.app.fragments.ConfigurationFragment;
import com.xzfg.app.fragments.G4ChatFragment;
import com.xzfg.app.fragments.HelpFragment;
import com.xzfg.app.fragments.NavigationDrawerFragment;
import com.xzfg.app.fragments.PagerFragment;
import com.xzfg.app.fragments.StatusFragment;
import com.xzfg.app.fragments.UnregisterFragment;
import com.xzfg.app.fragments.WipeFragment;
import com.xzfg.app.fragments.alerts.AlertDetailsFragment;
import com.xzfg.app.fragments.alerts.AlertRecordListFragment;
import com.xzfg.app.fragments.alerts.AlertRootFragment;
import com.xzfg.app.fragments.boss.BossModeFragment;
import com.xzfg.app.fragments.chat.ChatConversationFragment;
import com.xzfg.app.fragments.chat.ChatRootFragment;
import com.xzfg.app.fragments.dialogs.AlertDialogFragment;
import com.xzfg.app.fragments.dialogs.ConfirmSosDialog;
import com.xzfg.app.fragments.media.AudioPlaybackFragment;
import com.xzfg.app.fragments.media.ImageViewerFragment;
import com.xzfg.app.fragments.media.MediaRecordListFragment;
import com.xzfg.app.fragments.media.MediaRootFragment;
import com.xzfg.app.fragments.media.VideoPlaybackFragment;
import com.xzfg.app.fragments.poi.CreatePoiFragment;
import com.xzfg.app.fragments.poi.PoiRecordListFragment;
import com.xzfg.app.fragments.poi.PoiRootFragment;
import com.xzfg.app.fragments.setup.ConnectionAwareness;
import com.xzfg.app.managers.AlertManager;
import com.xzfg.app.managers.ChatManager;
import com.xzfg.app.managers.CollectedMediaManager;
import com.xzfg.app.managers.FixManager;
import com.xzfg.app.managers.G4Manager;
import com.xzfg.app.managers.MediaManager;
import com.xzfg.app.managers.PoiManager;
import com.xzfg.app.model.AgentRoleComponent;
import com.xzfg.app.model.AgentRoles;
import com.xzfg.app.model.AgentSettings;
import com.xzfg.app.model.AlertContent;
import com.xzfg.app.model.Media;
import com.xzfg.app.model.UploadPackage;
import com.xzfg.app.model.url.MessageUrl;
import com.xzfg.app.services.GattClient;
import com.xzfg.app.services.ProfileService;
import com.xzfg.app.util.BatteryWhiteListUtil;
import com.xzfg.app.util.MessageUtil;

import java.io.File;
import java.util.Calendar;
import java.util.List;

import javax.inject.Inject;

import de.greenrobot.event.EventBus;
import timber.log.Timber;

/**
 * The agent activity is actually the heart of the application, where users will spend the majority
 * of their time. It provides the swipe-able tabs.
 */
public class AgentActivity extends BaseActivity implements
        AgentRoleComponent, NavigationDrawerFragment.NavigationDrawerCallbacks,
        ViewPager.OnPageChangeListener, ViewTreeObserver.OnGlobalLayoutListener,
        View.OnClickListener {

    private static final String FTAG = "AgentActivityFragment";
    @Inject
    public InputMethodManager inputMethodManager;
    @Inject
    public ChatManager chatManager;
    @Inject
    public CollectedMediaManager collectedMediaManager;
    @Inject
    public PoiManager poiManager;
    @Inject
    public AlertManager alertManager;
    @Inject
    MediaManager mediaManager;
    @Inject
    @SuppressWarnings("unused")
    FixManager fixManager;
    @Inject
    G4Manager g4Manager;
    private ConfirmSosDialog confirmSosDialog;
    private String NAME_ALESSA = "Echo-3BR";

    /**
     * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
     */
    private NavigationDrawerFragment mNavigationDrawerFragment;
    private Application application;
    private ActionBar actionBar;
    private FragmentManager fragmentManager;
    private boolean bossMode;
    private View containerView;
    private View decorView;
    private PopupWindow popupWindow;
    private boolean inBossMode;
    GattClient gattClient;
    // SOS sequence
    final private String mSosSequence = "udud";
    final private long mSosSequenceMaxTime = 5000;
    private long mSosSequenceStart = 0;
    private String mSosSequenceQueue = "";
    static String BLUETOOTH_NAME;
    FrameLayout frameLayout;
    View v;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED);
        try {
            getActionBar().hide();
            actionBar = getActionBar();
        } catch (Exception e) {
            Timber.w(e, "Failed to hide actionbar.");
        }

        application = (Application) getApplication();
        application.inject(this);

        fragmentManager = getSupportFragmentManager();

        // turn off screenshot and the snapshot view in android's activity pager, but only for
        // non-debug builds
        if (!BuildConfig.DEBUG) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        }
        setContentView(R.layout.activity_agent);
        decorView = getWindow().getDecorView();
        decorView.setVisibility(View.GONE);

        // Set up the navigation drawer.
        mNavigationDrawerFragment = (NavigationDrawerFragment) fragmentManager.findFragmentById(R.id.navigation_drawer);
        mNavigationDrawerFragment.setUp(R.id.navigation_drawer, (DrawerLayout) findViewById(R.id.drawer_layout));
        mNavigationDrawerFragment.lock();
        containerView = findViewById(R.id.agent_container);

        // Start live tracking automatically if required
        if (!fixManager.isTracking() && application.getAgentSettings().getAgentRoles().livetrackingon()) {
            EventBus.getDefault().post(new Events.StartLiveTracking());
        }


      /*  IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED);
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);*/
       // this.registerReceiver(mReceiver, filter);


    }

    @Override
    public void onStart() {
      super.onStart();
      BatteryWhiteListUtil.checkBatteryWhiteList(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        fixManager.destroyVolumeObserver();
        EventBus.getDefault().removeStickyEvent(Events.Registration.class);
        bossMode = application.isBossModeEnabled();
        if (bossMode && !application.isFromResult()) {
            inBossMode = true;
            resumeBossMode();
        } else {
            inBossMode = false;
            resumeAgentMode();
        }
        decorView.setVisibility(View.VISIBLE);

        EventBus.getDefault().registerSticky(this);
        inputMethodManager.hideSoftInputFromWindow(decorView.getWindowToken(), 0);
        if (containerView != null) {
            ViewTreeObserver viewTreeObserver = containerView.getViewTreeObserver();
            if (viewTreeObserver != null) {
                viewTreeObserver.addOnGlobalLayoutListener(this);
            } else {
                Timber.w("View Tree Observer Is Null");
            }
        } else {
            Timber.w("Container view is Null");
        }

        EventBus.getDefault().postSticky(new Events.DisplayChanged("Status",R.id.status));
        //SystemClock.sleep(2000);
        EventBus.getDefault().postSticky(new Events.ActivityPaused());
        EventBus.getDefault().post(new Events.InitSettingStatus());

        if(application.getG4Manager() ==null|| application.getG4Manager().isConnectionState()==false){
            //  EventBus.getDefault().post(new Events.handleDisConnect("Connect"));
        }
        EventBus.getDefault().post(new Events.ReceiverMessageG4(false));
        EventBus.getDefault().post(new Events.InitChat(true));
    }


    public void resumeBossMode() {

 /*       if (BuildConfig.DEBUG) {
            Timber.w("RESUMING IN BOSS MODE");
        }

        inBossMode = true;

        // hide the actionbar.
        if (actionBar != null) {
            actionBar.hide();
            actionBar.setBackgroundDrawable(ContextCompat.getDrawable(this,R.drawable.ab_transparent_apptheme));
        }

        // lock the navigation drawer.
        mNavigationDrawerFragment.lock();

        if (BuildConfig.DEBUG) {
            Timber.e("RUNNING BOSS MODE FRAGMENT TRANSACTION");
        }

        fragmentManager.beginTransaction()
            .setTransition(FragmentTransaction.TRANSIT_NONE)
            .replace(
                R.id.agent_container,
                BossModeFragment.newInstance(application.getAgentSettings()),
                FTAG
            )
            .commitAllowingStateLoss();
        fragmentManager.executePendingTransactions();
        if (BuildConfig.DEBUG) {
            Timber.e("CLEARING THE FRAGMENT BACK STACK");
        }

        clearBackStack();*/
    }

    public void resumeAgentMode() {
        chatManager.onResumeFromUI();
        collectedMediaManager.onResumeFromUI();
        alertManager.onResumeFromUI();
        poiManager.onResumeFromUI();
        application.setFromResult(false);

        if (actionBar != null) {
            actionBar.setDisplayShowHomeEnabled(true);
            actionBar.setDisplayShowCustomEnabled(true);
            actionBar.setDisplayShowTitleEnabled(false);
            actionBar.setDisplayUseLogoEnabled(true);
            actionBar.setLogo(R.drawable.navigation);
            actionBar.setCustomView(R.layout.app_actionbar);
            // Here we set action bar color programmatically
           // actionBar.setBackgroundDrawable(ContextCompat.getDrawable(this,R.drawable.ab_bottom_solid_apptheme));
            actionBar.setBackgroundDrawable(ContextCompat.getDrawable(this,R.color.orange));
            actionBar.show();
        }
        containerView.setBackgroundColor(ContextCompat.getColor(this,R.color.timberwolf));
        inBossMode = false;

        mNavigationDrawerFragment.unlock();
        //if(g4Manager.isStatusScreen() || g4Manager.isMarkAlertScreen() || g4Manager.isChatScreen()) {

            fragmentManager.beginTransaction().setTransition(FragmentTransaction.TRANSIT_NONE).replace(R.id.agent_container, new PagerFragment(), FTAG).commitAllowingStateLoss();
            fragmentManager.executePendingTransactions();
        //}
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //deleteCache(this);
      //  unregisterReceiver(mReceiver);
    }
    public static void deleteCache(Context context) {
        try {
            File dir = context.getCacheDir();
            deleteDir(dir);
        } catch (Exception e) {}
    }

    public static boolean deleteDir(File dir) {
        if (dir != null && dir.isDirectory()) {
            String[] children = dir.list();
            for (int i = 0; i < children.length; i++) {
                boolean success = deleteDir(new File(dir, children[i]));
                if (!success) {
                    return false;
                }
            }
            return dir.delete();
        } else if(dir!= null && dir.isFile()) {
            return dir.delete();
        } else {
            return false;
        }
    }
    @Override
    public void onPause() {
        fixManager.createVolumeObserver();
        EventBus.getDefault().postSticky(new Events.ActivityPaused());
        EventBus.getDefault().unregister(this);
        if (confirmSosDialog != null) {
            confirmSosDialog.dismiss();
        }
        if (popupWindow != null && popupWindow.isShowing()) {
            popupWindow.dismiss();
        }

        bossMode = application.isBossModeEnabled();
        if (bossMode) {
            inBossMode = true;
            pauseForBossMode();
        }
        chatManager.onPauseFromUI();
        collectedMediaManager.onPauseFromUI();
        alertManager.onPauseFromUI();
        poiManager.onPauseFromUI();
        inputMethodManager.hideSoftInputFromWindow(decorView.getWindowToken(), 0);
        super.onPause();
        MessageUtil.sendMessage(this, MessageUtil.getMessageUrl(this, MessageUrl.MESSAGE_APP_HIDDEN));
    }

    public void pauseForBossMode() {
     /*   inBossMode = true;
        getWindow().getDecorView().setVisibility(View.GONE);
        if (actionBar != null) {
            actionBar.hide();
            actionBar.setBackgroundDrawable(ContextCompat.getDrawable(this,R.drawable.ab_transparent_apptheme));
        }
        containerView.setBackgroundColor(ContextCompat.getColor(this,R.color.black));

        clearBackStack();*/
    }


    public void clearBackStack() {
        // clear the backstack
        if (fragmentManager.getBackStackEntryCount() > 0) {
            FragmentManager.BackStackEntry first = fragmentManager.getBackStackEntryAt(0);
            fragmentManager.popBackStack(first.getId(), FragmentManager.POP_BACK_STACK_INCLUSIVE);
        }
    }
//jgjhhhj
    /**
     * Handle boss mode status changes.
     */
    @SuppressWarnings("unused")
    public void onEventMainThread(Events.BossModeStatus bossModeStatus) {
        if (bossModeStatus.getStatus().equals(Givens.ACTION_BOSSMODE_DISABLE) || bossModeStatus.getStatus().equals(Givens.ACTION_BOSSMODE_EXIT)) {
            // Exit boss mode only if allowed
            if (application.getAgentSettings().getAgentRoles().exitbossmode()) {
                if (fragmentManager.findFragmentByTag(FTAG) instanceof BossModeFragment) {
                    resumeAgentMode();
                }
            }
        }
        if (bossModeStatus.getStatus().equals(Givens.ACTION_BOSSMODE_ENABLE)) {
            if (!(fragmentManager.findFragmentByTag(FTAG) instanceof BossModeFragment)) {
                pauseForBossMode();
                resumeBossMode();
               // resumeAgentMode();
                getWindow().getDecorView().setVisibility(View.VISIBLE);
            }
        }
    }

    public void onEventMainThread(Events.AgentSettingsAcquired event) {
        updatePopup(event.getAgentSettings());
    }

    public void updatePopup(AgentSettings settings) {
        if (settings != null && popupWindow != null) {
            View root = popupWindow.getContentView();
            Timber.d("Updating popup.");

/*            root.findViewById(R.id.myself).setOnClickListener(this);
            root.findViewById(R.id.others).setOnClickListener(this);
            root.findViewById(R.id.media).setOnClickListener(this);
            root.findViewById(R.id.autotrack).setOnClickListener(this);
            root.findViewById(R.id.details).setOnClickListener(this);
            root.findViewById(R.id.create_poi).setOnClickListener(this);*/
            root.findViewById(R.id.canned_msg).setOnClickListener(this);

         /*   if (settings.getMapMyself() == 1) {
                //Timber.d("Enabling myself");
                ((CheckBox) root.findViewById(R.id.myself_checkbox)).setChecked(true);
            } else {
                //Timber.d("Disabling myself.");
                ((CheckBox) root.findViewById(R.id.myself_checkbox)).setChecked(false);
            }

            if (settings.getMapOthers() == 1) {
                //Timber.d("Enabling Others.");
                ((CheckBox) root.findViewById(R.id.others_checkbox)).setChecked(true);
            } else {
                //Timber.d("Disabling Others.");
                ((CheckBox) root.findViewById(R.id.others_checkbox)).setChecked(false);
            }

            if (settings.getMapMedia() == 1) {
                //Timber.d("Enabling media");
                ((CheckBox) root.findViewById(R.id.media_checkbox)).setChecked(true);
            } else {
                //Timber.d("Disabling media.");
                ((CheckBox) root.findViewById(R.id.media_checkbox)).setChecked(false);
            }

            if (settings.getAgentRoles().broadcastingservice()) {
                root.findViewById(R.id.autotrack_checkbox).setVisibility(View.GONE);
            } else {
                root.findViewById(R.id.autotrack_checkbox).setVisibility(View.GONE);
            }

            if (settings.getAutoTrack() == 1) {
                //Timber.d("Enabling auto-track.");
                ((CheckBox) root.findViewById(R.id.autotrack_checkbox)).setChecked(true);
            } else {
                //Timber.d("Disabling auto-track.");
                ((CheckBox) root.findViewById(R.id.autotrack_checkbox)).setChecked(false);
            }


            if (settings.getShowDetails() == 1) {
                //Timber.d("Enabling details.");
                ((CheckBox) root.findViewById(R.id.details_checkbox)).setChecked(true);
            } else {
                //Timber.d("Disabling details.");
                ((CheckBox) root.findViewById(R.id.details_checkbox)).setChecked(false);
            }*/

        }

    }
    public void onEventMainThread(Events.TrackingConnect event) {
        DebugLog.d("tracking connect:");
        EventBus.getDefault().postSticky(new Events.TrackingConnect());
    }
    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("paused", true);
    }

    public void onEventMainThread(Events.ChatStatus chatStatus) {
        if (chatStatus.hasChats()) {
            Fragment f = fragmentManager.findFragmentByTag(FTAG);
            if (f instanceof PagerFragment) {
                PagerFragment pf = (PagerFragment) f;
                if (pf.getSelected() == R.id.chat) {
                    FragmentManager cfm = pf.getChildFragmentManager();
                    //noinspection RestrictedApi
                    List<Fragment> fragments = cfm.getFragments();
                    if (fragments != null) {
                        for (Fragment fragment : fragments) {
                            if (fragment instanceof ChatRootFragment) {
                                FragmentManager fm = fragment.getChildFragmentManager();
                                if (fm.getBackStackEntryCount() == 0) {
                                    //EventBus.getDefault().postSticky(new Events.ChatStatus(false));
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public int getSelected() {
        Fragment f = fragmentManager.findFragmentByTag(FTAG);
        if (f != null && f instanceof PagerFragment) {
            PagerFragment pf = (PagerFragment) f;
            return pf.getSelected();
        }
        return -1;
    }

    public void setSelected(int id) {
        Fragment f = fragmentManager.findFragmentByTag(FTAG);
        if (f instanceof PagerFragment) {
            PagerFragment pf = ((PagerFragment) f);
            pf.setSelected(id);
            fragmentManager.executePendingTransactions();
        }
    }

    @Override
    public void onBackPressed() {
        boolean handle = false;
        Fragment f = fragmentManager.findFragmentByTag(FTAG);

        if (f == null) {
            super.onBackPressed();
            return;
        }

        if (f instanceof BossModeFragment) {
            finish();
            return;
        }

        if (f instanceof PagerFragment) {

            PagerFragment pf = (PagerFragment) f;
            int selected = pf.getSelected();

            FragmentManager cfm = pf.getChildFragmentManager();
            List<Fragment> fragments = null;
            if (cfm != null) {
                //noinspection RestrictedApi
                fragments = cfm.getFragments();
            }
            FragmentManager scfm = null;
            if (fragments != null) {
                for (Fragment fragment : fragments) {
                    if (fragment != null) {
                        switch (selected) {
                            case R.id.chat:

                                DebugLog.d("check back pressed:"+handle);
                                if (fragment instanceof BaseFragment){
                                    handle=((BaseFragment)fragment).onBackPressed();
                                    if (handle){
                                        DebugLog.d("check back pressed:"+handle);
                                        return;
                                    }
                                }
                                if (!handle) {
                                    if (fragment instanceof ChatRootFragment) {
                                        scfm = fragment.getChildFragmentManager();

                                    }
                                }
                                break;
                            case R.id.poi:
                                if (fragment instanceof PoiRootFragment) {
                                    scfm = fragment.getChildFragmentManager();
                                }
                                break;
                            case R.id.alerts:
                                if (fragment instanceof AlertRootFragment) {
                                    scfm = fragment.getChildFragmentManager();
                                }
                                break;
                            case R.id.collected_media:
                                if (fragment instanceof MediaRootFragment) {
                                    scfm = fragment.getChildFragmentManager();
                                }
                                break;
                        }
                    }
                    if (scfm != null && scfm.getBackStackEntryCount() > 0) {
                        scfm.popBackStack();
                        return;
                    }
                }
            }
        }

        if (fragmentManager.getBackStackEntryCount() > 0) {
            fragmentManager.popBackStack();
            return;
        }
      /*  if(getSupportFragmentManager().getBackStackEntryCount()>0){
            getSupportFragmentManager().popBackStack();
        }*/
        super.onBackPressed();
    }

    @Override
    public void onPageScrolled(int i, float v, int i2) {
        // noop
    }

    @Override
    public void onPageSelected(int i) {
        inputMethodManager.hideSoftInputFromWindow(containerView.getWindowToken(), 0);

        Fragment f = fragmentManager.findFragmentByTag(FTAG);

        if (!(f instanceof PagerFragment)) {
            return;
        } else {
            PagerFragment pf = (PagerFragment) f;
            int selected = pf.getSelected();
            SharedPreferences.Editor editor = getSharedPreferences("ignoreStatus", 0).edit();
            switch (selected) {
                case R.id.home:
                    chatManager.setChatTabSelected(false);
                    alertManager.setSelected(false);
                    poiManager.setSelected(false);
                    collectedMediaManager.setSelected(false);
                    EventBus.getDefault().postSticky(new Events.DisplayChanged(getString(R.string.tab_home), R.id.home));
                    EventBus.getDefault().postSticky(new Events.ActivityPaused());
                    break;
                case R.id.timer:
                    chatManager.setChatTabSelected(false);
                    alertManager.setSelected(false);
                    poiManager.setSelected(false);
                    collectedMediaManager.setSelected(false);
                    EventBus.getDefault().postSticky(new Events.DisplayChanged(getString(R.string.tab_timer), R.id.timer));
                    EventBus.getDefault().postSticky(new Events.ActivityPaused());
                    break;
                case R.id.checkin:
                    chatManager.setChatTabSelected(false);
                    alertManager.setSelected(false);
                    poiManager.setSelected(false);
                    collectedMediaManager.setSelected(false);
                    EventBus.getDefault().postSticky(new Events.DisplayChanged(getString(R.string.tab_checkin), R.id.checkin));
                    EventBus.getDefault().postSticky(new Events.ActivityPaused());
                    break;
                case R.id.contacts:
                    chatManager.setChatTabSelected(false);
                    alertManager.setSelected(false);
                    poiManager.setSelected(false);
                    collectedMediaManager.setSelected(false);
                    EventBus.getDefault().postSticky(new Events.DisplayChanged(getString(R.string.tab_contacts), R.id.contacts));
                    EventBus.getDefault().postSticky(new Events.ActivityPaused());
                    break;
                case R.id.chat:

                    chatManager.setChatTabSelected(true);
                    poiManager.setSelected(false);
                    alertManager.setSelected(false);
                    collectedMediaManager.setSelected(false);
                    EventBus.getDefault().postSticky(new Events.DisplayChanged(getString(R.string.tab_chat), R.id.chat));

                    EventBus.getDefault().postSticky(new Events.ActivityPaused());
//                    EventBus.getDefault().post(new Events.UpdateList());
                   // EventBus.getDefault().post(new Events.ChatStatus(false));
//                    try{
//                        if(application.getG4Manager() != null || application.getG4Manager().isConnectionState()){
//                            EventBus.getDefault().post(new Events.ReceiverMessageG4(true));
//                        }
//                    }
//                    catch (Exception e)
//                    {
//
//                    }
//                    new StatusFragment().onPause();
//                 //   EventBus.getDefault().post(new Events.ReceiverMessageG4(true));
//                    EventBus.getDefault().post(new Events.InitChat(false));
//                    EventBus.getDefault().post(new Events.RunSignal(false));
//
//                    editor.putBoolean("ignore",true);
//                    editor.apply();

                    break;
                case R.id.collect:
                    chatManager.setChatTabSelected(false);
                    poiManager.setSelected(false);
                    alertManager.setSelected(false);
                    collectedMediaManager.setSelected(false);
                    EventBus.getDefault().postSticky(new Events.DisplayChanged(getString(R.string.tab_collect), R.id.collect));
                    if (!inBossMode) {
                        EventBus.getDefault().postSticky(new Events.ActivityResumed());
                    }
                    break;
                case R.id.map:
                    chatManager.setChatTabSelected(false);
                    alertManager.setSelected(false);
                    poiManager.setSelected(false);
                    collectedMediaManager.setSelected(false);
                    EventBus.getDefault().postSticky(new Events.DisplayChanged(getString(R.string.tab_map), R.id.map));
                    EventBus.getDefault().postSticky(new Events.ActivityPaused());
                    break;
                case R.id.poi:
                    chatManager.setChatTabSelected(false);
                    poiManager.setSelected(true);
                    alertManager.setSelected(false);
                    collectedMediaManager.setSelected(false);
                    EventBus.getDefault().postSticky(new Events.DisplayChanged(getString(R.string.tab_poi), R.id.poi));
                    EventBus.getDefault().postSticky(new Events.ActivityPaused());
                    break;
                case R.id.alerts:
                    chatManager.setChatTabSelected(false);
                    poiManager.setSelected(false);
                    alertManager.setSelected(true);
                    collectedMediaManager.setSelected(false);
                    EventBus.getDefault().postSticky(new Events.DisplayChanged(getString(R.string.tab_alerts), R.id.alerts));
                    alertManager.setSelected(true);
                    EventBus.getDefault().postSticky(new Events.ActivityPaused());
                    break;
                case R.id.collected_media:
                    chatManager.setChatTabSelected(false);
                    poiManager.setSelected(false);
                    alertManager.setSelected(false);
                    collectedMediaManager.setSelected(true);
                    EventBus.getDefault().postSticky(new Events.DisplayChanged(getString(R.string.tab_media), R.id.collected_media));
                    collectedMediaManager.setSelected(true);
                    EventBus.getDefault().postSticky(new Events.ActivityPaused());
                    break;
             /*   case R.id.atcommands:

                    if(application.getG4Manager() == null){
                        Toast.makeText(AgentActivity.this,"Please, Connect with G4",Toast.LENGTH_LONG).show();
                    }
                    else {
                        EventBus.getDefault().postSticky(new Events.DisplayChanged("AT Commands", R.id.atcommands));
                        EventBus.getDefault().postSticky(new Events.ActivityPaused());
                    }
                    break;*/
                case R.id.markalert:

                    EventBus.getDefault().postSticky(new Events.DisplayChanged("Mark/Alert",R.id.markalert));
                   // SystemClock.sleep(2000);
                    EventBus.getDefault().postSticky(new Events.ActivityPaused());
//                    EventBus.getDefault().post(new Events.stopStatusSignal());
                    //EventBus.getDefault().post(new Events.InitSetting());
//                    EventBus.getDefault().post(new Events.checkAlert());
//                    EventBus.getDefault().post(new Events.ReceiverMessageG4(false));
//                    EventBus.getDefault().post(new Events.RunSignal(false));
//                    new StatusFragment().onPause();
//
//                    editor.putBoolean("ignore",true);
//                    editor.apply();

                    break;
                case R.id.status:

                    EventBus.getDefault().postSticky(new Events.DisplayChanged("Status",R.id.status));
                    //SystemClock.sleep(2000);
                    EventBus.getDefault().postSticky(new Events.ActivityPaused());
                    EventBus.getDefault().post(new Events.InitSettingStatus());

                    if(application.getG4Manager() ==null|| application.getG4Manager().isConnectionState()==false){
                      //  EventBus.getDefault().post(new Events.handleDisConnect("Connect"));

                    }
                    EventBus.getDefault().post(new Events.ReceiverMessageG4(false));
                    EventBus.getDefault().post(new Events.InitChat(true));

//                    editor.putBoolean("ignore",false);
//                    editor.apply();

                    break;
            }

        }
        supportInvalidateOptionsMenu();


    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(Events.CreatePoi createPoi) {
        clearBackStack();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.addToBackStack(null);
        transaction.replace(R.id.agent_container, CreatePoiFragment.newInstance(createPoi), FTAG);
        transaction.commitAllowingStateLoss();
        fragmentManager.executePendingTransactions();
        //

   }

    public void onEventMainThread(Events.AckMessage event) {
        AlertDialogFragment.newInstance(getString(R.string.notice), event.getAckMessage()).show(getSupportFragmentManager(), "ack_message");
    }

    public void onEventMainThread(Events.MenuItemSelected event) {
        if (mNavigationDrawerFragment != null) {
            mNavigationDrawerFragment.close();
        }
        Fragment f = fragmentManager.findFragmentByTag(FTAG);

        switch (event.getId()) {
            case R.id.sos:
                confirmSosDialog = ConfirmSosDialog.newInstance(getString(R.string.warning), getString(R.string.sos_confirm));
                confirmSosDialog.show(getSupportFragmentManager(), "sos_confirm");
                break;
            case R.id.chat:
            case R.id.collect:
            case R.id.home:
            case R.id.timer:
            case R.id.checkin:
            case R.id.contacts:
            case R.id.map:
            case R.id.alerts:
            case R.id.poi:
            case R.id.collected_media:
            case R.id.status:
            case R.id.markalert:


                getPagerFragmentAt(event.getId());

                break;

            case R.id.wipe: {
                clearBackStack();
                FragmentTransaction transaction = fragmentManager.beginTransaction();
                transaction.replace(R.id.agent_container, new WipeFragment(), FTAG);
                transaction.commitAllowingStateLoss();
                fragmentManager.executePendingTransactions();
                break;
            }

            case R.id.boss: {
                //application.enableBossMode();
              //  application.enableBossModeWithoutSetboss();
                EventBus.getDefault().post(new Events.BossModeStatus(Givens.ACTION_BOSSMODE_ENABLE));
                break;
            }

            case R.id.preferences: {
                EventBus.getDefault().postSticky(new Events.ActivityPaused());
                clearBackStack();
                FragmentTransaction transaction = fragmentManager.beginTransaction();
                transaction.replace(R.id.agent_container, new AppPrefsFragment(), FTAG);
                transaction.commitNow();
                break;
            }
            case R.id.configuration: {

                    EventBus.getDefault().postSticky(new Events.ActivityPaused());
                    clearBackStack();
                    FragmentTransaction transaction11 = fragmentManager.beginTransaction();
                    transaction11.replace(R.id.agent_container, new ATCommandsFragment(), FTAG);
                    transaction11.commitNow();
                    fragmentManager.executePendingTransactions();

                    //
                    EventBus.getDefault().postSticky(new Events.ActivityPaused());
                    clearBackStack();
                    FragmentTransaction transaction = AgentActivity.this.fragmentManager.beginTransaction();
                    transaction.replace(R.id.agent_container, new ConfigurationFragment(), FTAG);
                    transaction.commit();
                    fragmentManager.executePendingTransactions();
                    SharedPreferences.Editor editor = getSharedPreferences("ignoreStatus", 0).edit();
                    editor.putBoolean("ignore",true);
                    editor.apply();
                    break;


            }
            case R.id.unregister:{
                     /*EventBus.getDefault().postSticky(new Events.ActivityPaused());
                    clearBackStack();
                    FragmentTransaction transaction11 = fragmentManager.beginTransaction();
                     transaction11.replace(R.id.agent_container, new ATCommandsFragment(), FTAG);
                     transaction11.commitNow();
                    fragmentManager.executePendingTransactions();

                    EventBus.getDefault().postSticky(new Events.ActivityPaused());
                    clearBackStack();
                    FragmentTransaction transaction = fragmentManager.beginTransaction();
                    transaction.replace(R.id.agent_container, new UnregisterFragment(), FTAG);
                    transaction.commitNow();
                    fragmentManager.executePendingTransactions();
                    SharedPreferences.Editor editor = getSharedPreferences("ignoreStatus", 0).edit();
                    editor.putBoolean("ignore",true);
                    editor.apply();*/
                EventBus.getDefault().postSticky(new Events.ActivityPaused());
                clearBackStack();
                FragmentTransaction transaction = fragmentManager.beginTransaction();
                transaction.replace(R.id.agent_container, new UnregisterFragment(), FTAG);
                transaction.commitNow();
                    break;

                }

            case R.id.settings: {
                EventBus.getDefault().postSticky(new Events.ActivityPaused());
                startActivity(new Intent(this, SetupActivity.class));
                break;
            }
            case R.id.help: {
                EventBus.getDefault().postSticky(new Events.ActivityPaused());
                clearBackStack();
                FragmentTransaction transaction = fragmentManager.beginTransaction();
                transaction.replace(R.id.agent_container, new HelpFragment(), FTAG);
                transaction.commitNow();
                fragmentManager.executePendingTransactions();
                break;
            }
            case R.id.atcommands:{


                    EventBus.getDefault().postSticky(new Events.ActivityPaused());
                    clearBackStack();
                    FragmentTransaction transaction = fragmentManager.beginTransaction();
                    transaction.replace(R.id.agent_container, new ATCommandsFragment(), FTAG);
                    transaction.commitNow();
                    fragmentManager.executePendingTransactions();
                    SharedPreferences.Editor editor = getSharedPreferences("ignoreStatus", 0).edit();
                    editor.putBoolean("ignore",true);
                    editor.apply();
                    break;

            }

        }

    }


    public PagerFragment getPagerFragmentAt(int tabId) {


        Fragment f = fragmentManager.findFragmentByTag(FTAG);

        if (!(f instanceof PagerFragment)) {
            clearBackStack();
            FragmentTransaction transaction = fragmentManager.beginTransaction();
            transaction.replace(R.id.agent_container, new PagerFragment(), FTAG);
            transaction.commitAllowingStateLoss();
            fragmentManager.executePendingTransactions();
            f = fragmentManager.findFragmentByTag(FTAG);
        }

        if (f instanceof PagerFragment) {
            EventBus.getDefault().postSticky(new Events.ActivityPaused());
            clearBackStack();
            FragmentTransaction transaction13 = fragmentManager.beginTransaction();
            transaction13.replace(R.id.agent_container, new ATCommandsFragment(), FTAG);

            PagerFragment pf = ((PagerFragment) f);
            if (pf.getSelected() != tabId) {
                pf.setSelected(tabId);
            }

            fragmentManager.executePendingTransactions();
            return pf;
        }

        return null;
    }
    public void onEventMainThread(Events.OpenStatus openStatus) {
        EventBus.getDefault().postSticky(new Events.ActivityPaused());
                clearBackStack();
//        FragmentTransaction transaction11 = fragmentManager.beginTransaction();
//        transaction11.replace(R.id.agent_container, new ATCommandsFragment(), FTAG);
//        transaction11.commitNow();
//        fragmentManager.executePendingTransactions();
        Fragment f = fragmentManager.findFragmentByTag(FTAG);
        PagerFragment pf = ((PagerFragment) f);
        if (pf.getSelected() !=  openStatus.getTabId()) {
            pf.setSelected(openStatus.getTabId());
        }
        fragmentManager.executePendingTransactions();
    }
    public void openChild(int tabId, int frameId, Class<?> fragmentType, Fragment newFragment, String fragmentTag, boolean clearBackStack) {
        PagerFragment pf = getPagerFragmentAt(tabId);
        fragmentTag = newFragment.getClass().getSimpleName() + "_" + fragmentTag;

        if (pf == null) {
            throw new RuntimeException("NULL PAGER FRAGMENT.");
        }

        //noinspection RestrictedApi
        List<Fragment> fragments = pf.getChildFragmentManager().getFragments();

        Fragment rootFragment = null;
        //Timber.d("TYPE EXPECTED: " + fragmentType.getName());
        for (Fragment fragment : fragments) {
            //Timber.d(fragment.getClass().getName());
            if (fragmentType.isInstance(fragment)) {
                //Timber.d("MATCH FOUND.");
                rootFragment = fragment;
                break;
            }
        }

        if (rootFragment == null) {
            throw new RuntimeException("NULL ROOT FRAGMENT");
        }

        //Timber.d("Parent Fragment: " + rootFragment.getClass().getSimpleName() + " tag: " + rootFragment.getTag() + ", id: " + rootFragment.getId());
        FragmentManager pfm = rootFragment.getChildFragmentManager();

        Fragment existingFragment = null;
        // clear the backstack if there is one.
        if (clearBackStack && pfm.getBackStackEntryCount() > 0) {
            //Timber.d("clearing backstack.");
            int id = pfm.getBackStackEntryAt(0).getId();
            pfm.popBackStack(id, FragmentManager.POP_BACK_STACK_INCLUSIVE);
            //pfm.executePendingTransactions();
        } else {
            existingFragment = pfm.findFragmentByTag(fragmentTag);
        }

        FragmentTransaction trans = pfm.beginTransaction();
        //trans.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
        if (existingFragment != null) {
            //Timber.d("USING EXISTING FRAGMENT FOR " + fragmentTag);
            trans.replace(frameId, existingFragment, fragmentTag);
        } else {
            //Timber.d("USING NEW FRAGMENT FOR " + fragmentTag);
            trans.replace(frameId, newFragment, fragmentTag);
        }

        trans.addToBackStack(null);
        trans.commitAllowingStateLoss();

        //pfm.executePendingTransactions();
        //fragmentManager.executePendingTransactions();

        supportInvalidateOptionsMenu();

    }

    public void onEventMainThread(Events.OpenAlertGroup event) {
        //Timber.d("OpenAlertGroup:" + String.valueOf(event.getGroup().getGroupId()) + "_" + event.getGroup().getGroupName());
        openChild(R.id.alerts, R.id.alert_root_frame, AlertRootFragment.class, AlertRecordListFragment.newInstance(event.getGroup()), String.valueOf(event.getGroup().getGroupId()) + "_" + event.getGroup().getGroupName(), true);
    }


    public void onEventMainThread(Events.OpenPoiGroup event) {
        //Timber.d("OpenPoiGroup:" + event.getGroup().getId() + "_" + event.getGroup().getName());
        openChild(R.id.poi, R.id.poi_root_frame, PoiRootFragment.class, PoiRecordListFragment.newInstance(event.getGroup()), event.getGroup().getId() + "_" + event.getGroup().getName(), true);
    }

    public void onEventMainThread(Events.OpenCase event) {
        //Timber.d("OpenCase:" + event.getCaseName());
        openChild(R.id.collected_media, R.id.media_root_frame, MediaRootFragment.class, MediaRecordListFragment.newInstance(event.getCaseName()), String.valueOf(event.getCaseName()), true);
    }

    // this opens a record in the collected_media tab to display media. So you have Case List -> Record. We don't want to clear the case list off the back stack.
    public void onEventMainThread(Events.OpenRecord event) {
        //Timber.d("OpenRecord:"+event.getRecord().toString());
        Media.Record record = event.getRecord();
        String type = record.getType().toLowerCase();
        Fragment f = null;
        switch (type) {
            case "audio":
                f = AudioPlaybackFragment.newInstance(record);
                break;
            case "video":
                f = VideoPlaybackFragment.newInstance(record);
                break;
            case "img":
                f = ImageViewerFragment.newInstance(record);
                break;
        }

        if (f != null) {
            openChild(R.id.collected_media, R.id.media_root_frame, MediaRootFragment.class, f, String.valueOf(record.getId()), false);
        } else {
            Timber.e("Media Record Fragment was null. " + record.toString());
        }
    }

    // this opens a record in the alerts tab to display alert details. So you have Alerts -> Record. We don't want to clear the alerts list off the back stack.
    public void onEventMainThread(Events.OpenAlertRecord event) {
        //Timber.d("OpenAlertRecord:"+event.getRecord().toString());
        AlertContent.Record record = event.getRecord();
        Fragment f = AlertDetailsFragment.newInstance(record);
        openChild(R.id.alerts, R.id.alert_root_frame, AlertRootFragment.class, f, String.valueOf(record.getAlertId()), false);
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(Events.StartChat event) {
       // openChild(R.id.chat, R.id.chat_root_frame, ChatRootFragment.class, ChatConversationFragment.newInstance(event.getContact()), String.valueOf(event.getContact().getUserId()), false);
    }


    public void onEventMainThread(Events.NetworkStatus networkStatus) {
        Fragment f = fragmentManager.findFragmentByTag(FTAG);
        if (f instanceof ConnectionAwareness) {
            if (networkStatus.isUp()) {
                ((ConnectionAwareness) f).connectionGained();
            } else {
                ((ConnectionAwareness) f).connectionLost();
            }
        }
    }

    @Override
    public void onPageScrollStateChanged(int i) {
        // noop
    }

    @Override
    public void onNavigationDrawerItemSelected(int id) {
        //Timber.d("Switching fragments: " + id);
        // update the main content by replacing fragments
        fragmentManager.beginTransaction()
                .replace(R.id.agent_container, DrawerPlaceholderFragment.newInstance(id))
                .commitAllowingStateLoss();
    }

    public void onSectionAttached(int number) {
        //Timber.d("Section Attached: " + number);
        /*
            switch (number) {
            }
        */
    }


    @SuppressWarnings("deprecation")
    public void restoreActionBar() {
        ActionBar actionBar = getActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        /*
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle(mTitle);
        */
    }

    public void onEventMainThread(Events.OpenHome openHome) {
        Fragment f = fragmentManager.findFragmentByTag(FTAG);
        PagerFragment pf = ((PagerFragment) f);
        if (pf.getSelected() !=  openHome.getTabId()) {
            pf.setSelected(openHome.getTabId());
        }
        fragmentManager.executePendingTransactions();
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //Timber.e("CALLED onCreateOptionsMenu");

        // if the nav drawer is open, we don't ever show a menu.
        if (mNavigationDrawerFragment != null && mNavigationDrawerFragment.isDrawerOpen()) {
            restoreActionBar();
            return super.onCreateOptionsMenu(menu);
        }


        int selected = getSelected();
        if ( selected == R.id.markalert || selected == R.id.status) {
            getMenuInflater().inflate(R.menu.map, menu);
            return true;
        }
        if(selected == R.id.chat){
            getMenuInflater().inflate(R.menu.chat, menu);
            return true;
        }
        if(selected == 2){
            getMenuInflater().inflate(R.menu.chat, menu);
            return true;
        }

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_more_chat) {
          //  openMenu(item);
            EventBus.getDefault().post(new Events.setCannedText());
        }
        if (id == R.id.action_more) {

        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onGlobalLayout() {
        if (containerView != null) {
            float heightDiff = (containerView.getRootView().getHeight() - containerView.getHeight()) / Resources.getSystem().getDisplayMetrics().density;
            EventBus.getDefault().post(new Events.KeyBoardShown(!(heightDiff > 100)));
        }
    }


    public void openMenu(MenuItem menuItem) {
        if (popupWindow == null) {
            LayoutInflater layoutInflater = LayoutInflater.from(this);
            @SuppressLint("InflateParams") View popupView = layoutInflater.inflate(R.layout.canned_message, null, false);
            popupWindow = new PopupWindow(popupView, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            popupWindow.setWindowLayoutMode(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            updatePopup(application.getAgentSettings());
        }
        if (popupWindow.isShowing()) {
            popupWindow.dismiss();
        } else {
            popupWindow.showAsDropDown(
                    findViewById(R.id.action_more),
                    0,
                    (int) (-1 * (8 * getResources().getDisplayMetrics().density))
            );
        }

    }

    @Override
    public void supportInvalidateOptionsMenu() {
        if (popupWindow != null && popupWindow.isShowing()) {
            popupWindow.dismiss();
        }
        super.supportInvalidateOptionsMenu();
    }
    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            View v = getCurrentFocus();
            if ( v instanceof EditText || v instanceof Button) {
                Rect outRect = new Rect();

                v.getGlobalVisibleRect(outRect);
                int a = outRect.centerX();
                int b = outRect.centerY();
                outRect.set(0,outRect.top - 40,outRect.right + 10000,outRect.bottom);
                int c = (int)event.getRawX();
                int d = (int)event.getRawY();
                if (!outRect.contains((int)event.getRawX()  , (int)event.getRawY() )) {

                    v.clearFocus();
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);

                }
            }
            else{

            }
        }
        return super.dispatchTouchEvent(event);
    }
    @Override
    public void onClick(View v) {
        if (popupWindow != null && popupWindow.isShowing()) {
            popupWindow.dismiss();
        }
        int id = v.getId();
        switch (id) {
          /*  case R.id.myself: {
                //Timber.d("Toggling myself.");
                CheckBox checkBox = (CheckBox) v.findViewById(R.id.myself_checkbox);
                if (checkBox.isChecked()) {
                    application.getAgentSettings().setMapMyself(0);
                    MessageUtil.sendMessage(this, MessageUtil.getMessageUrl(application, "Set Settings|MapMyselfCASESAgent@0"));
                    EventBus.getDefault().post(new Events.AgentSettingsAcquired(application.getAgentSettings()));
                    checkBox.setChecked(false);
                } else {
                    application.getAgentSettings().setMapMyself(1);
                    MessageUtil.sendMessage(this, MessageUtil.getMessageUrl(application, "Set Settings|MapMyselfCASESAgent@1"));
                    EventBus.getDefault().post(new Events.AgentSettingsAcquired(application.getAgentSettings()));
                    checkBox.setChecked(true);
                }
                break;
            }
            case R.id.others: {
                //Timber.d("Toggling others");
                CheckBox checkBox = (CheckBox) v.findViewById(R.id.others_checkbox);
                if (checkBox.isChecked()) {
                    application.getAgentSettings().setMapOthers(0);
                    MessageUtil.sendMessage(this, MessageUtil.getMessageUrl(application, "Set Settings|MapOthersCASESAgent@0"));
                    EventBus.getDefault().post(new Events.AgentSettingsAcquired(application.getAgentSettings()));
                    checkBox.setChecked(false);
                } else {
                    application.getAgentSettings().setMapOthers(1);
                    MessageUtil.sendMessage(this, MessageUtil.getMessageUrl(application, "Set Settings|MapOthersCASESAgent@1"));
                    EventBus.getDefault().post(new Events.AgentSettingsAcquired(application.getAgentSettings()));
                    checkBox.setChecked(true);
                }
                break;
            }
            case R.id.media: {
                //Timber.d("Toggling media.");
                CheckBox checkBox = (CheckBox) v.findViewById(R.id.media_checkbox);
                if (checkBox.isChecked()) {
                    application.getAgentSettings().setMapMedia(0);
                    EventBus.getDefault().post(new Events.AgentSettingsAcquired(application.getAgentSettings()));
                    MessageUtil.sendMessage(this, MessageUtil.getMessageUrl(application, "Set Settings|MapMediaCASESAgent@0"));
                    checkBox.setChecked(false);
                } else {
                    application.getAgentSettings().setMapMedia(1);
                    MessageUtil.sendMessage(this, MessageUtil.getMessageUrl(application, "Set Settings|MapMediaCASESAgent@1"));
                    EventBus.getDefault().post(new Events.AgentSettingsAcquired(application.getAgentSettings()));
                    checkBox.setChecked(true);
                }
                break;
            }
            case R.id.autotrack: {
                //Timber.d("Toggling autotrack.");
                CheckBox checkBox = (CheckBox) v.findViewById(R.id.autotrack_checkbox);
                if (checkBox.isChecked()) {
                    application.getAgentSettings().setAutoTrack(0);
                    MessageUtil.sendMessage(this, MessageUtil.getMessageUrl(application, "Set Settings|AutoTrackCASESAgent@"));
                    EventBus.getDefault().post(new Events.AgentSettingsAcquired(application.getAgentSettings()));
                    checkBox.setChecked(false);
                } else {
                    application.getAgentSettings().setAutoTrack(1);
                    MessageUtil.sendMessage(this, MessageUtil.getMessageUrl(application, "Set Settings|AutoTrackCASESAgent@1"));
                    EventBus.getDefault().post(new Events.AgentSettingsAcquired(application.getAgentSettings()));
                    checkBox.setChecked(true);
                }
                break;
            }
            case R.id.details: {
                //Timber.d("Toggling details.");
                CheckBox checkBox = (CheckBox) v.findViewById(R.id.details_checkbox);
                if (checkBox.isChecked()) {
                    application.getAgentSettings().setShowDetails(0);
                    MessageUtil.sendMessage(this, MessageUtil.getMessageUrl(application, "Set Settings|ShowDetailsCASESAgent@0"));
                    EventBus.getDefault().post(new Events.AgentSettingsAcquired(application.getAgentSettings()));
                    checkBox.setChecked(false);
                } else {
                    application.getAgentSettings().setShowDetails(1);
                    MessageUtil.sendMessage(this, MessageUtil.getMessageUrl(application, "Set Settings|ShowDetailsCASESAgent@1"));
                    EventBus.getDefault().post(new Events.AgentSettingsAcquired(application.getAgentSettings()));
                    checkBox.setChecked(true);
                }
                break;
            }
            case R.id.create_poi: {
                //Timber.d("Create POI");
                Location location = fixManager.getLastLocation();
                if (location != null) {
                    EventBus.getDefault().post(new Events.CreatePoi(location.getLatitude(), location.getLongitude(), ""));
                }
                break;
            }*/
            case R.id.canned_msg:
                EventBus.getDefault().post(new Events.setCannedText());

        }
        EventBus.getDefault().post(new Events.AgentSettingsAcquired(application.getAgentSettings()));
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode != Activity.RESULT_OK){
            return;
        }
        //if (data == null) return;
        // Getting back from external "Select Picture" activity
        if (requestCode == CheckinFragment.PICK_CHECKIN_ATTACHMENT) {
            // TODO: using delayed post as a temporary solution
            // Find better solution how to pass picked media to Checkin fragment
            Handler handler = new Handler();
            final Events.CheckinAttachmentSelected event = new Events.CheckinAttachmentSelected(data);
            handler.postDelayed(new Runnable() {
                public void run() {
                    EventBus.getDefault().post(event);
                }
            }, 500);
        } else if (requestCode == Givens.SELECT_PROFILE_PHOTO) {
            Uri contentUri = data.getData();
            File outputFile = ProfileService.createTempFile();
            Crop.of(contentUri, Uri.fromFile(outputFile)).asSquare().withMaxSize(Givens.AVATAR_SIZE_PIXELS, Givens.AVATAR_SIZE_PIXELS).start(this);
        } else if (requestCode == Crop.REQUEST_CROP) {
            Uri contentUri = Crop.getOutput(data);
            ProfileService.saveAvatar(this, contentUri, true, true);
        } else if (requestCode == Givens.GALLERY_INTENT || requestCode == Givens.GALLERY_INTENT_KITKAT) {
            UploadPackage uploadPackage = new UploadPackage();
            Location location = fixManager.getLastLocation();
            if (location != null) {
                uploadPackage.setLatitude(location.getLatitude());
                uploadPackage.setLongitude(location.getLongitude());
                uploadPackage.setAccuracy(location.getAccuracy());
            }
            uploadPackage.setCaseNumber(application.getAgentSettings().getCaseNumber());
            uploadPackage.setCaseDescription(application.getAgentSettings().getCaseDescription());
            application.setFromResult(true);
            mediaManager.submitUpload(uploadPackage, data.getData());
        }
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(Events.AgentRolesUpdated agentRolesUpdated) {
        updateRoles(agentRolesUpdated.getAgentRoles());
    }

    @Override
    public void updateRoles(AgentRoles roles) {
        supportInvalidateOptionsMenu();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        int action = event.getAction();
        int keyCode = event.getKeyCode();

        // Reset timed-out sequence
        long now = Calendar.getInstance().getTimeInMillis();
        long period = now - mSosSequenceStart;
        if (period > mSosSequenceMaxTime || mSosSequenceQueue.length() > mSosSequence.length()) {
            mSosSequenceQueue = "";
            mSosSequenceStart = now;
        }

        // Handle volume buttons
        if (action == KeyEvent.ACTION_DOWN) {
            if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
                Timber.d("ddd: AgentActivity->UP");
                mSosSequenceQueue += 'u';
            } else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
                Timber.d("ddd: AgentActivity->DOWN");
                mSosSequenceQueue += 'd';
            }
        }

        // Sequence completed?
        if (mSosSequence.equals(mSosSequenceQueue) && (period < mSosSequenceMaxTime)) {
            mSosSequenceQueue = "";
            // Send covert SOS
            if (application.getAgentSettings().getAgentRoles().panicvolumebuttons()) {
                alertManager.startPanicMode(false);
            }
        }

        return super.dispatchKeyEvent(event);
    }


    /**
     * A placeholder fragment containing a simple view.
     */
    public static class DrawerPlaceholderFragment extends Fragment {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_SECTION_NUMBER = "section_number";

        public DrawerPlaceholderFragment() {
        }

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static DrawerPlaceholderFragment newInstance(int sectionNumber) {
            DrawerPlaceholderFragment fragment = new DrawerPlaceholderFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup viewGroup,
                                 Bundle savedInstanceState) {
            return inflater.inflate(R.layout.fragment_navdrawer, viewGroup, false);
        }

        @Override
        public void onAttach(Activity activity) {
            super.onAttach(activity);
            ((AgentActivity) activity).onSectionAttached(
                    getArguments().getInt(ARG_SECTION_NUMBER));
        }

    }
    public void onEventMainThread(Events.NavigationDrawerClosed event) {

    }

}
