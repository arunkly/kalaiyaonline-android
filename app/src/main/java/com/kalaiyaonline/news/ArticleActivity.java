package com.kalaiyaonline.news;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ArticleActivity extends AppCompatActivity {
    private final SiteClient client = new SiteClient();
    private final ExecutorService io = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String slug = getIntent().getStringExtra("slug");
        String fallbackTitle = getIntent().getStringExtra("title");
        String fallbackImage = getIntent().getStringExtra("image");

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
        page.setPadding(dp(16), dp(8), dp(16), dp(24));
        ImageView image = new ImageView(this);
        image.setScaleType(ImageView.ScaleType.CENTER_CROP);
        image.setBackgroundColor(0xFFE8F6EE);
        page.addView(image, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(220)));
        TextView title = new TextView(this);
        title.setText(fallbackTitle == null ? "" : fallbackTitle);
        title.setTextColor(0xFF1C1917);
        title.setTextSize(22);
        title.setPadding(0, dp(12), 0, dp(8));
        page.addView(title);
        TextView body = new TextView(this);
        body.setText("पूर्ण समाचार लोड हुँदैछ…");
        body.setTextColor(0xFF1C1917);
        body.setTextSize(17);
        body.setLineSpacing(dp(4), 1f);
        page.addView(body);
        TextView share = new TextView(this);
        share.setText("सेयर");
        share.setTextColor(0xFFFFFFFF);
        share.setBackgroundColor(0xFF0F8A3C);
        share.setPadding(dp(18), dp(10), dp(18), dp(10));
        LinearLayout.LayoutParams shareLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        shareLp.topMargin = dp(16);
        share.setLayoutParams(shareLp);
        page.addView(share);
        scroll.addView(page);
        root.addView(scroll);
        setContentView(root);

        if (fallbackImage != null && !fallbackImage.isEmpty()) load(fallbackImage, image);
        share.setOnClickListener(v -> {
            Intent send = new Intent(Intent.ACTION_SEND);
            send.setType("text/plain");
            send.putExtra(Intent.EXTRA_SUBJECT, title.getText().toString());
            send.putExtra(Intent.EXTRA_TEXT, title.getText() + "\n" + SiteClient.ORIGIN + "/news/" + slug);
            startActivity(Intent.createChooser(send, "सेयर"));
        });

        io.execute(() -> {
            try {
                Story story = client.article(slug);
                runOnUiThread(() -> {
                    if (!story.title.isEmpty()) title.setText(story.title);
                    body.setText(story.body.isEmpty() ? "पूर्ण पाठ भेटिएन।" : story.body);
                    if (!story.image.isEmpty()) load(story.image, image);
                });
            } catch (Exception error) {
                runOnUiThread(() -> body.setText("समाचार खोल्न सकिएन।"));
            }
        });
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
