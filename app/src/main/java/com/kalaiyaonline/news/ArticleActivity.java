package com.kalaiyaonline.news;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.graphics.drawable.DrawableCompat;

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
    private TextView saveView;
    private TextView likeView;
    private TextView dislikeView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences("article", MODE_PRIVATE);
        slug = getIntent().getStringExtra("slug");
        String fallbackTitle = getIntent().getStringExtra("title");
        String fallbackImage = getIntent().getStringExtra("image");
        String fallbackCategory = getIntent().getStringExtra("category");
        String fallbackDate = getIntent().getStringExtra("date");

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFFF6F7F4);
        TextView back = new TextView(this);
        back.setText("‹  समाचार");
        back.setTextColor(0xFF0F8A3C);
        back.setTextSize(16);
        back.setPadding(dp(16), dp(14), dp(16), dp(8));
        back.setOnClickListener(v -> finish());
        root.addView(back);

        android.widget.ScrollView scroll = new android.widget.ScrollView(this);
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(16), dp(4), dp(16), dp(28));
        page.setGravity(Gravity.CENTER_HORIZONTAL);

        categoryView = new TextView(this);
        categoryView.setText(fallbackCategory == null || fallbackCategory.isEmpty() ? "समाचार" : fallbackCategory);
        categoryView.setTextColor(0xFFFFFFFF);
        categoryView.setBackgroundResource(R.drawable.bg_badge);
        categoryView.setPadding(dp(14), dp(6), dp(14), dp(6));
        categoryView.setTextSize(13);
        LinearLayout.LayoutParams catLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        catLp.gravity = Gravity.CENTER_HORIZONTAL;
        categoryView.setLayoutParams(catLp);
        page.addView(categoryView);

        TextView title = new TextView(this);
        title.setText(fallbackTitle == null ? "" : fallbackTitle);
        title.setTextColor(0xFF1C1917);
        title.setGravity(Gravity.CENTER);
        title.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 25);
        Typeface bold = ResourcesCompat.getFont(this, R.font.mukta_bold);
        title.setTypeface(bold);
        title.setPadding(dp(8), dp(14), dp(8), dp(8));
        page.addView(title);

        dateView = new TextView(this);
        dateView.setText(fallbackDate == null ? "" : fallbackDate);
        dateView.setTextColor(0xFF78716C);
        dateView.setGravity(Gravity.CENTER);
        dateView.setTextSize(14);
        page.addView(dateView);

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams actionLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        actionLp.topMargin = dp(12);
        actionLp.bottomMargin = dp(16);
        actions.setLayoutParams(actionLp);
        saveView = roundAction("सेभ", R.drawable.ic_bookmark, 0xFF0F8A3C);
        likeView = roundAction("मन पर्यो", R.drawable.ic_like, 0xFF0F8A3C);
        dislikeView = roundAction("मन परेन", R.drawable.ic_dislike, 0xFFB45309);
        actions.addView(saveView);
        actions.addView(likeView);
        actions.addView(dislikeView);
        page.addView(actions);
        paintActions();
        saveView.setOnClickListener(v -> {
            prefs.edit().putBoolean(key("saved"), !saved()).apply();
            paintActions();
        });
        likeView.setOnClickListener(v -> {
            prefs.edit().putString(key("react"), "like".equals(reaction()) ? "" : "like").apply();
            paintActions();
        });
        dislikeView.setOnClickListener(v -> {
            prefs.edit().putString(key("react"), "dislike".equals(reaction()) ? "" : "dislike").apply();
            paintActions();
        });

        MaterialCardView frame = new MaterialCardView(this);
        frame.setRadius(dp(16));
        frame.setCardElevation(0);
        ImageView image = new ImageView(this);
        image.setScaleType(ImageView.ScaleType.CENTER_CROP);
        image.setBackgroundColor(0xFFE8F6EE);
        frame.addView(image, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(220)));
        page.addView(frame, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView body = new TextView(this);
        body.setText("पूर्ण समाचार लोड हुँदैछ…");
        body.setTextColor(0xFF1C1917);
        body.setTextSize(17);
        body.setLineSpacing(dp(6), 1f);
        body.setPadding(0, dp(16), 0, dp(8));
        page.addView(body);

        TextView shareLabel = new TextView(this);
        shareLabel.setText("सेयर गर्नुहोस्");
        shareLabel.setGravity(Gravity.CENTER);
        shareLabel.setTextColor(0xFF0B5C28);
        shareLabel.setTextSize(16);
        shareLabel.setTypeface(bold);
        shareLabel.setPadding(0, dp(18), 0, dp(10));
        page.addView(shareLabel);

        LinearLayout shares = new LinearLayout(this);
        shares.setOrientation(LinearLayout.HORIZONTAL);
        shares.setGravity(Gravity.CENTER);
        String link = SiteClient.ORIGIN + "/news/" + slug;
        shares.addView(shareButton("WhatsApp", 0xFF25D366, "com.whatsapp", link, title));
        shares.addView(shareButton("Facebook", 0xFF1877F2, "com.facebook.katana", link, title));
        shares.addView(shareButton("Viber", 0xFF7360F2, "com.viber.voip", link, title));
        shares.addView(shareButton("X", 0xFF111111, "com.twitter.android", link, title));
        shares.addView(shareButton("थप", 0xFF0F8A3C, null, link, title));
        page.addView(shares);

        scroll.addView(page);
        root.addView(scroll);
        setContentView(root);

        if (fallbackImage != null && !fallbackImage.isEmpty()) load(fallbackImage, image);
        io.execute(() -> {
            try {
                Story story = client.article(slug);
                runOnUiThread(() -> {
                    if (!story.category.isEmpty()) categoryView.setText(story.category);
                    if (!story.title.isEmpty()) title.setText(story.title);
                    if (!story.date.isEmpty()) dateView.setText(story.date);
                    body.setText(story.body.isEmpty() ? "पूर्ण पाठ भेटिएन।" : story.body);
                    if (!story.image.isEmpty()) load(story.image, image);
                });
            } catch (Exception error) {
                runOnUiThread(() -> body.setText("समाचार खोल्न सकिएन।"));
            }
        });
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
        paintOne(likeView, "like".equals(reaction()));
        paintOne(dislikeView, "dislike".equals(reaction()));
    }

    private void paintOne(TextView view, boolean on) {
        int tint = on ? 0xFFFFFFFF : (Integer) view.getTag(0x7f010001);
        Drawable drawable = (Drawable) view.getTag();
        DrawableCompat.setTint(drawable, tint);
        view.setCompoundDrawables(drawable, null, null, null);
        view.setTextColor(on ? 0xFFFFFFFF : 0xFF1C1917);
        view.setBackgroundResource(on ? R.drawable.bg_badge : R.drawable.bg_chip);
    }

    private LinearLayout shareButton(String label, int color, String pkg, String link, TextView title) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER);
        TextView mark = new TextView(this);
        mark.setText(label.substring(0, 1));
        mark.setTextColor(0xFFFFFFFF);
        mark.setTextSize(16);
        mark.setGravity(Gravity.CENTER);
        mark.setBackgroundResource(R.drawable.bg_dot);
        mark.setBackgroundTintList(android.content.res.ColorStateList.valueOf(color));
        box.addView(mark, new LinearLayout.LayoutParams(dp(48), dp(48)));
        TextView name = new TextView(this);
        name.setText(label);
        name.setTextSize(11);
        name.setTextColor(0xFF57534E);
        name.setGravity(Gravity.CENTER);
        name.setPadding(0, dp(4), 0, 0);
        box.addView(name);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.leftMargin = dp(8);
        lp.rightMargin = dp(8);
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

    private boolean saved() {
        return prefs.getBoolean(key("saved"), false);
    }

    private String reaction() {
        return prefs.getString(key("react"), "");
    }

    private String key(String name) {
        return name + ":" + slug;
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
}
