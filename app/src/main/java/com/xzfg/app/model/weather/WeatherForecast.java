package com.xzfg.app.model.weather;

import java.io.Serializable;

public class WeatherForecast implements Serializable {
    private Response response;
    private Forecast forecast;

    public Response getResponse() {
        return response;
    }

    public void setResponse(Response response) {
        this.response = response;
    }

    public Forecast getForecast() {
        return forecast;
    }

    public void setForecast(Forecast forecast) {
        this.forecast = forecast;
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
            private Integer forecast10day;

            public Integer getForecast10Day() {
                return forecast10day;
            }

            public void setForecast10Day(Integer forecast10day) {
                this.forecast10day = forecast10day;
            }
        }
    }

    public class Forecast implements Serializable {
        private TextForecast txt_forecast;
        private SimpleForecast simpleforecast;

        public TextForecast getTxtForecast() {
            return txt_forecast;
        }

        public void setTxtForecast(TextForecast txt_forecast) {
            this.txt_forecast = txt_forecast;
        }

        public SimpleForecast getSimpleForecast() {
            return simpleforecast;
        }

        public void setSimpleForecast(SimpleForecast simpleforecast) {
            this.simpleforecast = simpleforecast;
        }

        public class TextForecast implements Serializable {
            private String date;
            private ForecastDay[] forecastday;

            public String getDate() {
                return date;
            }

            public void setDate(String date) {
                this.date = date;
            }

            public ForecastDay[] getForecastDay() {
                return forecastday;
            }

            public void setForecastDay(ForecastDay[] forecastday) {
                this.forecastday = forecastday;
            }

            public class ForecastDay implements Serializable {
                private Integer period;
                private String icon;
                private String icon_url;
                private String title;
                private String fcttext;
                private String fcttext_metric;
                private String pop;

                public Integer getPeriod() {
                    return period;
                }

                public void setPeriod(Integer period) {
                    this.period = period;
                }

                public String getIcon() {
                    return icon;
                }

                public void setIcon(String icon) {
                    this.icon = icon;
                }

                public String getIconURL() {
                    return icon_url;
                }

                public void setIconURL(String icon_url) {
                    this.icon_url = icon_url;
                }

                public String getTitle() {
                    return title;
                }

                public void setTitle(String title) {
                    this.title = title;
                }

                public String getFctText() {
                    return fcttext;
                }

                public void setFctText(String fcttext) {
                    this.fcttext = fcttext;
                }

                public String getFctTextMetric() {
                    return fcttext_metric;
                }

                public void setFctTextMetric(String fcttext_metric) {
                    this.fcttext_metric = fcttext_metric;
                }

                public String getPop() {
                    return pop;
                }

                public void setPop(String pop) {
                    this.pop = pop;
                }
            }
        }

        public class SimpleForecast implements Serializable {
            private ForecastDay[] forecastday;

            public ForecastDay[] getForecastDay() {
                return forecastday;
            }

            public void setForecastDay(ForecastDay[] forecastday) {
                this.forecastday = forecastday;
            }

            public class ForecastDay implements Serializable {
                private Date date;
                private Integer period;
                private HighLowTemp high;
                private HighLowTemp low;
                private String conditions;
                private String icon;
                private Integer pop;
                private Wind maxwind;
                private Wind avewind;
                private Integer avehumidity;
                private Integer maxhumidity;
                private Integer minhumidity;

                public Date getDate() {
                    return date;
                }

                public void setDate(Date date) {
                    this.date = date;
                }

                public Integer getPeriod() {
                    return period;
                }

                public void setPeriod(Integer period) {
                    this.period = period;
                }

                public HighLowTemp getHigh() {
                    return high;
                }

                public void setHigh(HighLowTemp high) {
                    this.high = high;
                }

                public HighLowTemp getLow() {
                    return low;
                }

                public void setLow(HighLowTemp low) {
                    this.low = low;
                }

                public String getConditions() {
                    return conditions;
                }

                public void setConditions(String conditions) {
                    this.conditions = conditions;
                }

                public String getIcon() {
                    return icon;
                }

                public void setIcon(String icon) {
                    this.icon = icon;
                }

                public Integer getPop() {
                    return pop;
                }

                public void setPop(Integer pop) {
                    this.pop = pop;
                }

                public Wind getMaxWind() {
                    return maxwind;
                }

                public void setMaxWind(Wind maxwind) {
                    this.maxwind = maxwind;
                }

                public Wind getAveWind() {
                    return avewind;
                }

                public void setAveWind(Wind avewind) {
                    this.avewind = avewind;
                }

                public Integer getAveHumidity() {
                    return avehumidity;
                }

                public void setAveHumidity(Integer avehumidity) {
                    this.avehumidity = avehumidity;
                }

                public Integer getMaxHumidity() {
                    return maxhumidity;
                }

                public void setMaxHumidity(Integer maxhumidity) {
                    this.maxhumidity = maxhumidity;
                }

                public Integer getMinHumidity() {
                    return minhumidity;
                }

                public void setMinHumidity(Integer minhumidity) {
                    this.minhumidity = minhumidity;
                }

                public class Date implements Serializable {
                    private String pretty;
                    private Integer day;
                    private Integer month;
                    private Integer year;
                    private String weekday_short;

                    public String getPretty() {
                        return pretty;
                    }

                    public void setPretty(String pretty) {
                        this.pretty = pretty;
                    }

                    public Integer getDay() {
                        return day;
                    }

                    public void setDay(Integer day) {
                        this.day = day;
                    }

                    public Integer getMonth() {
                        return month;
                    }

                    public void setMonth(Integer month) {
                        this.month = month;
                    }

                    public Integer getYear() {
                        return year;
                    }

                    public void setYear(Integer year) {
                        this.year = year;
                    }

                    public String getWeekdayShort() {
                        return weekday_short;
                    }

                    public void setWeekdayShort(String weekday_short) {
                        this.weekday_short = weekday_short;
                    }
                }

                public class HighLowTemp implements Serializable {
                    private Integer fahrenheit;
                    private Integer celsius;

                    public Integer getFahrenheit() {
                        return fahrenheit;
                    }

                    public void setFahrenheit(Integer fahrenheit) {
                        this.fahrenheit = fahrenheit;
                    }

                    public Integer getCelsius() {
                        return celsius;
                    }

                    public void setCelsius(Integer celsius) {
                        this.celsius = celsius;
                    }
                }

                public class Wind implements Serializable {
                    private Integer mph;
                    private Integer kph;
                    private String dir;
                    private Integer degrees;

                    public Integer getMPH() {
                        return mph;
                    }

                    public void setMPH(Integer mph) {
                        this.mph = mph;
                    }

                    public Integer getKPH() {
                        return kph;
                    }

                    public void setKPH(Integer kph) {
                        this.kph = kph;
                    }

                    public String getDir() {
                        return dir;
                    }

                    public void setDir(String dir) {
                        this.dir = dir;
                    }

                    public Integer getDegrees() {
                        return degrees;
                    }

                    public void setDegrees(Integer degrees) {
                        this.degrees = degrees;
                    }
                }
            }
        }
    }

}

