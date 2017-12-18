package com.xzfg.app.fragments.boss;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.CallSuper;
import android.support.v4.app.Fragment;
import android.view.View;

import com.xzfg.app.Application;
import com.xzfg.app.BuildConfig;
import com.xzfg.app.Events;
import com.xzfg.app.Givens;
import com.xzfg.app.R;
import com.xzfg.app.managers.AlertManager;
import com.xzfg.app.managers.FixManager;
import com.xzfg.app.managers.MediaManager;
import com.xzfg.app.model.AgentSettings;

import javax.inject.Inject;

import de.greenrobot.event.EventBus;
import okhttp3.OkHttpClient;
import timber.log.Timber;

public abstract class BossModeFragment extends Fragment {
  protected final static boolean isJdar = BuildConfig.FLAVOR.equals(Givens.FLAVOR_JDAR);

  @Inject
  Application application;

  @Inject
  FixManager fixManager;

  @Inject
  AlertManager alertManager;
  @Inject
  MediaManager mediaManager;
  @Inject
  OkHttpClient okHttpClient;

  AgentSettings settings;

  public abstract int getType();

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    if (BuildConfig.DEBUG) {
      switch(getType()) {
        case Givens.BOSSMODE_TYPE_PINPAD:
          Timber.w("CREATING MODE FRAGMENT - PINPAD");
          break;
        case Givens.BOSSMODE_TYPE_SLOT:
          Timber.w("CREATING MODE FRAGMENT - SLOT");
          break;
        case Givens.BOSSMODE_TYPE_WEATHER:
          Timber.w("CREATING MODE FRAGMENT - WEATHER");
          break;
      }
    }
  }

  @Override
  public void onAttach(Activity activity) {
    super.onAttach(activity);
    ((Application) getActivity().getApplication()).inject(this);
    settings = application.getAgentSettings();
  }

  @Override
  public void onStart() {
    if (BuildConfig.DEBUG) {
      Timber.w("FRAGMENT STARTING");
    }
    super.onStart();
    if (!EventBus.getDefault().isRegistered(this)) {
      EventBus.getDefault().register(this);
    }
  }

  @Override
  public void onStop() {
    if (BuildConfig.DEBUG) {
      Timber.w("FRAGMENT STOPPING");
    }
    super.onStop();
    if (EventBus.getDefault().isRegistered(this)) {
      EventBus.getDefault().unregister(this);
    }
  }

  @CallSuper
  @Override
  public void onViewCreated(View view, Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    EventBus.getDefault().post(new Events.DisplayChanged("Boss Mode", R.id.boss));
  }

  // update our agent settings.
  public void onEventMainThread(Events.AgentSettingsAcquired agentSettingsAcquired) {
    if (agentSettingsAcquired.getAgentSettings() != null) {
      settings = agentSettingsAcquired.getAgentSettings();
    }
  }

  public static BossModeFragment newInstance(AgentSettings settings) {
    if (BuildConfig.DEBUG) {
      Timber.w("GETTING NEW BOSSMODE FRAGMENT INSTANCE");
    }
    if (settings != null) {
      switch(settings.getBossModeStyle()) {
        case Givens.BOSSMODE_TYPE_WEATHER:
          return new WeatherFragment();
        case Givens.BOSSMODE_TYPE_PINPAD:
          return new PinPadFragment();
        case Givens.BOSSMODE_TYPE_SLOT:
        default:
          return new DefaultFragment();
      }
    }
    return null;
  }
}
