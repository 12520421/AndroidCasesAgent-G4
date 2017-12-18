package com.xzfg.app.fragments.boss;

import com.xzfg.app.model.weather.WeatherConditions;
import com.xzfg.app.model.weather.WeatherForecast;
import com.xzfg.app.model.weather.WeatherGeolookup;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface WeatherService {
    @GET("geolookup/q/{latitude},{longitude}.json")
    Call<WeatherGeolookup> geolookup(@Path("latitude") Double latitude, @Path("longitude") Double longitude);

    @GET("forecast10day{location}.json")
    Call<WeatherForecast> forecast(@Path(value = "location", encoded = true) String location);

    @GET("conditions{location}.json")
    Call<WeatherConditions> conditions(@Path(value = "location", encoded = true) String location);
}
