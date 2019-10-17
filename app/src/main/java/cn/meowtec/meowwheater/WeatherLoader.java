package cn.meowtec.meowwheater;

import android.content.Context;
import android.content.res.XmlResourceParser;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

class WeatherLoader implements Runnable {

    Handler callback;
    String cityName;
    JSONObject cityInfo, weatherData;
    JSONArray forecast;
    String updateTime;
    Context context;

    public void setCityName(String cityName) {
        this.cityName = cityName;
    }

    public static Map<String, String> loadCityCodeMap(XmlResourceParser xrp) {
        Map<String, String> cityCodeMap = new HashMap<>();
        while(true){
            try {
                if (xrp.getEventType() == XmlResourceParser.END_DOCUMENT)
                    break;
                if (xrp.getEventType() == XmlResourceParser.START_TAG) {
                    if(xrp.getName().equals("city")){
                        String name = null, code = null;
                        for(int i = 0; i < xrp.getAttributeCount(); i++) {
                            if(xrp.getAttributeName(i).equals("name")) {
                                name = xrp.getAttributeValue(i);
                            } else if(xrp.getAttributeName(i).equals("code")) {
                                code = xrp.getAttributeValue(i);
                            }
                        }
                        if(name == null || code == null)
                            throw new RuntimeException("Bad city entry");
                        cityCodeMap.put(name, code);
                    }
                }
                xrp.next();
            } catch (Exception e) {
                throw new RuntimeException(e.getMessage());
            }
        }
        return cityCodeMap;
    }

    public WeatherLoader(Context ctx, Handler cb) {
        context = ctx;
        callback = cb;
        cityName = null;
    }

    public static String loadURLData(String url) throws IOException {
        InputStream is = new URL(url).openStream();
        try {
            int ch;
            byte[] arr = new byte[0x800];
            int i = 0;
            while((ch = is.read()) != -1) {
                arr[i++] = (byte)ch;
            }
            return new String(Arrays.copyOf(arr, i), "UTF-8");
        } finally {
            is.close();
        }
    }

    @Override
    public void run() {
        Map<String, String> cityCodeMap =
                loadCityCodeMap(context.getResources().getXml(R.xml.cities));
        try {
            String code = cityCodeMap.get(cityName);
            String data = loadURLData("http://weather.meowtec.cn/weather/now?location=CN" + code);
            JSONObject cityData = new JSONObject(data).getJSONArray("HeWeather6").getJSONObject(0);
            if(!cityData.getString("status").equals("ok")) {
                callback.sendEmptyMessage(3);
                return;
            }
            cityInfo = cityData.getJSONObject("basic");
            updateTime = cityData.getJSONObject("update").getString("loc");
            weatherData = cityData.getJSONObject("now");
            data = loadURLData("http://weather.meowtec.cn/weather/forecast?location=CN" + code);
            cityData = new JSONObject(data).getJSONArray("HeWeather6").getJSONObject(0);
            forecast = cityData.getJSONArray("daily_forecast");
            callback.sendEmptyMessage(0);
        } catch (Exception e) {
            e.printStackTrace();
            callback.sendEmptyMessage(4);
        }
    }

    public String getUpdateTime() {
        if(updateTime == null)
            return "--";
        return updateTime.split(" ")[1];
    }

    public String get(String key) {
        try {
            if(cityInfo.has(key))
                return cityInfo.getString(key);
            return weatherData.getString(key);
        } catch (JSONException e) {
            return null;
        }
    }

    public JSONArray getForecastData() {
        return forecast;
    }
}