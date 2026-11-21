package com.kalaiyaonline.news;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.TextView;

import androidx.core.content.res.ResourcesCompat;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

public final class Notices {
    private static final String PREF = "notices";
    private static final String IDS = "seen";
    private static final String COUNT = "count";

    private Notices() {
    }

    public static int unread(Context context, List<String> ids) {
        SharedPreferences prefs = context.getSharedPreferences(PREF, Context.MODE_PRIVATE);
        if (!prefs.contains(IDS)) {
            mark(context, ids);
            return 0;
        }
        LinkedHashSet<String> seen = read(prefs.getString(IDS, ""));
        int count = 0;
        if (ids != null) {
            for (String id : ids) if (id != null && !id.isEmpty() && !seen.contains(id)) count++;
        }
        prefs.edit().putInt(COUNT, count).apply();
        return count;
    }

    public static int count(Context context) {
        return context.getSharedPreferences(PREF, Context.MODE_PRIVATE).getInt(COUNT, 0);
    }

    public static void mark(Context context, List<String> ids) {
        SharedPreferences prefs = context.getSharedPreferences(PREF, Context.MODE_PRIVATE);
        LinkedHashSet<String> seen = read(prefs.getString(IDS, ""));
        if (ids != null) seen.addAll(ids);
        List<String> kept = new ArrayList<>(seen);
        if (kept.size() > 200) kept = kept.subList(kept.size() - 200, kept.size());
        StringBuilder raw = new StringBuilder();
        for (String id : kept) {
            if (raw.length() > 0) raw.append(',');
            raw.append(id);
        }
        prefs.edit().putString(IDS, raw.toString()).putInt(COUNT, 0).apply();
    }

    public static TextView badge(Context context) {
        TextView view = new TextView(context);
        view.setTextColor(0xFFFFFFFF);
        view.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 9);
        view.setGravity(Gravity.CENTER);
        view.setIncludeFontPadding(false);
        Typeface face = ResourcesCompat.getFont(context, R.font.mukta_bold);
        if (face != null) view.setTypeface(face);
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(context, 10));
        bg.setColor(0xFFE11D48);
        view.setBackground(bg);
        int pad = dp(context, 3);
        view.setPadding(pad, 0, pad, 0);
        view.setMinWidth(dp(context, 16));
        view.setMinimumHeight(dp(context, 16));
        view.setVisibility(TextView.GONE);
        return view;
    }

    public static void paint(TextView badge, int count) {
        if (badge == null) return;
        if (count <= 0) {
            badge.setVisibility(TextView.GONE);
            return;
        }
        badge.setVisibility(TextView.VISIBLE);
        badge.setText(count > 99 ? "९९+" : nepali(count));
    }

    private static LinkedHashSet<String> read(String raw) {
        LinkedHashSet<String> seen = new LinkedHashSet<>();
        if (raw == null || raw.isEmpty()) return seen;
        for (String id : raw.split(",")) if (!id.isEmpty()) seen.add(id);
        return seen;
    }

    private static String nepali(int value) {
        String digits = "०१२३४५६७८९";
        String raw = String.valueOf(value);
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < raw.length(); i++) out.append(digits.charAt(raw.charAt(i) - '0'));
        return out.toString();
    }

    private static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
}
