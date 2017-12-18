package com.xzfg.app.modules;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.LocationManager;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;

import com.google.gson.Gson;
import com.xzfg.app.Application;
import com.xzfg.app.activities.AgentActivity;
import com.xzfg.app.activities.ConfigActivity;
import com.xzfg.app.activities.EulaActivity;
import com.xzfg.app.activities.MainActivity;
import com.xzfg.app.activities.UrlActivity;
import com.xzfg.app.fragments.ATCommandsFragment;
import com.xzfg.app.fragments.AppPrefsFragment;
import com.xzfg.app.fragments.CheckinFragment;
import com.xzfg.app.fragments.ConfigurationFragment;
import com.xzfg.app.fragments.ContactsFragment;
import com.xzfg.app.fragments.G4ChatFragment;
import com.xzfg.app.fragments.MarkAlertFragment;
import com.xzfg.app.fragments.NavigationDrawerFragment;
import com.xzfg.app.fragments.PagerFragment;
import com.xzfg.app.fragments.PanicTimerFragment;
import com.xzfg.app.fragments.StatusFragment;
import com.xzfg.app.fragments.WebMapFragment;
import com.xzfg.app.fragments.alerts.AlertDetailsFragment;
import com.xzfg.app.fragments.alerts.AlertHeaderListFragment;
import com.xzfg.app.fragments.alerts.AlertRecordListFragment;
import com.xzfg.app.fragments.boss.BossModeFragment;
import com.xzfg.app.fragments.boss.DefaultFragment;
import com.xzfg.app.fragments.boss.PinPadFragment;
import com.xzfg.app.fragments.boss.WeatherFragment;
import com.xzfg.app.fragments.chat.ChatConversationFragment;
import com.xzfg.app.fragments.chat.ChatListFragment;
import com.xzfg.app.fragments.collect.CollectFragment;
import com.xzfg.app.fragments.dialogs.ConfirmSosDialog;
import com.xzfg.app.fragments.dialogs.ConfirmStorageDialog;
import com.xzfg.app.fragments.dialogs.ConfirmWipeDialog;
import com.xzfg.app.fragments.dialogs.UserOfflineDialog;
import com.xzfg.app.fragments.home.CancelPanicListDialogFragment;
import com.xzfg.app.fragments.home.HomeFragment;
import com.xzfg.app.fragments.media.AudioPlaybackFragment;
import com.xzfg.app.fragments.media.ImageViewerFragment;
import com.xzfg.app.fragments.media.MediaHeaderListFragment;
import com.xzfg.app.fragments.media.MediaRecordListFragment;
import com.xzfg.app.fragments.media.VideoPlaybackFragment;
import com.xzfg.app.fragments.poi.CreatePoiFragment;
import com.xzfg.app.fragments.poi.PoiHeaderListFragment;
import com.xzfg.app.fragments.poi.PoiRecordListFragment;
import com.xzfg.app.fragments.setup.CreateAccountFragment;
import com.xzfg.app.fragments.setup.ForgotPasswordChangePasswordFragment;
import com.xzfg.app.fragments.setup.ForgotPasswordSendCodeFragment;
import com.xzfg.app.fragments.setup.ForgotPasswordVerifyCodeFragment;
import com.xzfg.app.fragments.setup.InviteFragment;
import com.xzfg.app.fragments.setup.KdcStartSetupFragment;
import com.xzfg.app.fragments.setup.LoginFragment;
import com.xzfg.app.fragments.setup.PaymentFragment;
import com.xzfg.app.fragments.setup.ScanFragment;
import com.xzfg.app.fragments.setup.SignupFragment;
import com.xzfg.app.fragments.setup.StartSetupFragment;
import com.xzfg.app.fragments.setup.resetup.ReScanFragment;
import com.xzfg.app.managers.AlertManager;
import com.xzfg.app.managers.ChatManager;
import com.xzfg.app.managers.CollectedMediaManager;
import com.xzfg.app.managers.FixManager;
import com.xzfg.app.managers.FreeSpaceManager;
import com.xzfg.app.managers.G4Manager;
import com.xzfg.app.managers.MediaManager;
import com.xzfg.app.managers.OrientationManager;
import com.xzfg.app.managers.PhoneLogManager;
import com.xzfg.app.managers.PingManager;
import com.xzfg.app.managers.PoiManager;
import com.xzfg.app.managers.SessionManager;
import com.xzfg.app.managers.VolumeManager;
import com.xzfg.app.receivers.BluetoothReceiver;
import com.xzfg.app.receivers.PanicAlarmReceiver;
import com.xzfg.app.receivers.PhoneReceiver;
import com.xzfg.app.receivers.VolumeChangeObserver;
import com.xzfg.app.receivers.VolumeSOSReceiver;
import com.xzfg.app.services.AudioRecordingService;
import com.xzfg.app.services.AudioStreamingService;
import com.xzfg.app.services.ChatService;
import com.xzfg.app.services.CreateAccountService;
import com.xzfg.app.services.CreatePoiService;
import com.xzfg.app.services.GattClient;
import com.xzfg.app.services.InviteService;
import com.xzfg.app.services.LoginService;
import com.xzfg.app.services.MainService;
import com.xzfg.app.services.MessageService;
import com.xzfg.app.services.MetaDataService;
import com.xzfg.app.services.PhoneLogDeliveryTimerService;
import com.xzfg.app.services.PhoneLogService;
import com.xzfg.app.services.PhoneService;
import com.xzfg.app.services.PhotoService;
import com.xzfg.app.services.ProfileService;
import com.xzfg.app.services.RegistrationService;
import com.xzfg.app.services.RetryRegistrationService;
import com.xzfg.app.services.SMSDeliveryTimerService;
import com.xzfg.app.services.SMSReceiverService;
import com.xzfg.app.services.SMSSentService;
import com.xzfg.app.services.SMSService;
import com.xzfg.app.services.SetSetupFieldService;
import com.xzfg.app.services.UrlService;
import com.xzfg.app.services.VideoRecordingService;
import com.xzfg.app.services.VideoStreamingService;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;


@Module(
        includes = {
                CryptoModule.class,
                NetworkModule.class
        },
        injects = {
                Application.class,
                // activities
                AgentActivity.class,
                MainActivity.class,
                ConfigActivity.class,
                EulaActivity.class,
                UrlActivity.class,
                // services
                MainService.class,
                RegistrationService.class,
                MessageService.class,
                ChatService.class,
                VideoRecordingService.class,
                AudioRecordingService.class,
                VideoStreamingService.class,
                AudioStreamingService.class,
                PhotoService.class,
                CreatePoiService.class,
                RetryRegistrationService.class,
                PhoneService.class,
                SMSReceiverService.class,
                SMSSentService.class,
                PhoneLogService.class,
                PhoneLogDeliveryTimerService.class,
                SMSDeliveryTimerService.class,
                // fragments
                StartSetupFragment.class,
                ScanFragment.class,
                PagerFragment.class,
                NavigationDrawerFragment.class,
                ChatListFragment.class,
                ChatConversationFragment.class,
                AppPrefsFragment.class,
                CollectFragment.class,
                WebMapFragment.class,
                ReScanFragment.class,
                MediaHeaderListFragment.class,
                MediaRecordListFragment.class,
                ImageViewerFragment.class,
                AlertHeaderListFragment.class,
                AlertRecordListFragment.class,
                PoiHeaderListFragment.class,
                PoiRecordListFragment.class,
                CreatePoiFragment.class,
                AudioPlaybackFragment.class,
                UserOfflineDialog.class,
                VideoPlaybackFragment.class,
                LoginFragment.class,
                CreateAccountFragment.class,
                BossModeFragment.class,
                WeatherFragment.class,
                DefaultFragment.class,
                PinPadFragment.class,
                ATCommandsFragment.class,
                StatusFragment.class,
                MarkAlertFragment.class,
                ConfigurationFragment.class,

                // misc
                AlertManager.class,
                PoiManager.class,
                ChatManager.class,
                PingManager.class,
                MediaManager.class,
                FreeSpaceManager.class,
                FixManager.class,
                VolumeManager.class,
                SessionManager.class,
                ConfirmStorageDialog.class,
                CollectedMediaManager.class,
                UrlService.class,
                CreateAccountService.class,
                LoginService.class,
                PhoneReceiver.class,
                PhoneLogManager.class,
                InviteFragment.class,
                HomeFragment.class,
                CheckinFragment.class,
                PanicTimerFragment.class,
                ContactsFragment.class,
                SMSService.class,
                ProfileService.class,
                InviteService.class,
                PanicAlarmReceiver.class,
                BluetoothReceiver.class,
                VolumeSOSReceiver.class,
                AlertDetailsFragment.class,
                SignupFragment.class,
                PaymentFragment.class,
                KdcStartSetupFragment.class,
                ForgotPasswordSendCodeFragment.class,
                ForgotPasswordVerifyCodeFragment.class,
                ForgotPasswordChangePasswordFragment.class,
                VolumeChangeObserver.class,
                OrientationManager.class,
                ConfirmSosDialog.class,
                CancelPanicListDialogFragment.class,
                SetSetupFieldService.class,
                G4Manager.class,
                MetaDataService.class,
                G4ChatFragment.class,
                ConfirmWipeDialog.class,

        }
)

public final class AppModule {
    private final Application app;

    public AppModule(Application app) {
        this.app = app;

    }


    @Provides
    @Singleton
    @SuppressWarnings("unused")
    Application provideApplication() {
        return app;
    }


    @Provides
    @Singleton
    @SuppressWarnings("unused")
    Gson provideGson() {
        return new Gson();
    }

    @Provides
    @Singleton
    @SuppressWarnings("unused")
    ConnectivityManager provideConnectivityManager() {
        return (ConnectivityManager) app.getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    @Provides
    @Singleton
    @SuppressWarnings("unused")
    LocationManager provideLocationManager() {
        return (LocationManager) app.getSystemService(Context.LOCATION_SERVICE);
    }


    @Provides
    @Singleton
    @SuppressWarnings("unused")
    OrientationManager provideOrientationManager() {
        return new OrientationManager(app);
    }

    @Provides
    @Singleton
    @SuppressWarnings("unused")
    PowerManager providePowerManager() {
        return (PowerManager) app.getSystemService(Context.POWER_SERVICE);
    }

    @Provides
    @Singleton
    @SuppressWarnings("unused")
    AudioManager provideAudioManager() {
        return (AudioManager) app.getSystemService(Context.AUDIO_SERVICE);
    }

    @Provides
    @Singleton
    @SuppressWarnings("unused")
    InputMethodManager provideInputMethodManager() {
        return (InputMethodManager) app.getSystemService(Context.INPUT_METHOD_SERVICE);
    }

    @Provides
    @Singleton
    @SuppressWarnings("unused")
    WindowManager provideWindowManager() {
        return (WindowManager) app.getSystemService(Context.WINDOW_SERVICE);
    }

    @Provides
    @Singleton
    @SuppressWarnings("unused")
    SharedPreferences provideSharedPreferences() {
        return PreferenceManager.getDefaultSharedPreferences(app);
    }

    @Provides
    @Singleton
    @SuppressWarnings("unused")
    ChatManager provideChatManager() {
        return new ChatManager(app);
    }

    @Provides
    @Singleton
    @SuppressWarnings("unused")
    PingManager providePingManager() {
        return new PingManager(app);
    }

    @Provides
    @Singleton
    @SuppressWarnings("unused")
    MediaManager provideMediaManager() {
        return new MediaManager(app);
    }

    @Provides
    @Singleton
    @SuppressWarnings("unused")
    FreeSpaceManager provideFreeSpaceManager() {
        return new FreeSpaceManager(app);
    }

    @Provides
    @Singleton
    @SuppressWarnings("unused")
    FixManager provideFixManager() {
        return new FixManager(app);
    }

    @Provides
    @Singleton
    @SuppressWarnings("unused")
    PhoneLogManager providePhoneLogManager() {
        return new PhoneLogManager(app);
    }

    @Provides
    @Singleton
    @SuppressWarnings("unused")
    VolumeManager provideVolumeManager() {
        return new VolumeManager(app);
    }

    @Provides
    @Singleton
    @SuppressWarnings("unused")
    SessionManager provideSessionManager() {
        return new SessionManager(app);
    }

    @Provides
    @Singleton
    @SuppressWarnings("unused")
    PoiManager providePoiManager() {
        return new PoiManager(app);
    }

    @Provides
    @Singleton
    @SuppressWarnings("unused")
    AlertManager provideAlertManager() {
        return new AlertManager(app);
    }

    @Provides
    @Singleton
    @SuppressWarnings("unused")
    CollectedMediaManager provideCollectedMediaManager() {
        return new CollectedMediaManager(app);
    }

   @Provides
    @Singleton
    @SuppressWarnings("unused")
    G4Manager provideG4Manager() {
       return new G4Manager(app);
   }



}
