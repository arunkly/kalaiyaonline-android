package com.kalaiyaonline.news;

import android.app.Dialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;

import com.google.android.material.card.MaterialCardView;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GalleryActivity extends AppCompatActivity {
    private final SiteClient client = new SiteClient();
    private final ExecutorService io = Executors.newFixedThreadPool(3);
    private final List<String> photos = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String url = getIntent().getStringExtra("url");
        String fallbackTitle = getIntent().getStringExtra("title");
        String fallbackImage = getIntent().getStringExtra("image");
        String fallbackCategory = getIntent().getStringExtra("category");

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFFF3F5F2);
        root.addView(brandHeader());

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

        TextView category = label(fallbackCategory == null || fallbackCategory.isEmpty() ? "फोटो" : fallbackCategory, 13, 0xFFFFFFFF, true);
        category.setBackgroundResource(R.drawable.bg_badge);
        category.setPadding(dp(14), dp(6), dp(14), dp(6));
        LinearLayout.LayoutParams catLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        catLp.gravity = Gravity.CENTER_HORIZONTAL;
        category.setLayoutParams(catLp);
        page.addView(category);

        TextView title = label(fallbackTitle == null ? "" : fallbackTitle, 26, 0xFF1C1917, true);
        title.setGravity(Gravity.CENTER);
        title.setPadding(dp(4), dp(8), dp(4), dp(2));
        page.addView(title);

        TextView date = label("", 14, 0xFF78716C, false);
        date.setGravity(Gravity.CENTER);
        page.addView(date);

        MaterialCardView cover = new MaterialCardView(this);
        cover.setRadius(dp(18));
        cover.setCardElevation(dp(2));
        cover.setCardBackgroundColor(0xFFE8F6EE);
        ImageView coverImage = new ImageView(this);
        coverImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
        cover.addView(coverImage, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(220)));
        LinearLayout.LayoutParams coverLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        coverLp.topMargin = dp(14);
        page.addView(cover, coverLp);

        TextView body = label("फोटो लोड हुँदैछ…", 16, 0xFF44403C, false);
        body.setPadding(dp(2), dp(14), dp(2), dp(8));
        body.setLineSpacing(dp(3), 1f);
        page.addView(body);

        GridLayout grid = new GridLayout(this);
        grid.setColumnCount(2);
        LinearLayout.LayoutParams gridLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        gridLp.topMargin = dp(6);
        page.addView(grid, gridLp);

        scroll.addView(page);
        root.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        setContentView(root);

        if (fallbackImage != null && !fallbackImage.isEmpty()) load(fallbackImage, coverImage, 1000);
        io.execute(() -> {
            try {
                SiteClient.GalleryPost post = client.galleryPost(url);
                runOnUiThread(() -> show(post, category, title, date, body, coverImage, grid));
            } catch (Exception error) {
                runOnUiThread(() -> body.setText("ग्यालरी खोल्न सकिएन।"));
            }
        });
    }

    private void show(SiteClient.GalleryPost post, TextView category, TextView title, TextView date, TextView body, ImageView coverImage, GridLayout grid) {
        if (!post.category.isEmpty()) category.setText(post.category);
        if (!post.title.isEmpty()) title.setText(post.title);
        if (!post.date.isEmpty()) date.setText(post.date);
        body.setText(post.body.isEmpty() ? "" : post.body);
        if (!post.cover.isEmpty()) load(post.cover, coverImage, 1200);
        photos.clear();
        photos.addAll(post.photos);
        grid.removeAllViews();
        int width = (getResources().getDisplayMetrics().widthPixels - dp(16) * 2 - dp(8)) / 2;
        for (int i = 0; i < photos.size(); i++) {
            MaterialCardView cell = new MaterialCardView(this);
            cell.setRadius(dp(14));
            cell.setCardElevation(0);
            cell.setCardBackgroundColor(0xFFE7E5E4);
            ImageView image = new ImageView(this);
            image.setScaleType(ImageView.ScaleType.CENTER_CROP);
            cell.addView(image, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(140)));
            GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
            lp.width = width;
            lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            lp.setMargins(dp(4), dp(4), dp(4), dp(4));
            cell.setLayoutParams(lp);
            int index = i;
            cell.setOnClickListener(v -> viewer(index));
            grid.addView(cell);
            load(photos.get(i), image, 600);
        }
    }

    private void viewer(int start) {
        if (photos.isEmpty()) return;
        Dialog dialog = new Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        FrameLayout frame = new FrameLayout(this);
        frame.setBackgroundColor(Color.BLACK);
        ImageView image = new ImageView(this);
        image.setScaleType(ImageView.ScaleType.FIT_CENTER);
        frame.addView(image, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        final int[] index = {start};
        TextView close = viewerButton("✕", Gravity.END | Gravity.TOP);
        close.setOnClickListener(v -> dialog.dismiss());
        TextView prev = viewerButton("‹", Gravity.START | Gravity.CENTER_VERTICAL);
        TextView next = viewerButton("›", Gravity.END | Gravity.CENTER_VERTICAL);
        TextView count = label("", 14, 0xFFFFFFFF, true);
        count.setPadding(dp(12), dp(8), dp(12), dp(8));
        FrameLayout.LayoutParams countLp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        countLp.bottomMargin = dp(24);
        count.setLayoutParams(countLp);
        Runnable paint = () -> {
            load(photos.get(index[0]), image, 1600);
            count.setText((index[0] + 1) + " / " + photos.size());
        };
        prev.setOnClickListener(v -> {
            index[0] = (index[0] - 1 + photos.size()) % photos.size();
            paint.run();
        });
        next.setOnClickListener(v -> {
            index[0] = (index[0] + 1) % photos.size();
            paint.run();
        });
        frame.addView(close);
        frame.addView(prev);
        frame.addView(next);
        frame.addView(count);
        dialog.setContentView(frame);
        dialog.show();
        paint.run();
    }

    private TextView viewerButton(String value, int gravity) {
        TextView view = label(value, 28, 0xFFFFFFFF, true);
        view.setGravity(Gravity.CENTER);
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(22));
        bg.setColor(0x66000000);
        view.setBackground(bg);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(dp(44), dp(44), gravity);
        lp.setMargins(dp(12), dp(16), dp(12), dp(16));
        view.setLayoutParams(lp);
        return view;
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

    private void load(String url, ImageView target, int maxSide) {
        io.execute(() -> {
            Bitmap bitmap = fetch(url, maxSide);
            if (bitmap != null) runOnUiThread(() -> target.setImageBitmap(bitmap));
        });
    }

    private Bitmap fetch(String url, int maxSide) {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setInstanceFollowRedirects(true);
            connection.setConnectTimeout(12000);
            connection.setReadTimeout(15000);
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
            int side = Math.max(bounds.outWidth, bounds.outHeight);
            if (side > 0) while (side / sample > maxSide) sample *= 2;
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = sample;
            return BitmapFactory.decodeByteArray(data, 0, data.length, options);
        } catch (Exception ignored) {
            return null;
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    private TextView label(String value, int size, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextColor(color);
        view.setTextSize(TypedValue.COMPLEX_UNIT_DIP, size);
        view.setTypeface(ResourcesCompat.getFont(this, bold ? R.font.mukta_bold : R.font.mukta_regular));
        return view;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
