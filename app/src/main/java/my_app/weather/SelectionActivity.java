package my_app.weather;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by Hans on 5/18/2015.
 */
public class SelectionActivity extends AbstractWeatherActivity {

    private ListView weatherResultView;

    protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_select);
        weatherResultView = findView(R.id.selection_list);
        weatherResultView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                intent.putExtra(CURRENT_INDEX_KEY, position);
                intent.putExtra(RESULT_LIST_KEY, getWeatherResults());
                startActivity(intent);
            }
        });
    }

    protected void updateUI() {
        weatherResultView.setAdapter(new SelectionListAdapter(new ArrayList<>(getWeatherResults())));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_select, menu);

        initSearchMenuItem(menu.findItem(R.id.search));

        return true;
    }

    class SelectionListAdapter extends ArrayAdapter<WeatherResult> {
        private final ArrayList<WeatherResult> values;

        SelectionListAdapter(ArrayList<WeatherResult> values) {
            super(SelectionActivity.this, R.layout.select_row, values);
            this.values = values;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LayoutInflater inflater = (LayoutInflater) SelectionActivity.this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View rowView = inflater.inflate(R.layout.select_row, parent, false);
            TextView locationView = (TextView) rowView.findViewById(R.id.desc_location_text);
            TextView tempView = (TextView) rowView.findViewById(R.id.desc_temp_text);
            TextView descView = (TextView) rowView.findViewById(R.id.desc_text);
            WeatherResult value = values.get(position);
            //extra spaces so text shadow isn't truncated
            locationView.setText(value.getLocation("Unknown Location") + " ");
            tempView.setText(value.get(WeatherResult.Key.temp, "__") + "\u00B0 ");
            descView.setText(value.get(WeatherResult.Key.text, ""));
            return rowView;
        }
    }
}
