package com.kalaiyaonline.news;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
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

    BloodBoard(AppCompatActivity activity) {
        this.activity = activity;
        root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        TextView intro = text("उपलब्ध रक्तदाता खोज्नुहोस्। रक्तदाता बन्न चाहनुहुन्छ भने दर्ता वेबसाइटमा खुल्छ।", 14, 0xFF57534E, false);
        root.addView(intro);
        root.addView(button("रक्तदाता बन्नुहोस्", true, () -> open("https://kalaiyaonline.com/account.php?mode=register&next=%2F%3Fblood%3Dall")));

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
        detailPane.setVisibility(View.VISIBLE);
    }

    private void showList() {
        detailPane.setVisibility(View.GONE);
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
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(address).openConnection();
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

    private void loadImage(String url, ImageView target) {
        new Thread(() -> {
            Bitmap bitmap = null;
            HttpURLConnection connection = null;
            try {
                connection = (HttpURLConnection) new URL(url).openConnection();
                connection.setRequestProperty("User-Agent", "Mozilla/5.0");
                connection.setConnectTimeout(12000);
                connection.setReadTimeout(12000);
                bitmap = BitmapFactory.decodeStream(connection.getInputStream());
            } catch (Exception ignored) {
            } finally {
                if (connection != null) connection.disconnect();
            }
            Bitmap image = bitmap;
            if (image != null) activity.runOnUiThread(() -> target.setImageBitmap(image));
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

    private static final class Donor {
        String name = "";
        String place = "";
        String views = "";
        String image = "";
        String phone = "";
        String slug = "";
    }
}
