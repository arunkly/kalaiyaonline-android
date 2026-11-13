package com.kalaiyaonline.news;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import android.util.TypedValue;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.card.MaterialCardView;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {
    private final SiteClient client = new SiteClient();
    private final ExecutorService io = Executors.newFixedThreadPool(3);
    private LinearLayout page;
    private TextView status;
    private String section = "home";

    private static final String[][] SECTIONS = {
            {"news", "समाचार"},
            {"local", "स्थानीय"},
            {"international", "अन्तर्राष्ट्रिय"},
            {"politics", "राजनीति"},
            {"artha", "अर्थ"},
            {"khelkud", "खेलकुद"},
            {"entertainment", "मनोरंजन"},
            {"lekhabichar", "लेख / बिचार"},
    };

    private final Handler sliderHandler = new Handler(Looper.getMainLooper());
    private ViewPager2 pager;
    private ObjectAnimator tickerAnim;
    private int homeGen;

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
        nav.setItemIconTintList(getColorStateList(R.color.nav_tint));
        nav.setItemTextColor(getColorStateList(R.color.nav_tint));
        nav.setLabelVisibilityMode(BottomNavigationView.LABEL_VISIBILITY_LABELED);
        nav.setElevation(dp(8));
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
        stopMotion();
        int gen = homeGen;
        setStatus("समाचार लोड हुँदैछ…");
        io.execute(() -> {
            try {
                SiteClient.Home home = client.home();
                runOnUiThread(() -> {
                    if (gen == homeGen) renderHome(home, gen);
                });
            } catch (Exception error) {
                runOnUiThread(() -> {
                    if (gen == homeGen) setStatus("समाचार ल्याउन सकिएन। इन्टरनेट जाँच्नुहोस्।");
                });
            }
        });
    }

    private void renderHome(SiteClient.Home home, int gen) {
        if (!"home".equals(section) || gen != homeGen) return;
        page.removeAllViews();
        if (!home.headlines.isEmpty()) page.addView(slider(home.headlines));
        if (!home.dateLine.isEmpty()) page.addView(meta(home.dateLine));
        if (!home.ticker.isEmpty()) page.addView(ticker(home.ticker));
        page.addView(chips());
        LinearLayout sections = new LinearLayout(this);
        sections.setOrientation(LinearLayout.VERTICAL);
        page.addView(sections);
        io.execute(() -> {
            for (String[] cat : SECTIONS) {
                List<Story> list;
                try {
                    list = client.category(cat[0]);
                } catch (Exception error) {
                    list = new ArrayList<>();
                }
                if (list.isEmpty()) continue;
                List<Story> top = list.subList(0, Math.min(3, list.size()));
                String slug = cat[0];
                String label = cat[1];
                runOnUiThread(() -> {
                    if (gen != homeGen || !"home".equals(section)) return;
                    sections.addView(categoryBlock(label, slug, top));
                });
            }
        });
    }

    private void showList(String kind) {
        section = kind;
        stopMotion();
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
        stopMotion();
        page.removeAllViews();
        page.addView(heading("समुदाय"));
        page.addView(message("कलैया अनलाइन\nतपाईंको शहर, तपाईंको समाचार\n\nसमाचार, फोटो र डाइरेक्टरी यही एपभित्र kalaiyaonline.com बाट आउँछ।"));
    }

    private void openCategory(String slug, String label) {
        section = "cat:" + slug;
        stopMotion();
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
        for (String[] cat : SECTIONS) {
            TextView chip = new TextView(this);
            chip.setText(cat[1]);
            chip.setTextColor(0xFF0B5C28);
            style(chip, 18, R.font.mukta_bold);
            chip.setGravity(Gravity.CENTER_VERTICAL);
            Drawable icon = DrawableCompat.wrap(getDrawable(iconFor(cat[0]))).mutate();
            DrawableCompat.setTint(icon, 0xFF0B5C28);
            icon.setBounds(0, 0, dp(18), dp(18));
            chip.setCompoundDrawables(icon, null, null, null);
            chip.setCompoundDrawablePadding(dp(6));
            chip.setBackgroundResource(R.drawable.bg_chip);
            chip.setPadding(dp(14), dp(8), dp(14), dp(8));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.rightMargin = dp(8);
            chip.setOnClickListener(v -> openCategory(cat[0], cat[1]));
            row.addView(chip, lp);
        }
        scroll.addView(row);
        return scroll;
    }

    private View slider(List<Story> stories) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        pager = new ViewPager2(this);
        pager.setAdapter(new HeroAdapter(stories));
        pager.setOffscreenPageLimit(stories.size());
        LinearLayout.LayoutParams pagerLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(268));
        pagerLp.bottomMargin = dp(8);
        box.addView(pager, pagerLp);
        LinearLayout dots = new LinearLayout(this);
        dots.setGravity(Gravity.CENTER);
        View[] marks = new View[stories.size()];
        for (int i = 0; i < stories.size(); i++) {
            View dot = new View(this);
            dot.setBackgroundResource(i == 0 ? R.drawable.bg_dot : R.drawable.bg_dot_off);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(i == 0 ? 18 : 8), dp(8));
            lp.leftMargin = dp(4);
            lp.rightMargin = dp(4);
            dots.addView(dot, lp);
            marks[i] = dot;
        }
        box.addView(dots);
        pager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                for (int i = 0; i < marks.length; i++) {
                    marks[i].setBackgroundResource(i == position ? R.drawable.bg_dot : R.drawable.bg_dot_off);
                    ViewGroup.LayoutParams lp = marks[i].getLayoutParams();
                    lp.width = dp(i == position ? 18 : 8);
                    marks[i].setLayoutParams(lp);
                }
                scheduleSlide(stories.size());
            }
        });
        scheduleSlide(stories.size());
        return box;
    }

    private void scheduleSlide(int count) {
        sliderHandler.removeCallbacksAndMessages(null);
        if (count < 2 || pager == null) return;
        sliderHandler.postDelayed(() -> {
            if (!"home".equals(section) || pager == null) return;
            int next = (pager.getCurrentItem() + 1) % count;
            pager.setCurrentItem(next, true);
        }, 3500);
    }

    private View ticker(List<Story> items) {
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setBackgroundResource(R.drawable.bg_ticker);
        bar.setPadding(dp(8), dp(8), dp(8), dp(8));
        TextView badge = new TextView(this);
        badge.setText("ताजा");
        badge.setTextColor(0xFFFFFFFF);
        badge.setTextSize(12);
        badge.setBackgroundResource(R.drawable.bg_badge);
        badge.setPadding(dp(10), dp(4), dp(10), dp(4));
        bar.addView(badge);
        FrameLayout clip = new FrameLayout(this);
        clip.setClipChildren(true);
        TextView moving = new TextView(this);
        StringBuilder text = new StringBuilder();
        for (int pass = 0; pass < 2; pass++) {
            for (Story item : items) {
                text.append(item.title).append("     •     ");
            }
        }
        moving.setText(text.toString());
        moving.setTextColor(0xFFFFFFFF);
        moving.setTextSize(14);
        moving.setSingleLine(true);
        clip.addView(moving, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER_VERTICAL));
        bar.addView(clip, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(8);
        lp.bottomMargin = dp(4);
        bar.setLayoutParams(lp);
        bar.setOnClickListener(v -> openStory(items.get(0)));
        moving.post(() -> {
            int travel = clip.getWidth() + moving.getWidth();
            if (travel <= 0) return;
            moving.setTranslationX(clip.getWidth());
            if (tickerAnim != null) tickerAnim.cancel();
            tickerAnim = ObjectAnimator.ofFloat(moving, "translationX", clip.getWidth(), -moving.getWidth());
            tickerAnim.setDuration(Math.max(12000, travel * 14L));
            tickerAnim.setRepeatCount(ValueAnimator.INFINITE);
            tickerAnim.setInterpolator(new LinearInterpolator());
            tickerAnim.start();
        });
        return bar;
    }

    private View categoryBlock(String label, String slug, List<Story> stories) {
        LinearLayout block = new LinearLayout(this);
        block.setOrientation(LinearLayout.VERTICAL);
        block.setPadding(0, dp(16), 0, dp(4));
        LinearLayout head = new LinearLayout(this);
        head.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = new TextView(this);
        title.setText(label);
        title.setTextColor(0xFF0B5C28);
        style(title, 22, R.font.mukta_extrabold);
        head.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView more = new TextView(this);
        more.setText("सबै");
        more.setTextColor(0xFFF15A22);
        more.setTextSize(14);
        more.setOnClickListener(v -> openCategory(slug, label));
        head.addView(more);
        block.addView(head);
        for (Story story : stories) block.addView(compact(story));
        return block;
    }

    private View compact(Story story) {
        MaterialCardView card = new MaterialCardView(this);
        card.setRadius(dp(14));
        card.setCardElevation(dp(1));
        card.setCardBackgroundColor(0xFFFFFFFF);
        card.setUseCompatPadding(true);
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(8), dp(8), dp(10), dp(8));
        if (!story.image.isEmpty()) {
            ImageView image = new ImageView(this);
            image.setScaleType(ImageView.ScaleType.CENTER_CROP);
            image.setBackgroundColor(0xFFE8F6EE);
            row.addView(image, new LinearLayout.LayoutParams(dp(92), dp(72)));
            loadImage(story.image, image);
        }
        TextView title = new TextView(this);
        title.setText(story.title);
        title.setTextColor(0xFF1C1917);
        style(title, 17, R.font.mukta_semibold);
        title.setMaxLines(3);
        title.setPadding(dp(10), 0, 0, 0);
        row.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        card.addView(row);
        card.setOnClickListener(v -> openStory(story));
        return card;
    }

    private void stopMotion() {
        homeGen++;
        sliderHandler.removeCallbacksAndMessages(null);
        if (tickerAnim != null) tickerAnim.cancel();
        pager = null;
    }

    @Override
    protected void onDestroy() {
        stopMotion();
        super.onDestroy();
    }

    private View card(Story story, boolean hero) {
        MaterialCardView card = new MaterialCardView(this);
        card.setRadius(dp(16));
        card.setCardElevation(dp(2));
        card.setCardBackgroundColor(0xFFFFFFFF);
        card.setUseCompatPadding(true);
        LinearLayout inner = new LinearLayout(this);
        inner.setOrientation(LinearLayout.VERTICAL);
        if (!story.image.isEmpty()) {
            ImageView image = new ImageView(this);
            image.setScaleType(ImageView.ScaleType.CENTER_CROP);
            image.setBackgroundColor(0xFFE8F6EE);
            inner.addView(image, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, hero ? dp(210) : dp(150)));
            loadImage(story.image, image);
        }
        LinearLayout pad = new LinearLayout(this);
        pad.setOrientation(LinearLayout.VERTICAL);
        pad.setPadding(dp(14), dp(12), dp(14), dp(14));
        TextView kicker = new TextView(this);
        kicker.setText(story.category.isEmpty() ? (hero ? "हेडलाइन" : "समाचार") : story.category);
        kicker.setTextColor(0xFFF15A22);
        kicker.setTextSize(12);
        pad.addView(kicker);
        TextView title = new TextView(this);
        title.setText(story.title);
        title.setTextColor(0xFF1C1917);
        title.setTextSize(hero ? 20 : 16);
        title.setPadding(0, dp(4), 0, 0);
        pad.addView(title);
        String meta = (story.author.isEmpty() ? "कलैया अनलाइन" : story.author) + (story.date.isEmpty() ? "" : " · " + story.date);
        TextView line = new TextView(this);
        line.setText(meta);
        line.setTextColor(0xFF78716C);
        line.setTextSize(12);
        line.setPadding(0, dp(6), 0, 0);
        pad.addView(line);
        inner.addView(pad);
        card.addView(inner);
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
        view.setTextSize(15);
        view.setBackgroundColor(0xFFE8F6EE);
        view.setPadding(dp(16), dp(10), dp(16), dp(10));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(-dp(12), -dp(8), -dp(12), dp(8));
        view.setLayoutParams(lp);
        return view;
    }

    private int iconFor(String slug) {
        if ("local".equals(slug)) return R.drawable.ic_pin;
        if ("international".equals(slug)) return R.drawable.ic_cat_globe;
        if ("politics".equals(slug)) return R.drawable.ic_cat_politics;
        if ("artha".equals(slug)) return R.drawable.ic_cat_wallet;
        if ("khelkud".equals(slug)) return R.drawable.ic_cat_trophy;
        if ("entertainment".equals(slug)) return R.drawable.ic_cat_film;
        if ("lekhabichar".equals(slug)) return R.drawable.ic_cat_pen;
        return R.drawable.ic_cat_news;
    }

    private void style(TextView view, int sizePx, int font) {
        view.setTextSize(TypedValue.COMPLEX_UNIT_DIP, sizePx);
        view.setTypeface(ResourcesCompat.getFont(this, font));
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private final class HeroAdapter extends RecyclerView.Adapter<HeroAdapter.Holder> {
        private final List<Story> stories;

        HeroAdapter(List<Story> stories) {
            this.stories = stories;
        }

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            FrameLayout frame = new FrameLayout(MainActivity.this);
            frame.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            return new Holder(frame);
        }

        @Override
        public void onBindViewHolder(@NonNull Holder holder, int position) {
            holder.bind(stories.get(position));
        }

        @Override
        public int getItemCount() {
            return stories.size();
        }

        final class Holder extends RecyclerView.ViewHolder {
            Holder(FrameLayout frame) {
                super(frame);
            }

            void bind(Story story) {
                FrameLayout frame = (FrameLayout) itemView;
                frame.removeAllViews();
                ImageView image = new ImageView(MainActivity.this);
                image.setScaleType(ImageView.ScaleType.CENTER_CROP);
                image.setBackgroundColor(0xFF0B5C28);
                frame.addView(image, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                View shade = new View(MainActivity.this);
                shade.setBackgroundResource(R.drawable.bg_shade);
                frame.addView(shade, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(140), Gravity.BOTTOM));
                TextView badge = new TextView(MainActivity.this);
                badge.setText("हेडलाइन");
                badge.setTextColor(0xFFFFFFFF);
                badge.setTextSize(12);
                badge.setBackgroundResource(R.drawable.bg_badge);
                badge.setPadding(dp(10), dp(4), dp(10), dp(4));
                FrameLayout.LayoutParams badgeLp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP | Gravity.START);
                badgeLp.setMargins(dp(14), dp(14), 0, 0);
                frame.addView(badge, badgeLp);
                TextView title = new TextView(MainActivity.this);
                title.setText(story.title);
                title.setTextColor(0xFFFFFFFF);
                style(title, 25, R.font.mukta_bold);
                title.setPadding(dp(14), 0, dp(14), dp(16));
                frame.addView(title, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM));
                frame.setOnClickListener(v -> openStory(story));
                if (!story.image.isEmpty()) loadImage(story.image, image);
            }
        }
    }
}
