package com.kalaiyaonline.news;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.text.TextUtils;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.graphics.drawable.DrawableCompat;

import com.google.android.gms.ads.AdView;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.card.MaterialCardView;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ArticleActivity extends AppCompatActivity {
    private final SiteClient client = new SiteClient();
    private final ExecutorService io = Executors.newFixedThreadPool(2);
    private SharedPreferences prefs;
    private String slug = "";
    private TextView categoryView;
    private TextView dateView;
    private TextView authorView;
    private TextView viewsView;
    private TextView saveView;
    private TextView bodyView;
    private String keptTitle = "";
    private String keptImage = "";
    private String keptCategory = "";
    private String keptDate = "";
    private LinearLayout relatedBox;
    private AdView articleAd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences("article", MODE_PRIVATE);
        slug = getIntent().getStringExtra("slug");
        String fallbackTitle = getIntent().getStringExtra("title");
        String fallbackImage = getIntent().getStringExtra("image");
        String fallbackCategory = getIntent().getStringExtra("category");
        String fallbackDate = getIntent().getStringExtra("date");
        keptTitle = fallbackTitle == null ? "" : fallbackTitle;
        keptImage = fallbackImage == null ? "" : fallbackImage;
        keptCategory = fallbackCategory == null ? "" : fallbackCategory;
        keptDate = fallbackDate == null ? "" : fallbackDate;

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFFFFFFFF);
        root.addView(homeHeader());
        TextView back = new TextView(this);
        back.setText("‹");
        back.setTextColor(0xFF0B5C28);
        back.setTextSize(22);
        back.setGravity(Gravity.CENTER);
        back.setBackgroundResource(R.drawable.bg_chip);
        LinearLayout.LayoutParams backLp = new LinearLayout.LayoutParams(dp(40), dp(40));
        backLp.setMargins(dp(12), dp(10), dp(12), dp(4));
        back.setLayoutParams(backLp);
        back.setOnClickListener(v -> finish());
        root.addView(back);

        android.widget.ScrollView scroll = new android.widget.ScrollView(this);
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(16), dp(4), dp(16), dp(28));
        page.setGravity(Gravity.CENTER_HORIZONTAL);
        page.setBackgroundColor(0xFFFFFFFF);

        categoryView = new TextView(this);
        categoryView.setText(fallbackCategory == null || fallbackCategory.isEmpty() ? "समाचार" : fallbackCategory);
        categoryView.setTextColor(0xFFFFFFFF);
        categoryView.setBackgroundResource(R.drawable.bg_badge);
        categoryView.setPadding(dp(14), dp(6), dp(14), dp(6));
        categoryView.setTextSize(13);
        categoryView.setTypeface(ResourcesCompat.getFont(this, R.font.mukta_bold));
        LinearLayout.LayoutParams catLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        catLp.gravity = Gravity.CENTER_HORIZONTAL;
        categoryView.setLayoutParams(catLp);
        page.addView(categoryView);

        TextView title = new TextView(this);
        title.setText(fallbackTitle == null ? "" : fallbackTitle);
        title.setTextColor(0xFF1C1917);
        title.setGravity(Gravity.CENTER);
        title.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 30);
        Typeface bold = ResourcesCompat.getFont(this, R.font.mukta_bold);
        title.setTypeface(bold);
        title.setIncludeFontPadding(false);
        title.setLineSpacing(0, 0.92f);
        if (Build.VERSION.SDK_INT >= 28) {
            title.setFallbackLineSpacing(false);
            title.setLineHeight(dp(40));
        }
        title.setPadding(dp(2), dp(12), dp(2), dp(2));
        page.addView(title);

        dateView = new TextView(this);
        dateView.setText(fallbackDate == null ? "" : fallbackDate);
        dateView.setTextColor(0xFF78716C);
        dateView.setGravity(Gravity.CENTER);
        dateView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        dateView.setTypeface(ResourcesCompat.getFont(this, R.font.mukta_regular));
        dateView.setPadding(0, dp(6), 0, dp(4));
        page.addView(dateView);

        LinearLayout meta = new LinearLayout(this);
        meta.setOrientation(LinearLayout.HORIZONTAL);
        meta.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams metaLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        metaLp.topMargin = dp(10);
        metaLp.bottomMargin = dp(8);
        meta.setLayoutParams(metaLp);
        meta.setGravity(Gravity.CENTER);

        ImageView authorIcon = new ImageView(this);
        authorIcon.setImageResource(R.drawable.ic_user);
        authorIcon.setPadding(dp(6), dp(6), dp(6), dp(6));
        GradientDrawable authorBg = new GradientDrawable();
        authorBg.setShape(GradientDrawable.OVAL);
        authorBg.setColor(0xFF0F8A3C);
        authorIcon.setBackground(authorBg);
        meta.addView(authorIcon, new LinearLayout.LayoutParams(dp(32), dp(32)));

        authorView = new TextView(this);
        authorView.setText("कलैया अनलाइन");
        authorView.setTextColor(0xFF1C1917);
        authorView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        authorView.setTypeface(bold);
        authorView.setMaxLines(1);
        authorView.setEllipsize(TextUtils.TruncateAt.END);
        authorView.setPadding(dp(8), 0, dp(8), 0);
        meta.addView(authorView);

        meta.addView(metaDivider());

        ImageView eye = new ImageView(this);
        eye.setImageResource(R.drawable.ic_eye);
        LinearLayout.LayoutParams eyeLp = new LinearLayout.LayoutParams(dp(18), dp(18));
        eyeLp.leftMargin = dp(8);
        meta.addView(eye, eyeLp);

        viewsView = new TextView(this);
        viewsView.setText("—");
        viewsView.setTextColor(0xFF0B5C28);
        viewsView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        viewsView.setTypeface(bold);
        viewsView.setPadding(dp(4), 0, dp(8), 0);
        meta.addView(viewsView);

        meta.addView(metaDivider());

        saveView = roundAction("सेभ", R.drawable.ic_bookmark, 0xFF0F8A3C);
        LinearLayout.LayoutParams saveLp = (LinearLayout.LayoutParams) saveView.getLayoutParams();
        saveLp.leftMargin = dp(8);
        meta.addView(saveView);
        page.addView(meta);
        paintActions();
        saveView.setOnClickListener(v -> {
            SavedStore.set(this, keptStory(), !saved());
            paintActions();
        });

        MaterialCardView frame = new MaterialCardView(this);
        frame.setRadius(dp(18));
        frame.setCardElevation(0);
        frame.setStrokeWidth(0);
        frame.setCardBackgroundColor(0xFFFFFFFF);
        ImageView image = new ImageView(this);
        image.setScaleType(ImageView.ScaleType.CENTER_CROP);
        frame.addView(image, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(230)));
        LinearLayout.LayoutParams frameLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        frameLp.topMargin = dp(8);
        page.addView(frame, frameLp);

        LinearLayout shares = new LinearLayout(this);
        shares.setOrientation(LinearLayout.HORIZONTAL);
        shares.setGravity(Gravity.CENTER);
        String link = SiteClient.ORIGIN + "/news/" + slug;
        shares.addView(shareButton("WhatsApp", 0xFF25D366, "com.whatsapp", link, title));
        shares.addView(shareButton("Facebook", 0xFF1877F2, "com.facebook.katana", link, title));
        shares.addView(shareButton("Viber", 0xFF7360F2, "com.viber.voip", link, title));
        shares.addView(shareButton("X", 0xFF111111, "com.twitter.android", link, title));
        shares.addView(shareButton("थप", 0xFF0F8A3C, null, link, title));
        LinearLayout.LayoutParams shareLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        shareLp.topMargin = dp(10);
        shareLp.bottomMargin = dp(4);
        page.addView(shares, shareLp);

        bodyView = new TextView(this);
        bodyView.setText("पूर्ण समाचार लोड हुँदैछ…");
        bodyView.setTextColor(0xFF292524);
        bodyView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 19);
        bodyView.setLineSpacing(dp(8), 1f);
        bodyView.setPadding(dp(2), dp(12), dp(2), dp(8));
        page.addView(bodyView);

        relatedBox = new LinearLayout(this);
        relatedBox.setOrientation(LinearLayout.VERTICAL);
        relatedBox.setVisibility(View.GONE);
        page.addView(relatedBox);

        scroll.addView(page);
        root.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        root.setPadding(0, 0, 0, dp(50));
        articleAd = Ads.attach(this, root);
        FrameLayout screen = new FrameLayout(this);
        screen.addView(root, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        View hairline = new View(this);
        hairline.setBackgroundColor(0xFFE7E5E4);
        FrameLayout.LayoutParams lineLp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1), Gravity.BOTTOM);
        lineLp.bottomMargin = dp(50);
        screen.addView(hairline, lineLp);
        BottomNavigationView nav = new BottomNavigationView(this);
        nav.inflateMenu(R.menu.bottom_nav);
        nav.setBackgroundColor(0xFFFFFFFF);
        nav.setItemIconTintList(getColorStateList(R.color.nav_tint));
        nav.setItemTextColor(getColorStateList(R.color.nav_tint));
        nav.setLabelVisibilityMode(BottomNavigationView.LABEL_VISIBILITY_LABELED);
        nav.setItemIconSize(dp(26));
        nav.setItemPaddingTop(dp(2));
        nav.setItemPaddingBottom(dp(1));
        nav.setItemActiveIndicatorEnabled(false);
        nav.setItemTextAppearanceActive(R.style.NavLabel);
        nav.setItemTextAppearanceInactive(R.style.NavLabel);
        nav.setElevation(dp(8));
        nav.setOnItemSelectedListener(item -> {
            leave(navTarget(item.getItemId()));
            return true;
        });
        nav.setOnItemReselectedListener(item -> leave(navTarget(item.getItemId())));
        screen.addView(nav, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(50), Gravity.BOTTOM));
        setContentView(screen);

        if (fallbackImage != null && !fallbackImage.isEmpty()) load(fallbackImage, image);
        io.execute(() -> {
            try {
                Story story = client.article(slug);
                runOnUiThread(() -> {
                    if (!story.category.isEmpty()) {
                        categoryView.setText(story.category);
                        keptCategory = story.category;
                    }
                    if (!story.title.isEmpty()) {
                        title.setText(story.title);
                        keptTitle = story.title;
                    }
                    if (!story.date.isEmpty()) {
                        dateView.setText(story.date);
                        keptDate = story.date;
                    }
                    if (!story.author.isEmpty()) authorView.setText(story.author);
                    viewsView.setText(story.views.isEmpty() ? "—" : story.views);
                    bodyView.setText(story.body.isEmpty() ? "पूर्ण पाठ भेटिएन।" : story.body);
                    if (!story.image.isEmpty()) {
                        keptImage = story.image;
                        load(story.image, image);
                    }
                    showRelated(story.related);
                });
            } catch (Exception error) {
                runOnUiThread(() -> bodyView.setText("समाचार खोल्न सकिएन।"));
            }
        });
    }

    private View homeHeader() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setBackgroundColor(0xFFFFFFFF);
        box.setElevation(dp(2));
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(16), dp(10), dp(12), dp(10));
        ImageView logo = new ImageView(this);
        logo.setAdjustViewBounds(true);
        logo.setScaleType(ImageView.ScaleType.FIT_START);
        logo.setImageResource(R.drawable.wordmark);
        row.addView(logo, new LinearLayout.LayoutParams(0, dp(38), 1f));
        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setGravity(Gravity.CENTER_VERTICAL);
        actions.setPadding(dp(3), dp(3), dp(3), dp(3));
        GradientDrawable pill = new GradientDrawable();
        pill.setColor(0xFFF3F6F4);
        pill.setCornerRadius(dp(22));
        actions.setBackground(pill);
        actions.addView(headerButton(R.drawable.ic_bell, () -> leave("bell")));
        actions.addView(headerButton(R.drawable.ic_search, () -> leave("search")));
        actions.addView(headerButton(R.drawable.ic_menu, () -> leave("menu")));
        row.addView(actions);
        box.addView(row);
        View accent = new View(this);
        accent.setBackground(new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, new int[]{0xFF0B5C28, 0xFF18A34A, 0xFFF15A22}));
        box.addView(accent, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(3)));
        return box;
    }

    private View headerButton(int icon, Runnable action) {
        ImageView view = new ImageView(this);
        view.setImageResource(icon);
        view.setPadding(dp(8), dp(8), dp(8), dp(8));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(36), dp(36));
        lp.leftMargin = dp(1);
        lp.rightMargin = dp(1);
        view.setLayoutParams(lp);
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(18));
        bg.setColor(0xFFF3F6F4);
        view.setBackground(bg);
        view.setOnClickListener(v -> action.run());
        return view;
    }

    private String navTarget(int id) {
        if (id == R.id.nav_gallery) return "gallery";
        if (id == R.id.nav_directory) return "directory";
        if (id == R.id.nav_saved) return "saved";
        if (id == R.id.nav_more) return "more";
        return "home";
    }

    private void leave(String open) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra("open", open);
        startActivity(intent);
        finish();
    }

    private View brandHeader() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setBackgroundColor(0xFFFFFFFF);
        ImageView logo = new ImageView(this);
        logo.setAdjustViewBounds(true);
        logo.setImageResource(R.drawable.wordmark);
        int pad = dp(10);
        logo.setPadding(pad, pad, pad, dp(6));
        box.addView(logo, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(56)));
        View accent = new View(this);
        accent.setBackground(new android.graphics.drawable.GradientDrawable(android.graphics.drawable.GradientDrawable.Orientation.LEFT_RIGHT, new int[]{0xFF0B5C28, 0xFF18A34A, 0xFFF15A22}));
        box.addView(accent, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(3)));
        return box;
    }

    private View metaDivider() {
        View line = new View(this);
        line.setBackgroundColor(0xFFE7E5E4);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(1), dp(18));
        lp.leftMargin = dp(2);
        lp.rightMargin = dp(2);
        line.setLayoutParams(lp);
        return line;
    }

    private TextView roundAction(String label, int icon, int tint) {
        TextView view = new TextView(this);
        view.setText(label);
        view.setTextSize(12);
        view.setGravity(Gravity.CENTER);
        view.setCompoundDrawablePadding(dp(4));
        Drawable drawable = DrawableCompat.wrap(getDrawable(icon)).mutate();
        drawable.setBounds(0, 0, dp(18), dp(18));
        view.setTag(drawable);
        view.setTag(0x7f010001, tint);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(44));
        lp.leftMargin = dp(6);
        lp.rightMargin = dp(6);
        view.setLayoutParams(lp);
        view.setPadding(dp(12), 0, dp(14), 0);
        view.setBackgroundResource(R.drawable.bg_chip);
        return view;
    }

    private void paintActions() {
        paintOne(saveView, saved());
    }

    private void paintOne(TextView view, boolean on) {
        int tint = on ? 0xFFFFFFFF : (Integer) view.getTag(0x7f010001);
        Drawable drawable = (Drawable) view.getTag();
        DrawableCompat.setTint(drawable, tint);
        view.setCompoundDrawables(drawable, null, null, null);
        view.setTextColor(on ? 0xFFFFFFFF : 0xFF1C1917);
        view.setBackgroundResource(on ? R.drawable.bg_badge : R.drawable.bg_chip);
    }

    private void showRelated(java.util.List<Story> stories) {
        relatedBox.removeAllViews();
        if (stories == null || stories.isEmpty()) {
            relatedBox.setVisibility(View.GONE);
            return;
        }
        relatedBox.setVisibility(View.VISIBLE);
        relatedBox.setPadding(0, dp(16), 0, dp(8));
        TextView head = new TextView(this);
        head.setText("सम्बन्धित समाचार");
        head.setTextColor(0xFF0B5C28);
        head.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
        head.setTypeface(ResourcesCompat.getFont(this, R.font.mukta_extrabold));
        head.setPadding(dp(2), 0, 0, dp(8));
        relatedBox.addView(head);
        LinearLayout row = null;
        int count = Math.min(4, stories.size());
        for (int i = 0; i < count; i++) {
            if (i % 2 == 0) {
                row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                relatedBox.addView(row);
            }
            LinearLayout.LayoutParams cell = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            cell.setMargins(dp(4), dp(4), dp(4), dp(4));
            row.addView(overlayCell(stories.get(i)), cell);
        }
        if (count % 2 == 1) {
            row.addView(new View(this), new LinearLayout.LayoutParams(0, dp(1), 1f));
        }
    }

    private View overlayCell(Story story) {
        MaterialCardView card = new MaterialCardView(this);
        card.setRadius(dp(14));
        card.setCardElevation(0);
        card.setStrokeWidth(0);
        card.setCardBackgroundColor(0xFF14532D);
        FrameLayout frame = new FrameLayout(this);
        ImageView image = new ImageView(this);
        image.setScaleType(ImageView.ScaleType.CENTER_CROP);
        image.setBackgroundColor(0xFF14532D);
        frame.addView(image, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(132)));
        if (!story.image.isEmpty()) load(story.image, image);
        View shade = new View(this);
        GradientDrawable fade = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[]{0x00000000, 0xE6000000});
        shade.setBackground(fade);
        frame.addView(shade, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(78), Gravity.BOTTOM));
        TextView title = new TextView(this);
        title.setText(story.title);
        title.setTextColor(0xFFFFFFFF);
        title.setMaxLines(3);
        title.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        title.setTypeface(ResourcesCompat.getFont(this, R.font.mukta_bold));
        title.setPadding(dp(8), dp(6), dp(8), dp(8));
        frame.addView(title, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM));
        card.addView(frame);
        card.setOnClickListener(v -> openRelated(story));
        return card;
    }

    private void openRelated(Story story) {
        Intent intent = new Intent(this, ArticleActivity.class);
        intent.putExtra("slug", story.slug);
        intent.putExtra("title", story.title);
        intent.putExtra("image", story.image);
        startActivity(intent);
    }

    private LinearLayout shareButton(String label, int color, String pkg, String link, TextView title) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER);
        TextView mark = new TextView(this);
        mark.setText(label.substring(0, 1));
        mark.setTextColor(0xFFFFFFFF);
        mark.setTextSize(12);
        mark.setGravity(Gravity.CENTER);
        mark.setBackgroundResource(R.drawable.bg_dot);
        mark.setBackgroundTintList(android.content.res.ColorStateList.valueOf(color));
        box.addView(mark, new LinearLayout.LayoutParams(dp(32), dp(32)));
        TextView name = new TextView(this);
        name.setText(label);
        name.setTextSize(9);
        name.setTextColor(0xFF78716C);
        name.setGravity(Gravity.CENTER);
        name.setPadding(0, dp(2), 0, 0);
        box.addView(name);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.leftMargin = dp(5);
        lp.rightMargin = dp(5);
        box.setLayoutParams(lp);
        box.setOnClickListener(v -> share(pkg, title.getText().toString(), link));
        return box;
    }

    private void share(String pkg, String title, String link) {
        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType("text/plain");
        send.putExtra(Intent.EXTRA_TEXT, title + "\n" + link);
        if (pkg != null) send.setPackage(pkg);
        try {
            startActivity(pkg == null ? Intent.createChooser(send, "सेयर") : send);
        } catch (Exception error) {
            send.setPackage(null);
            startActivity(Intent.createChooser(send, "सेयर"));
        }
    }

    private Story keptStory() {
        return new Story(slug, keptTitle, keptImage, keptCategory, keptDate, "", "");
    }

    private boolean saved() {
        return SavedStore.isSaved(this, slug);
    }

    private void load(String url, ImageView target) {
        io.execute(() -> {
            HttpURLConnection connection = null;
            try {
                connection = (HttpURLConnection) new URL(url).openConnection();
                connection.setConnectTimeout(12000);
                connection.setReadTimeout(12000);
                InputStream stream = connection.getInputStream();
                Bitmap bitmap = BitmapFactory.decodeStream(stream);
                if (bitmap != null) runOnUiThread(() -> target.setImageBitmap(bitmap));
            } catch (Exception ignored) {
            } finally {
                if (connection != null) connection.disconnect();
            }
        });
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (articleAd != null) articleAd.resume();
    }

    @Override
    protected void onPause() {
        if (articleAd != null) articleAd.pause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (articleAd != null) articleAd.destroy();
        super.onDestroy();
    }
}
