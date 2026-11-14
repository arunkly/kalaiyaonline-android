package com.kalaiyaonline.news;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class DirectoryBoard {
    private final AppCompatActivity activity;
    private final LinearLayout root;
    private final LinearLayout list;
    private final TextView status;
    private final EditText query;
    private final List<Listing> listings = new ArrayList<>();
    private int page;
    private int generation;

    DirectoryBoard(AppCompatActivity activity) {
        this.activity = activity;
        root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);

        query = new EditText(activity);
        query.setHint("डाइरेक्टरी खोज्नुहोस्…");
        query.setSingleLine(true);
        query.setBackground(field());
        query.setPadding(dp(12), dp(10), dp(12), dp(10));
        query.setTypeface(ResourcesCompat.getFont(activity, R.font.mukta_regular));
        query.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                page = 0;
                showPage();
            }
            public void afterTextChanged(Editable s) {}
        });
        root.addView(query);

        status = text("", 13, 0xFF57534E, false);
        status.setPadding(0, dp(8), 0, 0);
        root.addView(status);

        list = new LinearLayout(activity);
        list.setOrientation(LinearLayout.VERTICAL);
        root.addView(list);
    }

    View view() {
        return root;
    }

    void load() {
        if (!listings.isEmpty()) {
            showPage();
            return;
        }
        status.setText("डाइरेक्टरी लोड हुँदैछ…");
        new Thread(() -> {
            String html = download("https://kalaiyaonline.com/?directory=all");
            List<Listing> parsed = html == null ? null : parse(html);
            activity.runOnUiThread(() -> {
                if (parsed == null) {
                    status.setText("डाइरेक्टरी आएन। फेरि थिच्नुहोस्।");
                    status.setOnClickListener(v -> {
                        listings.clear();
                        load();
                    });
                    return;
                }
                status.setOnClickListener(null);
                listings.clear();
                listings.addAll(parsed);
                page = 0;
                showPage();
            });
        }).start();
    }

    private List<Listing> parse(String html) {
        List<Listing> rows = new ArrayList<>();
        Matcher cards = Pattern.compile("<article class=\"directory-card-v3\">([\\s\\S]*?)</article>").matcher(html);
        while (cards.find()) {
            String block = cards.group(1);
            Listing item = new Listing();
            item.title = textOf(block, "<h2><a [^>]*>([^<]+)</a>");
            item.page = textOf(block, "href=\"(https://kalaiyaonline.com/directory/[^\"]+)\"");
            item.image = textOf(block, "<img src=\"([^\"]+)\"");
            item.category = textOf(block, "directory-card-kicker\">([^<]+)");
            item.location = textOf(block, "directory-card-location\">([^<]+)").replace("⌖", "").trim();
            item.blurb = textOf(block, "<p>([^<]+)</p>");
            if (!item.title.isEmpty()) rows.add(item);
        }
        return rows;
    }

    private void showPage() {
        if (listings.isEmpty()) return;
        generation++;
        String needle = query.getText().toString().trim();
        List<Listing> matches = new ArrayList<>();
        for (Listing item : listings) {
            String blob = item.title + " " + item.category + " " + item.location + " " + item.blurb;
            if (needle.isEmpty() || blob.contains(needle)) matches.add(item);
        }
        list.removeAllViews();
        if (matches.isEmpty()) {
            status.setText("सूची भेटिएन");
            list.addView(text("यो खोजमा केही भेटिएन।", 14, 0xFF57534E, false));
            return;
        }
        int pages = Math.max(1, (matches.size() + 6) / 7);
        if (page >= pages) page = pages - 1;
        int start = page * 7;
        int end = Math.min(matches.size(), start + 7);
        status.setText((start + 1) + "–" + end + " / " + matches.size());
        int token = generation;
        for (int i = start; i < end; i++) list.addView(card(matches.get(i), token));
        LinearLayout pager = new LinearLayout(activity);
        pager.setOrientation(LinearLayout.HORIZONTAL);
        pager.setGravity(Gravity.CENTER);
        pager.setPadding(0, dp(12), 0, dp(8));
        if (page > 0) pager.addView(action("अघिल्लो", () -> { page--; showPage(); }));
        TextView label = text("  " + (page + 1) + " / " + pages + "  ", 14, 0xFF142033, true);
        pager.addView(label);
        if (page + 1 < pages) pager.addView(action("पछिल्लो", () -> { page++; showPage(); }));
        list.addView(pager);
    }

    private View card(Listing item, int token) {
        LinearLayout card = new LinearLayout(activity);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setBackground(cardBg());
        card.setPadding(dp(8), dp(8), dp(8), dp(8));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(8);
        card.setLayoutParams(lp);

        ImageView image = new ImageView(activity);
        image.setScaleType(ImageView.ScaleType.CENTER_CROP);
        image.setBackgroundColor(0xFFE7E5E4);
        card.addView(image, new LinearLayout.LayoutParams(dp(64), dp(64)));
        if (!item.image.isEmpty()) loadImage(item.image, image, token);

        LinearLayout copy = new LinearLayout(activity);
        copy.setOrientation(LinearLayout.VERTICAL);
        copy.setPadding(dp(10), 0, 0, 0);
        copy.addView(text(item.title, 16, 0xFF142033, true));
        if (!item.category.isEmpty()) copy.addView(text(item.category, 12, 0xFF0B5C28, false));
        if (!item.location.isEmpty()) copy.addView(text(item.location, 12, 0xFF78716C, false));
        card.addView(copy, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        card.setOnClickListener(v -> openDetail(item));
        return card;
    }

    private void openDetail(Listing item) {
        generation++;
        int token = generation;
        list.removeAllViews();
        status.setText("");
        list.addView(button("← सूची", this::showPage));
        TextView title = text(item.title, 20, 0xFF142033, true);
        title.setPadding(0, dp(10), 0, dp(4));
        list.addView(title);
        if (!item.category.isEmpty()) list.addView(text(item.category, 13, 0xFF0B5C28, true));
        if (!item.location.isEmpty()) list.addView(text(item.location, 14, 0xFF57534E, false));
        ImageView image = new ImageView(activity);
        image.setScaleType(ImageView.ScaleType.CENTER_CROP);
        LinearLayout.LayoutParams ip = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(160));
        ip.topMargin = dp(8);
        list.addView(image, ip);
        if (!item.image.isEmpty()) loadImage(item.image, image, token);
        TextView body = text(item.blurb.isEmpty() ? "विवरण लोड हुँदैछ…" : item.blurb, 14, 0xFF44403C, false);
        body.setPadding(0, dp(8), 0, 0);
        list.addView(body);
        new Thread(() -> {
            String html = item.page.isEmpty() ? null : download(item.page);
            activity.runOnUiThread(() -> {
                if (token != generation || html == null) return;
                String about = textOf(html, "kp-short-description\">([^<]+)");
                String address = textOf(html, "ठेगाना</strong><p>([^<]+)</p>");
                String phone = textOf(html, "सम्पर्क नम्बर</strong><p>([^<]+)</p>");
                String map = textOf(html, "href=\"(https://maps\\.google\\.com/[^\"]+)\"");
                String whatsapp = textOf(html, "href=\"(https://wa\\.me/[^\"]+)\"");
                if (!about.isEmpty()) body.setText(about);
                if (!address.isEmpty()) list.addView(text("ठेगाना: " + address, 14, 0xFF142033, false));
                LinearLayout actions = new LinearLayout(activity);
                actions.setOrientation(LinearLayout.HORIZONTAL);
                actions.setPadding(0, dp(10), 0, 0);
                if (!phone.isEmpty()) actions.addView(action("फोन", () -> open("tel:" + phone.replace(" ", ""))));
                if (!whatsapp.isEmpty()) actions.addView(action("WhatsApp", () -> open(whatsapp)));
                if (!map.isEmpty()) actions.addView(action("नक्सा", () -> open(map)));
                if (actions.getChildCount() > 0) list.addView(actions);
            });
        }).start();
    }

    private String download(String address) {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(address).openConnection();
            connection.setRequestProperty("User-Agent", "Mozilla/5.0");
            connection.setConnectTimeout(12000);
            connection.setReadTimeout(20000);
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

    private void loadImage(String url, ImageView target, int token) {
        new Thread(() -> {
            Bitmap bitmap = null;
            HttpURLConnection connection = null;
            try {
                connection = (HttpURLConnection) new URL(url).openConnection();
                connection.setRequestProperty("User-Agent", "Mozilla/5.0");
                connection.setConnectTimeout(12000);
                connection.setReadTimeout(12000);
                InputStream input = connection.getInputStream();
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                byte[] buffer = new byte[8192];
                int read;
                while ((read = input.read(buffer)) != -1) bytes.write(buffer, 0, read);
                input.close();
                byte[] data = bytes.toByteArray();
                BitmapFactory.Options bounds = new BitmapFactory.Options();
                bounds.inJustDecodeBounds = true;
                BitmapFactory.decodeByteArray(data, 0, data.length, bounds);
                int sample = 1;
                int width = Math.max(bounds.outWidth, 1);
                while (width / sample > 240) sample *= 2;
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inSampleSize = sample;
                bitmap = BitmapFactory.decodeByteArray(data, 0, data.length, options);
            } catch (Exception ignored) {
            } finally {
                if (connection != null) connection.disconnect();
            }
            Bitmap image = bitmap;
            if (image != null && token == generation) activity.runOnUiThread(() -> {
                if (token == generation) target.setImageBitmap(image);
            });
        }).start();
    }

    private String textOf(String html, String source) {
        Matcher matcher = Pattern.compile(source).matcher(html);
        return matcher.find() ? decode(matcher.group(1).trim()) : "";
    }

    private String decode(String value) {
        return value.replace("\u0026amp;", "\u0026").replace("\u0026quot;", "\"").replace("\u0026#039;", "'");
    }

    private View button(String label, Runnable action) {
        TextView view = text(label, 15, 0xFFFFFFFF, true);
        view.setGravity(Gravity.CENTER);
        view.setPadding(dp(12), dp(10), dp(12), dp(10));
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(12));
        bg.setColor(0xFF0F8A3C);
        view.setBackground(bg);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(8);
        view.setLayoutParams(lp);
        view.setOnClickListener(v -> action.run());
        return view;
    }

    private View action(String label, Runnable task) {
        TextView view = text(label, 14, 0xFF0B5C28, true);
        view.setPadding(dp(12), dp(8), dp(12), dp(8));
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(12));
        bg.setColor(0xFFE5F6EC);
        view.setBackground(bg);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.rightMargin = dp(8);
        view.setLayoutParams(lp);
        view.setOnClickListener(v -> task.run());
        return view;
    }

    private void open(String url) {
        try {
            activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (Exception ignored) {
        }
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
        bg.setCornerRadius(dp(14));
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

    private static final class Listing {
        String title = "";
        String category = "";
        String location = "";
        String blurb = "";
        String image = "";
        String page = "";
    }
}
