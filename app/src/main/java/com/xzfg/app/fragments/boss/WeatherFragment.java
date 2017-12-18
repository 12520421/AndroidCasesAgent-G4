package com.xzfg.app.fragments.boss;


import android.content.Context;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import com.crashlytics.android.Crashlytics;
import com.facebook.stetho.okhttp3.StethoInterceptor;
import com.xzfg.app.BuildConfig;
import com.xzfg.app.Events;
import com.xzfg.app.Givens;
import com.xzfg.app.R;
import com.xzfg.app.fragments.dialogs.AllowLiveTrackingDialogFragment;
import com.xzfg.app.model.weather.WeatherConditions;
import com.xzfg.app.model.weather.WeatherData;
import com.xzfg.app.model.weather.WeatherForecast;
import com.xzfg.app.model.weather.WeatherGeolookup;
import com.xzfg.app.widgets.SlidingTabLayout;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

import de.greenrobot.event.EventBus;
import okhttp3.OkHttpClient;
import okhttp3.ResponseBody;
import okhttp3.logging.HttpLoggingInterceptor;
import okhttp3.logging.HttpLoggingInterceptor.Level;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import timber.log.Timber;


public class WeatherFragment extends BossModeFragment {
  private static final char DEGREE_CHAR = (char) 0x00B0;
  private static final String WEATHER_API_URL = "https://api.wunderground.com/api/15eb46176a46df30/";
  private WeatherService api;
  AllowLiveTrackingDialogFragment trackingDialog;
  ViewPager viewPager;
  Animation rotationAnimation;
  TextView weatherCity;
  TextView weatherTemperature;
  TextView weatherConditions;
  TextView weatherTemperatureRange;
  TextView weatherHumidity;
  TextView weatherWind;
  ImageView refreshButton;
  SlidingTabLayout slidingTabLayout;

  public WeatherFragment() {
  }

  @Override
  public int getType() {
    return Givens.BOSSMODE_TYPE_WEATHER;
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    // use the injected okHttpClient instance by default.
    OkHttpClient weatherClient = okHttpClient;

    // unless SSL PINNING is enabled, then we need to route around that.  We could used qualifiers
    // and set this up via dagger, but since is the only place it's used, that's probably overkill.
    if (BuildConfig.SSL_PINNING) {
      if (BuildConfig.DEBUG) {
        Timber.w("CONSTRUCTING HTTP CLIENT FOR WEATHER");
      }

      OkHttpClient.Builder builder = new OkHttpClient.Builder();
      builder.connectTimeout(30, TimeUnit.SECONDS);
      builder.readTimeout(60, TimeUnit.SECONDS);
      builder.writeTimeout(30, TimeUnit.SECONDS);

      if (BuildConfig.DEBUG) {
        builder.addNetworkInterceptor(new StethoInterceptor());
        HttpLoggingInterceptor httpLoggingInterceptor = new HttpLoggingInterceptor(new HttpLoggingInterceptor.Logger() {
          @Override
          public void log(String message) {
            Timber.tag("OkHttp-Weather").d(message);
          }
        });
        httpLoggingInterceptor.setLevel(Level.HEADERS);
        builder.addInterceptor(httpLoggingInterceptor);
      }

      weatherClient = builder.build();
    }

    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl(WEATHER_API_URL)
        .client(weatherClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build();

    api = retrofit.create(WeatherService.class);

    if (BuildConfig.DEBUG) {
      Timber.w("WEATHER API INSTANCE CREATED");
    }
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    if (BuildConfig.DEBUG) {
      Timber.w("CREATING WEATHER VIEW");
    }
    View view = inflater.inflate(R.layout.fragment_boss_mode_weather, container, false);
    weatherCity = (TextView)view.findViewById(R.id.city);
    weatherTemperature = (TextView)view.findViewById(R.id.temperature);
    weatherConditions = (TextView)view.findViewById(R.id.conditions);
    weatherTemperatureRange = (TextView)view.findViewById(R.id.temperature_range);
    weatherHumidity = (TextView)view.findViewById(R.id.humidity);
    weatherWind = (TextView)view.findViewById(R.id.wind);
    refreshButton = (ImageView)view.findViewById(R.id.refresh_button);
    viewPager = (ViewPager) view.findViewById(R.id.viewpager);
    viewPager.setAdapter(new WeatherPagerAdapter());
    slidingTabLayout = (SlidingTabLayout) view.findViewById(R.id.sliding_tabs);
    slidingTabLayout.setCustomTabView(R.layout.tabview_weather, R.id.tabview_text);
    slidingTabLayout.setDistributeEvenly(true);
    slidingTabLayout.setViewPager(viewPager);
    rotationAnimation = AnimationUtils.loadAnimation(getContext(), R.anim.rotate_clockwise);
    rotationAnimation.setRepeatCount(Animation.INFINITE);

    view.findViewById(R.id.temperature_system_icon).setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        Context ctx = getActivity();
        //Creating the instance of PopupMenu
        PopupMenu popup = new PopupMenu(ctx, view);
        //Inflating the Popup using xml file
        popup.getMenuInflater().inflate(R.menu.temperature_system, popup.getMenu());
        popup.getMenu().getItem(application.getWeatherData().getUseCelsius() ? 1 : 0).setCheckable(true).setChecked(true);
        // Register popup with OnMenuItemClickListener
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
          public boolean onMenuItemClick(MenuItem item) {
            boolean useCelsius = (item.getItemId() == R.id.temperature_c);
            // Update and save WeatherData
            WeatherData wd = application.getWeatherData();
            wd.setUseCelsius(useCelsius);
            application.setWeatherData(wd);
            // Update UI
            updateWeatherUI(wd);
            return true;
          }
        });

        // Show popup menu
        popup.show();

      }
    });

    refreshButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        // Enable/disable live tracking manually
        if (!fixManager.isTracking()) {
          if (settings.getAllowTracking() == 0) {
            if (trackingDialog != null) {
              trackingDialog.dismissAllowingStateLoss();
              trackingDialog = null;
            }
            trackingDialog = AllowLiveTrackingDialogFragment
                .newInstance(getString(R.string.allow_live_tracking_), getString(R.string.allow_live_tracking_description));
            trackingDialog.show(getFragmentManager(), "allow-tracking");
          } else {
            EventBus.getDefault().post(new Events.StartLiveTracking());
          }
        } else {
          EventBus.getDefault().post(new Events.StopLiveTracking());
        }
      }
    });

    // Debug button to exit boss mode
    /*
    if (BuildConfig.DEBUG) {
      Button bossModeButton = (Button) view.findViewById(R.id.exit_bossmode_button);
      bossModeButton.setVisibility(View.VISIBLE);
      bossModeButton.findViewById(R.id.exit_bossmode_button)
          .setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
              // Exit Boss Mode
              EventBus.getDefault().post(new Events.BossModeStatus(Givens.ACTION_BOSSMODE_DISABLE));
              //Toast.makeText(application.getBaseContext(), "Boss Mode Cancelled!", Toast.LENGTH_SHORT).show();
            }
          });
    }*/

    if (BuildConfig.DEBUG) {
      Timber.w("WEATHER VIEW CREATED");
    }

    return view;
  }

  @Override
  public void onViewCreated(View view, Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    if (BuildConfig.DEBUG) {
      Timber.w("UI CREATED - LOOKING UP WEATHER DATA.");
    }
    // Refresh button
    setRefreshButtonIcon(fixManager.isTracking());

    // Update weather from prefs data
    updateWeatherUI(application.getWeatherData());

    // Retrieve current location and request fresh weather conditions and forecast
    getWeatherGeolookup(fixManager.getLastLocation());

  }

  @Override
  public void onStop() {
    if (BuildConfig.DEBUG) {
      Timber.w("STOPPING WEATHER FRAGMENT");
    }
    if (trackingDialog != null) {
      trackingDialog.dismissAllowingStateLoss();
      trackingDialog = null;
    }
    super.onStop();
  }

  @Override
  public void onDestroy() {
    if (BuildConfig.DEBUG) {
      Timber.w("DESTROYING WEATHER FRAGMENT");
    }

    if (trackingDialog != null) {
      trackingDialog.dismissAllowingStateLoss();
      trackingDialog = null;
    }
    super.onDestroy();
  }

  public void onEventMainThread(Events.WeatherUpdated event) {
    if (BuildConfig.DEBUG) {
      Timber.w("WEATHER UPDATED EVEN RECEIVED");
    }
    updateWeatherUI(event.getWeatherData());
  }

  public void onEventMainThread(Events.TrackingStatus event) {
    if (BuildConfig.DEBUG) {
      Timber.w("TRACKING STATUS CHANGED, UPDATING REFRESH ICON");
    }
    setRefreshButtonIcon(event.isTracking());
  }

  // Retrieve request URL suffix by lat/lon coordinates
  private void getWeatherGeolookup(Location location) {
    if (location != null) {
      // Call Weather API
      Call<WeatherGeolookup> call = api.geolookup(location.getLatitude(), location.getLongitude());
      if (BuildConfig.DEBUG) {
        Timber.w("ENQUEUING CALL TO geolookup");
      }
      call.enqueue(new Callback<WeatherGeolookup>() {
        @Override
        public void onResponse(Call<WeatherGeolookup> call, Response<WeatherGeolookup> response) {
          if (response.isSuccessful()) {
            String urlSuffix = response.body().getLocation().getL();
            // Retrieve current weather conditions
            getWeatherConditions(urlSuffix);
          } else {
            if (BuildConfig.DEBUG) {
              Crashlytics.setInt("Response Code", response.code());
              Crashlytics.setString("Response Message", response.message());
              ResponseBody errorBody = response.errorBody();
              if (errorBody != null) {
                Crashlytics.setString("Response Body", errorBody.toString());
                try {
                  errorBody.close();
                }
                catch (Exception e) {
                  Timber.w(e,"Couldn't close error body.");
                }
              }
            }
            Timber.e(new Exception("WeatherGeoLookupFailed"), "Couldn't get weather geo lookup");
          }
        }

        @Override
        public void onFailure(Call<WeatherGeolookup> call, Throwable t) {
          Timber.e(t,"Weather Geo Lookup Failure");
        }
      });
    }
  }

  // Retrieve current weather conditions
  private void getWeatherConditions(final String location) {
    // Call Weather API
    Call<WeatherConditions> call = api.conditions(location);
    if (BuildConfig.DEBUG) {
      Timber.w("ENQUEUING CALL TO weather conditions");
    }
    call.enqueue(new Callback<WeatherConditions>() {
      @Override
      public void onResponse(Call<WeatherConditions> call, Response<WeatherConditions> response) {
        if (response.isSuccessful()) {
          // Retrieve weather forecast
          getWeatherForecast(location, response.body());
        } else {
          if (BuildConfig.DEBUG) {
            Crashlytics.setInt("Response Code", response.code());
            Crashlytics.setString("Response Message", response.message());
            ResponseBody errorBody = response.errorBody();
            if (errorBody != null) {
              Crashlytics.setString("Response Body", errorBody.toString());
              try {
                errorBody.close();
              } catch (Exception e) {
                Timber.w(e, "Couldn't close error body.");
              }
            }
          }
          Timber.e(new Exception("WeatherConditionsLookupFailed"), "Couldn't get weather conditions lookup");
        }
      }

      @Override
      public void onFailure(Call<WeatherConditions> call, Throwable t) {
        Timber.e(t,"Weather Conditions Lookup Failure.");
      }
    });
  }

  // Retrieve 10 day weather forecast
  private void getWeatherForecast(String location, final WeatherConditions conditions) {
    // Call Weather API
    Call<WeatherForecast> call = api.forecast(location);
    if (BuildConfig.DEBUG) {
      Timber.w("ENQUEUING CALL TO weather forecast");
    }
    call.enqueue(new Callback<WeatherForecast>() {
      @Override
      public void onResponse(Call<WeatherForecast> call, Response<WeatherForecast> response) {
        if (response.isSuccessful()) {
          WeatherData oldData = application.getWeatherData();
          application.setWeatherData(new WeatherData(conditions, response.body(),
              oldData == null ? false : oldData.getUseCelsius()));
        } else {
          if (BuildConfig.DEBUG) {
            Crashlytics.setInt("Response Code", response.code());
            Crashlytics.setString("Response Message", response.message());
            ResponseBody errorBody = response.errorBody();
            if (errorBody != null) {
              Crashlytics.setString("Response Body", errorBody.toString());
              try {
                errorBody.close();
              }
              catch (Exception e) {
                Timber.w(e,"Couldn't close error body.");
              }
            }
          }
          Timber.e(new Exception("WeatherForecastFailed"), "Couldn't get weather forecast");
          // error response, no access to resource?
        }

      }

      @Override
      public void onFailure(Call<WeatherForecast> call, Throwable t) {
        // something went wrong (like no internet connection)
      }
    });
  }

  // Update weather UI
  private void updateWeatherUI(WeatherData data) {
    if (data != null) {
      if (BuildConfig.DEBUG) {
        Timber.w("UPDATING WEATHER UI");
      }

      WeatherConditions wc = data.getConditions();
      WeatherForecast wf = data.getForecast();

      // Update current conditions
      WeatherConditions.CurrentObservation co = wc.getCurrentObservation();
      weatherCity.setText(co.getDisplayLocation().getCity());
      String temperature = data.getUseCelsius() ? co.getTempC() : co.getTempF();
      temperature = temperature.substring(0, temperature.indexOf('.')) + DEGREE_CHAR;
      weatherTemperature.setText(temperature);
      String tempRange;
      if (data.getUseCelsius()) {
        tempRange = wf.getForecast().getSimpleForecast().getForecastDay()[0].getHigh().getCelsius().toString() + DEGREE_CHAR + " / ";
        tempRange += wf.getForecast().getSimpleForecast().getForecastDay()[0].getLow().getCelsius().toString() + DEGREE_CHAR;
      } else {
        tempRange = wf.getForecast().getSimpleForecast().getForecastDay()[0].getHigh().getFahrenheit().toString() + DEGREE_CHAR + " / ";
        tempRange += wf.getForecast().getSimpleForecast().getForecastDay()[0].getLow().getFahrenheit().toString() + DEGREE_CHAR;
      }
      weatherTemperatureRange.setText(tempRange);
      weatherConditions.setText(co.getWeather());
      weatherHumidity.setText(application.getString(R.string.humidity) + " " + co.getRelativeHumidity());
      String wind = application.getString(
          R.string.wind,
          (co.getWindDir().length() < 3 ? co.getWindDir() : co.getWindDir().charAt(0)),
          (int) Math.round(co.getWindMPH())
      );
      weatherWind.setText(wind);
    }
    viewPager.getAdapter().notifyDataSetChanged();
  }

  private void setRefreshButtonIcon(boolean trackingOn) {
    if (trackingOn) {
      refreshButton.setImageResource(R.drawable.refresh_gold);
    } else {
      refreshButton.setImageResource(R.drawable.refresh);
    }
  }

  //
  public static int getIconByLabel(String label) {
    int result = R.drawable.ic_weather_clear;
    String l = label.toLowerCase();

    // Day
    switch (l) {
      case "chanceflurries":
        result = R.drawable.ic_weather_chanceflurries;
        break;
      case "chancerain":
        result = R.drawable.ic_weather_chancerain;
        break;
      case "chanceslit":
        result = R.drawable.ic_weather_chancesleet;
        break;
      case "chancesnow":
        result = R.drawable.ic_weather_chancesnow;
        break;
      case "chancetstorms":
        result = R.drawable.ic_weather_chancetstorms;
        break;
      case "clear":
        result = R.drawable.ic_weather_clear;
        break;
      case "cloudy":
        result = R.drawable.ic_weather_cloudy;
        break;
      case "flurries":
        result = R.drawable.ic_weather_flurries;
        break;
      case "fog":
        result = R.drawable.ic_weather_fog;
        break;
      case "hazy":
        result = R.drawable.ic_weather_hazy;
        break;
      case "mostlycloudy":
        result = R.drawable.ic_weather_mostlycloudy;
        break;
      case "mostlysunny":
        result = R.drawable.ic_weather_mostlysunny;
        break;
      case "partlycloudy":
        result = R.drawable.ic_weather_partlycloudy;
        break;
      case "partlysunny":
        result = R.drawable.ic_weather_partlysunny;
        break;
      case "slit":
        result = R.drawable.ic_weather_sleet;
        break;
      case "rain":
        result = R.drawable.ic_weather_rain;
        break;
      case "snow":
        result = R.drawable.ic_weather_snow;
        break;
      case "sunny":
        result = R.drawable.ic_weather_sunny;
        break;
      case "tstorms":
        result = R.drawable.ic_weather_tstorms;
        break;
    }

    // Night
    switch (l) {
      case "nt_chanceflurries":
        result = R.drawable.ic_weather_nt_chanceflurries;
        break;
      case "nt_chancerain":
        result = R.drawable.ic_weather_nt_chancerain;
        break;
      case "nt_chanceslit":
        result = R.drawable.ic_weather_nt_chancesleet;
        break;
      case "nt_chancesnow":
        result = R.drawable.ic_weather_nt_chancesnow;
        break;
      case "nt_chancetstorms":
        result = R.drawable.ic_weather_nt_chancetstorms;
        break;
      case "nt_clear":
        result = R.drawable.ic_weather_nt_clear;
        break;
      case "nt_cloudy":
        result = R.drawable.ic_weather_nt_cloudy;
        break;
      case "nt_flurries":
        result = R.drawable.ic_weather_nt_flurries;
        break;
      case "nt_fog":
        result = R.drawable.ic_weather_nt_fog;
        break;
      case "nt_hazy":
        result = R.drawable.ic_weather_nt_hazy;
        break;
      case "nt_mostlycloudy":
        result = R.drawable.ic_weather_nt_mostlycloudy;
        break;
      case "nt_mostlysunny":
        result = R.drawable.ic_weather_nt_mostlysunny;
        break;
      case "nt_partlycloudy":
        result = R.drawable.ic_weather_nt_partlycloudy;
        break;
      case "nt_partlysunny":
        result = R.drawable.ic_weather_nt_partlysunny;
        break;
      case "nt_slit":
        result = R.drawable.ic_weather_nt_sleet;
        break;
      case "nt_rain":
        result = R.drawable.ic_weather_nt_rain;
        break;
      case "nt_snow":
        result = R.drawable.ic_weather_nt_snow;
        break;
      case "nt_sunny":
        result = R.drawable.ic_weather_nt_sunny;
        break;
      case "nt_tstorms":
        result = R.drawable.ic_weather_nt_tstorms;
        break;
    }

    return result;
  }

  class WeatherPagerAdapter extends PagerAdapter {
    // Boss Mode exit sequence
    final private String mExitSequence = "12321";
    final private long mExitSequenceMaxTime = 5000;
    private long mExitSequenceStart = 0;
    private String mExitSequenceQueue = "";

    @Override
    public int getCount() {
      return 2;
    }

    @Override
    public boolean isViewFromObject(View view, Object o) {
      return o == view;
    }

    @Override
    public CharSequence getPageTitle(int position) {
      return (position == 0) ? application.getString(R.string.today) : application.getString(R.string.four_days);
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
      // Inflate a new layout from our resources
      int layout = (position == 0) ? R.layout.tab_weather_today : R.layout.tab_weather_4days;
      View view = getActivity().getLayoutInflater().inflate(layout, container, false);
      // Add the newly created View to the ViewPager
      container.addView(view);

      WeatherData data = application.getWeatherData();
      if (data != null) {
        if (position == 0) {
          updateWeatherToday(view, data);
        } else {
          updateWeather4Days(view, data);
        }
      }

      return view;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
      container.removeView((View) object);
    }

    // Update weather-today UI
    private void updateWeatherToday(View v, WeatherData data) {
      ImageView dayIcon = (ImageView) v.findViewById(R.id.day_icon);
      ImageView nightIcon = (ImageView) v.findViewById(R.id.night_icon);
      TextView dayText = (TextView) v.findViewById(R.id.day_text);
      TextView nightText = (TextView) v.findViewById(R.id.night_text);

      if (data != null && data.getForecast() != null) {
        WeatherForecast wf = data.getForecast();

        dayIcon.setImageResource(getIconByLabel(wf.getForecast().getTxtForecast().getForecastDay()[0].getIcon()));
        dayText.setText(wf.getForecast().getTxtForecast().getForecastDay()[0].getFctText());
        nightIcon.setImageResource(getIconByLabel(wf.getForecast().getTxtForecast().getForecastDay()[1].getIcon()));
        nightText.setText(wf.getForecast().getTxtForecast().getForecastDay()[1].getFctText());
      }
    }

    // Update weather-4days UI
    private void updateWeather4Days(View v, WeatherData wd) {
      TextView day1Label = (TextView) v.findViewById(R.id.day1_label);
      TextView day2Label = (TextView) v.findViewById(R.id.day2_label);
      TextView day3Label = (TextView) v.findViewById(R.id.day3_label);
      TextView day4Label = (TextView) v.findViewById(R.id.day4_label);
      ImageView day1Icon = (ImageView) v.findViewById(R.id.day1_icon);
      ImageView day2Icon = (ImageView) v.findViewById(R.id.day2_icon);
      ImageView day3Icon = (ImageView) v.findViewById(R.id.day3_icon);
      ImageView day4Icon = (ImageView) v.findViewById(R.id.day4_icon);
      TextView day1Temp = (TextView) v.findViewById(R.id.day1_temp);
      TextView day2Temp = (TextView) v.findViewById(R.id.day2_temp);
      TextView day3Temp = (TextView) v.findViewById(R.id.day3_temp);
      TextView day4Temp = (TextView) v.findViewById(R.id.day4_temp);

      // Exit Boss Mode handlers
      day1Icon.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View view) {
          // Reset timed-out exit sequence
          if (Calendar.getInstance().getTimeInMillis() - mExitSequenceStart > mExitSequenceMaxTime) {
            mExitSequenceQueue = "";
          }
          mExitSequenceQueue += '1';
          if (mExitSequenceQueue.length() == 1) {
            mExitSequenceStart = Calendar.getInstance().getTimeInMillis();
          } else {
            long time = (Calendar.getInstance().getTimeInMillis() - mExitSequenceStart);
            if (mExitSequence.equals(mExitSequenceQueue) && (time <= mExitSequenceMaxTime)) {
              // Exit Boss Mode
              if (BuildConfig.DEBUG) {
                Timber.w("EXITING BOSS MODE");
              }
              EventBus.getDefault().post(new Events.BossModeStatus(Givens.ACTION_BOSSMODE_DISABLE));
              //Toast.makeText(application.getBaseContext(), "Exit sequence passed: " + time, Toast.LENGTH_SHORT).show();
            }
            // Reset exit sequence
            mExitSequenceQueue = "";
          }
        }
      });

      day2Icon.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View view) {
          mExitSequenceQueue += '2';
        }
      });

      day3Icon.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View view) {
          mExitSequenceQueue += '3';
        }
      });

      if (wd != null) {
        set4DayItem(wd, 1, day1Label, day1Icon, day1Temp);
        set4DayItem(wd, 2, day2Label, day2Icon, day2Temp);
        set4DayItem(wd, 3, day3Label, day3Icon, day3Temp);
        set4DayItem(wd, 4, day4Label, day4Icon, day4Temp);
      }
    }

    private void set4DayItem(WeatherData wd, int dayIndex, TextView day, ImageView icon, TextView temperature) {
      WeatherForecast.Forecast.SimpleForecast.ForecastDay wf = wd.getForecast().getForecast().getSimpleForecast().getForecastDay()[dayIndex];
      String tempRange;

      day.setText(wf.getDate().getWeekdayShort());
      icon.setImageResource(getIconByLabel(wf.getIcon()));

      if (wd.getUseCelsius()) {
        tempRange = wf.getHigh().getCelsius().toString() + DEGREE_CHAR + " / ";
        tempRange += wf.getLow().getCelsius().toString() + DEGREE_CHAR;
      } else {
        tempRange = wf.getHigh().getFahrenheit().toString() + DEGREE_CHAR + " / ";
        tempRange += wf.getLow().getFahrenheit().toString() + DEGREE_CHAR;
      }
      temperature.setText(tempRange);
    }

  }


}
