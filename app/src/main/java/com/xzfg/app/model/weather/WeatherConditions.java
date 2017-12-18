package com.xzfg.app.model.weather;

import java.io.Serializable;

public class WeatherConditions implements Serializable {
    private Response response;
    private CurrentObservation current_observation;

    public Response getResponse() {
        return response;
    }

    public void setResponse(Response response) {
        this.response = response;
    }

    public CurrentObservation getCurrentObservation() {
        return current_observation;
    }

    public void setCurrentObservation(CurrentObservation current_observation) {
        this.current_observation = current_observation;
    }

    public class Response implements Serializable {
        private String version;
        private String termsofService;
        private Features features;

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }

        public String getTermsOfService() {
            return termsofService;
        }

        public void setTermsOfService(String termsofService) {
            this.termsofService = termsofService;
        }

        public Features getFeatures() {
            return features;
        }

        public void setFeatures(Features features) {
            this.features = features;
        }

        public class Features implements Serializable {
            public Integer conditions;
        }
    }

    public class CurrentObservation implements Serializable {
        private Location display_location;
        private Location observation_location;

        private String observation_time_rfc822;
        private String local_time_rfc822;
        private String weather;
        private String temp_f;
        private String temp_c;
        private String relative_humidity;
        private String wind_dir;
        private Double wind_mph;
        private String UV;
        private String icon;

        public Location getDisplayLocation() {
            return display_location;
        }

        public void setDisplayLocation(Location display_location) {
            this.display_location = display_location;
        }

        public Location getObservationLocation() {
            return observation_location;
        }

        public void setObservationLocation(Location observation_location) {
            this.observation_location = observation_location;
        }

        public String getObservationTimeRFC822() {
            return observation_time_rfc822;
        }

        public void setObservationTimeRFC822(String observation_time_rfc822) {
            this.observation_time_rfc822 = observation_time_rfc822;
        }

        public String getLocalTimeRFC822() {
            return local_time_rfc822;
        }

        public void setLocalTimeRFC822(String local_time_rfc822) {
            this.local_time_rfc822 = local_time_rfc822;
        }

        public String getWeather() {
            return weather;
        }

        public void setWeather(String weather) {
            this.weather = weather;
        }

        public String getTempF() {
            return temp_f;
        }

        public void setTempF(String temp_f) {
            this.temp_f = temp_f;
        }

        public String getTempC() {
            return temp_c;
        }

        public void setTempC(String temp_c) {
            this.temp_c = temp_c;
        }

        public String getRelativeHumidity() {
            return relative_humidity;
        }

        public void setRelativeHumidity(String relative_humidity) {
            this.relative_humidity = relative_humidity;
        }

        public String getWindDir() {
            return wind_dir;
        }

        public void setWindDir(String wind_dir) {
            this.wind_dir = wind_dir;
        }

        public Double getWindMPH() {
            return wind_mph;
        }

        public void setWindMPH(Double wind_mph) {
            this.wind_mph = wind_mph;
        }

        public String getUV() {
            return UV;
        }

        public void setUV(String UV) {
            this.UV = UV;
        }

        public String getIcon() {
            return icon;
        }

        public void setIcon(String icon) {
            this.icon = icon;
        }

        public class Location implements Serializable {
            private String city;
            private String state;
            private String country;
            private String latitude;
            private String longitude;
            private String elevation;

            public String getCity() {
                return city;
            }

            public void setCity(String city) {
                this.city = city;
            }

            public String getState() {
                return state;
            }

            public void setState(String state) {
                this.state = state;
            }

            public String getCountry() {
                return country;
            }

            public void setCountry(String country) {
                this.country = country;
            }

            public String getLatitude() {
                return latitude;
            }

            public void setLatitude(String latitude) {
                this.latitude = latitude;
            }

            public String getLongitude() {
                return longitude;
            }

            public void setLongitude(String longitude) {
                this.longitude = longitude;
            }

            public String getElevation() {
                return elevation;
            }

            public void setElevation(String elevation) {
                this.elevation = elevation;
            }
        }
    }

}

