package com.kalaiyaonline.news;

import android.app.DatePickerDialog;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.util.Calendar;

final class ConverterBoard {
    private static final String DIGITS = "०१२३४५६७८९";
    private static final String[] MONTHS = {"बैशाख", "जेठ", "असार", "साउन", "भदौ", "असोज", "कार्तिक", "मंसिर", "पुस", "माघ", "फागुन", "चैत"};
    private static final String[] WEEK = {"आइतबार", "सोमबार", "मंगलबार", "बुधबार", "बिहीबार", "शुक्रबार", "शनिबार"};
    private static final int[][] DAYS = new int[93][12];

    private final AppCompatActivity activity;
    private final LinearLayout root;
    private int adYear;
    private int adMonth;
    private int adDay;
    private NumberPicker bsYear;
    private NumberPicker bsMonth;
    private NumberPicker bsDay;
    private TextView adLabel;
    private TextView adResult;
    private TextView bsResult;

    ConverterBoard(AppCompatActivity activity) {
        this.activity = activity;
        loadTable();
        Calendar today = Calendar.getInstance();
        adYear = today.get(Calendar.YEAR);
        adMonth = today.get(Calendar.MONTH) + 1;
        adDay = today.get(Calendar.DAY_OF_MONTH);
        root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        root.addView(intro());
        root.addView(adCard());
        root.addView(bsCard());
    }

    View view() {
        return root;
    }

    private View intro() {
        TextView note = text("वि.सं. र ईस्वी संवत् बीच मिति बदल्नुहोस्। तालिका वि.सं. २००० देखि २०९२ सम्म हो।", 14, 0xFF57534E, false);
        note.setPadding(0, 0, 0, dp(8));
        return note;
    }

    private View adCard() {
        LinearLayout card = card();
        card.addView(text("ईस्वी › विक्रम संवत्", 18, 0xFF142033, true));
        card.addView(text("अंग्रेजी मिति छान्नुहोस्।", 13, 0xFF78716C, false));
        adLabel = text("", 16, 0xFF1C1917, true);
        adLabel.setPadding(0, dp(8), 0, dp(8));
        paintAd();
        adLabel.setOnClickListener(v -> new DatePickerDialog(activity, (view, year, month, day) -> {
            adYear = year;
            adMonth = month + 1;
            adDay = day;
            paintAd();
        }, adYear, adMonth - 1, adDay).show());
        card.addView(adLabel);
        card.addView(button("रूपान्तरण गर्नुहोस्", this::convertAd));
        adResult = text("", 20, 0xFF0B5C28, true);
        adResult.setPadding(0, dp(8), 0, 0);
        card.addView(adResult);
        return card;
    }

    private View bsCard() {
        LinearLayout card = card();
        card.addView(text("विक्रम संवत् › ईस्वी", 18, 0xFF142033, true));
        card.addView(text("वर्ष, महिना र गते छान्नुहोस्।", 13, 0xFF78716C, false));
        int[] today = adToBs(adYear, adMonth, adDay);
        bsYear = picker(2000, 2092, null);
        bsMonth = picker(1, 12, MONTHS);
        bsDay = picker(1, 32, null);
        if (today != null) {
            bsYear.setValue(today[0]);
            bsMonth.setValue(today[1]);
            refreshDays();
            bsDay.setValue(today[2]);
        }
        bsYear.setOnValueChangedListener((picker, oldValue, newValue) -> refreshDays());
        bsMonth.setOnValueChangedListener((picker, oldValue, newValue) -> refreshDays());
        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.addView(bsYear, weight());
        row.addView(bsMonth, weight());
        row.addView(bsDay, weight());
        card.addView(row);
        card.addView(button("ईस्वीमा रूपान्तरण गर्नुहोस्", this::convertBs));
        bsResult = text("", 20, 0xFF0B5C28, true);
        bsResult.setPadding(0, dp(8), 0, 0);
        card.addView(bsResult);
        return card;
    }

    private void convertAd() {
        int[] bs = adToBs(adYear, adMonth, adDay);
        adResult.setText(bs == null ? "यो मिति तालिकाभन्दा बाहिर छ।" : nep(bs[0]) + " " + MONTHS[bs[1] - 1] + " " + nep(bs[2]));
    }

    private void convertBs() {
        refreshDays();
        int[] ad = bsToAd(bsYear.getValue(), bsMonth.getValue(), bsDay.getValue());
        if (ad == null) {
            bsResult.setText("यो मिति मिलेन।");
            return;
        }
        Calendar calendar = Calendar.getInstance();
        calendar.set(ad[0], ad[1] - 1, ad[2], 12, 0, 0);
        bsResult.setText(nep(ad[0]) + "-" + nep(ad[1]) + "-" + nep(ad[2]) + "\n" + WEEK[calendar.get(Calendar.DAY_OF_WEEK) - 1]);
    }

    private void refreshDays() {
        int max = daysIn(bsYear.getValue(), bsMonth.getValue());
        int current = Math.min(bsDay.getValue(), max);
        bsDay.setMaxValue(max);
        bsDay.setValue(current);
    }

    private void paintAd() {
        adLabel.setText(adYear + "-" + pad(adMonth) + "-" + pad(adDay) + "   छान्नुहोस्");
    }

    private int[] adToBs(int year, int month, int day) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(1943, Calendar.APRIL, 14, 12, 0, 0);
        Calendar target = Calendar.getInstance();
        target.set(year, month - 1, day, 12, 0, 0);
        int left = (int) ((target.getTimeInMillis() - calendar.getTimeInMillis()) / 86400000L);
        if (left < 0) return null;
        for (int y = 0; y < DAYS.length; y++) {
            int yearDays = sum(DAYS[y]);
            if (left < yearDays) {
                for (int m = 0; m < 12; m++) {
                    if (left < DAYS[y][m]) return new int[]{2000 + y, m + 1, left + 1};
                    left -= DAYS[y][m];
                }
            }
            left -= yearDays;
        }
        return null;
    }

    private int[] bsToAd(int year, int month, int day) {
        if (year < 2000 || year > 2092 || month < 1 || month > 12) return null;
        int max = daysIn(year, month);
        if (day < 1 || day > max) return null;
        int total = day - 1;
        for (int m = 1; m < month; m++) total += daysIn(year, m);
        for (int y = 2000; y < year; y++) total += sum(DAYS[y - 2000]);
        Calendar calendar = Calendar.getInstance();
        calendar.set(1943, Calendar.APRIL, 14, 12, 0, 0);
        calendar.add(Calendar.DATE, total);
        return new int[]{calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.DAY_OF_MONTH)};
    }

    private int daysIn(int year, int month) {
        return DAYS[year - 2000][month - 1];
    }

    private int sum(int[] values) {
        int total = 0;
        for (int value : values) total += value;
        return total;
    }

    private void loadTable() {
        try {
            InputStream input = activity.getAssets().open("bs-months.json");
            StringBuilder body = new StringBuilder();
            byte[] buffer = new byte[4096];
            int read;
            while ((read = input.read(buffer)) != -1) body.append(new String(buffer, 0, read, "UTF-8"));
            input.close();
            JSONObject table = new JSONObject(body.toString());
            for (int year = 2000; year <= 2092; year++) {
                JSONArray months = table.getJSONArray(String.valueOf(year));
                for (int month = 0; month < 12; month++) DAYS[year - 2000][month] = months.getInt(month);
            }
        } catch (Exception ignored) {
        }
    }

    private NumberPicker picker(int min, int max, String[] labels) {
        NumberPicker picker = new NumberPicker(activity);
        picker.setMinValue(min);
        picker.setMaxValue(max);
        picker.setWrapSelectorWheel(false);
        if (labels != null) picker.setDisplayedValues(labels);
        return picker;
    }

    private TextView button(String label, Runnable action) {
        TextView button = text(label, 15, 0xFFFFFFFF, true);
        button.setGravity(Gravity.CENTER);
        button.setPadding(dp(12), dp(10), dp(12), dp(10));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xFF0F8A3C);
        bg.setCornerRadius(dp(14));
        button.setBackground(bg);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(8);
        button.setLayoutParams(lp);
        button.setOnClickListener(v -> action.run());
        return button;
    }

    private LinearLayout card() {
        LinearLayout card = new LinearLayout(activity);
        card.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xFFFFFFFF);
        bg.setCornerRadius(dp(16));
        card.setBackground(bg);
        card.setPadding(dp(14), dp(12), dp(14), dp(12));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(8);
        card.setLayoutParams(lp);
        return card;
    }

    private LinearLayout.LayoutParams weight() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        return lp;
    }

    private String nep(int value) {
        String raw = String.valueOf(value);
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < raw.length(); i++) out.append(DIGITS.charAt(raw.charAt(i) - '0'));
        return out.toString();
    }

    private String pad(int value) {
        return (value < 10 ? "0" : "") + value;
    }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView view = new TextView(activity);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        view.setTypeface(ResourcesCompat.getFont(activity, bold ? R.font.mukta_bold : R.font.mukta_regular));
        return view;
    }

    private int dp(int value) {
        return Math.round(value * activity.getResources().getDisplayMetrics().density);
    }
}
