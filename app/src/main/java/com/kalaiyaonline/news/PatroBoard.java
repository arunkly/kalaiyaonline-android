package com.kalaiyaonline.news;

import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class PatroBoard {
    private static final String[] WEEK = {"आ", "सो", "मं", "बु", "बि", "शु", "श"};
    private static final String[] MONTHS = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};

    private final AppCompatActivity activity;
    private final LinearLayout root;
    private TextView yearLabel;
    private TextView monthLabel;
    private final TextView subtitle;
    private final TextView status;
    private final TextView note;
    private final LinearLayout detail;
    private final GridLayout week;
    private final GridLayout grid;
    private final LinearLayout upcoming;
    private int year = 2083;
    private int month;
    private int day;

    PatroBoard(AppCompatActivity activity) {
        this.activity = activity;
        root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        root.addView(chrome());
        subtitle = text("", 13, 0xFF78716C, false);
        subtitle.setGravity(Gravity.CENTER);
        subtitle.setPadding(0, dp(2), 0, dp(4));
        root.addView(subtitle);
        note = text("", 12, 0xFF9A3412, false);
        note.setVisibility(View.GONE);
        root.addView(note);
        status = text("पात्रो लोड हुँदैछ…", 14, 0xFF0B5C28, true);
        status.setPadding(0, dp(4), 0, dp(4));
        root.addView(status);
        detail = new LinearLayout(activity);
        detail.setOrientation(LinearLayout.VERTICAL);
        root.addView(detail);
        week = new GridLayout(activity);
        week.setColumnCount(7);
        root.addView(week);
        grid = new GridLayout(activity);
        grid.setColumnCount(7);
        root.addView(grid);
        upcoming = new LinearLayout(activity);
        upcoming.setOrientation(LinearLayout.VERTICAL);
        root.addView(upcoming);
    }

    View view() {
        return root;
    }

    void loadToday() {
        load(0, 0);
    }

    private View chrome() {
        LinearLayout box = new LinearLayout(activity);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(0, dp(4), 0, dp(6));
        LinearLayout yearRow = new LinearLayout(activity);
        yearRow.setGravity(Gravity.CENTER_VERTICAL);
        yearRow.addView(arrow("‹", () -> shiftYear(-1)));
        yearLabel = text(nepali(year), 26, 0xFF142033, true);
        yearLabel.setGravity(Gravity.CENTER);
        yearRow.addView(yearLabel, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        yearRow.addView(chip("आज", this::loadToday));
        yearRow.addView(arrow("›", () -> shiftYear(1)));
        box.addView(yearRow);
        LinearLayout monthRow = new LinearLayout(activity);
        monthRow.setGravity(Gravity.CENTER_VERTICAL);
        monthRow.setPadding(0, dp(8), 0, 0);
        monthRow.addView(arrow("‹", () -> shiftMonth(-1)));
        monthLabel = text("महिना", 18, 0xFF0B5C28, true);
        monthLabel.setGravity(Gravity.CENTER);
        monthRow.addView(monthLabel, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        monthRow.addView(arrow("›", () -> shiftMonth(1)));
        box.addView(monthRow);
        return box;
    }

    private void shiftYear(int delta) {
        int next = year + delta;
        if (next < 2075 || next > 2092) return;
        year = next;
        yearLabel.setText(nepali(year));
        load(month > 0 ? month : 1, 1);
    }

    private void shiftMonth(int delta) {
        if (month <= 0) {
            loadToday();
            return;
        }
        int next = month + delta;
        int nextYear = year;
        if (next < 1) {
            next = 12;
            nextYear--;
        } else if (next > 12) {
            next = 1;
            nextYear++;
        }
        if (nextYear < 2075 || nextYear > 2092) return;
        year = nextYear;
        yearLabel.setText(nepali(year));
        load(next, 1);
    }

    private void load(int nextMonth, int nextDay) {
        final int requestMonth = nextMonth;
        final int requestDay = nextDay;
        if (nextMonth > 0) month = nextMonth;
        else year = todayYear();
        if (nextDay > 0) day = nextDay;
        if (yearLabel != null) yearLabel.setText(nepali(year));
        activity.runOnUiThread(() -> {
            status.setVisibility(View.VISIBLE);
            status.setText("पात्रो लोड हुँदैछ…");
        });
        new Thread(() -> {
            String html = fetch(requestMonth, requestDay);
            activity.runOnUiThread(() -> {
                if (html == null) {
                    status.setText("पात्रो आएन। फेरि प्रयास गर्नुहोस्।");
                    return;
                }
                render(html);
            });
        }).start();
    }

    private void render(String html) {
        Matcher hero = Pattern.compile("<h1>([^<]+)</h1><p>([^<]+)</p>").matcher(html);
        if (hero.find()) subtitle.setText(hero.group(2).trim());
        Matcher linkedYear = Pattern.compile("calendar=(\\d{4})").matcher(html);
        if (linkedYear.find()) {
            year = Integer.parseInt(linkedYear.group(1));
            yearLabel.setText(nepali(year));
        }
        String sub = subtitle.getText().toString();
        int split = sub.indexOf('·');
        monthLabel.setText(split > 0 ? sub.substring(0, split).trim() : (sub.isEmpty() ? "महिना" : sub));
        Matcher count = Pattern.compile("patro-hero-month\"><strong>([^<]+)</strong><span>([^<]+)</span>").matcher(html);
        if (count.find()) subtitle.setText(sub + (sub.isEmpty() ? "" : " · ") + count.group(1) + " " + count.group(2));

        Matcher warning = Pattern.compile("सूचना:\\s*([^<]{8,180})").matcher(html);
        if (warning.find()) {
            note.setText(warning.group(1).trim());
            note.setVisibility(View.VISIBLE);
        } else {
            note.setVisibility(View.GONE);
        }

        List<Day> days = new ArrayList<>();
        Matcher cells = Pattern.compile("<a class=\"patro-day([^\"]*)\" href=\"\\?calendar=(\\d+)&(?:amp;)?month=(\\d+)&(?:amp;)?day=(\\d+)[^\"]*\"><b>([^<]*)</b><small>([^<]*)</small>(?:<em>([^<]*)</em>)?").matcher(html);
        while (cells.find()) {
            Day item = new Day();
            item.cls = cells.group(1);
            item.month = Integer.parseInt(cells.group(3));
            item.day = Integer.parseInt(cells.group(4));
            item.nepali = cells.group(5);
            item.ad = cells.group(6);
            item.event = cells.group(7) == null ? "" : cells.group(7);
            days.add(item);
        }
        if (days.isEmpty()) {
            status.setText("पात्रो पढ्न सकिएन।");
            return;
        }
        month = days.get(0).month;
        status.setVisibility(View.GONE);

        detail.removeAllViews();
        Matcher tile = Pattern.compile("patro-date-tile\"><strong>([^<]+)</strong><b>([^<]+)</b><span>([^<]+)</span><small>([^<]+)</small>").matcher(html);
        if (tile.find()) {
            LinearLayout card = card();
            card.addView(text(tile.group(1) + "  " + tile.group(2), 18, 0xFF142033, true));
            card.addView(text(tile.group(3) + " · " + tile.group(4), 14, 0xFF57534E, false));
            Matcher sun = Pattern.compile("<b>सूर्योदय</b><strong>([^<]*)</strong>").matcher(html);
            Matcher set = Pattern.compile("<b>सूर्यास्त</b><strong>([^<]*)</strong>").matcher(html);
            if (sun.find() && set.find()) card.addView(text("सूर्योदय " + sun.group(1) + "   सूर्यास्त " + set.group(1), 13, 0xFF0B5C28, false));
            detail.addView(card);
        }

        week.removeAllViews();
        for (int i = 0; i < WEEK.length; i++) {
            TextView label = text(WEEK[i], 12, i == 0 || i == 6 ? 0xFFDC2626 : 0xFF78716C, true);
            label.setGravity(Gravity.CENTER);
            addCell(week, label);
        }

        grid.removeAllViews();
        int lead = leadBlanks(days.get(0), html);
        for (int i = 0; i < lead; i++) addCell(grid, new View(activity));
        for (Day item : days) addCell(grid, dayCell(item));

        upcoming.removeAllViews();
        TextView head = text("आगामी पर्व", 18, 0xFF142033, true);
        head.setPadding(0, dp(16), 0, dp(6));
        upcoming.addView(head);
        Matcher events = Pattern.compile("<a href=\"\\?calendar=\\d+&(?:amp;)?month=(\\d+)&(?:amp;)?day=(\\d+)#patro-detail\"><time><b>([^<]*)</b><small>([^<]*)</small></time><span><strong>([^<]*)</strong><small>([^<]*)</small>").matcher(html);
        int shown = 0;
        while (events.find() && shown < 6) {
            shown++;
            int eventMonth = Integer.parseInt(events.group(1));
            int eventDay = Integer.parseInt(events.group(2));
            LinearLayout row = card();
            row.addView(text(events.group(3) + " " + events.group(4), 13, 0xFF0B5C28, true));
            row.addView(text(events.group(5), 16, 0xFF1C1917, true));
            row.addView(text(events.group(6), 12, 0xFF78716C, false));
            row.setOnClickListener(v -> load(eventMonth, eventDay));
            upcoming.addView(row);
        }
        if (shown == 0) upcoming.removeAllViews();
    }

    private View dayCell(Day item) {
        LinearLayout cell = new LinearLayout(activity);
        cell.setOrientation(LinearLayout.VERTICAL);
        cell.setGravity(Gravity.CENTER_HORIZONTAL);
        boolean today = item.cls.contains("today");
        boolean holiday = item.cls.contains("holiday");
        boolean selected = item.month == month && item.day == day && day > 0;
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(12));
        bg.setColor(today || selected ? 0xFFE5F6EC : 0xFFFFFFFF);
        if (today || selected) bg.setStroke(dp(1), 0xFF0F8A3C);
        cell.setBackground(bg);
        cell.setPadding(dp(2), dp(6), dp(2), dp(4));
        cell.setMinimumHeight(dp(58));
        TextView number = text(item.nepali, 15, holiday ? 0xFFC2410C : 0xFF1C1917, true);
        number.setGravity(Gravity.CENTER);
        cell.addView(number);
        TextView ad = text(item.ad, 9, 0xFF78716C, false);
        ad.setGravity(Gravity.CENTER);
        cell.addView(ad);
        if (!item.event.isEmpty()) {
            TextView event = text(item.event, 9, 0xFFC2410C, false);
            event.setGravity(Gravity.CENTER);
            event.setMaxLines(2);
            cell.addView(event);
        }
        cell.setOnClickListener(v -> load(item.month, item.day));
        return cell;
    }

    private int leadBlanks(Day first, String html) {
        Matcher full = Pattern.compile("([A-Z][a-z]{2}) (\\d{1,2}), (\\d{4})").matcher(html);
        if (!full.find()) return 0;
        String[] parts = first.ad.trim().split(" ");
        if (parts.length < 2) return 0;
        try {
            int anchor = dateNumber(Integer.parseInt(full.group(3)), monthIndex(full.group(1)), Integer.parseInt(full.group(2)));
            int cellMonth = monthIndex(parts[0]);
            int cellDay = Integer.parseInt(parts[1].replaceAll("\\D", ""));
            if (cellDay < 1) return 0;
        int year = Integer.parseInt(full.group(3));
        int bestYear = year;
        int bestGap = Integer.MAX_VALUE;
        for (int tryYear = year - 1; tryYear <= year + 1; tryYear++) {
            int gap = Math.abs(dateNumber(tryYear, cellMonth, cellDay) - anchor);
            if (gap < bestGap) {
                bestGap = gap;
                bestYear = tryYear;
            }
        }
        Calendar calendar = Calendar.getInstance();
        calendar.set(bestYear, cellMonth, cellDay, 12, 0, 0);
        return calendar.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY;
        } catch (Exception ignored) {
            return 0;
        }
    }

    private int dateNumber(int year, int month, int day) {
        return year * 372 + month * 31 + day;
    }

    private int monthIndex(String name) {
        for (int i = 0; i < MONTHS.length; i++) if (MONTHS[i].equals(name)) return i;
        return 0;
    }

    private String fetch(int requestMonth, int requestDay) {
        int requestYear = year;
        if (requestMonth <= 0) requestYear = todayYear();
        String url = "https://kalaiyaonline.com/?calendar=" + requestYear;
        if (requestMonth > 0) url += "&month=" + requestMonth + "&day=" + Math.max(requestDay, 1);
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestProperty("User-Agent", "Mozilla/5.0");
            connection.setConnectTimeout(12000);
            connection.setReadTimeout(15000);
            InputStream input = connection.getInputStream();
            StringBuilder body = new StringBuilder();
            byte[] buffer = new byte[8192];
            int read;
            while ((read = input.read(buffer)) != -1) body.append(new String(buffer, 0, read, "UTF-8"));
            input.close();
            return body.toString();
        } catch (Exception ignored) {
            return null;
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    private int todayYear() {
        Calendar calendar = Calendar.getInstance();
        int adYear = calendar.get(Calendar.YEAR);
        int adMonth = calendar.get(Calendar.MONTH) + 1;
        int adDay = calendar.get(Calendar.DAY_OF_MONTH);
        int bs = adYear + 56;
        if (adMonth > 4 || (adMonth == 4 && adDay >= 14)) bs++;
        return bs;
    }

    private String nepali(int number) {
        String digits = "०१२३४५६७८९";
        String raw = String.valueOf(number);
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < raw.length(); i++) out.append(digits.charAt(raw.charAt(i) - '0'));
        return out.toString();
    }

    private TextView arrow(String label, Runnable action) {
        TextView chip = text(label, 15, 0xFF0B5C28, true);
        chip.setGravity(Gravity.CENTER);
        chip.setMinWidth(dp(44));
        chip.setPadding(dp(12), dp(6), dp(12), dp(6));
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(16));
        bg.setColor(0xFFE5F6EC);
        chip.setBackground(bg);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.leftMargin = dp(6);
        chip.setLayoutParams(lp);
        chip.setOnClickListener(v -> action.run());
        return chip;
    }

    private TextView chip(String label, Runnable action) {
        TextView chip = arrow(label, action);
        chip.setMinWidth(dp(52));
        return chip;
    }

    private LinearLayout card() {
        LinearLayout card = new LinearLayout(activity);
        card.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xFFFFFFFF);
        bg.setCornerRadius(dp(14));
        bg.setStroke(dp(1), 0xFFE7E5E4);
        card.setBackground(bg);
        card.setPadding(dp(12), dp(10), dp(12), dp(10));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(8);
        card.setLayoutParams(lp);
        return card;
    }

    private void addCell(GridLayout parent, View child) {
        GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
        lp.width = 0;
        lp.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        lp.setMargins(dp(1), dp(1), dp(1), dp(1));
        child.setLayoutParams(lp);
        parent.addView(child);
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

    private static final class Day {
        String cls = "";
        String nepali = "";
        String ad = "";
        String event = "";
        int month;
        int day;
    }
}
