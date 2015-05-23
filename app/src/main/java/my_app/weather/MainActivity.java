package my_app.weather;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;

public class MainActivity extends AbstractWeatherActivity {

    private TextView locationView;
    private TextView tempView;
    private TextView status;
    private TextView descriptionView;
    private ListView forecastView;
    private TextView bonusText0;
    private TextView bonusText1;
    private TextView bonusText2;
    private TextView bonusText3;
    private int currentResultIndex;

    protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_main);
        //find UI views
        locationView = findView(R.id.location_text);
        tempView  = findView(R.id.temperature_text);
        status = findView(R.id.status);
        descriptionView = findView(R.id.description_text);
        forecastView = findView(R.id.forecast_list);
        forecastView.addFooterView(getLayoutInflater().inflate(R.layout.main_list_footer, null));
        bonusText0 = findView(R.id.bonus_text_0);
        bonusText1 = findView(R.id.bonus_text_1);
        bonusText2 = findView(R.id.bonus_text_2);
        bonusText3 = findView(R.id.bonus_text_3);
        //find index of current weather result
        currentResultIndex = getIntent().getIntExtra(CURRENT_INDEX_KEY, state == null ? 0 : state.getInt(CURRENT_INDEX_KEY, 0));
    }

    protected void updateUI() {
        WeatherResult r = getWeatherResults().get(currentResultIndex);
        //pad with spaces so text shadow isn't truncated
        locationView.setText(" " + r.getLocation("Unknown Location") + " ");
        descriptionView.setText(" " + r.get(WeatherResult.Key.text, "") + " ");
        tempView.setText(" " + r.get(WeatherResult.Key.temp, "__") + "\u00B0F ");
        status.setText(currentResultIndex == 0 ? r.get("As of %s", WeatherResult.Key.date, "") : "Delete");
        status.setTextColor(currentResultIndex == 0 ? Color.BLACK : Color.RED);
        forecastView.setAdapter(new MainListAdapter(new ArrayList<>(r.forecasts)));
        bonusText0.setText(r.get("Feels like %s\u00B0", WeatherResult.Key.chill, ""));
        bonusText1.setText(r.get("Humidity: %s%%", WeatherResult.Key.humidity, ""));
        bonusText2.setText(r.get("Pressure: %s in", WeatherResult.Key.pressure, ""));
        bonusText3.setText(r.get("Wind Speed: %s mph", WeatherResult.Key.speed, ""));
    }

    public void deleteItem(View textView) {
        if (currentResultIndex > 0) {
            getWeatherResults().remove(currentResultIndex);
            currentResultIndex = 0;
            goToSelectionActivity();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        bundle.putInt(CURRENT_INDEX_KEY, currentResultIndex);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);

        initSearchMenuItem(menu.findItem(R.id.main_search));

        return true;
    }

    protected void onWeatherResultAddedFromSearch(int position) {
        currentResultIndex = position;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            goToSelectionActivity();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void goToSelectionActivity() {
        Intent intent = new Intent(getApplicationContext(), SelectionActivity.class);
        intent.putExtra(RESULT_LIST_KEY, getWeatherResults());
        // sending data to new activity
        startActivity(intent);
    }

    class MainListAdapter extends ArrayAdapter<String[]> {
        private final ArrayList<String[]> values;

        MainListAdapter(ArrayList<String[]> values) {
            super(MainActivity.this, R.layout.main_row, values);
            this.values = values;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LayoutInflater inflater = (LayoutInflater) MainActivity.this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View rowView = inflater.inflate(R.layout.main_row, parent, false);
            TextView dayView = (TextView) rowView.findViewById(R.id.day_text);
            TextView dayDescView = (TextView) rowView.findViewById(R.id.day_desc_text);
            TextView highView = (TextView) rowView.findViewById(R.id.high_text);
            TextView lowView = (TextView) rowView.findViewById(R.id.low_text);
            String[] value = values.get(position);
            dayView.setText(value[WeatherResult.FORECAST_DAY]);
            dayDescView.setText(value[WeatherResult.FORECAST_TEXT]);
            highView.setText(value[WeatherResult.FORECAST_HIGH]);
            lowView.setText(value[WeatherResult.FORECAST_LOW]);
            return rowView;
        }
    }

}
