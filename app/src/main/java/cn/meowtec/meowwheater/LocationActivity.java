package cn.meowtec.meowwheater;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

class LocationLoader implements Runnable {

    Handler callback;
    JSONObject result;

    LocationLoader(Handler callback) {
        this.callback = callback;
    }

    @Override
    public void run() {
        try {
            String data = WeatherLoader.loadURLData("http://weather.meowtec.cn/location");
            result = new JSONObject(data).getJSONObject("data");
            Thread.sleep(1000);
            callback.sendEmptyMessage(0);
        } catch (Exception e) {
            callback.sendEmptyMessage(4);
        }
    }

    public String get(String key) {
        try {
            return result.getString(key);
        } catch (JSONException e) {
            return null;
        }
    }
}

public class LocationActivity extends AppCompatActivity {

    LocationLoader loader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location);
        loader = new LocationLoader(new Handler() {
            @Override
            public void handleMessage(Message msg) {
                if(msg.what == 0) {
                    Intent result = new Intent(LocationActivity.this, MainActivity.class);
                    result.putExtra("cityName", loader.get("city"));
                    setResult(msg.what, result);
                }
                finish();
            }
        });
    }

    @Override
    protected void onStart() {
         super.onStart();
         new Thread(loader).start();
    }
}
