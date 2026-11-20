package com.kalaiyaonline.news;

import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;

import org.json.JSONArray;
import org.json.JSONObject;

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
    private static final int[][] DAYS = new int[93][12];
    private static boolean tableReady;

    private final AppCompatActivity activity;
    private final LinearLayout root;
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
        ensureTable();
        root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(2), dp(4), dp(2), dp(8));
        root.addView(header());
        note = text("", 12, 0xFF9A3412, false);
        note.setVisibility(View.GONE);
        note.setPadding(0, dp(6), 0, 0);
        root.addView(note);
        status = text("पात्रो लोड हुँदैछ…", 14, 0xFF178A45, true);
        status.setGravity(Gravity.CENTER);
        status.setPadding(0, dp(10), 0, dp(6));
        root.addView(status);
        week = new GridLayout(activity);
        week.setColumnCount(7);
        root.addView(week);
        grid = new GridLayout(activity);
        grid.setColumnCount(7);
        grid.setBackgroundColor(0xFFE6E8EC);
        LinearLayout.LayoutParams gridLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        grid.setPadding(1, 1, 1, 1);
        grid.setLayoutParams(gridLp);
        root.addView(grid);
        detail = new LinearLayout(activity);
        detail.setOrientation(LinearLayout.VERTICAL);
        root.addView(detail);
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

    private View header() {
        LinearLayout row = new LinearLayout(activity);
        row.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout titles = new LinearLayout(activity);
        titles.setOrientation(LinearLayout.VERTICAL);
        TextView monthYear = text("पात्रो", 24, 0xFF111827, true);
        monthYear.setIncludeFontPadding(false);
        titles.addView(monthYear);
        TextView english = text("", 14, 0xFF6B7280, false);
        english.setPadding(0, dp(1), 0, 0);
        titles.addView(english);
        row.addView(titles, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(todayChip());
        row.addView(arrow("‹", () -> shiftMonth(-1)));
        row.addView(arrow("›", () -> shiftMonth(1)));
        row.setTag(new TextView[]{monthYear, english});
        return row;
    }

    private TextView titleView() {
        return ((TextView[]) ((LinearLayout) root.getChildAt(0)).getTag())[0];
    }

    private TextView rangeView() {
        return ((TextView[]) ((LinearLayout) root.getChildAt(0)).getTag())[1];
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
        load(next, 1);
    }

    private void load(int nextMonth, int nextDay) {
        final int requestMonth = nextMonth;
        final int requestDay = nextDay;
        if (nextMonth > 0) month = nextMonth;
        else year = todayYear();
        if (nextDay > 0) day = nextDay;
        activity.runOnUiThread(() -> {
            if (activity.isDestroyed()) return;
            status.setVisibility(View.VISIBLE);
            status.setText("पात्रो लोड हुँदैछ…");
        });
        new Thread(() -> {
            String html = fetch(requestMonth, requestDay);
            activity.runOnUiThread(() -> {
                if (activity.isDestroyed()) return;
                if (html == null) {
                    status.setText("पात्रो आएन। फेरि प्रयास गर्नुहोस्।");
                    return;
                }
                render(html);
            });
        }).start();
    }

    private void render(String html) {
        Matcher linkedYear = Pattern.compile("calendar=(\\d{4})").matcher(html);
        if (linkedYear.find()) year = Integer.parseInt(linkedYear.group(1));
        String monthName = "महिना";
        Matcher hero = Pattern.compile("<h1>([^<]+)</h1><p>([^<]+)</p>").matcher(html);
        if (hero.find()) {
            String sub = hero.group(2).trim();
            int split = sub.indexOf('·');
            if (split > 0) monthName = sub.substring(0, split).trim();
        }
        titleView().setText(monthName + ", " + nepali(year));

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
            item.year = year;
            try {
                item.month = Integer.parseInt(cells.group(3));
                item.day = Integer.parseInt(cells.group(4));
            } catch (NumberFormatException ignored) {
                continue;
            }
            item.nepali = cells.group(5);
            item.ad = cells.group(6);
            item.adNum = cells.group(6).replaceAll("\\D", "");
            item.event = cells.group(7) == null ? "" : cells.group(7);
            days.add(item);
        }
        if (days.isEmpty()) {
            status.setText("पात्रो पढ्न सकिएन।");
            return;
        }
        month = days.get(0).month;
        status.setVisibility(View.GONE);
        paintGrid(days, html);
        String selectedEvent = "";
        Day selected = null;
        for (Day item : days) {
            boolean picked = day > 0 ? item.month == month && item.day == day : item.cls.contains("today");
            if (picked) {
                selected = item;
                selectedEvent = item.event;
            }
        }
        if (selected == null && !days.isEmpty()) selected = days.get(0);
        paintDetail(html, selectedEvent, selected == null ? "" : tithiLine(selected));
        paintUpcoming(html);
    }

    private void paintGrid(List<Day> days, String html) {
        week.removeAllViews();
        for (int i = 0; i < WEEK.length; i++) {
            TextView label = text(WEEK[i], 13, i == 0 || i == 6 ? 0xFFDC2626 : 0xFF6B7280, true);
            label.setGravity(Gravity.CENTER);
            label.setPadding(0, dp(10), 0, dp(8));
            addCell(week, label, false);
        }
        grid.removeAllViews();
        Calendar first = gregorian(days.get(0), html);
        int lead = first == null ? 0 : first.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY;
        if (first != null) {
            Calendar cursor = (Calendar) first.clone();
            for (Day item : days) {
                item.gy = cursor.get(Calendar.YEAR);
                item.gm = cursor.get(Calendar.MONTH) + 1;
                item.gd = cursor.get(Calendar.DAY_OF_MONTH);
                cursor.add(Calendar.DATE, 1);
            }
        }
        if (first != null) {
            Calendar cursor = (Calendar) first.clone();
            cursor.add(Calendar.DATE, -lead);
            for (int i = 0; i < lead; i++) {
                addCell(grid, dayCell(fromAd(cursor)), true);
                cursor.add(Calendar.DATE, 1);
            }
            Calendar last = (Calendar) first.clone();
            last.add(Calendar.DATE, days.size() - 1);
            rangeView().setText(englishRange(first, last));
        }
        int index = lead;
        for (Day item : days) {
            item.weekend = index % 7 == 0 || index % 7 == 6;
            addCell(grid, dayCell(item), true);
            index++;
        }
        int trail = (7 - (index % 7)) % 7;
        if (first != null && trail > 0) {
            Calendar cursor = (Calendar) first.clone();
            cursor.add(Calendar.DATE, days.size());
            for (int i = 0; i < trail; i++) {
                addCell(grid, dayCell(fromAd(cursor)), true);
                cursor.add(Calendar.DATE, 1);
            }
        }
    }

    private String englishRange(Calendar first, Calendar last) {
        String start = MONTHS[first.get(Calendar.MONTH)];
        String end = MONTHS[last.get(Calendar.MONTH)];
        int adYear = last.get(Calendar.YEAR);
        if (start.equals(end)) return start + ", " + adYear;
        return start + "/" + end + ", " + adYear;
    }

    private void paintDetail(String html, String event, String tithi) {
        detail.removeAllViews();
        Matcher tile = Pattern.compile("patro-date-tile\"><strong>([^<]+)</strong><b>([^<]+)</b><span>([^<]+)</span><small>([^<]+)</small>").matcher(html);
        if (!tile.find()) return;
        LinearLayout card = new LinearLayout(activity);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setBackground(round(0xFFFFFFFF, 18, 0xFFE5E7EB));
        card.setPadding(dp(14), dp(12), dp(14), dp(12));
        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cardLp.topMargin = dp(12);
        card.setLayoutParams(cardLp);
        TextView big = text(tile.group(1), 32, 0xFF111827, true);
        big.setGravity(Gravity.CENTER);
        card.addView(big, new LinearLayout.LayoutParams(dp(48), ViewGroup.LayoutParams.WRAP_CONTENT));
        LinearLayout copy = new LinearLayout(activity);
        copy.setOrientation(LinearLayout.VERTICAL);
        copy.setPadding(dp(8), 0, 0, 0);
        copy.addView(text(tile.group(2), 14, 0xFF111827, true));
        copy.addView(text(tile.group(3) + "   ·   " + tile.group(4), 12, 0xFF6B7280, false));
        if (!tithi.isEmpty()) copy.addView(text(tithi, 13, 0xFFDC2626, true));
        if (!event.isEmpty()) copy.addView(text(event, 13, 0xFF111827, true));
        card.addView(copy, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        detail.addView(card);
    }

    private void paintUpcoming(String html) {
        upcoming.removeAllViews();
        Matcher events = Pattern.compile("<a href=\"\\?calendar=\\d+&(?:amp;)?month=(\\d+)&(?:amp;)?day=(\\d+)#patro-detail\"><time><b>([^<]*)</b><small>([^<]*)</small></time><span><strong>([^<]*)</strong><small>([^<]*)</small>").matcher(html);
        List<String[]> rows = new ArrayList<>();
        while (events.find() && rows.size() < 6) {
            rows.add(new String[]{events.group(1), events.group(2), events.group(3), events.group(4), events.group(5), events.group(6)});
        }
        if (rows.isEmpty()) return;
        TextView head = text("आगामी पर्व", 16, 0xFF111827, true);
        head.setPadding(dp(2), dp(16), 0, dp(4));
        upcoming.addView(head);
        for (String[] event : rows) {
            int eventMonth;
            int eventDay;
            try {
                eventMonth = Integer.parseInt(event[0]);
                eventDay = Integer.parseInt(event[1]);
            } catch (NumberFormatException ignored) {
                continue;
            }
            LinearLayout row = new LinearLayout(activity);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(2), dp(10), dp(2), dp(10));
            View line = new View(activity);
            line.setBackgroundColor(0xFFE5E7EB);
            LinearLayout.LayoutParams lineLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1);
            upcoming.addView(line, lineLp);
            TextView num = text(event[2], 18, 0xFF178A45, true);
            num.setGravity(Gravity.CENTER);
            row.addView(num, new LinearLayout.LayoutParams(dp(42), ViewGroup.LayoutParams.WRAP_CONTENT));
            LinearLayout copy = new LinearLayout(activity);
            copy.setOrientation(LinearLayout.VERTICAL);
            copy.setPadding(dp(8), 0, 0, 0);
            copy.addView(text(event[4], 15, 0xFF111827, true));
            copy.addView(text(event[3] + "  ·  " + event[5], 12, 0xFF6B7280, false));
            row.addView(copy, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            row.setOnClickListener(v -> load(eventMonth, eventDay));
            upcoming.addView(row);
        }
    }

    private View dayCell(Day item) {
        boolean today = !item.outside && item.cls.contains("today");
        boolean holiday = !item.outside && item.cls.contains("holiday");
        boolean weekend = item.weekend || item.cls.contains("weekend");
        String tithi = tithiLine(item);
        boolean special = tithi.contains("पूर्णिमा") || tithi.contains("औंसी") || tithi.contains("एकादशी");
        boolean selected = !item.outside && !today && item.month == month && item.day == day && day > 0;
        LinearLayout cell = new LinearLayout(activity);
        cell.setOrientation(LinearLayout.VERTICAL);
        cell.setGravity(Gravity.CENTER_HORIZONTAL);
        cell.setMinimumHeight(dp(64));
        cell.setPadding(dp(1), dp(5), dp(1), dp(4));
        if (today) cell.setBackgroundColor(0xFF1F7A45);
        else if (holiday) cell.setBackgroundColor(0xFFFFF1F2);
        else if (selected) cell.setBackgroundColor(0xFFF3F4F6);
        else cell.setBackgroundColor(0xFFFFFFFF);

        int outsideColor = weekend ? 0xFFE7B4B8 : 0xFFC5CAD3;
        int number = today ? 0xFFFFFFFF : item.outside ? outsideColor : (holiday || weekend || special ? 0xFFDC2626 : 0xFF111827);
        int muted = today ? 0xE6FFFFFF : item.outside ? outsideColor : 0xFF9CA3AF;
        int captionColor = today ? 0xFFFFFFFF : item.outside ? outsideColor : (holiday || weekend || special ? 0xFFDC2626 : 0xFF6B7280);
        LinearLayout top = new LinearLayout(activity);
        top.setGravity(Gravity.CENTER);
        top.addView(text(item.nepali, 16, number, true));
        TextView english = text(item.adNum, 10, muted, false);
        english.setPadding(dp(2), dp(3), 0, 0);
        top.addView(english);
        cell.addView(top);

        String captionText = tithi.isEmpty() ? " " : tithi;
        TextView caption = text(captionText, 9, captionColor, false);
        caption.setGravity(Gravity.CENTER);
        caption.setMaxLines(1);
        caption.setEllipsize(TextUtils.TruncateAt.END);
        cell.addView(caption);

        View dot = new View(activity);
        GradientDrawable oval = new GradientDrawable();
        oval.setShape(GradientDrawable.OVAL);
        int dotColor = today ? 0xFFFFFFFF : item.outside ? outsideColor : (holiday || weekend || special ? 0xFFDC2626 : 0xFF111827);
        oval.setColor(dotColor);
        dot.setBackground(oval);
        LinearLayout.LayoutParams dotLp = new LinearLayout.LayoutParams(dp(3), dp(3));
        dotLp.topMargin = dp(2);
        cell.addView(dot, dotLp);
        if (item.month > 0) {
            cell.setOnClickListener(v -> open(item));
        }
        return cell;
    }

    private void open(Day item) {
        if (item.year > 0 && item.year != year) year = item.year;
        load(item.month, item.day);
    }

    private Day fromAd(Calendar calendar) {
        Day item = new Day();
        item.outside = true;
        item.adNum = String.valueOf(calendar.get(Calendar.DAY_OF_MONTH));
        item.gy = calendar.get(Calendar.YEAR);
        item.gm = calendar.get(Calendar.MONTH) + 1;
        item.gd = calendar.get(Calendar.DAY_OF_MONTH);
        int dow = calendar.get(Calendar.DAY_OF_WEEK);
        item.weekend = dow == Calendar.SUNDAY || dow == Calendar.SATURDAY;
        int[] bs = adToBs(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.DAY_OF_MONTH));
        if (bs != null) {
            item.year = bs[0];
            item.month = bs[1];
            item.day = bs[2];
            item.nepali = nepali(bs[2]);
        }
        return item;
    }

    private Calendar gregorian(Day first, String html) {
        Matcher full = Pattern.compile("([A-Z][a-z]{2}) (\\d{1,2}), (\\d{4})").matcher(html);
        if (!full.find()) return null;
        String[] parts = first.ad.trim().split(" ");
        if (parts.length < 2) return null;
        try {
            int anchor = dateNumber(Integer.parseInt(full.group(3)), monthIndex(full.group(1)), Integer.parseInt(full.group(2)));
            int cellMonth = monthIndex(parts[0]);
            int cellDay = Integer.parseInt(parts[1].replaceAll("\\D", ""));
            if (cellDay < 1) return null;
            int adYear = Integer.parseInt(full.group(3));
            int bestYear = adYear;
            int bestGap = Integer.MAX_VALUE;
            for (int tryYear = adYear - 1; tryYear <= adYear + 1; tryYear++) {
                int gap = Math.abs(dateNumber(tryYear, cellMonth, cellDay) - anchor);
                if (gap < bestGap) {
                    bestGap = gap;
                    bestYear = tryYear;
                }
            }
            Calendar calendar = Calendar.getInstance();
            calendar.set(bestYear, cellMonth, cellDay, 12, 0, 0);
            calendar.set(Calendar.MILLISECOND, 0);
            return calendar;
        } catch (Exception ignored) {
            return null;
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
        return Net.text(url);
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

    private void ensureTable() {
        if (tableReady) return;
        try {
            InputStream input = activity.getAssets().open("bs-months.json");
            StringBuilder body = new StringBuilder();
            byte[] buffer = new byte[4096];
            int read;
            while ((read = input.read(buffer)) != -1) body.append(new String(buffer, 0, read, "UTF-8"));
            input.close();
            JSONObject table = new JSONObject(body.toString());
            for (int bsYear = 2000; bsYear <= 2092; bsYear++) {
                JSONArray months = table.getJSONArray(String.valueOf(bsYear));
                for (int bsMonth = 0; bsMonth < 12; bsMonth++) DAYS[bsYear - 2000][bsMonth] = months.getInt(bsMonth);
            }
            tableReady = true;
        } catch (Exception ignored) {
        }
    }

    private int[] adToBs(int year, int month, int day) {
        if (!tableReady) return null;
        Calendar calendar = Calendar.getInstance();
        calendar.set(1943, Calendar.APRIL, 14, 12, 0, 0);
        Calendar target = Calendar.getInstance();
        target.set(year, month - 1, day, 12, 0, 0);
        int left = (int) ((target.getTimeInMillis() - calendar.getTimeInMillis()) / 86400000L);
        if (left < 0) return null;
        for (int y = 0; y < DAYS.length; y++) {
            int yearDays = 0;
            for (int value : DAYS[y]) yearDays += value;
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

    private String nepali(int number) {
        String digits = "०१२३४५६७८९";
        String raw = String.valueOf(number);
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < raw.length(); i++) {
            char ch = raw.charAt(i);
            if (ch >= '0' && ch <= '9') out.append(digits.charAt(ch - '0'));
        }
        return out.toString();
    }

    private TextView arrow(String label, Runnable action) {
        TextView chip = text(label, 22, 0xFF111827, true);
        chip.setGravity(Gravity.CENTER);
        chip.setPadding(dp(8), 0, dp(8), 0);
        chip.setOnClickListener(v -> action.run());
        return chip;
    }

    private TextView todayChip() {
        TextView chip = text("आज", 14, 0xFF178A45, true);
        chip.setGravity(Gravity.CENTER);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xFFE7F6EE);
        bg.setCornerRadius(dp(20));
        chip.setBackground(bg);
        chip.setPadding(dp(14), dp(6), dp(14), dp(6));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.rightMargin = dp(4);
        chip.setLayoutParams(lp);
        chip.setOnClickListener(v -> loadToday());
        return chip;
    }

    private void addCell(GridLayout parent, View child, boolean lined) {
        GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
        lp.width = 0;
        lp.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        if (lined) lp.setMargins(0, 0, 1, 1);
        child.setLayoutParams(lp);
        parent.addView(child);
    }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView view = new TextView(activity);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        view.setIncludeFontPadding(false);
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
        String adNum = "";
        String event = "";
        int year;
        int month;
        int day;
        boolean outside;
        boolean weekend;
        int gy;
        int gm;
        int gd;
    }

    private String tithiLine(Day item) {
        if (item.gd < 1) return "";
        String name = tithiName(item.gy, item.gm, item.gd);
        if (name.isEmpty()) return "";
        boolean star = !item.event.isEmpty() || name.contains("एकादशी") || name.contains("पूर्णिमा") || name.contains("औंसी");
        return star ? name + "*" : name;
    }

    private String tithiName(int year, int month, int day) {
        double jd = julian(year, month, day, 2.0);
        double time = jd - 2451543.5;
        double sun = sunLongitude(time);
        double moon = moonLongitude(time, sun);
        double gap = (moon - sun) % 360.0;
        if (gap < 0) gap += 360.0;
        int index = ((int) Math.floor(gap / 12.0)) % 30;
        String[] names = {"प्रतिपदा", "द्वितीया", "तृतीया", "चतुर्थी", "पञ्चमी", "षष्ठी", "सप्तमी", "अष्टमी", "नवमी", "दशमी", "एकादशी", "द्वादशी", "त्रयोदशी", "चतुर्दशी", "पूर्णिमा"};
        if (index == 29) return "औंसी";
        if (index >= 15) return names[index - 15];
        return names[index];
    }

    private double julian(int year, int month, int day, double ut) {
        int y = year;
        int m = month;
        if (m <= 2) {
            y -= 1;
            m += 12;
        }
        int a = y / 100;
        int b = 2 - a + a / 4;
        return Math.floor(365.25 * (y + 4716)) + Math.floor(30.6001 * (m + 1)) + day + ut / 24.0 + b - 1524.5;
    }

    private double sunLongitude(double d) {
        double mean = Math.toRadians((356.0470 + 0.9856002585 * d) % 360.0);
        double lon = 282.9404 + 4.70935e-5 * d + Math.toDegrees(mean) + 1.9146 * Math.sin(mean) + 0.019993 * Math.sin(2 * mean);
        return floor360(lon);
    }

    private double moonLongitude(double d, double sunLon) {
        double node = floor360(125.1228 - 0.0529538083 * d);
        double peri = floor360(318.0634 + 0.1643573223 * d);
        double anomaly = floor360(115.3654 + 13.0649929509 * d);
        double e = 0.054900;
        double mr = Math.toRadians(anomaly);
        double eccentric = mr + e * Math.sin(mr) * (1 + e * Math.cos(mr));
        double xv = Math.cos(eccentric) - e;
        double yv = Math.sqrt(1 - e * e) * Math.sin(eccentric);
        double v = Math.atan2(yv, xv);
        double nr = Math.toRadians(node);
        double wr = Math.toRadians(peri);
        double i = Math.toRadians(5.1454);
        double xh = Math.cos(nr) * Math.cos(v + wr) - Math.sin(nr) * Math.sin(v + wr) * Math.cos(i);
        double yh = Math.sin(nr) * Math.cos(v + wr) + Math.cos(nr) * Math.sin(v + wr) * Math.cos(i);
        double lon = Math.toDegrees(Math.atan2(yh, xh));
        double moonMean = floor360(node + peri + anomaly);
        double sunMean = floor360(282.9404 + 4.70935e-5 * d + ((356.0470 + 0.9856002585 * d) % 360.0));
        double dr = Math.toRadians(floor360(moonMean - sunMean));
        double fr = Math.toRadians(floor360(moonMean - node));
        double mm = Math.toRadians(anomaly);
        double ms = Math.toRadians(floor360(356.0470 + 0.9856002585 * d));
        lon += -1.274 * Math.sin(mm - 2 * dr) + 0.658 * Math.sin(2 * dr) - 0.186 * Math.sin(ms)
                - 0.059 * Math.sin(2 * mm - 2 * dr) - 0.057 * Math.sin(mm - 2 * dr + ms)
                + 0.053 * Math.sin(mm + 2 * dr) + 0.046 * Math.sin(2 * dr - ms) + 0.041 * Math.sin(mm - ms)
                - 0.035 * Math.sin(dr) - 0.031 * Math.sin(mm + ms) - 0.015 * Math.sin(2 * fr - 2 * dr)
                + 0.011 * Math.sin(mm - 4 * dr);
        return floor360(lon + sunLon - sunLon);
    }

    private double floor360(double value) {
        double wrapped = value % 360.0;
        return wrapped < 0 ? wrapped + 360.0 : wrapped;
    }

    private GradientDrawable round(int color, int radius, int stroke) {
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(color);
        bg.setCornerRadius(dp(radius));
        if (stroke != 0) bg.setStroke(dp(1), stroke);
        return bg;
    }
}
