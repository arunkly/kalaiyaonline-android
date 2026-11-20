package com.kalaiyaonline.news;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.style.ReplacementSpan;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.TypedValue;
import android.graphics.Outline;
import android.view.ViewOutlineProvider;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.graphics.drawable.DrawableCompat;

import com.google.android.gms.ads.AdView;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.card.MaterialCardView;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;
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
    private ImageView saveView;
    private ImageView authorPhoto;
    private TextView bodyView;
    private LinearLayout bodyHost;
    private String keptTitle = "";
    private String keptImage = "";
    private String keptCategory = "";
    private String keptDate = "";
    private LinearLayout relatedBox;
    private LinearLayout tagHost;
    private LinearLayout feelHost;
    private Story loaded;
    private AdView articleAd;
    private TextView listenButton;
    private Drawable speakerIcon;
    private TextToSpeech speech;
    private boolean speaking;
    private boolean preparing;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Night.sync(this);
        Bitmaps.use(getCacheDir());
        Net.use(getCacheDir());
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
        dateView.setTextColor(0xFF57534E);
        dateView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
        dateView.setTypeface(ResourcesCompat.getFont(this, R.font.mukta_semibold));
        dateView.setMaxLines(1);
        dateView.setEllipsize(TextUtils.TruncateAt.END);
        dateView.setPadding(dp(6), 0, dp(6), 0);

        LinearLayout meta = new LinearLayout(this);
        meta.setOrientation(LinearLayout.HORIZONTAL);
        meta.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams metaLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        metaLp.topMargin = dp(8);
        metaLp.bottomMargin = dp(8);
        meta.setLayoutParams(metaLp);

        authorPhoto = new ImageView(this);
        authorPhoto.setImageResource(R.mipmap.ic_launcher);
        authorPhoto.setScaleType(ImageView.ScaleType.CENTER_CROP);
        authorPhoto.setClipToOutline(true);
        authorPhoto.setOutlineProvider(new ViewOutlineProvider() {
            public void getOutline(View view, Outline outline) {
                outline.setOval(0, 0, view.getWidth(), view.getHeight());
            }
        });
        meta.addView(authorPhoto, new LinearLayout.LayoutParams(dp(28), dp(28)));

        authorView = new TextView(this);
        authorView.setText("कलैया अनलाइन");
        authorView.setTextColor(0xFF1C1917);
        authorView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        authorView.setTypeface(bold);
        authorView.setMaxLines(1);
        authorView.setEllipsize(TextUtils.TruncateAt.END);
        authorView.setPadding(dp(6), 0, dp(2), 0);
        meta.addView(authorView, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        meta.addView(metaDivider());
        meta.addView(dateView);
        meta.addView(metaDivider());

        ImageView eye = new ImageView(this);
        eye.setImageResource(R.drawable.ic_eye);
        LinearLayout.LayoutParams eyeLp = new LinearLayout.LayoutParams(dp(15), dp(15));
        eyeLp.leftMargin = dp(6);
        meta.addView(eye, eyeLp);

        viewsView = new TextView(this);
        viewsView.setText("—");
        viewsView.setTextColor(0xFF0B5C28);
        viewsView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
        viewsView.setTypeface(bold);
        viewsView.setMaxLines(1);
        viewsView.setPadding(dp(3), 0, dp(4), 0);
        meta.addView(viewsView);

        saveView = new ImageView(this);
        saveView.setImageResource(R.drawable.ic_bookmark);
        saveView.setPadding(dp(7), dp(7), dp(7), dp(7));
        LinearLayout.LayoutParams saveLp = new LinearLayout.LayoutParams(dp(32), dp(32));
        saveLp.leftMargin = dp(4);
        meta.addView(saveView, saveLp);
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

        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.addView(listenControl());
        bar.addView(new View(this), new LinearLayout.LayoutParams(0, 1, 1f));
        String link = SiteClient.ORIGIN + "/news/" + slug;
        bar.addView(shareIcon(R.drawable.ic_share_fb, 0xFF1877F2, "com.facebook.katana", link, title));
        bar.addView(shareIcon(R.drawable.ic_share_x, 0xFF111111, "com.twitter.android", link, title));
        bar.addView(shareIcon(R.drawable.ic_share_ms, 0xFF0084FF, "com.facebook.orca", link, title));
        bar.addView(shareIcon(R.drawable.ic_share_wa, 0xFF25D366, "com.whatsapp", link, title));
        bar.addView(shareIcon(R.drawable.ic_share_link, 0xFF0F8A3C, "copy", link, title));
        LinearLayout.LayoutParams barLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        barLp.topMargin = dp(12);
        barLp.bottomMargin = dp(2);
        page.addView(bar, barLp);

        bodyView = new TextView(this);
        bodyView.setText("पूर्ण समाचार लोड हुँदैछ…");
        bodyView.setTextColor(0xFF292524);
        bodyView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 19);
        bodyView.setLineSpacing(dp(8), 1f);
        bodyView.setPadding(dp(2), dp(12), dp(2), dp(8));
        page.addView(bodyView);

        bodyHost = new LinearLayout(this);
        bodyHost.setOrientation(LinearLayout.VERTICAL);
        bodyHost.setVisibility(View.GONE);
        page.addView(bodyHost);

        tagHost = new LinearLayout(this);
        tagHost.setOrientation(LinearLayout.VERTICAL);
        tagHost.setVisibility(View.GONE);
        page.addView(tagHost);

        feelHost = new LinearLayout(this);
        feelHost.setOrientation(LinearLayout.VERTICAL);
        feelHost.setVisibility(View.GONE);
        page.addView(feelHost);

        relatedBox = new LinearLayout(this);
        relatedBox.setOrientation(LinearLayout.VERTICAL);
        relatedBox.setVisibility(View.GONE);
        page.addView(relatedBox);

        scroll.addView(page);
        Night.allow(scroll, Night.on(this));
        root.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        root.setPadding(0, 0, 0, DockBar.inset(this));
        articleAd = Ads.attach(this, root);
        FrameLayout screen = new FrameLayout(this);
        screen.addView(root, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        DockBar nav = new DockBar(this);
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
                    if (!story.authorImage.isEmpty()) load(story.authorImage, authorPhoto);
                    viewsView.setText(story.views.isEmpty() ? "—" : story.views);
                    paintArticle(story);
                    if (!story.image.isEmpty()) {
                        keptImage = story.image;
                        load(story.image, image);
                    }
                    loaded = story;
                    paintTags();
                    paintFeelings();
                    showRelated(story.related);
                });
            } catch (Exception error) {
                runOnUiThread(() -> bodyView.setText("समाचार खोल्न सकिएन।"));
            }
        });
    }

    private View listenControl() {
        listenButton = new TextView(this);
        listenButton.setText("सुन्नुहोस्");
        listenButton.setTextColor(0xFF0F8A3C);
        listenButton.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        listenButton.setTypeface(ResourcesCompat.getFont(this, R.font.mukta_bold));
        listenButton.setGravity(Gravity.CENTER_VERTICAL);
        listenButton.setIncludeFontPadding(false);
        listenButton.setPadding(dp(10), 0, dp(12), 0);
        speakerIcon = DrawableCompat.wrap(getDrawable(R.drawable.ic_speaker)).mutate();
        DrawableCompat.setTint(speakerIcon, 0xFF0F8A3C);
        speakerIcon.setBounds(0, 0, dp(16), dp(16));
        listenButton.setCompoundDrawables(speakerIcon, null, null, null);
        listenButton.setCompoundDrawablePadding(dp(5));
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(18));
        bg.setColor(0xFFE7F6EE);
        listenButton.setBackground(bg);
        listenButton.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(36)));
        listenButton.setOnClickListener(v -> toggleSpeech());
        return listenButton;
    }

    private void toggleSpeech() {
        if (speaking || (speech != null && speech.isSpeaking())) {
            stopSpeech();
            return;
        }
        String text = speakable();
        if (text.length() < 2) return;
        if (speech == null) {
            if (preparing) return;
            preparing = true;
            paintListen(false, true);
            speech = new TextToSpeech(this, status -> runOnUiThread(() -> {
                preparing = false;
                if (isDestroyed()) return;
                if (status != TextToSpeech.SUCCESS || speech == null) {
                    paintListen(false, false);
                    Toast.makeText(this, "यो फोनमा आवाज उपलब्ध छैन।", Toast.LENGTH_LONG).show();
                    return;
                }
                int lang = speech.setLanguage(new Locale("ne", "NP"));
                if (lang == TextToSpeech.LANG_MISSING_DATA || lang == TextToSpeech.LANG_NOT_SUPPORTED) {
                    lang = speech.setLanguage(new Locale("ne"));
                }
                if (lang == TextToSpeech.LANG_MISSING_DATA || lang == TextToSpeech.LANG_NOT_SUPPORTED) {
                    paintListen(false, false);
                    Toast.makeText(this, "नेपाली आवाज डाउनलोड गर्नुहोस्।", Toast.LENGTH_LONG).show();
                    try {
                        startActivity(new Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA));
                    } catch (Exception ignored) {
                    }
                    return;
                }
                speech.setSpeechRate(0.92f);
                speech.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                    @Override
                    public void onStart(String utteranceId) {
                    }

                    @Override
                    public void onDone(String utteranceId) {
                        if ("end".equals(utteranceId)) runOnUiThread(() -> paintListen(false, false));
                    }

                    @Override
                    public void onError(String utteranceId) {
                        runOnUiThread(() -> paintListen(false, false));
                    }
                });
                speakNow();
            }));
            return;
        }
        speakNow();
    }

    private void speakNow() {
        if (speech == null) return;
        String[] parts = chunks(speakable());
        if (parts.length == 0) return;
        speech.stop();
        for (int i = 0; i < parts.length; i++) {
            String id = i == parts.length - 1 ? "end" : "part" + i;
            speech.speak(parts[i], i == 0 ? TextToSpeech.QUEUE_FLUSH : TextToSpeech.QUEUE_ADD, null, id);
        }
        paintListen(true, false);
    }

    private void stopSpeech() {
        if (speech != null) speech.stop();
        paintListen(false, false);
    }

    private void paintListen(boolean playing, boolean waiting) {
        speaking = playing;
        if (listenButton == null) return;
        listenButton.setText(waiting ? "सुन्नुहोस्" : playing ? "रोक्नुहोस्" : "सुन्नुहोस्");
        listenButton.setTextColor(playing ? 0xFFFFFFFF : 0xFF0F8A3C);
        if (speakerIcon != null) {
            DrawableCompat.setTint(speakerIcon, playing ? 0xFFFFFFFF : 0xFF0F8A3C);
            listenButton.setCompoundDrawables(speakerIcon, null, null, null);
        }
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(18));
        bg.setColor(playing ? 0xFF0F8A3C : 0xFFE7F6EE);
        listenButton.setBackground(bg);
    }

    private String speakable() {
        String title = keptTitle == null ? "" : keptTitle.trim();
        String body = loaded != null && loaded.body != null && !loaded.body.isEmpty()
                ? loaded.body.trim()
                : bodyView == null ? "" : bodyView.getText().toString().trim();
        if (body.startsWith("पूर्ण समाचार") || body.startsWith("समाचार खोल्न") || body.startsWith("पूर्ण पाठ")) body = "";
        if (body.isEmpty()) return title;
        if (title.isEmpty()) return body;
        return title + "। " + body;
    }

    private String[] chunks(String text) {
        if (text.isEmpty()) return new String[0];
        java.util.ArrayList<String> parts = new java.util.ArrayList<>();
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(text.length(), start + 3200);
            if (end < text.length()) {
                int breakAt = text.lastIndexOf('।', end);
                if (breakAt < start + 800) breakAt = text.lastIndexOf(' ', end);
                if (breakAt > start + 400) end = breakAt + 1;
            }
            parts.add(text.substring(start, end).trim());
            start = end;
        }
        return parts.toArray(new String[0]);
    }

    private void paintArticle(Story story) {
        bodyHost.removeAllViews();
        if (story.pieces.isEmpty()) {
            bodyView.setVisibility(View.VISIBLE);
            bodyHost.setVisibility(View.GONE);
            bodyView.setText(story.body.isEmpty() ? "पूर्ण पाठ भेटिएन।" : story.body);
            if (Build.VERSION.SDK_INT >= 26) bodyView.setJustificationMode(android.text.Layout.JUSTIFICATION_MODE_INTER_WORD);
            return;
        }
        bodyView.setVisibility(View.GONE);
        bodyHost.setVisibility(View.VISIBLE);
        boolean first = true;
        for (Story.Piece piece : story.pieces) {
            if (piece.ad()) {
                bodyHost.addView(adBanner(piece));
                continue;
            }
            TextView view = new TextView(this);
            view.setTextColor(0xFF292524);
            view.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 19);
            view.setLineSpacing(dp(2), 1f);
            view.setPadding(dp(2), 0, dp(2), 0);
            if (Build.VERSION.SDK_INT >= 26) view.setJustificationMode(android.text.Layout.JUSTIFICATION_MODE_INTER_WORD);
            if (!piece.lead.isEmpty() && piece.text.startsWith(piece.lead)) {
                SpannableString styled = new SpannableString(piece.text);
                styled.setSpan(new LeadBadgeSpan(piece.lead), 0, piece.lead.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                view.setText(styled);
            } else {
                view.setText(piece.text);
            }
            LinearLayout.LayoutParams textLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            textLp.topMargin = first ? dp(10) : 0;
            textLp.bottomMargin = dp(10);
            view.setLayoutParams(textLp);
            bodyHost.addView(view);
            first = false;
        }
    }

    private View adBanner(Story.Piece piece) {
        MaterialCardView card = new MaterialCardView(this);
        card.setRadius(dp(14));
        card.setCardElevation(0);
        card.setStrokeWidth(0);
        card.setCardBackgroundColor(0xFFFFFFFF);
        ImageView image = new ImageView(this);
        image.setAdjustViewBounds(true);
        image.setScaleType(ImageView.ScaleType.FIT_CENTER);
        card.addView(image, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(8);
        lp.bottomMargin = dp(8);
        card.setLayoutParams(lp);
        load(piece.adImage, image);
        if (!piece.adLink.isEmpty()) {
            card.setOnClickListener(v -> {
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(piece.adLink)));
                } catch (Exception ignored) {
                }
            });
        }
        return card;
    }

    private final class LeadBadgeSpan extends ReplacementSpan {
        private final String word;

        LeadBadgeSpan(String word) {
            this.word = word;
        }

        @Override
        public int getSize(Paint paint, CharSequence text, int start, int end, Paint.FontMetricsInt fm) {
            Paint badge = badgePaint(paint);
            if (fm != null) {
                int extra = dp(3);
                fm.ascent -= extra;
                fm.top -= extra;
                fm.descent += extra;
                fm.bottom += extra;
            }
            return Math.round(badge.measureText(word) + dp(20));
        }

        @Override
        public void draw(Canvas canvas, CharSequence text, int start, int end, float x, int top, int y, int bottom, Paint paint) {
            Paint badge = badgePaint(paint);
            Paint.FontMetrics metrics = badge.getFontMetrics();
            float height = metrics.descent - metrics.ascent + dp(7);
            float width = badge.measureText(word) + dp(18);
            float topY = y + metrics.ascent - dp(3);
            float slant = height * 0.18f;
            Path shape = new Path();
            shape.moveTo(x + slant, topY);
            shape.lineTo(x + width, topY);
            shape.lineTo(x + width - slant * 0.35f, topY + height);
            shape.lineTo(x, topY + height);
            shape.close();
            Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
            fill.setColor(0xFF0F9046);
            canvas.drawPath(shape, fill);
            float bar = dp(4);
            Path gold = new Path();
            gold.moveTo(x + width - bar, topY);
            gold.lineTo(x + width, topY);
            gold.lineTo(x + width - slant * 0.35f, topY + height);
            gold.lineTo(x + width - bar - slant * 0.2f, topY + height);
            gold.close();
            Paint yellow = new Paint(Paint.ANTI_ALIAS_FLAG);
            yellow.setColor(0xFFFACC15);
            canvas.drawPath(gold, yellow);
            canvas.drawText(word, x + dp(8), y, badge);
        }

        private Paint badgePaint(Paint base) {
            TextPaint badge = new TextPaint(Paint.ANTI_ALIAS_FLAG);
            badge.setColor(0xFFFFFFFF);
            badge.setTextSize(base.getTextSize() * 0.86f);
            Typeface face = ResourcesCompat.getFont(ArticleActivity.this, R.font.mukta_bold);
            badge.setTypeface(face == null ? Typeface.DEFAULT_BOLD : face);
            return badge;
        }
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
        boolean on = saved();
        Drawable icon = DrawableCompat.wrap(saveView.getDrawable()).mutate();
        DrawableCompat.setTint(icon, on ? 0xFFFFFFFF : 0xFF0F8A3C);
        saveView.setImageDrawable(icon);
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.OVAL);
        bg.setColor(on ? 0xFF0F8A3C : 0xFFF3F6F4);
        saveView.setBackground(bg);
    }

    private View shareIcon(int icon, int color, String pkg, String link, TextView title) {
        ImageView mark = new ImageView(this);
        mark.setImageResource(icon);
        mark.setScaleType(ImageView.ScaleType.FIT_CENTER);
        mark.setPadding(dp(8), dp(8), dp(8), dp(8));
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.OVAL);
        bg.setColor(color);
        mark.setBackground(bg);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(36), dp(36));
        lp.leftMargin = dp(6);
        mark.setLayoutParams(lp);
        mark.setOnClickListener(v -> share(pkg, title.getText().toString(), link));
        return mark;
    }

    private void paintTags() {
        tagHost.removeAllViews();
        if (loaded == null || loaded.tags.isEmpty()) {
            tagHost.setVisibility(View.GONE);
            return;
        }
        tagHost.setVisibility(View.VISIBLE);
        tagHost.setPadding(0, dp(8), 0, dp(2));
        android.widget.HorizontalScrollView scroll = new android.widget.HorizontalScrollView(this);
        scroll.setHorizontalScrollBarEnabled(false);
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, dp(2), 0, dp(2));
        for (String tag : loaded.tags) {
            TextView chip = new TextView(this);
            chip.setText(tag);
            chip.setTextColor(0xFF0B5C28);
            chip.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
            chip.setTypeface(ResourcesCompat.getFont(this, R.font.mukta_bold));
            chip.setIncludeFontPadding(false);
            chip.setMaxLines(1);
            chip.setPadding(dp(12), dp(7), dp(12), dp(7));
            GradientDrawable bg = new GradientDrawable();
            bg.setColor(0xFFF3F6F4);
            bg.setCornerRadius(dp(16));
            bg.setStroke(dp(1), 0xFFD7E5DC);
            chip.setBackground(bg);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.rightMargin = dp(8);
            chip.setLayoutParams(lp);
            row.addView(chip);
        }
        scroll.addView(row);
        tagHost.addView(scroll);
    }

    private void paintFeelings() {
        feelHost.removeAllViews();
        if (loaded == null) {
            feelHost.setVisibility(View.GONE);
            return;
        }
        feelHost.setVisibility(View.VISIBLE);
        String voted = prefs.getString("feel-" + slug, "");
        int[] shown = loaded.feelings.clone();
        String note = loaded.feelingNote;
        if (!voted.isEmpty() && feelingTotal(shown) == 0) {
            int picked = feelingIndex(voted);
            if (picked >= 0) shown[picked] = 100;
            note = "तपाईंको प्रतिक्रिया सेभ भयो।";
        }
        MaterialCardView card = new MaterialCardView(this);
        card.setRadius(dp(22));
        card.setCardElevation(0);
        card.setStrokeWidth(dp(1));
        card.setStrokeColor(0xFFE7E5E4);
        card.setCardBackgroundColor(0xFFFFFFFF);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(10), dp(14), dp(10), dp(12));
        TextView title = new TextView(this);
        title.setText("यो खबर पढेर तपाईंलाई कस्तो महसुस भयो ?");
        title.setTextColor(0xFF1C1917);
        title.setGravity(Gravity.CENTER);
        title.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        title.setTypeface(ResourcesCompat.getFont(this, R.font.mukta_extrabold));
        title.setIncludeFontPadding(false);
        box.addView(title);
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, dp(12), 0, 0);
        String[] emoji = {"👍", "❤️", "😂", "😮", "😢", "😠"};
        String[] labels = {"मन पर्यो", "माया लाग्यो", "हाँस्यो", "अचम्मित", "दुखी", "रिस उठ्यो"};
        String[] keys = {"like", "love", "haha", "wow", "sad", "angry"};
        for (int i = 0; i < labels.length; i++) {
            final int index = i;
            LinearLayout cell = new LinearLayout(this);
            cell.setOrientation(LinearLayout.VERTICAL);
            cell.setGravity(Gravity.CENTER_HORIZONTAL);
            cell.setPadding(dp(2), dp(8), dp(2), dp(8));
            GradientDrawable bg = new GradientDrawable();
            bg.setColor(keys[i].equals(voted) ? 0xFFFFF4E8 : 0xFFFCFCFD);
            bg.setCornerRadius(dp(14));
            bg.setStroke(dp(1), keys[i].equals(voted) ? 0xFFF15A22 : 0xFFE7E5E4);
            cell.setBackground(bg);
            TextView face = new TextView(this);
            face.setText(emoji[i]);
            face.setGravity(Gravity.CENTER);
            face.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
            face.setIncludeFontPadding(false);
            cell.addView(face);
            TextView label = new TextView(this);
            label.setText(labels[i]);
            label.setGravity(Gravity.CENTER);
            label.setTextColor(0xFF1C1917);
            label.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 10);
            label.setTypeface(ResourcesCompat.getFont(this, R.font.mukta_bold));
            label.setMaxLines(2);
            label.setIncludeFontPadding(false);
            label.setPadding(dp(1), dp(4), dp(1), 0);
            cell.addView(label);
            TextView percent = new TextView(this);
            percent.setText(shown[i] + "%");
            percent.setGravity(Gravity.CENTER);
            percent.setTextColor(0xFF78716C);
            percent.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 11);
            percent.setTypeface(ResourcesCompat.getFont(this, R.font.mukta_semibold));
            percent.setIncludeFontPadding(false);
            percent.setPadding(0, dp(2), 0, 0);
            cell.addView(percent);
            cell.setOnClickListener(v -> chooseFeeling(keys[index], loaded.feelingUrls[index]));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            lp.leftMargin = dp(3);
            lp.rightMargin = dp(3);
            cell.setLayoutParams(lp);
            row.addView(cell);
        }
        box.addView(row);
        if (note != null && !note.isEmpty()) {
            TextView foot = new TextView(this);
            foot.setText(note);
            foot.setGravity(Gravity.CENTER);
            foot.setTextColor(0xFF64748B);
            foot.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
            foot.setTypeface(ResourcesCompat.getFont(this, R.font.mukta_semibold));
            foot.setIncludeFontPadding(false);
            foot.setPadding(0, dp(12), 0, dp(2));
            box.addView(foot);
        }
        card.addView(box);
        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cardLp.topMargin = dp(12);
        feelHost.addView(card, cardLp);
    }

    private void chooseFeeling(String key, String url) {
        prefs.edit().putString("feel-" + slug, key).apply();
        paintFeelings();
        if (url != null && !url.isEmpty()) io.execute(() -> client.ping(url));
    }

    private int feelingIndex(String key) {
        String[] keys = {"like", "love", "haha", "wow", "sad", "angry"};
        for (int i = 0; i < keys.length; i++) if (keys[i].equals(key)) return i;
        return -1;
    }

    private int feelingTotal(int[] values) {
        int total = 0;
        for (int value : values) total += value;
        return total;
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

    private void share(String pkg, String title, String link) {
        if ("copy".equals(pkg)) {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            clipboard.setPrimaryClip(ClipData.newPlainText("kalaiya", link));
            Toast.makeText(this, "लिंक कपी भयो", Toast.LENGTH_SHORT).show();
            return;
        }
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
            Bitmap bitmap = Bitmaps.load(url, 960);
            if (bitmap != null) runOnUiThread(() -> {
                if (!isDestroyed()) target.setImageBitmap(bitmap);
            });
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
        stopSpeech();
        if (articleAd != null) articleAd.pause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (speech != null) {
            speech.stop();
            speech.shutdown();
            speech = null;
        }
        if (articleAd != null) articleAd.destroy();
        super.onDestroy();
    }
}
