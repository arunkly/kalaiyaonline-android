package com.kalaiyaonline.news;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.text.InputType;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
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
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class BloodBoard {
    private static final String[] GROUPS = {"", "A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"};
    private static final String[] DISTRICTS = {"", "सप्तरी जिल्ला", "सिरहा जिल्ला", "धनुषा जिल्ला", "महोत्तरी जिल्ला", "सर्लाही जिल्ला", "रौतहट जिल्ला", "बारा जिल्ला", "पर्सा जिल्ला"};

    private final AppCompatActivity activity;
    private final LinearLayout root;
    private final LinearLayout listPane;
    private final LinearLayout detailPane;
    private final LinearLayout results;
    private final EditText query;
    private final Spinner group;
    private final Spinner district;
    private final TextView status;
    private final LinearLayout joinPane;
    private boolean pendingJoin;

    BloodBoard(AppCompatActivity activity) {
        this.activity = activity;
        root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        TextView intro = text("उपलब्ध रक्तदाता खोज्नुहोस्। रक्तदाता बन्नुहोस् बाट एपमै दर्ता हुन्छ।", 14, 0xFF57534E, false);
        root.addView(intro);
        root.addView(button("रक्तदाता बन्नुहोस्", true, this::become));
        joinPane = new LinearLayout(activity);
        joinPane.setOrientation(LinearLayout.VERTICAL);
        joinPane.setVisibility(View.GONE);
        root.addView(joinPane);

        listPane = new LinearLayout(activity);
        listPane.setOrientation(LinearLayout.VERTICAL);
        query = new EditText(activity);
        query.setHint("नाम वा स्थान…");
        query.setTextColor(0xFF1C1917);
        query.setPadding(dp(12), dp(10), dp(12), dp(10));
        query.setBackground(field());
        LinearLayout.LayoutParams gap = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        gap.topMargin = dp(8);
        listPane.addView(query, gap);
        group = spinner(labels("रक्त समूह", GROUPS));
        district = spinner(labels("सबै जिल्ला", DISTRICTS));
        listPane.addView(group);
        listPane.addView(district);
        listPane.addView(button("खोज्नुहोस्", true, this::search));
        status = text("", 13, 0xFF57534E, false);
        listPane.addView(status);
        results = new LinearLayout(activity);
        results.setOrientation(LinearLayout.VERTICAL);
        listPane.addView(results);
        root.addView(listPane);

        detailPane = new LinearLayout(activity);
        detailPane.setOrientation(LinearLayout.VERTICAL);
        detailPane.setVisibility(View.GONE);
        root.addView(detailPane);
    }

    View view() {
        return root;
    }

    void load() {
        showList();
        search();
    }

    void resume() {
        if (pendingJoin && Account.loggedIn()) {
            pendingJoin = false;
            become();
        }
    }

    private void become() {
        if (!Account.loggedIn()) {
            pendingJoin = true;
            joinPane.removeAllViews();
            joinPane.addView(text("रक्तदाता बन्न पहिले एपको खातामा लगइन गर्नुहोस्।", 15, 0xFF142033, true));
            joinPane.addView(button("लगइन / खाता", true, () -> activity.startActivity(new Intent(activity, AccountActivity.class))));
            joinPane.addView(button("‹ सूची", false, this::showList));
            listPane.setVisibility(View.GONE);
            detailPane.setVisibility(View.GONE);
            joinPane.setVisibility(View.VISIBLE);
            return;
        }
        listPane.setVisibility(View.GONE);
        detailPane.setVisibility(View.GONE);
        joinPane.setVisibility(View.VISIBLE);
        joinPane.removeAllViews();
        joinPane.addView(button("‹ सूची", false, this::showList));
        joinPane.addView(text("दर्ता फारम लोड हुँदैछ…", 14, 0xFF57534E, false));
        new Thread(() -> {
            String html = request("GET", "https://kalaiyaonline.com/?blood=all", null, null);
            if (!activity.isDestroyed()) activity.runOnUiThread(() -> renderJoin(html));
        }).start();
    }

    private void renderJoin(String html) {
        joinPane.removeAllViews();
        joinPane.addView(button("‹ सूची", false, this::showList));
        Document doc = Jsoup.parse(html == null ? "" : html, "https://kalaiyaonline.com/?blood=all");
        Element form = doc.selectFirst("form.donor-manage-form, form.donor-register-form, .donor-details-manager form");
        if (form == null) {
            joinPane.addView(text("दर्ता फारम आएन। खाताबाट फेरि लगइन गरेर प्रयास गर्नुहोस्।", 15, 0xFFB42318, false));
            joinPane.addView(button("लगइन", true, () -> activity.startActivity(new Intent(activity, AccountActivity.class))));
            return;
        }
        Element title = form.parent() == null ? null : form.parent().selectFirst("h1, h2, h3, b");
        joinPane.addView(text(title == null || title.text().isEmpty() ? "रक्तदाता बन्नुहोस्" : title.text(), 18, 0xFF142033, true));
        List<Field> fields = new ArrayList<>();
        boolean file = form.selectFirst("input[type=file]") != null;
        for (Element input : form.select("input, textarea, select")) {
            String name = input.attr("name");
            if (name.isEmpty()) continue;
            String type = input.attr("type").toLowerCase();
            if ("hidden".equals(type)) {
                fields.add(Field.hidden(name, input.val()));
                continue;
            }
            if ("submit".equals(type) || "button".equals(type) || "file".equals(type) || "image".equals(type)) continue;
            String label = labelOf(input);
            if ("select".equals(input.tagName())) {
                List<String> labels = new ArrayList<>();
                List<String> values = new ArrayList<>();
                int selected = 0;
                int index = 0;
                for (Element option : input.select("option")) {
                    labels.add(option.text());
                    values.add(option.hasAttr("value") ? option.attr("value") : option.text());
                    if (option.hasAttr("selected")) selected = index;
                    index++;
                }
                if (labels.isEmpty()) continue;
                Spinner spinner = spinner(labels.toArray(new String[0]));
                spinner.setSelection(selected);
                joinPane.addView(text(label, 13, 0xFF44403C, true));
                joinPane.addView(spinner);
                fields.add(Field.choice(name, spinner, values));
            } else if ("checkbox".equals(type) || "radio".equals(type)) {
                boolean[] on = {input.hasAttr("checked")};
                TextView box = text((on[0] ? "☑ " : "☐ ") + label, 15, 0xFF142033, false);
                box.setPadding(0, dp(8), 0, dp(4));
                box.setOnClickListener(v -> {
                    on[0] = !on[0];
                    box.setText((on[0] ? "☑ " : "☐ ") + label);
                });
                joinPane.addView(box);
                fields.add(Field.check(name, input.val().isEmpty() ? "1" : input.val(), on));
            } else {
                EditText edit = new EditText(activity);
                edit.setHint(input.attr("placeholder").isEmpty() ? label : input.attr("placeholder"));
                edit.setText(input.val());
                edit.setTextColor(0xFF1C1917);
                edit.setPadding(dp(12), dp(10), dp(12), dp(10));
                edit.setBackground(field());
                edit.setSingleLine(!"textarea".equals(input.tagName()));
                if ("textarea".equals(input.tagName())) edit.setMinLines(3);
                if ("email".equals(type)) edit.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
                else if ("tel".equals(type)) edit.setInputType(InputType.TYPE_CLASS_PHONE);
                else if ("number".equals(type)) edit.setInputType(InputType.TYPE_CLASS_NUMBER);
                else if ("date".equals(type)) edit.setHint(label + " (YYYY-MM-DD)");
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                lp.topMargin = dp(4);
                lp.bottomMargin = dp(6);
                edit.setLayoutParams(lp);
                joinPane.addView(text(label, 13, 0xFF44403C, true));
                joinPane.addView(edit);
                fields.add(Field.text(name, edit));
            }
        }
        if (file) joinPane.addView(text("फोटो अहिले चाहिँदैन। बाँकी विवरण पठाए पुग्छ।", 12, 0xFF78716C, false));
        Element submit = form.selectFirst("button[type=submit], input[type=submit], button");
        String label = submit == null || submit.text().trim().isEmpty() ? "दर्ता गर्नुहोस्" : submit.text().trim();
        boolean multi = form.attr("enctype").toLowerCase().contains("multipart");
        String action = form.hasAttr("action") && !form.attr("action").isEmpty() ? form.absUrl("action") : "https://kalaiyaonline.com/?blood=all";
        if (submit != null && submit.hasAttr("name") && !submit.attr("name").isEmpty()) fields.add(Field.hidden(submit.attr("name"), submit.attr("value")));
        joinPane.addView(button(label, true, () -> sendJoin(action, fields, multi)));
    }

    private void sendJoin(String action, List<Field> fields, boolean multi) {
        joinPane.addView(text("पठाउँदैछ…", 14, 0xFF57534E, false));
        new Thread(() -> {
            String page = multi ? request("POST", action, null, fields) : request("POST", action, encodeFields(fields), null);
            String shown = messageOf(page);
            if (!activity.isDestroyed()) activity.runOnUiThread(() -> {
                joinPane.removeAllViews();
                joinPane.addView(button("‹ सूची", false, () -> {
                    showList();
                    search();
                }));
                joinPane.addView(text(shown, 16, 0xFF142033, true));
            });
        }).start();
    }

    private String request(String method, String url, String body, List<Field> multi) {
        String current = url;
        String verb = method;
        String send = body;
        List<Field> parts = multi;
        for (int hop = 0; hop < 5; hop++) {
            HttpURLConnection connection = null;
            try {
                connection = (HttpURLConnection) new URL(current).openConnection();
                connection.setInstanceFollowRedirects(false);
                connection.setConnectTimeout(30000);
                connection.setReadTimeout(45000);
                connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 Chrome/131.0.0.0 Mobile Safari/537.36");
                connection.setRequestProperty("Referer", "https://kalaiyaonline.com/?blood=all");
                Account.apply(connection);
                if ("POST".equals(verb)) {
                    byte[] bytes = parts != null ? multipart(parts) : (send == null ? new byte[0] : send.getBytes(StandardCharsets.UTF_8));
                    connection.setRequestMethod("POST");
                    connection.setDoOutput(true);
                    if (parts != null) connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=KalaiyaDonor");
                    else connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
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
                    parts = null;
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

    private String encodeFields(List<Field> fields) {
        StringBuilder body = new StringBuilder();
        for (Field field : fields) {
            String value = field.current();
            if (value == null) continue;
            if (body.length() > 0) body.append('&');
            body.append(enc(field.name)).append('=').append(enc(value));
        }
        return body.toString();
    }

    private byte[] multipart(List<Field> fields) {
        StringBuilder body = new StringBuilder();
        for (Field field : fields) {
            String value = field.current();
            if (value == null) continue;
            body.append("--KalaiyaDonor\r\n");
            body.append("Content-Disposition: form-data; name=\"").append(field.name).append("\"\r\n\r\n");
            body.append(value).append("\r\n");
        }
        body.append("--KalaiyaDonor--\r\n");
        return body.toString().getBytes(StandardCharsets.UTF_8);
    }

    private String messageOf(String html) {
        if (html == null || html.isEmpty()) return "पठाउन सकिएन। नेट जाँच गरेर फेरि प्रयास गर्नुहोस्।";
        Document doc = Jsoup.parse(html);
        Element alert = doc.selectFirst(".donor-thanks-card, .success, .ko-error, .error, .alert");
        if (alert != null && !alert.text().trim().isEmpty()) return alert.text().replaceAll("\\s+", " ").trim();
        if (html.contains("धन्यवाद") || html.contains("donor-thanks")) return "दर्ता भयो। स्वीकृत भएपछि सूचीमा देखिन्छ।";
        return "विवरण पठाइयो।";
    }

    private String labelOf(Element input) {
        Element parent = input.parent();
        if (parent != null && "label".equals(parent.tagName())) {
            String label = parent.ownText().replaceAll("\\s+", " ").trim();
            if (!label.isEmpty()) return label;
        }
        if (input.hasAttr("id")) {
            Element label = input.ownerDocument().selectFirst("label[for=" + input.id() + "]");
            if (label != null && !label.text().trim().isEmpty()) return label.text().trim();
        }
        String hint = input.attr("placeholder").trim();
        return hint.isEmpty() ? input.attr("name") : hint;
    }

    private String read(InputStream input) throws Exception {
        if (input == null) return "";
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int count;
        while ((count = input.read(buffer)) != -1) bytes.write(buffer, 0, count);
        input.close();
        return bytes.toString("UTF-8");
    }

    private void search() {
        status.setText("रक्तदाता खोजिँदैछ…");
        results.removeAllViews();
        String url = "https://kalaiyaonline.com/?blood=all&q=" + enc(query.getText().toString().trim())
                + "&blood_group=" + enc(GROUPS[group.getSelectedItemPosition()])
                + "&district=" + enc(DISTRICTS[district.getSelectedItemPosition()]);
        fetch(url, html -> {
            if (html == null) {
                status.setText("सूची आएन। फेरि खोज्नुहोस्।");
                return;
            }
            List<Donor> donors = parseList(html);
            results.removeAllViews();
            status.setText(donors.isEmpty() ? "रक्तदाता भेटिएन।" : donors.size() + " रक्तदाता");
            for (Donor donor : donors) results.addView(card(donor));
        });
    }

    private View card(Donor donor) {
        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(10), dp(10), dp(10), dp(10));
        row.setBackground(cardBg());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(8);
        row.setLayoutParams(lp);
        ImageView photo = new ImageView(activity);
        photo.setScaleType(ImageView.ScaleType.CENTER_CROP);
        LinearLayout.LayoutParams photoLp = new LinearLayout.LayoutParams(dp(56), dp(56));
        photoLp.rightMargin = dp(10);
        row.addView(photo, photoLp);
        if (!donor.image.isEmpty()) loadImage(donor.image, photo);
        LinearLayout copy = new LinearLayout(activity);
        copy.setOrientation(LinearLayout.VERTICAL);
        copy.addView(text(donor.name, 16, 0xFF142033, true));
        if (!donor.place.isEmpty()) copy.addView(text(donor.place, 13, 0xFF57534E, false));
        if (!donor.views.isEmpty()) copy.addView(text(donor.views, 12, 0xFF78716C, false));
        row.addView(copy, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        row.setOnClickListener(v -> openDonor(donor.slug));
        return row;
    }

    private void openDonor(String slug) {
        status.setText("विवरण लोड हुँदैछ…");
        fetch("https://kalaiyaonline.com/?donor=" + slug, html -> {
            status.setText("");
            if (html == null) {
                status.setText("विवरण आएन।");
                return;
            }
            showDetail(html, slug);
        });
    }

    private void showDetail(String html, String slug) {
        detailPane.removeAllViews();
        detailPane.addView(button("‹ सूची", false, this::showList));
        LinearLayout card = new LinearLayout(activity);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER_HORIZONTAL);
        card.setPadding(dp(14), dp(14), dp(14), dp(14));
        card.setBackground(cardBg());
        ImageView photo = new ImageView(activity);
        photo.setScaleType(ImageView.ScaleType.CENTER_CROP);
        card.addView(photo, new LinearLayout.LayoutParams(dp(96), dp(96)));
        String image = attr(html, "<div class=\"blood-profile-photo\"><img src=\"([^\"]+)\"");
        if (!image.isEmpty()) loadImage(image, photo);
        card.addView(text(textOf(html, "<h1>([^<]+)</h1>"), 22, 0xFF142033, true));
        TextView blood = text(textOf(html, "blood-big\"><span>([^<]+)</span>"), 32, 0xFFB91C1C, true);
        blood.setPadding(0, dp(4), 0, dp(4));
        card.addView(blood);
        card.addView(text(clean(textOf(html, "blood-location\">([^<]+)")), 14, 0xFF57534E, false));
        String available = textOf(html, "donor-availability-badge[^\"]*\">([^<]+)");
        if (!available.isEmpty()) card.addView(text(clean(available), 14, 0xFF0B5C28, true));
        String last = textOf(html, "donor-last-donation\">([^<]+)");
        if (!last.isEmpty()) card.addView(text(clean(last), 14, 0xFF57534E, false));
        String phone = attr(html, "href=\"tel:([^\"]+)\"");
        String email = attr(html, "href=\"mailto:([^\"]+)\"");
        if (!phone.isEmpty()) card.addView(button("फोन गर्नुहोस्", true, () -> open("tel:" + phone)));
        if (!email.isEmpty()) card.addView(button("इमेल पठाउनुहोस्", false, () -> open("mailto:" + email)));
        card.addView(button("वेबमा पूरा विवरण", false, () -> open("https://kalaiyaonline.com/?donor=" + slug)));
        detailPane.addView(card);
        listPane.setVisibility(View.GONE);
        joinPane.setVisibility(View.GONE);
        detailPane.setVisibility(View.VISIBLE);
    }

    private void showList() {
        detailPane.setVisibility(View.GONE);
        joinPane.setVisibility(View.GONE);
        listPane.setVisibility(View.VISIBLE);
    }

    private List<Donor> parseList(String html) {
        List<Donor> donors = new ArrayList<>();
        Matcher cards = Pattern.compile("<article class=\"blood-card\">([\\s\\S]*?)</article>").matcher(html);
        while (cards.find()) {
            String block = cards.group(1);
            Donor donor = new Donor();
            donor.name = textOf(block, "<h2>([^<]+)</h2>");
            donor.place = clean(textOf(block, "<p>([^<]+)</p>"));
            donor.views = textOf(block, "([०-९0-9]+\\s*पटक हेरिएको)");
            donor.image = attr(block, "<img src=\"([^\"]+)\"");
            donor.phone = attr(block, "href=\"tel:([^\"]+)\"");
            donor.slug = attr(block, "href=\"\\?donor=([^\"]+)\"");
            if (!donor.slug.isEmpty()) donors.add(donor);
        }
        return donors;
    }

    private void fetch(String address, Result result) {
        new Thread(() -> {
            String html = download(address);
            activity.runOnUiThread(() -> result.onResult(html));
        }).start();
    }

    private String download(String address) {
        return Net.text(address);
    }

    private void loadImage(String url, ImageView target) {
        new Thread(() -> {
            Bitmap bitmap = Bitmaps.load(url, 800);
            if (bitmap != null && !activity.isDestroyed()) activity.runOnUiThread(() -> {
                if (!activity.isDestroyed()) target.setImageBitmap(bitmap);
            });
        }).start();
    }

    private String textOf(String html, String source) {
        Matcher matcher = Pattern.compile(source).matcher(html);
        return matcher.find() ? decode(matcher.group(1).trim()) : "";
    }

    private String attr(String html, String source) {
        return textOf(html, source);
    }

    private String clean(String value) {
        return value.replace("📍", "").replace("●", "").replace("🗓️", "").trim();
    }

    private String decode(String value) {
        return value.replace("\u0026amp;", "\u0026").replace("\u0026quot;", "\"").replace("\u0026#039;", "'");
    }

    private String enc(String value) {
        try {
            return URLEncoder.encode(value, "UTF-8");
        } catch (Exception ignored) {
            return "";
        }
    }

    private void open(String url) {
        activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
    }

    private Spinner spinner(String[] labels) {
        Spinner spinner = new Spinner(activity);
        spinner.setAdapter(new ArrayAdapter<>(activity, android.R.layout.simple_spinner_dropdown_item, labels));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(8);
        spinner.setLayoutParams(lp);
        return spinner;
    }

    private String[] labels(String first, String[] values) {
        String[] labels = values.clone();
        labels[0] = first;
        return labels;
    }

    private TextView button(String label, boolean solid, Runnable action) {
        TextView button = text(label, 15, solid ? 0xFFFFFFFF : 0xFFB91C1C, true);
        button.setGravity(Gravity.CENTER);
        button.setPadding(dp(12), dp(10), dp(12), dp(10));
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(14));
        bg.setColor(solid ? 0xFFB91C1C : 0xFFFDECEC);
        button.setBackground(bg);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(8);
        button.setLayoutParams(lp);
        button.setOnClickListener(v -> action.run());
        return button;
    }

    private GradientDrawable field() {
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xFFFFFFFF);
        bg.setCornerRadius(dp(12));
        bg.setStroke(dp(1), 0xFFE7E5E4);
        return bg;
    }

    private GradientDrawable cardBg() {
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xFFFFFFFF);
        bg.setCornerRadius(dp(16));
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

    private interface Result {
        void onResult(String html);
    }

    private static final class Field {
        final String name;
        final String kind;
        final String fixed;
        final EditText edit;
        final Spinner spinner;
        final List<String> values;
        final boolean[] on;

        private Field(String name, String kind, String fixed, EditText edit, Spinner spinner, List<String> values, boolean[] on) {
            this.name = name;
            this.kind = kind;
            this.fixed = fixed;
            this.edit = edit;
            this.spinner = spinner;
            this.values = values;
            this.on = on;
        }

        static Field hidden(String name, String value) {
            return new Field(name, "hidden", value, null, null, null, null);
        }

        static Field text(String name, EditText edit) {
            return new Field(name, "text", "", edit, null, null, null);
        }

        static Field choice(String name, Spinner spinner, List<String> values) {
            return new Field(name, "choice", "", null, spinner, values, null);
        }

        static Field check(String name, String value, boolean[] on) {
            return new Field(name, "check", value, null, null, null, on);
        }

        String current() {
            if ("text".equals(kind)) return edit.getText() == null ? "" : edit.getText().toString().trim();
            if ("choice".equals(kind)) {
                int index = spinner.getSelectedItemPosition();
                return index >= 0 && index < values.size() ? values.get(index) : "";
            }
            if ("check".equals(kind)) return on[0] ? fixed : null;
            return fixed == null ? "" : fixed;
        }
    }

    private static final class Donor {
        String name = "";
        String place = "";
        String views = "";
        String image = "";
        String phone = "";
        String slug = "";
    }
}
