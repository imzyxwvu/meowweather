package cn.meowtec.meowwheater;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;

import java.util.Arrays;
import java.util.Collections;

public class CitySelectionActivity extends AppCompatActivity {

    ListView listCities;
    EditText editCitySearch;
    ArrayAdapter<String> citiesAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_city_selection);
        listCities = findViewById(R.id.list_cities);
        editCitySearch = findViewById(R.id.search_cityName);
        citiesAdapter =
                new ArrayAdapter<String>(
                        this,
                        R.layout.item_city,
                        R.id.textView_cityName,
                        getResources().getStringArray(R.array.city_names));
        editCitySearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                citiesAdapter.getFilter().filter(editCitySearch.getText());
                citiesAdapter.notifyDataSetChanged();
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });
        listCities.setAdapter(citiesAdapter);
        listCities.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Intent intent = new Intent(CitySelectionActivity.this, MainActivity.class);
                intent.putExtra("cityName", citiesAdapter.getItem(i));
                setResult(0, intent);
                finish();
            }
        });
        editCitySearch.requestFocus();
    }
}
