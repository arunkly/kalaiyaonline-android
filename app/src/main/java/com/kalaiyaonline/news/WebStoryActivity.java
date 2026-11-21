package com.kalaiyaonline.news;

import android.graphics.Bitmap;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import java.util.ArrayList;
import java.util.List;

public class WebStoryActivity extends AppCompatActivity {
    private static final long PAGE_MS = 4000;
    private final Handler ticks = new Handler(Looper.getMainLooper());
    private final List<SiteClient.StoryPage> pages = new ArrayList<>();
    private final List<View> fills = new ArrayList<>();
    private String[] urls = new String[0];
    private String[] titles = new String[0];
    private String[] images = new String[0];
    private ImageView photo;
    private TextView titleView;
    private TextView caption;
    private LinearLayout bars;
    private int storyIndex;
    private int index;
    private int loadGen;
    private int imageGen;
    private boolean holding;
    private long pageStart;
    private float downX;
    private long downAt;

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            if (isDestroyed()) return;
            if (!holding && !pages.isEmpty()) {
                long elapsed = SystemClock.uptimeMillis() - pageStart;
                paint(elapsed);
                if (elapsed >= PAGE_MS) {
                    next();
                    return;
                }
            }
            ticks.postDelayed(this, 40);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Night.sync(this);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setBackgroundDrawable(new ColorDrawable(0xFF000000));
        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        if (controller != null) controller.setAppearanceLightStatusBars(false);
        urls = textArray("urls");
        titles = textArray("titles");
        images = textArray("images");
        storyIndex = getIntent().getIntExtra("index", 0);
        if (urls.length == 0) {
            finish();
            return;
        }

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(0xFF000000);
        photo = new ImageView(this);
        photo.setScaleType(ImageView.ScaleType.CENTER_CROP);
        root.addView(photo, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        View shade = new View(this);
        shade.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[]{0x00000000, 0xE6102018}));
        root.addView(shade, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(280), Gravity.BOTTOM));

        LinearLayout copy = new LinearLayout(this);
        copy.setOrientation(LinearLayout.VERTICAL);
        copy.setPadding(dp(16), 0, dp(16), dp(28));
        titleView = label("", 22, true);
        caption = label("", 15, false);
        caption.setPadding(0, dp(6), 0, 0);
        copy.addView(titleView);
        copy.addView(caption);
        root.addView(copy, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM));

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.VERTICAL);
        top.setPadding(dp(8), dp(10), dp(8), 0);
        bars = new LinearLayout(this);
        bars.setOrientation(LinearLayout.HORIZONTAL);
        top.addView(bars, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(3)));
        LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);
        head.setPadding(dp(6), dp(10), dp(2), 0);
        TextView brand = label("वेब स्टोरी", 14, true);
        head.addView(brand, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView close = label("✕", 18, true);
        close.setGravity(Gravity.CENTER);
        close.setPadding(dp(12), dp(4), dp(8), dp(4));
        close.setOnClickListener(v -> finish());
        head.addView(close);
        top.addView(head);
        root.addView(top, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP));

        root.setOnTouchListener(this::onStoryTouch);
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, insets) -> {
            Insets system = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            top.setPadding(dp(8), system.top + dp(8), dp(8), 0);
            copy.setPadding(dp(16), 0, dp(16), system.bottom + dp(22));
            return insets;
        });
        setContentView(root);
        openStory(Math.max(0, Math.min(storyIndex, urls.length - 1)));
        ticks.post(tick);
    }

    private boolean onStoryTouch(View view, MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            holding = true;
            downX = event.getX();
            downAt = SystemClock.uptimeMillis();
            return true;
        }
        if (event.getAction() == MotionEvent.ACTION_UP) {
            holding = false;
            float dx = event.getX() - downX;
            long held = SystemClock.uptimeMillis() - downAt;
            if (Math.abs(dx) > dp(56)) {
                if (dx < 0) next();
                else prev();
            } else if (held < 280) {
                if (event.getX() < view.getWidth() * 0.33f) prev();
                else next();
            }
            return true;
        }
        if (event.getAction() == MotionEvent.ACTION_CANCEL) holding = false;
        return true;
    }

    private void openStory(int which) {
        if (which < 0) return;
        if (which >= urls.length) {
            finish();
            return;
        }
        storyIndex = which;
        int gen = ++loadGen;
        pages.clear();
        index = 0;
        titleView.setText(titles.length > which ? titles[which] : "");
        caption.setText("");
        buildBars(1);
        paint(0);
        if (images.length > which && images[which] != null && !images[which].isEmpty()) loadImage(images[which]);
        String url = urls[which];
        new Thread(() -> {
            List<SiteClient.StoryPage> loaded = null;
            try {
                loaded = new SiteClient().webStory(url);
            } catch (Exception ignored) {
            }
            List<SiteClient.StoryPage> result = loaded;
            runOnUiThread(() -> {
                if (isDestroyed() || gen != loadGen) return;
                if (result == null || result.isEmpty()) {
                    caption.setText("कथा लोड हुन सकेन।");
                    return;
                }
                pages.clear();
                pages.addAll(result);
                show(0);
            });
        }).start();
    }

    private void show(int at) {
        if (pages.isEmpty()) return;
        index = Math.max(0, Math.min(at, pages.size() - 1));
        pageStart = SystemClock.uptimeMillis();
        SiteClient.StoryPage page = pages.get(index);
        if (!page.title.isEmpty()) titleView.setText(page.title);
        caption.setText(page.text);
        buildBars(pages.size());
        paint(0);
        if (!page.image.isEmpty()) loadImage(page.image);
    }

    private void next() {
        if (pages.isEmpty() || index >= pages.size() - 1) openStory(storyIndex + 1);
        else show(index + 1);
    }

    private void prev() {
        if (!pages.isEmpty() && index > 0) show(index - 1);
        else if (storyIndex > 0) openStory(storyIndex - 1);
    }

    private void buildBars(int count) {
        bars.removeAllViews();
        fills.clear();
        int total = Math.max(count, 1);
        for (int i = 0; i < total; i++) {
            FrameLayout track = new FrameLayout(this);
            GradientDrawable trackBg = new GradientDrawable();
            trackBg.setColor(0x55FFFFFF);
            trackBg.setCornerRadius(dp(2));
            track.setBackground(trackBg);
            View fill = new View(this);
            GradientDrawable fillBg = new GradientDrawable();
            fillBg.setColor(0xFFFFFFFF);
            fillBg.setCornerRadius(dp(2));
            fill.setBackground(fillBg);
            track.addView(fill, new FrameLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(3), 1f);
            lp.leftMargin = dp(2);
            lp.rightMargin = dp(2);
            bars.addView(track, lp);
            fills.add(fill);
        }
    }

    private void paint(long elapsed) {
        float frac = pages.isEmpty() ? 0f : Math.min(1f, elapsed / (float) PAGE_MS);
        for (int i = 0; i < fills.size(); i++) {
            View fill = fills.get(i);
            View track = (View) fill.getParent();
            float amount = i < index ? 1f : i == index ? frac : 0f;
            ViewGroup.LayoutParams lp = fill.getLayoutParams();
            int width = Math.round(track.getWidth() * amount);
            if (lp.width != width) {
                lp.width = width;
                fill.setLayoutParams(lp);
            }
        }
    }

    private void loadImage(String url) {
        int gen = ++imageGen;
        new Thread(() -> {
            Bitmap bitmap = Bitmaps.load(url, 1080);
            runOnUiThread(() -> {
                if (isDestroyed() || gen != imageGen || bitmap == null) return;
                Night.allow(photo, false);
                photo.setImageBitmap(bitmap);
            });
        }).start();
    }

    private String[] textArray(String key) {
        String[] value = getIntent().getStringArrayExtra(key);
        return value == null ? new String[0] : value;
    }

    private TextView label(String value, int size, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextColor(0xFFFFFFFF);
        view.setTextSize(TypedValue.COMPLEX_UNIT_DIP, size);
        view.setTypeface(ResourcesCompat.getFont(this, bold ? R.font.mukta_bold : R.font.mukta_regular));
        return view;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onDestroy() {
        ticks.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}
