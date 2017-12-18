package com.xzfg.app.model.weather;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

public class WeatherData implements Serializable {
    private WeatherConditions conditions;
    private WeatherForecast forecast;
    private boolean useCelsius = false;

    public WeatherData(WeatherConditions conditions, WeatherForecast forecast, boolean useCelsius) {
        this.conditions = conditions;
        this.forecast = forecast;
        this.useCelsius = useCelsius;
    }

    public WeatherData(WeatherData data) {
        this.conditions = data.conditions;
        this.forecast = data.forecast;
        this.useCelsius = data.useCelsius;
    }

    public WeatherConditions getConditions() {
        return conditions;
    }

    public WeatherForecast getForecast() {
        return forecast;
    }

    public boolean getUseCelsius() {
        return useCelsius;
    }

    public void setUseCelsius(boolean value) {
        this.useCelsius = value;
    }

    @Override
    public boolean equals(Object o) {
        //if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        WeatherData that = (WeatherData) o;
        if (conditions != that.conditions) return false;
        if (forecast != that.forecast) return false;
        if (useCelsius != that.useCelsius) return false;

        return true;
    }

    public WeatherData getCopy() {
        return (WeatherData) deepCopy(this);
    }

    // Returns a deep copy of an object
    static private Object deepCopy(Object oldObj) {
        ObjectOutputStream oos = null;
        ObjectInputStream ois = null;
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            oos = new ObjectOutputStream(bos);
            // serialize and pass the object
            oos.writeObject(oldObj);
            oos.flush();
            ByteArrayInputStream bin = new ByteArrayInputStream(bos.toByteArray());
            ois = new ObjectInputStream(bin);
            // return the new object
            return ois.readObject();
        } catch (Exception e) {
            //throw(e);
        } finally {
            try {
                oos.close();
                ois.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return null;
    }
}
