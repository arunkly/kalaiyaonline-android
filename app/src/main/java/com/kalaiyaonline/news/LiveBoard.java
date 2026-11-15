package com.kalaiyaonline.news;

import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class LiveBoard {
    private static final String PAGE = "https://kalaiyaonline.com/live.php";
    private static final String UA = "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36";

    private final AppCompatActivity activity;
    private final LinearLayout root;
    private final LinearLayout body;
    private final TextView status;
    private WebView player;

    LiveBoard(AppCompatActivity activity) {
        this.activity = activity;
        root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        status = text("प्रत्यक्ष प्रसारण जाँच हुँदैछ…", 14, 0xFF57534E, false);
        status.setPadding(0, 0, 0, dp(8));
        root.addView(status);
        body = new LinearLayout(activity);
        body.setOrientation(LinearLayout.VERTICAL);
        root.addView(body);
        root.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            @Override
            public void onViewAttachedToWindow(View v) {
                if (player != null) {
                    player.resumeTimers();
                    player.onResume();
                }
            }

            @Override
            public void onViewDetachedFromWindow(View v) {
                if (player != null) {
                    player.onPause();
                    player.pauseTimers();
                }
            }
        });
    }

    View view() {
        return root;
    }

    void load() {
        status.setText("प्रत्यक्ष प्रसारण जाँच हुँदैछ…");
        status.setOnClickListener(null);
        body.removeAllViews();
        releasePlayer();
        new Thread(() -> {
            Document doc = null;
            try {
                doc = Jsoup.connect(PAGE).userAgent(UA).referrer("https://kalaiyaonline.com/").timeout(20000).get();
            } catch (Exception ignored) {
            }
            Document ready = doc;
            if (!activity.isDestroyed()) activity.runOnUiThread(() -> show(ready));
        }).start();
    }

    private void show(Document doc) {
        body.removeAllViews();
        releasePlayer();
        if (doc == null) {
            status.setText("लाइभ पृष्ठ आएन।");
            body.addView(button("फेरि जाँच गर्नुहोस्", this::load));
            return;
        }
        String embed = embedOf(doc);
        boolean offline = embed.isEmpty() || doc.body().text().contains("उपलब्ध छैन") && !hasPlayer(doc);
        if (offline || embed.isEmpty()) {
            status.setText("");
            body.addView(offlineCard(messageOf(doc)));
            return;
        }
        status.setText("");
        body.addView(liveCard(doc, embed));
    }

    private boolean hasPlayer(Document doc) {
        return doc.selectFirst("iframe[src], iframe[data-src], video[src], video source[src]") != null;
    }

    private String embedOf(Document doc) {
        Element iframe = doc.selectFirst("iframe[src], iframe[data-src]");
        if (iframe != null) {
            String src = iframe.absUrl("src");
            if (src.isEmpty()) src = iframe.absUrl("data-src");
            if (!src.isEmpty()) return toEmbed(src);
        }
        Element video = doc.selectFirst("video[src], video source[src]");
        if (video != null && !video.absUrl("src").isEmpty()) return video.absUrl("src");
        if (doc.body().text().contains("उपलब्ध छैन")) return "";
        for (Element link : doc.select("a[href]")) {
            String href = link.absUrl("href");
            if (href.contains("youtube.com") || href.contains("youtu.be") || href.contains("facebook.com") || href.contains("fb.watch")) {
                return toEmbed(href);
            }
        }
        return "";
    }

    private String toEmbed(String raw) {
        Matcher id = Pattern.compile("(?:v=|youtu\\.be/|/embed/|/live/)([A-Za-z0-9_-]{6,})").matcher(raw);
        if (id.find() && (raw.contains("youtu") || raw.contains("youtube"))) {
            return "https://www.youtube.com/embed/" + id.group(1) + "?autoplay=1&playsinline=1&rel=0";
        }
        return raw;
    }

    private String messageOf(Document doc) {
        Element note = doc.selectFirst("h2, p");
        String text = note == null ? "" : note.text().trim();
        if (text.isEmpty() || text.equals("प्रत्यक्ष प्रसारण")) return "अहिले Live प्रसारण उपलब्ध छैन";
        return text;
    }

    private View offlineCard(String message) {
        LinearLayout card = card();
        card.setGravity(Gravity.CENTER);
        card.setPadding(dp(18), dp(28), dp(18), dp(24));
        ImageView icon = new ImageView(activity);
        icon.setImageResource(R.drawable.ic_mod_live);
        LinearLayout.LayoutParams iconLp = new LinearLayout.LayoutParams(dp(56), dp(56));
        iconLp.gravity = Gravity.CENTER;
        card.addView(icon, iconLp);
        TextView title = text(message, 18, 0xFF142033, true);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, dp(14), 0, 0);
        card.addView(title);
        TextView sub = text("प्रसारण सुरु भएपछि यो पृष्ठमा देखिनेछ।", 14, 0xFF78716C, false);
        sub.setGravity(Gravity.CENTER);
        sub.setPadding(0, dp(6), 0, dp(16));
        card.addView(sub);
        card.addView(button("फेरि जाँच गर्नुहोस्", this::load));
        return card;
    }

    private View liveCard(Document doc, String embed) {
        LinearLayout card = card();
        LinearLayout badgeRow = new LinearLayout(activity);
        badgeRow.setGravity(Gravity.CENTER_VERTICAL);
        badgeRow.setPadding(dp(12), dp(12), dp(12), dp(8));
        TextView badge = text("LIVE", 12, 0xFFFFFFFF, true);
        badge.setPadding(dp(8), dp(2), dp(8), dp(2));
        GradientDrawable pill = new GradientDrawable();
        pill.setCornerRadius(dp(10));
        pill.setColor(0xFFE11D48);
        badge.setBackground(pill);
        badgeRow.addView(badge);
        TextView live = text("प्रत्यक्ष प्रसारण", 14, 0xFF142033, true);
        live.setPadding(dp(8), 0, 0, 0);
        badgeRow.addView(live);
        card.addView(badgeRow);

        player = new WebView(activity);
        player.setWebChromeClient(new WebChromeClient());
        player.setWebViewClient(new WebViewClient());
        player.getSettings().setJavaScriptEnabled(true);
        player.getSettings().setDomStorageEnabled(true);
        player.getSettings().setMediaPlaybackRequiresUserGesture(false);
        player.setBackgroundColor(0xFF000000);
        int width = activity.getResources().getDisplayMetrics().widthPixels - dp(32);
        LinearLayout.LayoutParams playerLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, width * 9 / 16);
        card.addView(player, playerLp);
        player.loadUrl(embed);

        String title = titleOf(doc);
        if (!title.isEmpty()) {
            TextView heading = text(title, 18, 0xFF142033, true);
            heading.setPadding(dp(12), dp(12), dp(12), dp(4));
            card.addView(heading);
        }
        LinearLayout actions = new LinearLayout(activity);
        actions.setPadding(dp(12), dp(8), dp(12), dp(12));
        actions.addView(button("बाहिर खोल्नुहोस्", () -> activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(watchUrl(embed))))));
        TextView gap = new TextView(activity);
        gap.setText("  ");
        actions.addView(gap);
        actions.addView(button("फेरि जाँच", this::load));
        card.addView(actions);
        return card;
    }

    private String titleOf(Document doc) {
        String og = doc.select("meta[property=og:title]").attr("content").trim();
        if (!og.isEmpty() && !og.toLowerCase().contains("live streaming")) return og;
        for (Element heading : doc.select("h1, h2")) {
            String text = heading.text().trim();
            if (!text.isEmpty() && !text.equals("प्रत्यक्ष प्रसारण") && !text.contains("उपलब्ध छैन")) return text;
        }
        return "";
    }

    private String watchUrl(String embed) {
        Matcher id = Pattern.compile("/embed/([A-Za-z0-9_-]{6,})").matcher(embed);
        if (id.find()) return "https://www.youtube.com/watch?v=" + id.group(1);
        return embed;
    }

    private void releasePlayer() {
        if (player == null) return;
        player.onPause();
        player.pauseTimers();
        player.loadUrl("about:blank");
        player.destroy();
        player = null;
    }

    private LinearLayout card() {
        LinearLayout card = new LinearLayout(activity);
        card.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xFFFFFFFF);
        bg.setCornerRadius(dp(16));
        bg.setStroke(dp(1), 0xFFE7E5E4);
        card.setBackground(bg);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(4);
        card.setLayoutParams(lp);
        return card;
    }

    private TextView button(String label, Runnable action) {
        TextView button = text(label, 14, 0xFFFFFFFF, true);
        button.setGravity(Gravity.CENTER);
        button.setPadding(dp(16), dp(10), dp(16), dp(10));
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(20));
        bg.setColor(0xFFF15A22);
        button.setBackground(bg);
        button.setOnClickListener(v -> action.run());
        return button;
    }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView view = new TextView(activity);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        view.setTypeface(ResourcesCompat.getFont(activity, bold ? R.font.mukta_bold : R.font.mukta_regular));
        return view;
    }

    private int dp(int value) {
        return Math.round(value * activity.getResources().getDisplayMetrics().density);
    }
}
