package com.kalaiyaonline.news;

import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

final class WeatherBoard {
    private final AppCompatActivity activity;
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final LinearLayout root;
    private final TextView place;
    private final TextView temp;
    private final TextView condition;
    private final TextView meta;
    private final LinearLayout days;
    private final LinearLayout results;
    private final EditText search;
    private double lat = 27.033;
    private double lon = 84.983;

    WeatherBoard(AppCompatActivity activity) {
        this.activity = activity;
        root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(8);
        root.setLayoutParams(lp);

        LinearLayout bar = new LinearLayout(activity);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        search = new EditText(activity);
        search.setHint("स्थान खोज्नुहोस्…");
        search.setSingleLine(true);
        search.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        search.setBackground(field());
        search.setPadding(dp(14), dp(10), dp(14), dp(10));
        search.setTypeface(ResourcesCompat.getFont(activity, R.font.mukta_regular));
        search.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                searchPlace();
                return true;
            }
            return false;
        });
        bar.addView(search, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView go = text("खोज", 15, 0xFFFFFFFF, true);
        go.setGravity(Gravity.CENTER);
        go.setBackground(pill(0xFF0F8A3C));
        go.setPadding(dp(16), dp(10), dp(16), dp(10));
        LinearLayout.LayoutParams goLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        goLp.leftMargin = dp(8);
        go.setLayoutParams(goLp);
        go.setOnClickListener(v -> searchPlace());
        bar.addView(go);
        root.addView(bar);

        HorizontalScrollView quickScroll = new HorizontalScrollView(activity);
        quickScroll.setHorizontalScrollBarEnabled(false);
        LinearLayout quick = new LinearLayout(activity);
        quick.setPadding(0, dp(8), 0, dp(4));
        quick.addView(quickChip("कलैया", 27.033, 84.983));
        quick.addView(quickChip("काठमाडौं", 27.717, 85.324));
        quick.addView(quickChip("वीरगञ्ज", 27.017, 84.867));
        quick.addView(quickChip("सिमरा", 27.164, 84.980));
        quickScroll.addView(quick);
        root.addView(quickScroll);

        results = new LinearLayout(activity);
        results.setOrientation(LinearLayout.VERTICAL);
        root.addView(results);

        LinearLayout hero = new LinearLayout(activity);
        hero.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable heroBg = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{0xFF0B5C28, 0xFF14944A});
        heroBg.setCornerRadius(dp(22));
        hero.setBackground(heroBg);
        hero.setPadding(dp(16), dp(16), dp(16), dp(16));
        LinearLayout.LayoutParams heroLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        heroLp.topMargin = dp(8);
        hero.setLayoutParams(heroLp);

        hero.addView(text("मौसम", 13, 0xFFD1FAE5, true));
        place = text("कलैया, बारा", 16, 0xFFFFFFFF, true);
        place.setPadding(0, dp(2), 0, dp(8));
        hero.addView(place);

        LinearLayout now = new LinearLayout(activity);
        now.setOrientation(LinearLayout.HORIZONTAL);
        now.setGravity(Gravity.CENTER_VERTICAL);
        temp = text("—°", 42, 0xFFFFFFFF, true);
        condition = text("लोड हुँदैछ…", 16, 0xFFD1FAE5, true);
        condition.setPadding(dp(12), dp(10), 0, 0);
        now.addView(temp);
        now.addView(condition);
        hero.addView(now);

        meta = text("", 13, 0xFFE7F8EE, false);
        meta.setPadding(0, dp(8), 0, 0);
        hero.addView(meta);
        root.addView(hero);

        TextView forecast = text("७ दिनको पूर्वानुमान", 16, 0xFF0B5C28, true);
        forecast.setPadding(dp(2), dp(14), 0, dp(8));
        root.addView(forecast);

        days = new LinearLayout(activity);
        days.setOrientation(LinearLayout.HORIZONTAL);
        HorizontalScrollView scroll = new HorizontalScrollView(activity);
        scroll.setHorizontalScrollBarEnabled(false);
        scroll.addView(days);
        root.addView(scroll);
    }

    ViewGroup view() {
        return root;
    }

    void load(double latitude, double longitude) {
        loadAt(latitude, longitude, null);
    }

    private void loadAt(double latitude, double longitude, String knownName) {
        lat = latitude;
        lon = longitude;
        activity.runOnUiThread(() -> {
            condition.setText("लोड हुँदैछ…");
            if (knownName != null) place.setText(knownName);
            results.removeAllViews();
        });
        io.execute(() -> {
            String where = knownName != null ? knownName : placeName(latitude, longitude);
            String open = get(openUrl(latitude, longitude));
            if (open != null && applyOpen(open, where)) return;
            String met = get(metUrl(latitude, longitude));
            if (met != null && applyMet(met, where)) return;
            activity.runOnUiThread(() -> condition.setText("मौसम आएन"));
        });
    }

    private void searchPlace() {
        String query = search.getText().toString().trim();
        if (query.length() < 2) return;
        hideKeyboard();
        results.removeAllViews();
        results.addView(note("खोज्दैछ…"));
        io.execute(() -> {
            String body = get("https://geocoding-api.open-meteo.com/v1/search?name=" + enc(query) + "&count=6&language=en&format=json");
            activity.runOnUiThread(() -> showPlaces(body));
        });
    }

    private void showPlaces(String body) {
        results.removeAllViews();
        try {
            JSONArray list = body == null ? null : new JSONObject(body).optJSONArray("results");
            if (list == null || list.length() == 0) {
                results.addView(note("स्थान भेटिएन। अङ्ग्रेजी नाम लेख्नुहोस्।"));
                return;
            }
            for (int i = 0; i < list.length(); i++) {
                JSONObject item = list.getJSONObject(i);
                String name = item.optString("name");
                String region = item.optString("admin1");
                String country = item.optString("country");
                String label = name;
                if (!region.isEmpty()) label += ", " + region;
                else if (!country.isEmpty()) label += ", " + country;
                double latitude = item.getDouble("latitude");
                double longitude = item.getDouble("longitude");
                String title = label;
                TextView row = text(title, 15, 0xFF1C1917, true);
                row.setBackground(field());
                row.setPadding(dp(14), dp(10), dp(14), dp(10));
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                lp.topMargin = dp(6);
                row.setLayoutParams(lp);
                row.setOnClickListener(v -> {
                    search.setText(name);
                    results.removeAllViews();
                    loadAt(latitude, longitude, title);
                });
                results.addView(row);
            }
        } catch (Exception error) {
            results.addView(note("स्थान भेटिएन।"));
        }
    }

    private View quickChip(String label, double latitude, double longitude) {
        TextView view = text(label, 13, 0xFF0B5C28, true);
        view.setBackground(pill(0xFFE5F6EC));
        view.setPadding(dp(12), dp(6), dp(12), dp(6));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.rightMargin = dp(8);
        view.setLayoutParams(lp);
        view.setOnClickListener(v -> {
            search.setText("");
            results.removeAllViews();
            loadAt(latitude, longitude, label);
        });
        return view;
    }

    private void hideKeyboard() {
        InputMethodManager manager = (InputMethodManager) activity.getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
        if (manager != null && search != null) manager.hideSoftInputFromWindow(search.getWindowToken(), 0);
    }

    private boolean applyOpen(String body, String where) {
        try {
            JSONObject json = new JSONObject(body);
            if (json.optBoolean("error")) return false;
            JSONObject current = json.getJSONObject("current");
            int code = current.getInt("weather_code");
            boolean day = current.optInt("is_day", 1) == 1;
            String summary = label(code, day) + "  " + symbol(code);
            String details = "अनुभूति " + round(current.getDouble("apparent_temperature"))
                    + "°   आर्द्रता " + current.getInt("relative_humidity_2m")
                    + "%   हावा " + round(current.getDouble("wind_speed_10m")) + " km/h";
            JSONObject daily = json.getJSONObject("daily");
            JSONArray times = daily.getJSONArray("time");
            JSONArray codes = daily.getJSONArray("weather_code");
            JSONArray highs = daily.getJSONArray("temperature_2m_max");
            JSONArray lows = daily.getJSONArray("temperature_2m_min");
            String[] labels = new String[times.length()];
            String[] values = new String[times.length()];
            for (int i = 0; i < times.length(); i++) {
                labels[i] = weekday(times.getString(i));
                values[i] = symbol(codes.getInt(i)) + "  " + round(highs.getDouble(i)) + "°/" + round(lows.getDouble(i)) + "°";
            }
            show(where, round(current.getDouble("temperature_2m")) + "°", summary, details, labels, values);
            return true;
        } catch (Exception error) {
            return false;
        }
    }

    private boolean applyMet(String body, String where) {
        try {
            JSONArray series = new JSONObject(body).getJSONObject("properties").getJSONArray("timeseries");
            JSONObject first = series.getJSONObject(0).getJSONObject("data");
            JSONObject now = first.getJSONObject("instant").getJSONObject("details");
            String symbol = symbolCode(first);
            String summary = metLabel(symbol) + "  " + metSymbol(symbol);
            String details = "आर्द्रता " + round(now.optDouble("relative_humidity", 0))
                    + "%   हावा " + round(now.optDouble("wind_speed", 0) * 3.6) + " km/h";
            java.util.LinkedHashMap<String, double[]> daysMap = new java.util.LinkedHashMap<>();
            java.util.LinkedHashMap<String, String> icons = new java.util.LinkedHashMap<>();
            for (int i = 0; i < series.length() && daysMap.size() < 7; i++) {
                JSONObject item = series.getJSONObject(i);
                Calendar calendar = calendar(item.getString("time"));
                if (calendar == null) continue;
                String key = calendar.get(Calendar.YEAR) + "-" + calendar.get(Calendar.DAY_OF_YEAR);
                double temperature = item.getJSONObject("data").getJSONObject("instant").getJSONObject("details").optDouble("air_temperature");
                double[] range = daysMap.get(key);
                if (range == null) {
                    range = new double[]{temperature, temperature};
                    daysMap.put(key, range);
                    icons.put(key, symbolCode(item.getJSONObject("data")));
                }
                range[0] = Math.max(range[0], temperature);
                range[1] = Math.min(range[1], temperature);
            }
            String[] labels = new String[daysMap.size()];
            String[] values = new String[daysMap.size()];
            int index = 0;
            for (String key : daysMap.keySet()) {
                double[] range = daysMap.get(key);
                labels[index] = weekdayFromKey(series, key);
                values[index] = metSymbol(icons.get(key)) + "  " + round(range[0]) + "°/" + round(range[1]) + "°";
                index++;
            }
            show(where, round(now.optDouble("air_temperature")) + "°", summary, details, labels, values);
            return true;
        } catch (Exception error) {
            return false;
        }
    }

    private String weekdayFromKey(JSONArray series, String key) throws Exception {
        for (int i = 0; i < series.length(); i++) {
            Calendar calendar = calendar(series.getJSONObject(i).getString("time"));
            if (calendar == null) continue;
            String itemKey = calendar.get(Calendar.YEAR) + "-" + calendar.get(Calendar.DAY_OF_YEAR);
            if (key.equals(itemKey)) return weekday(calendar);
        }
        return "";
    }

    private void show(String where, String temperature, String summary, String details, String[] labels, String[] values) {
        activity.runOnUiThread(() -> {
            place.setText(where);
            temp.setText(temperature);
            condition.setText(summary);
            meta.setText(details);
            days.removeAllViews();
            for (int i = 0; i < labels.length; i++) {
                LinearLayout chip = new LinearLayout(activity);
                chip.setOrientation(LinearLayout.VERTICAL);
                chip.setGravity(Gravity.CENTER);
                chip.setBackground(field());
                chip.setPadding(dp(12), dp(10), dp(12), dp(10));
                chip.setMinimumWidth(dp(76));
                chip.addView(text(labels[i], 13, 0xFF0B5C28, true));
                String[] parts = values[i].split("  ", 2);
                chip.addView(text(parts[0], 18, 0xFF1C1917, false));
                chip.addView(text(parts.length > 1 ? parts[1] : values[i], 12, 0xFF57534E, false));
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                lp.rightMargin = dp(8);
                days.addView(chip, lp);
            }
        });
    }

    private String placeName(double latitude, double longitude) {
        if (Math.abs(latitude - 27.033) < 0.08 && Math.abs(longitude - 84.983) < 0.08) return "कलैया, बारा";
        try {
            String body = get("https://nominatim.openstreetmap.org/reverse?format=json&lat=" + latitude + "&lon=" + longitude);
            JSONObject address = new JSONObject(body).optJSONObject("address");
            if (address == null) return "तपाईंको स्थान";
            String city = first(address, "city", "town", "municipality", "village", "suburb");
            String district = first(address, "county", "state");
            if (city.isEmpty()) return "तपाईंको स्थान";
            return district.isEmpty() ? city : city + ", " + district;
        } catch (Exception error) {
            return "तपाईंको स्थान";
        }
    }

    private String get(String address) {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(address).openConnection();
            connection.setRequestProperty("User-Agent", "KalaiyaOnline/2.5 (arunkly@gmail.com)");
            connection.setConnectTimeout(12000);
            connection.setReadTimeout(15000);
            if (connection.getResponseCode() >= 400) return null;
            InputStream input = connection.getInputStream();
            StringBuilder body = new StringBuilder();
            byte[] buffer = new byte[4096];
            int read;
            while ((read = input.read(buffer)) != -1) body.append(new String(buffer, 0, read, "UTF-8"));
            input.close();
            return body.toString();
        } catch (Exception error) {
            return null;
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    private String openUrl(double latitude, double longitude) {
        return "https://api.open-meteo.com/v1/forecast?latitude=" + latitude
                + "&longitude=" + longitude
                + "&current=temperature_2m,relative_humidity_2m,apparent_temperature,weather_code,is_day,wind_speed_10m"
                + "&daily=weather_code,temperature_2m_max,temperature_2m_min"
                + "&timezone=" + enc("Asia/Kathmandu") + "&forecast_days=7";
    }

    private String metUrl(double latitude, double longitude) {
        return "https://api.met.no/weatherapi/locationforecast/2.0/compact?lat=" + latitude + "&lon=" + longitude;
    }

    private String symbolCode(JSONObject data) {
        JSONObject hour = data.optJSONObject("next_1_hours");
        if (hour == null) hour = data.optJSONObject("next_6_hours");
        if (hour == null) return "";
        JSONObject summary = hour.optJSONObject("summary");
        return summary == null ? "" : summary.optString("symbol_code");
    }

    private Calendar calendar(String time) {
        try {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
            format.setTimeZone(TimeZone.getTimeZone("UTC"));
            Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Kathmandu"));
            calendar.setTime(format.parse(time));
            return calendar;
        } catch (Exception error) {
            return null;
        }
    }

    private String weekday(String date) {
        try {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Kathmandu"));
            calendar.setTime(format.parse(date));
            return weekday(calendar);
        } catch (Exception error) {
            return date;
        }
    }

    private String weekday(Calendar calendar) {
        String[] names = {"", "आइत", "सोम", "मंगल", "बुध", "बिहि", "शुक्र", "शनि"};
        return names[calendar.get(Calendar.DAY_OF_WEEK)];
    }

    private String label(int code, boolean day) {
        if (code == 0) return day ? "घमाइलो" : "सफा रात";
        if (code <= 3) return day ? "आंशिक बादल" : "हल्का बादल";
        if (code == 45 || code == 48) return "हुस्सु";
        if (code <= 67) return "वर्षा";
        if (code <= 77) return "हिउँ";
        if (code <= 82) return "झरी";
        if (code >= 95) return "चट्याङ";
        return "बादल";
    }

    private String symbol(int code) {
        if (code == 0) return "☀";
        if (code <= 3) return "⛅";
        if (code == 45 || code == 48) return "≡";
        if (code >= 95) return "⚡";
        if (code >= 71 && code <= 77) return "❄";
        if (code >= 51) return "🌧";
        return "☁";
    }

    private String metLabel(String code) {
        String value = code == null ? "" : code;
        if (value.contains("thunder")) return "चट्याङ";
        if (value.contains("snow")) return "हिउँ";
        if (value.contains("rain") || value.contains("sleet")) return "वर्षा";
        if (value.contains("fog")) return "हुस्सु";
        if (value.contains("cloud")) return value.contains("partly") || value.contains("fair") ? "आंशिक बादल" : "बादल";
        if (value.contains("clear") || value.contains("fair")) {
            int hour = Calendar.getInstance(TimeZone.getTimeZone("Asia/Kathmandu")).get(Calendar.HOUR_OF_DAY);
            return hour >= 6 && hour < 18 ? "घमाइलो" : "सफा रात";
        }
        return "मौसम";
    }

    private String metSymbol(String code) {
        String value = code == null ? "" : code;
        if (value.contains("thunder")) return "⚡";
        if (value.contains("snow")) return "❄";
        if (value.contains("rain") || value.contains("sleet")) return "🌧";
        if (value.contains("fog")) return "≡";
        if (value.contains("cloud")) return "☁";
        return "☀";
    }

    private String first(JSONObject object, String... keys) {
        for (String key : keys) {
            String value = object.optString(key, "");
            if (!value.isEmpty()) return value;
        }
        return "";
    }

    private int round(double value) {
        return (int) Math.round(value);
    }

    private String enc(String value) {
        try {
            return URLEncoder.encode(value, "UTF-8");
        } catch (Exception error) {
            return value;
        }
    }

    private GradientDrawable field() {
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xFFFFFFFF);
        bg.setCornerRadius(dp(16));
        bg.setStroke(dp(1), 0xFFE5E7EB);
        return bg;
    }

    private GradientDrawable pill(int color) {
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(color);
        bg.setCornerRadius(dp(16));
        return bg;
    }

    private TextView note(String value) {
        TextView view = text(value, 13, 0xFF78716C, false);
        view.setPadding(dp(4), dp(8), dp(4), 0);
        return view;
    }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView view = new TextView(activity);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        if (bold) view.setTypeface(ResourcesCompat.getFont(activity, R.font.mukta_bold));
        return view;
    }

    private int dp(int value) {
        return Math.round(value * activity.getResources().getDisplayMetrics().density);
    }
}
