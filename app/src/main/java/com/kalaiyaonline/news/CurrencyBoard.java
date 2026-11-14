package com.kalaiyaonline.news;

import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class CurrencyBoard {
    private static final String[] ORDER = {"USD", "INR", "EUR", "GBP", "AUD", "CAD", "JPY", "AED", "SAR", "QAR", "SGD", "MYR", "CNY", "CHF", "KWD", "BHD", "OMR", "THB", "KRW", "HKD", "SEK", "DKK"};

    private final AppCompatActivity activity;
    private final LinearLayout root;
    private final EditText amount;
    private final Spinner currency;
    private final TextView direction;
    private final TextView buyChip;
    private final TextView sellChip;
    private final TextView result;
    private final TextView formula;
    private final TextView status;
    private final LinearLayout table;
    private final List<Rate> rates = new ArrayList<>();
    private boolean toNpr = true;
    private boolean sell = true;

    CurrencyBoard(AppCompatActivity activity) {
        this.activity = activity;
        root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        TextView intro = text("नेपाल राष्ट्र बैंकको दैनिक विनिमय दरबाट विदेशी मुद्रा र रुपैयाँ बदल्नुहोस्।", 14, 0xFF57534E, false);
        intro.setPadding(0, 0, 0, dp(8));
        root.addView(intro);

        LinearLayout card = card();
        card.addView(text("मुद्रा क्याल्कुलेटर", 18, 0xFF142033, true));
        card.addView(label("रकम"));
        amount = new EditText(activity);
        amount.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        amount.setText("100");
        amount.setTextColor(0xFF1C1917);
        amount.setTextSize(18);
        amount.setPadding(dp(12), dp(10), dp(12), dp(10));
        GradientDrawable field = new GradientDrawable();
        field.setColor(0xFFF6F7F4);
        field.setCornerRadius(dp(12));
        amount.setBackground(field);
        amount.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            public void afterTextChanged(Editable s) { render(); }
        });
        card.addView(amount);

        card.addView(label("विदेशी मुद्रा"));
        currency = new Spinner(activity);
        currency.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) { render(); }
            public void onNothingSelected(AdapterView<?> parent) {}
        });
        card.addView(currency);

        direction = text("विदेशी मुद्रा › NPR", 15, 0xFF0B5C28, true);
        direction.setGravity(Gravity.CENTER);
        direction.setPadding(dp(12), dp(10), dp(12), dp(10));
        direction.setBackground(chipBg(true));
        direction.setOnClickListener(v -> {
            toNpr = !toNpr;
            direction.setText(toNpr ? "विदेशी मुद्रा › NPR" : "NPR › विदेशी मुद्रा");
            render();
        });
        LinearLayout.LayoutParams dirLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dirLp.topMargin = dp(10);
        card.addView(direction, dirLp);

        LinearLayout rateRow = new LinearLayout(activity);
        rateRow.setOrientation(LinearLayout.HORIZONTAL);
        sellChip = rateChip("बिक्री दर", true);
        buyChip = rateChip("खरिद दर", false);
        sellChip.setOnClickListener(v -> setSell(true));
        buyChip.setOnClickListener(v -> setSell(false));
        rateRow.addView(sellChip, weight());
        rateRow.addView(buyChip, weight());
        LinearLayout.LayoutParams rateLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rateLp.topMargin = dp(8);
        card.addView(rateRow, rateLp);

        result = text("दर लोड हुँदैछ…", 26, 0xFF0B5C28, true);
        result.setPadding(0, dp(12), 0, 0);
        card.addView(result);
        formula = text("", 13, 0xFF78716C, false);
        card.addView(formula);
        status = text("नेपाल राष्ट्र बैंकबाट दर लोड हुँदैछ…", 13, 0xFF57534E, false);
        status.setPadding(0, dp(8), 0, 0);
        status.setOnClickListener(v -> load());
        card.addView(status);
        root.addView(card);

        LinearLayout list = card();
        list.addView(text("आजको आधिकारिक विनिमय दर", 18, 0xFF142033, true));
        table = new LinearLayout(activity);
        table.setOrientation(LinearLayout.VERTICAL);
        list.addView(table);
        TextView source = text("नेपाल राष्ट्र बैंकको आधिकारिक दर ›", 14, 0xFF0F8A3C, true);
        source.setPadding(0, dp(8), 0, 0);
        source.setOnClickListener(v -> activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.nrb.org.np/forex/"))));
        list.addView(source);
        root.addView(list);
    }

    View view() {
        return root;
    }

    void load() {
        activity.runOnUiThread(() -> status.setText("नेपाल राष्ट्र बैंकबाट दर लोड हुँदैछ…"));
        new Thread(() -> {
            String body = fetch();
            activity.runOnUiThread(() -> apply(body));
        }).start();
    }

    private void apply(String body) {
        rates.clear();
        if (body == null) {
            status.setText("दर आएन। थिचेर पुनः लोड गर्नुहोस्।");
            result.setText("दर उपलब्ध छैन");
            return;
        }
        try {
            JSONObject payload = new JSONObject(body).getJSONObject("payload");
            String date = payload.optString("date", "");
            JSONArray rows = payload.getJSONArray("rates");
            for (int i = 0; i < rows.length(); i++) {
                JSONObject row = rows.getJSONObject(i);
                JSONObject money = row.getJSONObject("currency");
                Rate rate = new Rate();
                rate.code = money.optString("iso3", "").trim().toUpperCase(Locale.US);
                rate.name = money.optString("name", rate.code);
                rate.unit = Math.max(1, money.optInt("unit", 1));
                rate.buy = row.optDouble("buy", 0);
                rate.sell = row.optDouble("sell", 0);
                if (!rate.code.isEmpty()) rates.add(rate);
            }
            rates.sort((a, b) -> Integer.compare(rank(a.code), rank(b.code)));
            List<String> labels = new ArrayList<>();
            int usd = 0;
            for (int i = 0; i < rates.size(); i++) {
                Rate rate = rates.get(i);
                labels.add(rate.code + " — " + rate.name);
                if ("USD".equals(rate.code)) usd = i;
            }
            ArrayAdapter<String> adapter = new ArrayAdapter<>(activity, android.R.layout.simple_spinner_dropdown_item, labels);
            currency.setAdapter(adapter);
            currency.setSelection(usd);
            status.setText("नेपाल राष्ट्र बैंकबाट प्राप्त · " + date);
            fillTable();
            render();
        } catch (Exception error) {
            status.setText("दर पढ्न सकिएन।");
            result.setText("दर उपलब्ध छैन");
        }
    }

    private void fillTable() {
        table.removeAllViews();
        LinearLayout head = row();
        head.addView(cell("मुद्रा", true, 1.4f));
        head.addView(cell("एकाइ", true, 0.6f));
        head.addView(cell("खरिद", true, 0.9f));
        head.addView(cell("बिक्री", true, 0.9f));
        table.addView(head);
        for (Rate rate : rates) {
            LinearLayout line = row();
            line.addView(cell(rate.code, false, 1.4f));
            line.addView(cell(String.valueOf(rate.unit), false, 0.6f));
            line.addView(cell(money(rate.buy), false, 0.9f));
            line.addView(cell(money(rate.sell), false, 0.9f));
            table.addView(line);
        }
    }

    private void render() {
        if (rates.isEmpty() || currency.getSelectedItemPosition() < 0 || currency.getSelectedItemPosition() >= rates.size()) return;
        Rate rate = rates.get(currency.getSelectedItemPosition());
        double value = 0;
        try {
            value = Math.max(0, Double.parseDouble(amount.getText().toString().trim()));
        } catch (Exception ignored) {
        }
        double used = sell ? rate.sell : rate.buy;
        if (used <= 0) {
            result.setText("दर उपलब्ध छैन");
            formula.setText("");
            return;
        }
        double out = toNpr ? (value / rate.unit) * used : (value * rate.unit) / used;
        String code = toNpr ? "NPR" : rate.code;
        result.setText((toNpr ? "रु. " : "") + money(out) + (toNpr ? "" : " " + code));
        formula.setText(toNpr
                ? trim(value) + " " + rate.code + " × रु. " + money(used) + " ÷ " + rate.unit
                : "रु. " + trim(value) + " ÷ रु. " + money(used) + " × " + rate.unit);
    }

    private void setSell(boolean next) {
        sell = next;
        sellChip.setBackground(chipBg(sell));
        sellChip.setTextColor(sell ? 0xFFFFFFFF : 0xFF0B5C28);
        buyChip.setBackground(chipBg(!sell));
        buyChip.setTextColor(!sell ? 0xFFFFFFFF : 0xFF0B5C28);
        render();
    }

    private TextView rateChip(String label, boolean active) {
        TextView chip = text(label, 14, active ? 0xFFFFFFFF : 0xFF0B5C28, true);
        chip.setGravity(Gravity.CENTER);
        chip.setPadding(dp(8), dp(8), dp(8), dp(8));
        chip.setBackground(chipBg(active));
        return chip;
    }

    private String fetch() {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL("https://kalaiyaonline.com/currency-converter.php?data=1").openConnection();
            connection.setRequestProperty("User-Agent", "Mozilla/5.0");
            connection.setRequestProperty("Accept", "application/json");
            connection.setConnectTimeout(12000);
            connection.setReadTimeout(15000);
            InputStream input = connection.getInputStream();
            StringBuilder body = new StringBuilder();
            byte[] buffer = new byte[4096];
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

    private int rank(String code) {
        for (int i = 0; i < ORDER.length; i++) if (ORDER[i].equals(code)) return i;
        return 999;
    }

    private String money(double value) {
        if (Math.abs(value - Math.rint(value)) < 0.001) return String.format(Locale.US, "%,.2f", value);
        return String.format(Locale.US, "%,.2f", value);
    }

    private String trim(double value) {
        if (value == Math.rint(value)) return String.format(Locale.US, "%,.0f", value);
        return String.format(Locale.US, "%,.2f", value);
    }

    private LinearLayout row() {
        LinearLayout line = new LinearLayout(activity);
        line.setOrientation(LinearLayout.HORIZONTAL);
        line.setPadding(0, dp(6), 0, dp(6));
        return line;
    }

    private TextView cell(String value, boolean bold, float weight) {
        TextView view = text(value, 13, bold ? 0xFF78716C : 0xFF1C1917, bold);
        view.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, weight));
        return view;
    }

    private TextView label(String value) {
        TextView view = text(value, 13, 0xFF57534E, false);
        view.setPadding(0, dp(8), 0, dp(4));
        return view;
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
        lp.rightMargin = dp(6);
        return lp;
    }

    private GradientDrawable chipBg(boolean active) {
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(14));
        bg.setColor(active ? 0xFF0F8A3C : 0xFFE5F6EC);
        return bg;
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

    private static final class Rate {
        String code = "";
        String name = "";
        int unit = 1;
        double buy;
        double sell;
    }
}
