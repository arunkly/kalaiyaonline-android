package com.kalaiyaonline.news;

import android.app.Dialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import com.google.android.gms.ads.AdView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;
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
    private AdView galleryAd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Night.sync(this);
        Bitmaps.use(getCacheDir());
        Net.use(getCacheDir());
        String url = getIntent().getStringExtra("url");
        String fallbackTitle = getIntent().getStringExtra("title");
        String fallbackImage = getIntent().getStringExtra("image");
        String fallbackCategory = getIntent().getStringExtra("category");

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFFF3F5F2);
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
        page.addView(shareRow(galleryLink(url), title));

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
        Night.allow(scroll, Night.on(this));
        root.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        root.setPadding(0, 0, 0, DockBar.inset(this));
        galleryAd = Ads.attach(this, root);
        FrameLayout screen = new FrameLayout(this);
        screen.addView(root, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        DockBar nav = new DockBar(this);
        nav.setSelectedItemId(R.id.nav_gallery);
        nav.setListener(new DockBar.Listener() {
            @Override
            public void onSelect(int id) {
                leave(navTarget(id));
            }

            @Override
            public void onReselect(int id) {
                leave(navTarget(id));
            }
        });
        screen.addView(nav, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, DockBar.inset(this), Gravity.BOTTOM));
        setContentView(screen);
        Night.belowStatus(this, screen);

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

    private String galleryLink(String url) {
        if (url == null || url.isEmpty()) return SiteClient.ORIGIN + "/gallery";
        if (url.startsWith("http")) return url;
        return SiteClient.ORIGIN + (url.startsWith("/") ? url : "/" + url);
    }

    private View shareRow(String link, TextView title) {
        LinearLayout shares = new LinearLayout(this);
        shares.setOrientation(LinearLayout.HORIZONTAL);
        shares.setGravity(Gravity.CENTER);
        shares.addView(shareIcon(R.drawable.ic_share_wa, 0xFF25D366, "com.whatsapp", link, title));
        shares.addView(shareIcon(R.drawable.ic_share_fb, 0xFF1877F2, "com.facebook.katana", link, title));
        shares.addView(shareIcon(R.drawable.ic_share_vb, 0xFF7360F2, "com.viber.voip", link, title));
        shares.addView(shareIcon(R.drawable.ic_share_x, 0xFF111111, "com.twitter.android", link, title));
        shares.addView(shareIcon(R.drawable.ic_share_more, 0xFF0F8A3C, null, link, title));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(12);
        shares.setLayoutParams(lp);
        return shares;
    }

    private View shareIcon(int icon, int color, String pkg, String link, TextView title) {
        ImageView mark = new ImageView(this);
        mark.setImageResource(icon);
        mark.setScaleType(ImageView.ScaleType.FIT_CENTER);
        mark.setPadding(dp(9), dp(9), dp(9), dp(9));
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.OVAL);
        bg.setColor(color);
        mark.setBackground(bg);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(42), dp(42));
        lp.leftMargin = dp(6);
        lp.rightMargin = dp(6);
        mark.setLayoutParams(lp);
        mark.setOnClickListener(v -> sendShare(pkg, title.getText().toString(), link));
        return mark;
    }

    private void sendShare(String pkg, String title, String link) {
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

    private View homeHeader() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setBackgroundColor(0xFFFFFFFF);
        box.setElevation(dp(2));
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(16), dp(8), dp(12), dp(8));
        ImageView logo = new ImageView(this);
        logo.setAdjustViewBounds(true);
        logo.setScaleType(ImageView.ScaleType.FIT_START);
        logo.setMaxWidth(dp(168));
        logo.setImageResource(R.drawable.wordmark);
        row.addView(logo, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(34)));
        row.addView(new View(this), new LinearLayout.LayoutParams(0, 0, 1f));
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
        view.setScaleType(ImageView.ScaleType.FIT_CENTER);
        view.setPadding(dp(8), dp(8), dp(8), dp(8));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(46), dp(46));
        lp.leftMargin = dp(2);
        lp.rightMargin = dp(2);
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
        if (id == R.id.nav_social) return "social";
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

    private void load(String url, ImageView target, int maxSide) {
        io.execute(() -> {
            Bitmap bitmap = fetch(url, maxSide);
            if (bitmap != null) runOnUiThread(() -> target.setImageBitmap(bitmap));
        });
    }

    private Bitmap fetch(String url, int maxSide) {
        return Bitmaps.load(url, maxSide);
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

    @Override
    protected void onResume() {
        super.onResume();
        if (galleryAd != null) galleryAd.resume();
    }

    @Override
    protected void onPause() {
        if (galleryAd != null) galleryAd.pause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (galleryAd != null) galleryAd.destroy();
        super.onDestroy();
    }
}
