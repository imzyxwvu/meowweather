package cn.meowtec.meowwheater;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.Image;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.ContactsContract;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

class ForecastAdapter extends ArrayAdapter<JSONObject> {

    int viewResourceId;
    JSONArray data;

    public ForecastAdapter(@NonNull Context context, int resource) {
        super(context, resource);
        viewResourceId = resource;
    }

    @Override
    public int getCount() {
        if(null == data)
            return 0;
        return data.length();
    }

    @Nullable
    @Override
    public JSONObject getItem(int position) {
        if(data == null)
            return null;
        try {
            return data.getJSONObject(position);
        } catch (JSONException e) {
            return null;
        }
    }

    @Override
    public boolean isEnabled(int position) {
        return false;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if(convertView == null)
            convertView = LayoutInflater.from(getContext()).inflate(viewResourceId, null);

        TextView textViewDate = convertView.findViewById(R.id.text_date);
        TextView textViewWeather = convertView.findViewById(R.id.weatherName);
        ImageView imageWeather = convertView.findViewById(R.id.weatherIcon);
        TextView textViewTemp = convertView.findViewById(R.id.temp);
        try {
            JSONObject item = data.getJSONObject(position);
            switch (position) {
                case 0: textViewDate.setText("今天"); break;
                case 1: textViewDate.setText("明天"); break;
                case 2: textViewDate.setText("后天"); break;
                default: textViewDate.setText(item.getString("date"));
            }
            String condDaytime = item.getString("cond_txt_d");
            String condNight = item.getString("cond_txt_n");
            if(condDaytime.equals(condNight))
                textViewWeather.setText(condDaytime);
            else
                textViewWeather.setText(condDaytime + "转" + condNight);
            imageWeather.setImageResource(MainActivity.getImageIdByWeather(condDaytime));
            textViewTemp.setText(String.format("%s℃ - %s℃",
                    item.getString("tmp_min"), item.getString("tmp_max")));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return convertView;
    }

    public void setData(JSONArray data) {
        this.data = data;
        this.notifyDataSetChanged();
    }
}

public class MainActivity extends AppCompatActivity {

    WeatherLoader weatherLoader;
    TextView textViewTemperature, textViewCityName, textViewWeatherName;
    TextView textViewUpdateTime, textViewWet;
    SwipeRefreshLayout swipeRefreshLayout;
    ImageView imageViewWeatherIcon;
    ProgressBar progressBarWet;
    ImageButton btnSelectCity;
    ListView listViewForecast;
    ForecastAdapter forecastAdapter;

    void refresh() {
        swipeRefreshLayout.setRefreshing(true);
        textViewUpdateTime.setText("正在刷新 ...");
        (new Thread(weatherLoader)).start();
    }

    public static int getImageIdByWeather(String weather) {
        if(weather.startsWith("多云")) {
            return R.mipmap.tq_cloudy_day;
        }
        else if(weather.startsWith("阴"))
            return R.mipmap.tq_overcast;
        else if(weather.startsWith("晴"))
            return R.mipmap.tq_sunny_day;
        else if(weather.startsWith("雨夹雪"))
            return R.mipmap.tq_snowyrain;
        else if(weather.startsWith("雷"))
            return R.mipmap.tq_thundershower;
        else if(weather.startsWith("暴雨"))
            return R.mipmap.tq_rainstorm;
        else if(weather.contains("雨"))
            return R.mipmap.tq_rainy;
        else if(weather.contains("雪"))
            return R.mipmap.tq_snowy;
        else if(weather.contains("雾"))
            return R.mipmap.tq_foggy_day;
        else return R.mipmap.launcher_icon;
    }

    public void setWeatherName(String weatherName) {
        textViewWeatherName.setText(weatherName);
        imageViewWeatherIcon.setImageResource(getImageIdByWeather(weatherName));
        if(weatherName.contains("晴") || weatherName.startsWith("多云"))
            swipeRefreshLayout.setBackgroundResource(R.mipmap.bg_sunny);
        else
            swipeRefreshLayout.setBackgroundResource(R.mipmap.bg_darkblue);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);
        textViewTemperature = findViewById(R.id.temp);
        textViewCityName = findViewById(R.id.city);
        textViewWeatherName = findViewById(R.id.weatherName);
        imageViewWeatherIcon = (ImageView) findViewById(R.id.weatherIcon);
        textViewUpdateTime = findViewById(R.id.text_updateTime);
        progressBarWet = (ProgressBar) findViewById(R.id.progressBar_wet);
        textViewWet = findViewById(R.id.textView_wet);
        listViewForecast = (ListView) findViewById(R.id.listView_forecast);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        btnSelectCity = findViewById(R.id.btn_switch_city);
        btnSelectCity.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivityForResult(new Intent(MainActivity.this, CitySelectionActivity.class), 0);
            }
        });
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refresh();
            }
        });
        forecastAdapter = new ForecastAdapter(this, R.layout.item_forecast);
        listViewForecast.setAdapter(forecastAdapter);
        weatherLoader = new WeatherLoader(this, new Handler() {
            @Override
            public void handleMessage(Message msg) {
                if(msg.what != 0) {
                    textViewCityName.setText("--");
                    textViewUpdateTime.setText("更新失败！");
                    return;
                }
                textViewCityName.setText(weatherLoader.get("location"));
                String temp = weatherLoader.get("tmp");
                if(temp.startsWith("暂无")) {
                    textViewTemperature.setText("-- ℃");
                    textViewUpdateTime.setText("暂无数据");
                    return;
                }
                textViewTemperature.setText(temp + "℃");
                setWeatherName(weatherLoader.get("cond_txt"));
                textViewUpdateTime.setText("更新于 " + weatherLoader.getUpdateTime());
                String wet = weatherLoader.get("hum");
                if(wet.matches("^[0-9]+"))
                    progressBarWet.setProgress(Integer.parseInt(wet));
                textViewWet.setText(String.format("湿度 %s° 气压 %s hPa %s", wet,
                        weatherLoader.get("pres"), weatherLoader.get("wind_dir")));
                forecastAdapter.setData(weatherLoader.getForecastData());
                swipeRefreshLayout.setRefreshing(false);
            }
        });

        String cityName = getPreferences(MODE_PRIVATE).getString("cityName", null);
        if(cityName == null) {
            startActivityForResult(new Intent(this, LocationActivity.class), 0);
        } else {
            weatherLoader.setCityName(cityName);
            refresh();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(data == null) {
            String cityName = getPreferences(MODE_PRIVATE).getString("cityName", "北京");
            weatherLoader.setCityName(cityName);
        }
        else if(data.hasExtra("cityName")) {
            String cityName = data.getStringExtra("cityName");
            weatherLoader.setCityName(cityName);
            refresh();
            SharedPreferences.Editor prefEditor = getPreferences(MODE_PRIVATE).edit();
            prefEditor.putString("cityName", cityName);
            prefEditor.apply();
        }
    }
}
