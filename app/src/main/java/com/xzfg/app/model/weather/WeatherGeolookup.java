package com.xzfg.app.model.weather;

import java.io.Serializable;

public class WeatherGeolookup implements Serializable {
    private Response response;
    private Location location;

    public Response getResponse() {
        return response;
    }

    public void setResponse(Response response) {
        this.response = response;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
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
            private Integer geolookup;

            public Integer getGeoLookup() {
                return geolookup;
            }

            public void setGeoLookup(Integer geolookup) {
                this.geolookup = geolookup;
            }
        }
    }

    public class Location implements Serializable {
        private String type;
        private String country;
        private String state;
        private String city;
        private String zip;
        private String lat;
        private String lon;
        // This is a location URL suffix: "/q/zmw:94107.1.99999"
        private String l;

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getCountry() {
            return country;
        }

        public void setCountry(String country) {
            this.country = country;
        }

        public String getState() {
            return state;
        }

        public void setState(String state) {
            this.state = state;
        }

        public String getCity() {
            return city;
        }

        public void setCity(String city) {
            this.city = city;
        }

        public String getZip() {
            return zip;
        }

        public void setZip(String zip) {
            this.zip = zip;
        }

        public String getLat() {
            return lat;
        }

        public void setLat(String lat) {
            this.lat = lat;
        }

        public String getLon() {
            return lon;
        }

        public void setLon(String lon) {
            this.lon = lon;
        }

        public String getL() {
            return l;
        }

        public void setL(String l) {
            this.l = l;
        }
    }

}

