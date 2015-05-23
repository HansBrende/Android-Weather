package my_app.weather;

import android.app.AlertDialog;
import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.SearchView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collection;

import my_app.weather.util.PeriodicUpdatee;
import my_app.weather.util.PeriodicUpdater;
import my_app.weather.util.Util;

/**
 * Created by Hans on 5/18/2015.
 */
public abstract class AbstractWeatherActivity extends AppCompatActivity implements PeriodicUpdatee, LocationListener {
    public static final String PROVIDER = LocationManager.NETWORK_PROVIDER;
    public static final long MIN_TIME_FOR_LOCATION_UPDATE = 1000 * 60 * 20; //20 minutes
    public static final float MIN_DISTANCE_FOR_LOCATION_UPDATE = 1000 * 20; //20 kilometers
    public static final long WEATHER_UPDATE_INTERVAL = 1000 * 60 * 5; // 5 minutes
    public static final String WOEIDS_KEY = "woeids";
    public static final String CURRENT_INDEX_KEY = "currentIndex";
    public static final String RESULT_LIST_KEY = "resultList";

    private SharedPreferences preferences;
    private ArrayList<WeatherResult> results;
    private LocationManager locationUpdater;
    private PeriodicUpdater weatherUpdater;
    private BroadcastReceiver networkConnectionUpdater;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //open saved data
        preferences = getSharedPreferences("my_app.weather", 0);
        //get location manager
        locationUpdater = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        //create update looper
        weatherUpdater = new PeriodicUpdater(this);
        networkConnectionUpdater = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                periodicUpdate();
            }
        };
        //get results from calling activity
        results = (ArrayList<WeatherResult>)getIntent().getSerializableExtra(RESULT_LIST_KEY);
        //get results from previous instance of this activity
        if (results == null && savedInstanceState != null)
            results = (ArrayList<WeatherResult>)savedInstanceState.getSerializable(RESULT_LIST_KEY);
    }

    protected <E extends View> E findView(int id) {
        return (E)findViewById(id);
    }

    public void onStart() {
        super.onStart();
        if (results == null) {//must retrieve results from storage
            results = fromPersistentInfoString(preferences.getString(WOEIDS_KEY, ""));
            weatherUpdater.run(); //must update right away
        } else { //use the results we found in onCreate
            weatherUpdater.run(WEATHER_UPDATE_INTERVAL / 2); //may not need to update right away
        }
        registerReceiver(networkConnectionUpdater, new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE"));
        try {
            locationUpdater.requestLocationUpdates(PROVIDER, MIN_TIME_FOR_LOCATION_UPDATE, MIN_DISTANCE_FOR_LOCATION_UPDATE, this);
        } catch (IllegalArgumentException e) {
            //network provider doesn't exist
        }
        updateUI();
    }

    public void onStop() {
        super.onStop();
        unregisterReceiver(networkConnectionUpdater);
        locationUpdater.removeUpdates(this);
        weatherUpdater.stop();
    }

    protected abstract void updateUI();

    protected ArrayList<WeatherResult> getWeatherResults() {
        return results;
    }

    public long periodicUpdate() {
        if (isNetworkAvailable()) {
            new AsyncTask<Void, Void, WeatherResult>() { //first: update location
                protected WeatherResult doInBackground(Void... params) {
                    Location loc = getLastKnownLocation();
                    if (loc != null)
                        return WeatherResult.of(loc.getLatitude(), loc.getLongitude());
                    return null;
                }
                protected void onPostExecute(final WeatherResult data) {
                    if (data != null && data.woeid >= 0 && data.woeid != results.get(0).woeid)
                        results.set(0, data); //location updated.
                    new AsyncTask<WeatherResult, Void, Void>() { //second: update forecasts
                        protected Void doInBackground(WeatherResult... params) {
                            for (WeatherResult r : params)
                                r.updateQuietly();
                            return null;
                        }
                        protected void onPostExecute(Void param) {
                            updateUI(); //forecasts updated.
                        }
                    }.execute(results.toArray(new WeatherResult[results.size()]));
                }
            }.execute();
        }
        return WEATHER_UPDATE_INTERVAL;
    }

    public void onLocationChanged(Location loc) {
        periodicUpdate();
    }

    public void onStatusChanged(String provider, int status, Bundle extras) {
        periodicUpdate();
    }

    public void onProviderEnabled(String provider) {
        periodicUpdate();
    }

    public void onProviderDisabled(String provider) {
        periodicUpdate();
    }

    protected Location getLastKnownLocation() {
        try {
            return locationUpdater.getLastKnownLocation(PROVIDER);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        exportWeatherResults();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(RESULT_LIST_KEY, results);
    }

    protected void exportWeatherResults() {
        SharedPreferences.Editor ed = preferences.edit();
        ed.putString(WOEIDS_KEY, toPersistentInfoString(results));
        ed.commit();
    }

    protected boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    protected void onWeatherResultAddedFromSearch(int position) {
    }

    private String toPersistentInfoString(Collection<WeatherResult> results) {
        try {
            JSONArray array = new JSONArray();
            for (WeatherResult w : results) {
                JSONObject obj = new JSONObject();
                obj.put("woeid", w.woeid);
                for (WeatherResult.Key key : WeatherResult.Tag.location.keys)
                    obj.put(key.name(), w.get(key, ""));
                array.put(obj);
            }
            return array.toString();
        } catch (JSONException e) {
            return "";
        }
    }

    private ArrayList<WeatherResult> fromPersistentInfoString(String s) {
        ArrayList<WeatherResult> results = new ArrayList<>();
        if (!Util.isBlank(s)) {
            try {
                JSONArray array = new JSONArray(s);
                for (int i = 0, len = array.length(); i < len; i++) {
                    JSONObject o = array.optJSONObject(i);
                    if (o != null) {
                        WeatherResult w = new WeatherResult(o.optLong("woeid", -1L));
                        for (WeatherResult.Key key : WeatherResult.Tag.location.keys)
                            w.put(key, o.optString(key.name(), ""));
                        results.add(w);
                    }
                }
            } catch (JSONException e) {
            }
        }
        if (results.isEmpty())
            results.add(new WeatherResult(-1L));
        return results;
    }

    protected void initSearchMenuItem(final MenuItem searchItem) {
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        final SearchView searchView = (SearchView) searchItem.getActionView();
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        searchView.setIconifiedByDefault(false);

        MenuItemCompat.setOnActionExpandListener(searchItem, new MenuItemCompat.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                searchView.requestFocus();
                final InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.toggleSoftInput(InputMethodManager.SHOW_IMPLICIT, 0);
                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(searchView.getWindowToken(), 0);
                return true;
            }
        });

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                new AsyncTask<String, Void, WeatherResult>() {
                    protected WeatherResult doInBackground(String... params) {
                        WeatherResult r = WeatherResult.of(params[0]);
                        if (r != null)
                            r.updateQuietly();
                        return r;
                    }

                    @Override
                    protected void onPostExecute(WeatherResult weatherResult) {
                        if (weatherResult == null) {
                            new AlertDialog.Builder(AbstractWeatherActivity.this)
                                    .setTitle("No Network Connection")
                                    .setIcon(android.R.drawable.ic_dialog_alert)
                                    .show();
                        } else if (weatherResult.woeid < 0) {
                            new AlertDialog.Builder(AbstractWeatherActivity.this)
                                    .setTitle("No Results Found")
                                    .setIcon(android.R.drawable.ic_dialog_alert)
                                    .show();
                        } else {
                            results.add(weatherResult);
                            onWeatherResultAddedFromSearch(results.size() - 1);
                            updateUI();
                        }
                    }
                }.execute(query);
                searchView.setQuery("", false);
                searchView.clearFocus();
                searchItem.collapseActionView();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });
    }
}