package com.kalaiyaonline.news;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.text.Layout;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
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
import java.util.LinkedHashMap;

final class SocialBoard {
    private static final String HOME = "https://kalaiyaonline.com/social.php";
    private final Activity activity;
    private final LinearLayout root;
    private int generation;
    private String pageUrl = HOME;
    private boolean gated;

    SocialBoard(Activity activity) {
        this.activity = activity;
        root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
    }

    View view() {
        return root;
    }

    boolean waitingForLogin() {
        return gated;
    }

    void load() {
        open(HOME);
    }

    void open(String url) {
        int token = ++generation;
        pageUrl = url == null || url.isEmpty() ? HOME : url;
        root.removeAllViews();
        root.addView(line("समुदाय लोड हुँदैछ…", 15, 0xFF78716C, false));
        new Thread(() -> {
            String html = fetch("GET", pageUrl, null);
            if (!activity.isDestroyed()) activity.runOnUiThread(() -> {
                if (token == generation) render(html, pageUrl);
            });
        }).start();
    }

    private void render(String html, String url) {
        root.removeAllViews();
        pageUrl = url;
        Document doc = Jsoup.parse(html == null ? "" : html, url);
        boolean login = doc.selectFirst("input[name=mode][value=login]") != null
                && (doc.title() == null || doc.title().contains("लग"));
        gated = login && doc.selectFirst(".social-post, .social-composer, .user-row") == null;
        if (gated || html == null) {
            root.addView(card(loginCard(html == null)));
            return;
        }
        Element alert = doc.selectFirst(".social-alert");
        if (alert != null && !alert.text().isEmpty()) root.addView(line(alert.text(), 14, 0xFF0F8A3C, true));
        if (!HOME.equals(url)) root.addView(chip("फिड", HOME));
        Element side = doc.selectFirst(".social-side");
        if (side != null && !side.select("a").isEmpty()) root.addView(chips(side.select("a")));
        Element composer = doc.selectFirst(".social-composer");
        if (composer != null) {
            Element form = composer.selectFirst("form");
            if (form == null && "form".equals(composer.tagName())) form = composer;
            if (form != null) root.addView(card(composerBox(form)));
        }
        Element profile = doc.selectFirst(".profile-hero, .profile-edit, .social-profile-page");
        if (profile != null) root.addView(card(profileBox(doc)));
        for (Element row : doc.select(".notification-row")) root.addView(card(noteBox(row)));
        Element chat = doc.selectFirst(".social-chat-layout, .chat-box");
        if (chat != null) root.addView(card(chatBox(doc)));
        for (Element row : doc.select(".user-row, .user-card")) root.addView(card(personBox(row)));
        int posts = 0;
        for (Element post : doc.select(".social-post")) {
            root.addView(card(postBox(post)));
            posts++;
        }
        if (posts == 0 && doc.select(".user-row, .user-card, .notification-row, .chat-msg").isEmpty() && composer == null && profile == null) {
            root.addView(line("यस खण्डमा अहिले केही छैन।", 15, 0xFF78716C, false));
        }
    }

    private View loginCard(boolean offline) {
        LinearLayout box = new LinearLayout(activity);
        box.setOrientation(LinearLayout.VERTICAL);
        box.addView(line(offline ? "समुदाय आएन। नेट जाँच गर्नुहोस्।" : "समुदाय हेर्न आफ्नो खातामा लगइन गर्नुहोस्।", 16, 0xFF142033, true));
        box.addView(button(offline ? "फेरि प्रयास" : "लगइन / खाता", () -> {
            if (offline) load();
            else activity.startActivity(new Intent(activity, AccountActivity.class));
        }));
        return box;
    }

    private View composerBox(Element form) {
        LinearLayout box = new LinearLayout(activity);
        box.setOrientation(LinearLayout.VERTICAL);
        Element area = form.selectFirst("textarea");
        EditText input = field(area == null ? "के सोच्दै हुनुहुन्छ?" : hint(area, "के सोच्दै हुनुहुन्छ?"), true);
        box.addView(input);
        LinkedHashMap<String, String> fields = fieldsOf(form);
        Element select = form.selectFirst("select");
        if (select != null && select.select("option").size() > 1) box.addView(choices(select, fields));
        String key = area == null ? "" : area.attr("name");
        Element submit = form.selectFirst("button, input[type=submit]");
        box.addView(button(submit == null || submit.text().isEmpty() ? "पोस्ट" : submit.text(), () -> {
            if (!key.isEmpty()) fields.put(key, input.getText().toString().trim());
            if (!key.isEmpty() && fields.get(key).isEmpty()) return;
            send(form, submit, fields);
        }));
        return box;
    }

    private View postBox(Element post) {
        LinearLayout box = new LinearLayout(activity);
        box.setOrientation(LinearLayout.VERTICAL);
        String author = text(post.selectFirst(".social-post-head b"));
        String when = text(post.selectFirst(".social-post-head small"));
        if (!author.isEmpty()) box.addView(line(author, 16, 0xFF142033, true));
        if (!when.isEmpty()) box.addView(line(when, 12, 0xFF78716C, false));
        String body = text(post.selectFirst(".social-post-body"));
        if (!body.isEmpty()) box.addView(paragraph(body));
        String image = post.select(".social-post-image").attr("abs:src");
        if (!image.isEmpty()) box.addView(picture(image));
        LinearLayout tools = new LinearLayout(activity);
        tools.setOrientation(LinearLayout.HORIZONTAL);
        for (Element form : post.select(".social-post-tools form")) {
            Element button = form.selectFirst("button, input[type=submit]");
            if (button == null) continue;
            boolean liked = button.hasClass("liked") || form.selectFirst(".liked") != null;
            TextView action = line(button.text().isEmpty() ? "मन पराउनुहोस्" : button.text(), 14, liked ? 0xFFE11D48 : 0xFF0F8A3C, true);
            action.setPadding(0, dp(8), dp(14), dp(4));
            action.setOnClickListener(v -> send(form, button, fieldsOf(form)));
            tools.addView(action);
        }
        if (tools.getChildCount() > 0) box.addView(tools);
        for (Element comment : post.select(".comment-item")) {
            String said = comment.text().replaceAll("\\s+", " ").trim();
            if (!said.isEmpty()) box.addView(line(said, 14, 0xFF44403C, false));
        }
        Element reply = post.selectFirst(".social-comment-form");
        if (reply != null) box.addView(replyBox(reply));
        return box;
    }

    private View replyBox(Element form) {
        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        Element input = form.selectFirst("input[name], textarea");
        EditText field = field(input == null ? "कमेन्ट" : hint(input, "कमेन्ट"), false);
        row.addView(field, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        Element button = form.selectFirst("button, input[type=submit]");
        TextView send = line(button == null || button.text().isEmpty() ? "पठाउनुहोस्" : button.text(), 14, 0xFFFFFFFF, true);
        send.setBackground(pill(0xFFF15A22));
        send.setPadding(dp(12), dp(10), dp(12), dp(10));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.leftMargin = dp(8);
        send.setLayoutParams(lp);
        send.setOnClickListener(v -> {
            LinkedHashMap<String, String> fields = fieldsOf(form);
            if (input != null && !input.attr("name").isEmpty()) fields.put(input.attr("name"), field.getText().toString().trim());
            if (input != null && !input.attr("name").isEmpty() && fields.get(input.attr("name")).isEmpty()) return;
            send(form, button, fields);
        });
        row.addView(send);
        return row;
    }

    private View personBox(Element row) {
        LinearLayout box = new LinearLayout(activity);
        box.setOrientation(LinearLayout.VERTICAL);
        String name = text(row.selectFirst("b"));
        if (name.isEmpty()) name = text(row.selectFirst("a"));
        String meta = text(row.selectFirst("small"));
        if (!name.isEmpty()) box.addView(line(name, 16, 0xFF142033, true));
        if (!meta.isEmpty() && !meta.equals(name)) box.addView(line(meta, 13, 0xFF78716C, false));
        Element link = row.selectFirst("a[href]");
        if (link != null) {
            String href = link.attr("abs:href");
            if (!href.isEmpty()) box.setOnClickListener(v -> open(href));
        }
        for (Element form : row.select("form")) {
            Element button = form.selectFirst("button, input[type=submit]");
            if (button == null) continue;
            box.addView(button(button.text().isEmpty() ? "पठाउनुहोस्" : button.text(), () -> send(form, button, fieldsOf(form))));
        }
        return box;
    }

    private View noteBox(Element row) {
        LinearLayout box = new LinearLayout(activity);
        box.setOrientation(LinearLayout.VERTICAL);
        box.addView(paragraph(row.text()));
        Element link = row.selectFirst("a[href]");
        if (link != null) box.setOnClickListener(v -> open(link.attr("abs:href")));
        return box;
    }

    private View chatBox(Document doc) {
        LinearLayout box = new LinearLayout(activity);
        box.setOrientation(LinearLayout.VERTICAL);
        HorizontalScrollView people = chips(doc.select(".chat-user"));
        if (people.getChildCount() > 0) box.addView(people);
        for (Element msg : doc.select(".chat-msg")) {
            String said = msg.text().replaceAll("\\s+", " ").trim();
            if (!said.isEmpty()) box.addView(line(said, 15, 0xFF292524, false));
        }
        Element form = doc.selectFirst(".chat-form");
        if (form != null) box.addView(replyBox(form));
        else {
            Element empty = doc.selectFirst(".chat-empty");
            if (empty != null) box.addView(line(empty.text(), 14, 0xFF78716C, false));
        }
        return box;
    }

    private View profileBox(Document doc) {
        LinearLayout box = new LinearLayout(activity);
        box.setOrientation(LinearLayout.VERTICAL);
        String name = text(doc.selectFirst(".profile-hero h1, .social-profile-mini h3"));
        String about = text(doc.selectFirst(".profile-hero p, .social-profile-mini small"));
        if (!name.isEmpty()) box.addView(line(name, 22, 0xFF142033, true));
        if (!about.isEmpty()) box.addView(line(about, 14, 0xFF78716C, false));
        Element stats = doc.selectFirst(".profile-stats, .social-stat");
        if (stats != null && !stats.text().isEmpty()) box.addView(line(stats.text(), 14, 0xFF0F8A3C, true));
        Element edit = doc.selectFirst(".profile-edit form, form.profile-edit");
        if (edit != null) {
            for (Element input : edit.select("input[name], textarea[name]")) {
                String type = input.attr("type").toLowerCase();
                if ("hidden".equals(type) || "submit".equals(type)) continue;
                EditText field = field(hint(input, input.attr("name")), "textarea".equals(input.tagName()));
                if (!input.val().isEmpty() && !"password".equals(type)) field.setText(input.val());
                box.addView(field);
            }
            Element button = edit.selectFirst("button, input[type=submit]");
            box.addView(button(button == null || button.text().isEmpty() ? "सेभ" : button.text(), () -> {
                LinkedHashMap<String, String> fields = fieldsOf(edit);
                int n = 0;
                for (Element input : edit.select("input[name], textarea[name]")) {
                    String type = input.attr("type").toLowerCase();
                    if ("hidden".equals(type) || "submit".equals(type)) continue;
                    View child = box.getChildAt(n + 3);
                    n++;
                    if (child instanceof EditText) fields.put(input.attr("name"), ((EditText) child).getText().toString().trim());
                }
                send(edit, button, fields);
            }));
        }
        return box;
    }

    private HorizontalScrollView chips(org.jsoup.select.Elements links) {
        HorizontalScrollView scroll = new HorizontalScrollView(activity);
        scroll.setHorizontalScrollBarEnabled(false);
        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, dp(4), 0, dp(8));
        for (Element link : links) {
            String href = link.attr("abs:href");
            String label = link.text().replaceAll("\\s+", " ").trim();
            if (href.isEmpty() || label.isEmpty()) continue;
            TextView chip = line(label, 13, 0xFF0F8A3C, true);
            chip.setBackground(pill(0xFFE7F6EE));
            chip.setPadding(dp(12), dp(8), dp(12), dp(8));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.rightMargin = dp(8);
            chip.setLayoutParams(lp);
            chip.setOnClickListener(v -> open(href));
            row.addView(chip);
        }
        scroll.addView(row);
        return scroll;
    }

    private TextView chip(String label, String url) {
        TextView view = line(label, 14, 0xFF0F8A3C, true);
        view.setPadding(0, 0, 0, dp(8));
        view.setOnClickListener(v -> open(url));
        return view;
    }

    private View choices(Element select, LinkedHashMap<String, String> fields) {
        String name = select.attr("name");
        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.HORIZONTAL);
        TextView[] views = new TextView[select.select("option").size()];
        int i = 0;
        for (Element option : select.select("option")) {
            TextView chip = line(option.text(), 13, 0xFF0F8A3C, true);
            chip.setBackground(pill(option.hasAttr("selected") ? 0xFF0F8A3C : 0xFFE7F6EE));
            chip.setTextColor(option.hasAttr("selected") ? 0xFFFFFFFF : 0xFF0F8A3C);
            chip.setPadding(dp(10), dp(6), dp(10), dp(6));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.rightMargin = dp(6);
            lp.bottomMargin = dp(8);
            chip.setLayoutParams(lp);
            int index = i;
            views[i] = chip;
            chip.setOnClickListener(v -> {
                fields.put(name, option.attr("value"));
                for (int n = 0; n < views.length; n++) {
                    views[n].setBackground(pill(n == index ? 0xFF0F8A3C : 0xFFE7F6EE));
                    views[n].setTextColor(n == index ? 0xFFFFFFFF : 0xFF0F8A3C);
                }
            });
            row.addView(chip);
            i++;
        }
        return row;
    }

    private void send(Element form, Element button, LinkedHashMap<String, String> fields) {
        if (button != null && button.hasAttr("name") && !button.attr("name").isEmpty()) {
            fields.put(button.attr("name"), button.attr("value"));
        }
        String action = form.hasAttr("action") && !form.attr("action").isEmpty() ? form.attr("abs:action") : pageUrl;
        String method = form.attr("method").toUpperCase();
        if (!"GET".equals(method)) method = "POST";
        int token = ++generation;
        root.removeAllViews();
        root.addView(line("पठाउँदैछ…", 15, 0xFF78716C, false));
        String verb = method;
        new Thread(() -> {
            String body = "GET".equals(verb) ? null : encode(fields);
            String target = action;
            if ("GET".equals(verb)) target = action + (action.contains("?") ? "&" : "?") + encode(fields);
            String html = fetch(verb, target, body);
            String next = target;
            if (!activity.isDestroyed()) activity.runOnUiThread(() -> {
                if (token == generation) render(html, "GET".equals(verb) ? next : pageUrl);
            });
        }).start();
    }

    private LinkedHashMap<String, String> fieldsOf(Element form) {
        LinkedHashMap<String, String> fields = new LinkedHashMap<>();
        for (Element input : form.select("input[name]")) {
            String type = input.attr("type").toLowerCase();
            if ("submit".equals(type) || "button".equals(type) || "file".equals(type) || "image".equals(type)) continue;
            if (("checkbox".equals(type) || "radio".equals(type)) && !input.hasAttr("checked")) continue;
            fields.put(input.attr("name"), input.attr("value"));
        }
        for (Element select : form.select("select[name]")) {
            Element chosen = select.selectFirst("option[selected]");
            if (chosen == null) chosen = select.selectFirst("option");
            if (chosen != null) fields.put(select.attr("name"), chosen.hasAttr("value") ? chosen.attr("value") : chosen.text());
        }
        return fields;
    }

    private String fetch(String method, String url, String body) {
        String current = url;
        String verb = method;
        String send = body;
        for (int hop = 0; hop < 5; hop++) {
            HttpURLConnection connection = null;
            try {
                connection = (HttpURLConnection) new URL(current).openConnection();
                connection.setInstanceFollowRedirects(false);
                connection.setConnectTimeout(30000);
                connection.setReadTimeout(hop == 0 ? 45000 : 70000);
                connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 Chrome/131.0.0.0 Mobile Safari/537.36");
                connection.setRequestProperty("Referer", HOME);
                Account.apply(connection);
                if ("POST".equals(verb) && send != null) {
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
                    if (location == null || location.isEmpty()) return null;
                    current = location.startsWith("http") ? location : "https://kalaiyaonline.com" + (location.startsWith("/") ? location : "/" + location);
                    verb = "GET";
                    send = null;
                    continue;
                }
                InputStream input = code >= 400 ? connection.getErrorStream() : connection.getInputStream();
                return read(input);
            } catch (Exception ignored) {
                if (hop == 0) continue;
                return null;
            } finally {
                if (connection != null) connection.disconnect();
            }
        }
        return null;
    }

    private View picture(String url) {
        ImageView image = new ImageView(activity);
        image.setAdjustViewBounds(true);
        image.setScaleType(ImageView.ScaleType.CENTER_CROP);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(180));
        lp.topMargin = dp(8);
        lp.bottomMargin = dp(8);
        image.setLayoutParams(lp);
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(12));
        bg.setColor(0xFFE7F6EE);
        image.setBackground(bg);
        new Thread(() -> {
            android.graphics.Bitmap bitmap = Bitmaps.load(url, 900);
            if (bitmap != null && !activity.isDestroyed()) activity.runOnUiThread(() -> image.setImageBitmap(bitmap));
        }).start();
        return image;
    }

    private View card(View child) {
        LinearLayout card = new LinearLayout(activity);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(14), dp(12), dp(14), dp(12));
        GradientDrawable bg = new GradientDrawable();
        boolean dark = Night.on(activity);
        bg.setColor(dark ? 0xFF1A2420 : 0xFFFFFFFF);
        bg.setCornerRadius(dp(16));
        bg.setStroke(dp(1), dark ? 0xFF2C3A34 : 0xFFE6EBE8);
        card.setBackground(bg);
        card.addView(child);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(10);
        card.setLayoutParams(lp);
        return card;
    }

    private View button(String label, Runnable action) {
        TextView view = line(label, 15, 0xFFFFFFFF, true);
        view.setGravity(Gravity.CENTER);
        view.setBackground(pill(0xFF0F8A3C));
        view.setPadding(dp(12), dp(10), dp(12), dp(10));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(8);
        view.setLayoutParams(lp);
        view.setOnClickListener(v -> action.run());
        return view;
    }

    private EditText field(String hint, boolean multi) {
        boolean dark = Night.on(activity);
        EditText input = new EditText(activity);
        input.setHint(hint);
        input.setMinLines(multi ? 3 : 1);
        input.setSingleLine(!multi);
        input.setTextColor(dark ? 0xFFF4F7F5 : 0xFF1C1917);
        input.setHintTextColor(0xFF9AA89F);
        style(input, 16, false);
        input.setPadding(dp(12), dp(10), dp(12), dp(10));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(dark ? 0xFF101412 : 0xFFF7FAF8);
        bg.setCornerRadius(dp(12));
        bg.setStroke(dp(1), 0xFFE3EBE6);
        input.setBackground(bg);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(8);
        input.setLayoutParams(lp);
        return input;
    }

    private TextView paragraph(String value) {
        TextView view = line(value, 16, Night.on(activity) ? 0xFFE7EEEA : 0xFF292524, false);
        if (Build.VERSION.SDK_INT >= 26) view.setJustificationMode(Layout.JUSTIFICATION_MODE_INTER_WORD);
        return view;
    }

    private TextView line(String value, int size, int color, boolean bold) {
        TextView view = new TextView(activity);
        view.setText(value);
        view.setTextColor(color);
        style(view, size, bold);
        return view;
    }

    private void style(TextView view, int size, boolean bold) {
        view.setTextSize(TypedValue.COMPLEX_UNIT_DIP, size);
        Typeface face = ResourcesCompat.getFont(activity, bold ? R.font.mukta_bold : R.font.mukta_regular);
        if (face != null) view.setTypeface(face);
    }

    private GradientDrawable pill(int color) {
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(color);
        bg.setCornerRadius(dp(14));
        return bg;
    }

    private String hint(Element element, String fallback) {
        String hint = element.attr("placeholder");
        return hint.isEmpty() ? fallback : hint;
    }

    private String text(Element element) {
        return element == null ? "" : element.text().replaceAll("\\s+", " ").trim();
    }

    private String encode(LinkedHashMap<String, String> fields) {
        StringBuilder body = new StringBuilder();
        for (java.util.Map.Entry<String, String> entry : fields.entrySet()) {
            if (body.length() > 0) body.append('&');
            body.append(url(entry.getKey())).append('=').append(url(entry.getValue()));
        }
        return body.toString();
    }

    private String url(String value) {
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
