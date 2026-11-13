package com.kalaiyaonline.news;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {
    private final SiteClient client = new SiteClient();
    private final ExecutorService io = Executors.newFixedThreadPool(3);
    private LinearLayout page;
    private TextView status;
    private String section = "home";

    private static final String[][] CATS = {
            {"", "समाचार"},
            {"local", "स्थानीय"},
            {"international", "अन्तर्राष्ट्रिय"},
            {"politics", "राजनीति"},
            {"artha", "अर्थ"},
            {"khelkud", "खेलकुद"},
            {"entertainment", "मनोरंजन"},
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFFF6F7F4);
        root.addView(header());

        page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(12), dp(8), dp(12), dp(12));
        android.widget.ScrollView scroll = new android.widget.ScrollView(this);
        scroll.addView(page);
        root.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        BottomNavigationView nav = new BottomNavigationView(this);
        nav.inflateMenu(R.menu.bottom_nav);
        nav.setBackgroundColor(0xFFFFFFFF);
        nav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) showHome();
            else if (id == R.id.nav_gallery) showList("gallery");
            else if (id == R.id.nav_directory) showList("directory");
            else if (id == R.id.nav_reels) showList("reels");
            else showCommunity();
            return true;
        });
        root.addView(nav);
        setContentView(root);
        showHome();
    }

    private View header() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setBackgroundColor(0xFFFFFFFF);
        ImageView logo = new ImageView(this);
        logo.setAdjustViewBounds(true);
        logo.setImageResource(R.drawable.wordmark);
        int pad = dp(10);
        logo.setPadding(pad, pad, pad, dp(6));
        box.addView(logo, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(58)));
        View green = new View(this);
        green.setBackgroundColor(0xFF0F8A3C);
        box.addView(green, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(4)));
        View orange = new View(this);
        orange.setBackgroundColor(0xFFF15A22);
        box.addView(orange, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(2)));
        return box;
    }

    private void showHome() {
        section = "home";
        setStatus("समाचार लोड हुँदैछ…");
        io.execute(() -> {
            try {
                SiteClient.Home home = client.home();
                runOnUiThread(() -> renderHome(home));
            } catch (Exception error) {
                runOnUiThread(() -> setStatus("समाचार ल्याउन सकिएन। इन्टरनेट जाँच्नुहोस्।"));
            }
        });
    }

    private void renderHome(SiteClient.Home home) {
        if (!"home".equals(section)) return;
        page.removeAllViews();
        if (!home.dateLine.isEmpty()) page.addView(meta(home.dateLine));
        page.addView(chips());
        if (!home.ticker.isEmpty()) page.addView(ticker(home.ticker));
        if (home.stories.isEmpty()) {
            page.addView(message("समाचार भेटिएन।"));
            return;
        }
        Story lead = home.stories.get(0);
        page.addView(card(lead, true));
        for (int i = 1; i < home.stories.size(); i++) page.addView(card(home.stories.get(i), false));
    }

    private void showList(String kind) {
        section = kind;
        setStatus("लोड हुँदैछ…");
        io.execute(() -> {
            try {
                List<Story> list = "gallery".equals(kind) ? client.gallery()
                        : "directory".equals(kind) ? client.directory() : client.reels();
                runOnUiThread(() -> renderList(kind, list));
            } catch (Exception error) {
                runOnUiThread(() -> setStatus("लोड हुन सकेन।"));
            }
        });
    }

    private void renderList(String kind, List<Story> list) {
        if (!kind.equals(section)) return;
        page.removeAllViews();
        String title = "gallery".equals(kind) ? "फोटो ग्यालरी" : "directory".equals(kind) ? "डाइरेक्टरी" : "रिल्स";
        page.addView(heading(title));
        if (list.isEmpty()) {
            page.addView(message("केही भेटिएन।"));
            return;
        }
        for (Story story : list) page.addView(card(story, false));
    }

    private void showCommunity() {
        section = "community";
        page.removeAllViews();
        page.addView(heading("समुदाय"));
        page.addView(message("कलैया अनलाइन\nतपाईंको शहर, तपाईंको समाचार\n\nसमाचार, फोटो र डाइरेक्टरी यही एपभित्र kalaiyaonline.com बाट आउँछ।"));
    }

    private void openCategory(String slug, String label) {
        section = "cat:" + slug;
        setStatus(label + " लोड हुँदैछ…");
        io.execute(() -> {
            try {
                List<Story> list = slug.isEmpty() ? client.home().stories : client.category(slug);
                runOnUiThread(() -> {
                    if (!section.equals("cat:" + slug)) return;
                    page.removeAllViews();
                    page.addView(chips());
                    page.addView(heading(label));
                    for (Story story : list) page.addView(card(story, false));
                    if (list.isEmpty()) page.addView(message("समाचार भेटिएन।"));
                });
            } catch (Exception error) {
                runOnUiThread(() -> setStatus("लोड हुन सकेन।"));
            }
        });
    }

    private View chips() {
        HorizontalScrollView scroll = new HorizontalScrollView(this);
        scroll.setHorizontalScrollBarEnabled(false);
        LinearLayout row = new LinearLayout(this);
        row.setPadding(0, dp(8), 0, dp(8));
        for (String[] cat : CATS) {
            TextView chip = new TextView(this);
            chip.setText(cat[1]);
            chip.setTextColor(0xFF0B5C28);
            chip.setTextSize(14);
            chip.setBackgroundColor(0xFFFFFFFF);
            chip.setPadding(dp(14), dp(8), dp(14), dp(8));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.rightMargin = dp(8);
            chip.setOnClickListener(v -> openCategory(cat[0], cat[1]));
            row.addView(chip, lp);
        }
        scroll.addView(row);
        return scroll;
    }

    private View ticker(List<Story> items) {
        TextView view = new TextView(this);
        StringBuilder text = new StringBuilder("ताजा  ");
        for (int i = 0; i < Math.min(4, items.size()); i++) {
            if (i > 0) text.append("   ·   ");
            text.append(items.get(i).title);
        }
        view.setText(text.toString());
        view.setTextColor(0xFFFFFFFF);
        view.setTextSize(14);
        view.setMaxLines(2);
        view.setBackgroundColor(0xFF1C1917);
        view.setPadding(dp(12), dp(10), dp(12), dp(10));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(12);
        view.setLayoutParams(lp);
        view.setOnClickListener(v -> openStory(items.get(0)));
        return view;
    }

    private View card(Story story, boolean hero) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundColor(0xFFFFFFFF);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(12);
        card.setLayoutParams(lp);
        if (!story.image.isEmpty()) {
            ImageView image = new ImageView(this);
            image.setScaleType(ImageView.ScaleType.CENTER_CROP);
            image.setBackgroundColor(0xFFE8F6EE);
            card.addView(image, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, hero ? dp(210) : dp(160)));
            loadImage(story.image, image);
        }
        LinearLayout pad = new LinearLayout(this);
        pad.setOrientation(LinearLayout.VERTICAL);
        pad.setPadding(dp(12), dp(10), dp(12), dp(12));
        if (!story.category.isEmpty()) {
            TextView kicker = new TextView(this);
            kicker.setText(story.category);
            kicker.setTextColor(0xFFF15A22);
            kicker.setTextSize(12);
            pad.addView(kicker);
        }
        TextView title = new TextView(this);
        title.setText(story.title);
        title.setTextColor(0xFF1C1917);
        title.setTextSize(hero ? 20 : 16);
        pad.addView(title);
        String meta = (story.author.isEmpty() ? "" : story.author) + (story.date.isEmpty() ? "" : " · " + story.date);
        if (!meta.isEmpty() && !meta.equals(" · ")) {
            TextView line = new TextView(this);
            line.setText(meta.replaceAll("^ · ", ""));
            line.setTextColor(0xFF78716C);
            line.setTextSize(12);
            line.setPadding(0, dp(4), 0, 0);
            pad.addView(line);
        }
        card.addView(pad);
        card.setOnClickListener(v -> openStory(story));
        return card;
    }

    private void openStory(Story story) {
        if (story.slug.startsWith("http") && !story.slug.contains("/news/")) return;
        Intent intent = new Intent(this, ArticleActivity.class);
        intent.putExtra("slug", story.slug);
        intent.putExtra("title", story.title);
        intent.putExtra("image", story.image);
        startActivity(intent);
    }

    private void loadImage(String url, ImageView target) {
        io.execute(() -> {
            Bitmap bitmap = fetchBitmap(url);
            if (bitmap == null) return;
            runOnUiThread(() -> target.setImageBitmap(bitmap));
        });
    }

    private Bitmap fetchBitmap(String url) {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setConnectTimeout(12000);
            connection.setReadTimeout(12000);
            connection.setRequestProperty("User-Agent", "Mozilla/5.0");
            InputStream stream = connection.getInputStream();
            return BitmapFactory.decodeStream(stream);
        } catch (Exception ignored) {
            return null;
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    private void setStatus(String text) {
        page.removeAllViews();
        status = message(text);
        page.addView(status);
    }

    private TextView message(String text) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextColor(0xFF78716C);
        view.setTextSize(16);
        view.setGravity(Gravity.CENTER);
        view.setPadding(dp(12), dp(28), dp(12), dp(12));
        return view;
    }

    private TextView heading(String text) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextColor(0xFF1C1917);
        view.setTextSize(22);
        view.setPadding(0, dp(8), 0, dp(12));
        return view;
    }

    private TextView meta(String text) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextColor(0xFF0F8A3C);
        view.setTextSize(14);
        view.setPadding(dp(4), dp(4), dp(4), dp(4));
        return view;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
