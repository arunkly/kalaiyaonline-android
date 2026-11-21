package com.kalaiyaonline.news;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.res.ResourcesCompat;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

final class ContactBoard {
    private static final String PAGE = "https://kalaiyaonline.com/page/contact-us";
    private static final String SUBMIT = "https://kalaiyaonline.com/contact_submit.php";
    private final Activity activity;
    private final LinearLayout root;
    private int generation;
    private String csrf = "";
    private String startedAt = "";
    private EditText name;
    private EditText address;
    private EditText phone;
    private EditText email;
    private EditText message;
    private TextView status;
    private boolean busy;

    ContactBoard(Activity activity) {
        this.activity = activity;
        root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
    }

    View view() {
        return root;
    }

    void load() {
        int token = ++generation;
        root.removeAllViews();
        root.addView(line("सम्पर्क लोड हुँदैछ…", 15, 0xFF78716C, false, 0));
        new Thread(() -> {
            String html = Net.text(PAGE);
            if (!activity.isDestroyed()) activity.runOnUiThread(() -> {
                if (token == generation) render(html);
            });
        }).start();
    }

    private void render(String html) {
        root.removeAllViews();
        Document doc = Jsoup.parse(html == null ? "" : html, PAGE);
        Element page = doc.selectFirst(".koc-page");
        if (page == null) {
            root.addView(line("सम्पर्क पृष्ठ आएन। नेट जाँच गरेर फेरि खोल्नुहोस्।", 15, 0xFFB42318, false, 0));
            return;
        }
        boolean dark = Night.on(activity);
        Element hero = page.selectFirst(".koc-hero p");
        if (hero != null && !hero.text().isEmpty()) root.addView(card(line(hero.text(), 15, dark ? 0xFFE7EEEA : 0xFF292524, false, 0), dark));
        LinearLayout details = new LinearLayout(activity);
        details.setOrientation(LinearLayout.VERTICAL);
        Element head = page.selectFirst(".koc-card h2");
        if (head != null) details.addView(line(head.text(), 18, dark ? 0xFFF4F7F5 : 0xFF142033, true, dp(8)));
        for (Element row : page.select(".koc-contact-row")) {
            String label = text(row.selectFirst("small"));
            Element link = row.selectFirst("a");
            String value = link != null ? link.text() : text(row.selectFirst("strong"));
            String href = link == null ? "" : link.attr("href");
            String copy = row.select("button[data-copy]").attr("data-copy");
            if (!value.isEmpty()) details.addView(infoRow(label, value, href, copy, dark));
        }
        Element socials = page.selectFirst(".koc-socials");
        if (socials != null && !socials.select("a[href]").isEmpty()) details.addView(socials(socials));
        Element map = page.selectFirst(".koc-map-head a");
        if (map != null && !map.attr("href").isEmpty()) details.addView(linkButton(map.text().isEmpty() ? "नक्सा हेर्नुहोस्" : map.text(), map.attr("abs:href")));
        root.addView(card(details, dark));

        Element form = page.selectFirst(".koc-form");
        if (form == null) return;
        csrf = form.select("input[name=csrf]").attr("value");
        startedAt = form.select("input[name=started_at]").attr("value");
        LinearLayout box = new LinearLayout(activity);
        box.setOrientation(LinearLayout.VERTICAL);
        Element formHead = form.parent() == null ? null : form.parent().selectFirst("h2");
        box.addView(line(formHead == null ? "हामीलाई सन्देश पठाउनुहोस्" : formHead.text(), 18, dark ? 0xFFF4F7F5 : 0xFF142033, true, dp(4)));
        status = line("", 14, 0xFF0F8A3C, false, dp(8));
        status.setVisibility(View.GONE);
        box.addView(status);
        name = field("नाम", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PERSON_NAME);
        address = field("ठेगाना", InputType.TYPE_CLASS_TEXT);
        phone = field("मोबाइल नम्बर", InputType.TYPE_CLASS_PHONE);
        email = field("इमेल", InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        message = field("सन्देश", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        message.setSingleLine(false);
        message.setMinLines(4);
        box.addView(name);
        box.addView(address);
        box.addView(phone);
        box.addView(email);
        box.addView(message);
        box.addView(linkButton("पठाउनुहोस्", null));
        box.addView(line("तपाईंको जानकारी गोपनीय राखिनेछ।", 12, 0xFF78716C, false, 0));
        root.addView(card(box, dark));
    }

    private View infoRow(String label, String value, String href, String copy, boolean dark) {
        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(0, dp(8), 0, dp(8));
        if (!label.isEmpty()) row.addView(line(label, 12, 0xFF78716C, false, 0));
        TextView main = line(value, 16, 0xFF0F8A3C, true, 0);
        row.addView(main);
        if (href.startsWith("tel:")) main.setOnClickListener(v -> activity.startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse(href))));
        else if (href.startsWith("mailto:")) main.setOnClickListener(v -> activity.startActivity(new Intent(Intent.ACTION_SENDTO, Uri.parse(href))));
        if (!copy.isEmpty()) {
            TextView copyBtn = line("कपी", 12, 0xFFF15A22, true, 0);
            copyBtn.setOnClickListener(v -> {
                ClipboardManager clipboard = (ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
                if (clipboard != null) clipboard.setPrimaryClip(ClipData.newPlainText("contact", copy));
                copyBtn.setText("कपी भयो");
            });
            row.addView(copyBtn);
        }
        return row;
    }

    private View socials(Element box) {
        HorizontalScrollView scroll = new HorizontalScrollView(activity);
        scroll.setHorizontalScrollBarEnabled(false);
        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, dp(8), 0, dp(4));
        for (Element link : box.select("a[href]")) {
            String href = link.attr("abs:href");
            String label = link.attr("aria-label");
            if (label.isEmpty()) label = link.text();
            if (href.isEmpty() || label.isEmpty()) continue;
            TextView chip = line(label, 13, 0xFF0F8A3C, true, 0);
            chip.setPadding(dp(12), dp(8), dp(12), dp(8));
            GradientDrawable bg = new GradientDrawable();
            bg.setColor(0xFFE7F6EE);
            bg.setCornerRadius(dp(16));
            chip.setBackground(bg);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.rightMargin = dp(8);
            chip.setLayoutParams(lp);
            chip.setOnClickListener(v -> activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(href))));
            row.addView(chip);
        }
        scroll.addView(row);
        return scroll;
    }

    private View linkButton(String label, String url) {
        TextView view = line(label, 15, 0xFFFFFFFF, true, 0);
        view.setGravity(Gravity.CENTER);
        view.setIncludeFontPadding(false);
        view.setPadding(dp(14), dp(8), dp(14), dp(8));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xFF0F8A3C);
        bg.setCornerRadius(dp(12));
        view.setBackground(bg);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(8);
        lp.bottomMargin = dp(8);
        view.setLayoutParams(lp);
        view.setOnClickListener(v -> {
            if (url != null) activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
            else send();
        });
        return view;
    }

    private void send() {
        if (busy) return;
        String person = value(name);
        String place = value(address);
        String mobile = value(phone);
        String mail = value(email);
        String note = value(message);
        if (person.isEmpty() || place.isEmpty() || mobile.isEmpty() || !mail.contains("@") || note.length() < 5) {
            flash("सबै विवरण लेख्नुहोस्। सन्देश कम्तीमा ५ अक्षरको होस्।", false);
            return;
        }
        busy = true;
        flash("पठाउँदैछ…", true);
        String body = form("csrf", csrf, "started_at", startedAt, "website", "", "name", person, "address", place, "phone", mobile, "email", mail, "message", note);
        new Thread(() -> {
            String html = post(body);
            Document doc = Jsoup.parse(html == null ? "" : html);
            Element alert = doc.selectFirst(".koc-alert");
            boolean ok = alert != null && alert.className().contains("success");
            String messageText = alert != null ? alert.text() : "";
            if (messageText.isEmpty() && html != null && html.contains("Invalid request token")) messageText = "पृष्ठ पुरानो भयो। फेरि खोलेर पठाउनुहोस्।";
            if (messageText.isEmpty()) messageText = ok ? "सन्देश पठाइयो।" : "सन्देश पठाउन सकिएन।";
            String shown = messageText;
            if (!activity.isDestroyed()) activity.runOnUiThread(() -> {
                busy = false;
                flash(shown, ok);
                if (ok) {
                    name.setText("");
                    address.setText("");
                    phone.setText("");
                    email.setText("");
                    message.setText("");
                }
            });
        }).start();
    }

    private String post(String body) {
        String current = SUBMIT;
        String verb = "POST";
        String send = body;
        for (int hop = 0; hop < 4; hop++) {
            HttpURLConnection connection = null;
            try {
                connection = (HttpURLConnection) new URL(current).openConnection();
                connection.setInstanceFollowRedirects(false);
                connection.setConnectTimeout(30000);
                connection.setReadTimeout(45000);
                connection.setRequestProperty("User-Agent", "Mozilla/5.0");
                connection.setRequestProperty("Referer", PAGE);
                Account.apply(connection);
                if ("POST".equals(verb)) {
                    byte[] bytes = send.getBytes(StandardCharsets.UTF_8);
                    connection.setRequestMethod("POST");
                    connection.setDoOutput(true);
                    connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
                    connection.setFixedLengthStreamingMode(bytes.length);
                    OutputStream output = connection.getOutputStream();
                    output.write(bytes);
                    output.close();
                }
                int code = connection.getResponseCode();
                Account.capture(connection);
                if (code >= 300 && code < 400) {
                    String location = connection.getHeaderField("Location");
                    if (location == null || location.isEmpty()) return "";
                    current = location.startsWith("http") ? location : "https://kalaiyaonline.com" + (location.startsWith("/") ? location : "/" + location);
                    verb = "GET";
                    continue;
                }
                InputStream input = code >= 400 ? connection.getErrorStream() : connection.getInputStream();
                return read(input);
            } catch (Exception ignored) {
                return null;
            } finally {
                if (connection != null) connection.disconnect();
            }
        }
        return null;
    }

    private View card(View child, boolean dark) {
        LinearLayout card = new LinearLayout(activity);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(14), dp(16), dp(14));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(dark ? 0xFF1A2420 : 0xFFFFFFFF);
        bg.setCornerRadius(dp(16));
        bg.setStroke(dp(1), dark ? 0xFF2C3A34 : 0xFFE6EBE8);
        card.setBackground(bg);
        card.addView(child);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(12);
        card.setLayoutParams(lp);
        return card;
    }

    private EditText field(String hint, int type) {
        boolean dark = Night.on(activity);
        EditText input = new EditText(activity);
        input.setHint(hint);
        input.setInputType(type);
        input.setSingleLine(true);
        input.setTextColor(dark ? 0xFFF4F7F5 : 0xFF1C1917);
        input.setHintTextColor(0xFF9AA89F);
        style(input, 16, false);
        input.setPadding(dp(14), dp(12), dp(14), dp(12));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(dark ? 0xFF101412 : 0xFFF7FAF8);
        bg.setCornerRadius(dp(12));
        bg.setStroke(dp(1), dark ? 0xFF2C3A34 : 0xFFE3EBE6);
        input.setBackground(bg);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(8);
        input.setLayoutParams(lp);
        return input;
    }

    private void flash(String text, boolean ok) {
        if (status == null) return;
        status.setText(text);
        status.setTextColor(ok ? 0xFF0F8A3C : 0xFFB42318);
        status.setVisibility(View.VISIBLE);
    }

    private TextView line(String value, int size, int color, boolean bold, int gap) {
        TextView view = new TextView(activity);
        view.setText(value);
        view.setTextColor(color);
        view.setTextSize(TypedValue.COMPLEX_UNIT_DIP, size);
        style(view, size, bold);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = gap;
        view.setLayoutParams(lp);
        return view;
    }

    private void style(TextView view, int size, boolean bold) {
        view.setTextSize(TypedValue.COMPLEX_UNIT_DIP, size);
        Typeface face = ResourcesCompat.getFont(activity, bold ? R.font.mukta_bold : R.font.mukta_regular);
        if (face != null) view.setTypeface(face);
    }

    private String value(EditText input) {
        return input == null || input.getText() == null ? "" : input.getText().toString().trim();
    }

    private String text(Element element) {
        return element == null ? "" : element.text().replaceAll("\\s+", " ").trim();
    }

    private String form(String... pairs) {
        StringBuilder body = new StringBuilder();
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            if (body.length() > 0) body.append('&');
            body.append(encode(pairs[i])).append('=').append(encode(pairs[i + 1]));
        }
        return body.toString();
    }

    private String encode(String value) {
        try {
            return URLEncoder.encode(value == null ? "" : value, "UTF-8");
        } catch (Exception ignored) {
            return "";
        }
    }

    private String read(InputStream input) throws Exception {
        if (input == null) return "";
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int n;
        while ((n = input.read(buffer)) != -1) bytes.write(buffer, 0, n);
        input.close();
        return bytes.toString("UTF-8");
    }

    private int dp(int value) {
        return Math.round(value * activity.getResources().getDisplayMetrics().density);
    }
}
