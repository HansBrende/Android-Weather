package my_app.weather;

import android.util.Log;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.EnumMap;

import javax.xml.parsers.ParserConfigurationException;

import my_app.weather.util.Util;

/**
 * Created by Hans on 5/16/2015.
 */
public final class WeatherResult extends DefaultHandler implements Serializable {

    public static final int FORECAST_DAY = 0;
    public static final int FORECAST_DATE = 1;
    public static final int FORECAST_LOW = 2;
    public static final int FORECAST_HIGH = 3;
    public static final int FORECAST_TEXT = 4;
    public static final int FORECAST_CODE = 5;

    public enum Key {
        city, region, country,
        text, code, temp, date,
        chill, direction, speed,
        humidity, pressure, rising, visibility,
        sunrise, sunset
    }

    public enum Tag {
        location(Key.city, Key.region, Key.country),
        condition(Key.text, Key.code, Key.temp, Key.date),
        wind(Key.chill, Key.direction, Key.speed),
        atmosphere(Key.humidity, Key.pressure, Key.rising, Key.visibility),
        astronomy(Key.sunrise, Key.sunset);

        final Key[] keys;
        Tag(Key... keys) {
            this.keys = keys;
        }
    }
    private static final Key[] noKeys = new Key[0];
    private static Key[] keys(String tag) {
        try {
            return Tag.valueOf(tag).keys;
        } catch (RuntimeException e) {
            return noKeys;
        }
    }

    public final long woeid;
    public final ArrayList<String[]> forecasts = new ArrayList<>();
    private final EnumMap<Key, String> values = new EnumMap<>(Key.class);

    public WeatherResult(long woeid) {
        this.woeid = woeid;
    }

    public String get(Key key, String def) {
        String s = values.get(key);
        return Util.isBlank(s) ? def : s;
    }

    public String get(String format, Key key, String def) {
        String s = values.get(key);
        return Util.isBlank(s) ? def : String.format(format, s);
    }

    public String put(Key key, String value) {
        return values.put(key, value);
    }

    public String getLocation(String def) {
        String city = get(Key.city, null);
        String region = get(Key.region, null);
        String country = get(Key.country, null);
        if (city == null) {
            if (region == null)
                return country == null ? def : country;
            return country == null ? region : (region + ", " + country);
        }
        if (region == null)
            return country == null ? city : (city + ", " + country);
        return city + ", " + region;
    }

    public boolean updateQuietly() {
        try {
            update();
            return true;
        } catch (Exception e) {
            Log.e(Util.TAG, e.getMessage());
            return false;
        }
    }

    public void update() throws IOException, ParserConfigurationException, SAXException {
        if (woeid < 0)
            return;
        InputStream stream = Util.forecastURL(woeid, true).openStream();
        //try with resources requires API level 19 :(
        try {
            Util.parseXml(stream, this);
        } finally {
            stream.close();
        }
    }

    @Override
    public void startDocument() throws SAXException {
        forecasts.clear();
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
        if ("forecast".equals(localName)) {
            forecasts.add(new String[]{
                    atts.getValue("day"), atts.getValue("date"), atts.getValue("low"),
                    atts.getValue("high"), atts.getValue("text"), atts.getValue("code")
            });
        } else {
            for (Key key : keys(localName)) {
                put(key, atts.getValue(key.name()));
            }
        }
    }

    public static WeatherResult of(String locationDescription) {
        try {
            return new WeatherResult(Util.calculateWoeid(locationDescription));
        } catch (IOException e) {
            Log.e("", e.getMessage());
            return null;
        }
    }

    public static WeatherResult of(double latitude, double longitude) {
        try {
            return new WeatherResult(Util.calculateWoeid(latitude, longitude));
        } catch (IOException e) {
            Log.e("", e.getMessage());
            return null;
        }
    }

}
