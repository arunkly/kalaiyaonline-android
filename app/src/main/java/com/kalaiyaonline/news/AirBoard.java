package com.kalaiyaonline.news;

import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;

import org.json.JSONObject;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

final class AirBoard {
    private static final String[][] PLACES = {
            {"कलैया", "27.033", "84.983"},
            {"वीरगन्ज", "27.016", "84.877"},
            {"काठमाडौं", "27.717", "85.324"},
            {"पोखरा", "28.210", "83.986"},
            {"विराटनगर", "26.453", "87.272"},
            {"नेपालगन्ज", "28.055", "81.615"},
            {"धनगढी", "28.685", "80.622"},
            {"जनकपुर", "26.729", "85.926"},
            {"हेटौंडा", "27.428", "85.032"},
            {"बुटवल", "27.701", "83.448"},
            {"धरान", "26.812", "87.284"},
            {"भरतपुर", "27.683", "84.433"}
    };

    private final AppCompatActivity activity;
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final LinearLayout root;
    private final LinearLayout summary;
    private final LinearLayout cities;
    private final TextView status;

    AirBoard(AppCompatActivity activity) {
        this.activity = activity;
        root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        TextView intro = text("नेपालका प्रमुख स्थानको AQI, PM2.5 र PM10।", 14, 0xFF57534E, false);
        intro.setPadding(0, 0, 0, dp(8));
        root.addView(intro);
        status = text("डेटा लोड हुँदैछ…", 14, 0xFF0B5C28, true);
        root.addView(status);
        summary = row();
        root.addView(summary);
        cities = new LinearLayout(activity);
        cities.setOrientation(LinearLayout.VERTICAL);
        root.addView(cities);
        TextView source = text("स्रोत: Open-Meteo Air Quality API। यो मोडेलको अनुमान हो, सरकारी स्टेशनको प्रत्यक्ष मापनभन्दा फरक हुन सक्छ।", 12, 0xFF78716C, false);
        source.setPadding(0, dp(8), 0, dp(4));
        root.addView(source);
        TextView link = text("नेपाल सरकारको Air Quality पोर्टल ›", 14, 0xFF0F8A3C, true);
        link.setOnClickListener(v -> activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://pollution.gov.np/"))));
        root.addView(link);
        root.setOnClickListener(v -> load());
    }

    ViewGroup view() {
        return root;
    }

    void load() {
        activity.runOnUiThread(() -> status.setText("डेटा लोड हुँदैछ…"));
        io.execute(() -> {
            List<Reading> list = new ArrayList<>();
            for (String[] place : PLACES) list.add(fetch(place[0], place[1], place[2]));
            activity.runOnUiThread(() -> render(list));
        });
    }

    private void render(List<Reading> list) {
        int count = 0;
        int total = 0;
        Reading low = null;
        Reading high = null;
        for (Reading reading : list) {
            if (!reading.ok) continue;
            count++;
            total += reading.aqi;
            if (low == null || reading.aqi < low.aqi) low = reading;
            if (high == null || reading.aqi > high.aqi) high = reading;
        }
        status.setText(count == 0 ? "डेटा आएन। थिचेर पुनः लोड गर्नुहोस्।" : "पुनः लोड");
        summary.removeAllViews();
        summary.addView(stat("औसत AQI", count == 0 ? "—" : String.valueOf(Math.round(total / (float) count))));
        summary.addView(stat("कम AQI", low == null ? "—" : low.name + " " + low.aqi));
        summary.addView(stat("उच्च AQI", high == null ? "—" : high.name + " " + high.aqi));
        summary.addView(stat("स्थान", count + "/" + list.size()));
        cities.removeAllViews();
        for (Reading reading : list) cities.addView(card(reading));
    }

    private LinearLayout card(Reading reading) {
        LinearLayout card = new LinearLayout(activity);
        card.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(16));
        bg.setColor(0xFFFFFFFF);
        int tone = reading.ok ? tone(reading.aqi) : 0xFFD6D3D1;
        bg.setStroke(dp("कलैया".equals(reading.name) ? 2 : 1), tone);
        card.setBackground(bg);
        card.setPadding(dp(14), dp(12), dp(14), dp(12));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(8);
        card.setLayoutParams(lp);

        LinearLayout top = row();
        top.setGravity(Gravity.CENTER_VERTICAL);
        TextView name = text(reading.name, 16, 0xFF1C1917, true);
        top.addView(name, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView aqi = text(reading.ok ? String.valueOf(reading.aqi) : "—", 28, tone, true);
        top.addView(aqi);
        card.addView(top);
        card.addView(text(reading.ok ? label(reading.aqi) : "डेटा उपलब्ध छैन", 13, tone, true));
        if (reading.ok) {
            card.addView(text("PM2.5 " + reading.pm25 + "   PM10 " + reading.pm10 + "   NO₂ " + reading.no2 + "   O₃ " + reading.o3, 13, 0xFF57534E, false));
        }
        return card;
    }

    private LinearLayout stat(String label, String value) {
        LinearLayout box = new LinearLayout(activity);
        box.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xFFF3FBF6);
        bg.setCornerRadius(dp(14));
        box.setBackground(bg);
        box.setPadding(dp(10), dp(8), dp(10), dp(8));
        box.addView(text(label, 11, 0xFF78716C, false));
        box.addView(text(value, 14, 0xFF0B5C28, true));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        lp.rightMargin = dp(6);
        lp.topMargin = dp(8);
        box.setLayoutParams(lp);
        return box;
    }

    private Reading fetch(String name, String lat, String lon) {
        Reading reading = new Reading(name);
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL("https://air-quality-api.open-meteo.com/v1/air-quality?latitude="
                    + lat + "&longitude=" + lon
                    + "&current=us_aqi,pm2_5,pm10,nitrogen_dioxide,ozone&timezone=Asia%2FKathmandu").openConnection();
            connection.setRequestProperty("User-Agent", "KalaiyaOnline/2.6");
            connection.setConnectTimeout(12000);
            connection.setReadTimeout(15000);
            if (connection.getResponseCode() >= 400) return reading;
            InputStream input = connection.getInputStream();
            StringBuilder body = new StringBuilder();
            byte[] buffer = new byte[4096];
            int read;
            while ((read = input.read(buffer)) != -1) body.append(new String(buffer, 0, read, "UTF-8"));
            input.close();
            JSONObject current = new JSONObject(body.toString()).getJSONObject("current");
            reading.aqi = (int) Math.round(current.getDouble("us_aqi"));
            reading.pm25 = (int) Math.round(current.getDouble("pm2_5"));
            reading.pm10 = (int) Math.round(current.getDouble("pm10"));
            reading.no2 = (int) Math.round(current.getDouble("nitrogen_dioxide"));
            reading.o3 = (int) Math.round(current.getDouble("ozone"));
            reading.ok = true;
        } catch (Exception ignored) {
            reading.ok = false;
        } finally {
            if (connection != null) connection.disconnect();
        }
        return reading;
    }

    private String label(int aqi) {
        if (aqi <= 50) return "राम्रो";
        if (aqi <= 100) return "मध्यम";
        if (aqi <= 150) return "संवेदनशीललाई अस्वस्थ";
        if (aqi <= 200) return "अस्वस्थ";
        if (aqi <= 300) return "धेरै अस्वस्थ";
        return "खतरनाक";
    }

    private int tone(int aqi) {
        if (aqi <= 50) return 0xFF15803D;
        if (aqi <= 100) return 0xFFA16207;
        if (aqi <= 150) return 0xFFC2410C;
        if (aqi <= 200) return 0xFFDC2626;
        if (aqi <= 300) return 0xFF7C3AED;
        return 0xFF7F1D1D;
    }

    private LinearLayout row() {
        LinearLayout line = new LinearLayout(activity);
        line.setOrientation(LinearLayout.HORIZONTAL);
        return line;
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

    private static final class Reading {
        final String name;
        int aqi;
        int pm25;
        int pm10;
        int no2;
        int o3;
        boolean ok;

        Reading(String name) {
            this.name = name;
        }
    }
}
