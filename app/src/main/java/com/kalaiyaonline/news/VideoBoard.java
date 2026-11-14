package com.kalaiyaonline.news;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class VideoBoard {
    private static final String[][] CATEGORIES = {
            {"सबै", "https://kalaiyaonline.com/videos"},
            {"मठ / मन्दिर", "https://kalaiyaonline.com/video-category/mathmandir"},
            {"समाचार", "https://kalaiyaonline.com/video-category/news"},
            {"समाज", "https://kalaiyaonline.com/video-category/society"},
            {"मनोरञ्जन", "https://kalaiyaonline.com/video-category/entertainment"},
            {"खेलकुद", "https://kalaiyaonline.com/video-category/sports"},
            {"पर्यटन", "https://kalaiyaonline.com/video-category/tourism"},
            {"अन्तर्वार्ता", "https://kalaiyaonline.com/video-category/interview"}
    };

    private final AppCompatActivity activity;
    private final LinearLayout root;
    private final LinearLayout chips;
    private final LinearLayout list;
    private final TextView status;
    private int selected;

    VideoBoard(AppCompatActivity activity) {
        this.activity = activity;
        root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        TextView intro = text("YouTube का समाचार, अन्तर्वार्ता र स्थानीय भिडियो।", 14, 0xFF57534E, false);
        intro.setPadding(0, 0, 0, dp(8));
        root.addView(intro);
        chips = new LinearLayout(activity);
        chips.setOrientation(LinearLayout.HORIZONTAL);
        HorizontalScrollView scroll = new HorizontalScrollView(activity);
        scroll.setHorizontalScrollBarEnabled(false);
        scroll.addView(chips);
        root.addView(scroll);
        status = text("", 13, 0xFF57534E, false);
        status.setPadding(0, dp(8), 0, 0);
        root.addView(status);
        list = new LinearLayout(activity);
        list.setOrientation(LinearLayout.VERTICAL);
        root.addView(list);
        paintChips();
    }

    View view() {
        return root;
    }

    void load() {
        fetch(selected);
    }

    private void fetch(int index) {
        selected = index;
        paintChips();
        status.setText("भिडियो लोड हुँदैछ…");
        list.removeAllViews();
        new Thread(() -> {
            String html = download(CATEGORIES[index][1]);
            activity.runOnUiThread(() -> show(html));
        }).start();
    }

    private void show(String html) {
        list.removeAllViews();
        if (html == null) {
            status.setText("भिडियो आएन। फेरि थिच्नुहोस्।");
            status.setOnClickListener(v -> fetch(selected));
            return;
        }
        status.setOnClickListener(null);
        List<Clip> clips = parse(html);
        status.setText(clips.isEmpty() ? "अहिले यो श्रेणीमा भिडियो छैन।" : clips.size() + " भिडियो");
        for (int i = 0; i < clips.size(); i++) list.addView(card(clips.get(i), i == 0));
    }

    private View card(Clip clip, boolean featured) {
        LinearLayout card = new LinearLayout(activity);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(cardBg());
        card.setPadding(dp(8), dp(8), dp(8), dp(10));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(10);
        card.setLayoutParams(lp);
        ImageView image = new ImageView(activity);
        image.setScaleType(ImageView.ScaleType.CENTER_CROP);
        int width = activity.getResources().getDisplayMetrics().widthPixels - dp(48);
        card.addView(image, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, featured ? width * 9 / 16 : dp(150)));
        if (!clip.image.isEmpty()) loadImage(clip.image, image);
        if (!clip.category.isEmpty()) {
            TextView cat = text(clip.category, 12, 0xFFB91C1C, true);
            cat.setPadding(0, dp(8), 0, 0);
            card.addView(cat);
        }
        card.addView(text(clip.title, featured ? 18 : 16, 0xFF142033, true));
        if (!clip.meta.isEmpty()) card.addView(text(clip.meta, 12, 0xFF78716C, false));
        card.setOnClickListener(v -> play(clip));
        return card;
    }

    private void play(Clip clip) {
        if (!clip.youtube.isEmpty()) {
            Intent app = new Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube:" + clip.youtube));
            app.setPackage("com.google.android.youtube");
            try {
                activity.startActivity(app);
                return;
            } catch (Exception ignored) {
                activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/watch?v=" + clip.youtube)));
                return;
            }
        }
        if (!clip.page.isEmpty()) activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(clip.page)));
    }

    private List<Clip> parse(String html) {
        List<Clip> clips = new ArrayList<>();
        Matcher cards = Pattern.compile("<a class=\"aks-video-(?:featured|mini-feature)[^\"]*\" href=\"([^\"]+)\">([\\s\\S]*?)</a>").matcher(html);
        while (cards.find()) {
            String block = cards.group(2);
            Clip clip = new Clip();
            clip.page = cards.group(1);
            clip.title = textOf(block, "<h2>([^<]+)</h2>");
            if (clip.title.isEmpty()) clip.title = textOf(block, "<h3>([^<]+)</h3>");
            if (clip.title.isEmpty()) clip.title = textOf(block, "alt=\"([^\"]+)\"");
            clip.category = textOf(block, "aks-video-category\">([^<]+)");
            if (clip.category.isEmpty()) clip.category = textOf(block, "<small>([^<]+)</small>");
            clip.image = textOf(block, "<img src=\"([^\"]+)\"");
            Matcher id = Pattern.compile("/vi/([A-Za-z0-9_-]{6,})/").matcher(clip.image);
            if (id.find()) clip.youtube = id.group(1);
            String meta = textOf(block, "aks-video-meta\">([\\s\\S]*?)</div>");
            clip.meta = meta.replaceAll("<[^>]+>", " ").replaceAll("\\s+", " ").trim();
            if (clip.meta.isEmpty()) clip.meta = textOf(block, "<p>([^<]*views[^<]*)</p>");
            if (!clip.title.isEmpty()) clips.add(clip);
        }
        return clips;
    }

    private void paintChips() {
        chips.removeAllViews();
        for (int i = 0; i < CATEGORIES.length; i++) {
            boolean on = i == selected;
            TextView chip = text(CATEGORIES[i][0], 14, on ? 0xFFFFFFFF : 0xFF0B5C28, true);
            chip.setPadding(dp(12), dp(8), dp(12), dp(8));
            GradientDrawable bg = new GradientDrawable();
            bg.setCornerRadius(dp(16));
            bg.setColor(on ? 0xFF0F8A3C : 0xFFE5F6EC);
            chip.setBackground(bg);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.rightMargin = dp(8);
            chip.setLayoutParams(lp);
            int index = i;
            chip.setOnClickListener(v -> fetch(index));
            chips.addView(chip);
        }
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

    private String decode(String value) {
        return value.replace("\u0026amp;", "\u0026").replace("\u0026quot;", "\"").replace("\u0026#039;", "'");
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
        view.setGravity(Gravity.START);
        view.setTypeface(ResourcesCompat.getFont(activity, bold ? R.font.mukta_bold : R.font.mukta_regular));
        return view;
    }

    private int dp(int value) {
        return Math.round(value * activity.getResources().getDisplayMetrics().density);
    }

    private static final class Clip {
        String title = "";
        String category = "";
        String meta = "";
        String image = "";
        String youtube = "";
        String page = "";
    }
}
