package com.kalaiyaonline.news;

import android.app.Activity;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.text.Html;
import android.text.Layout;
import android.text.Spanned;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.res.ResourcesCompat;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

final class PrivacyBoard {
    private static final String PAGE = "https://kalaiyaonline.com/page/privacy%20policy";
    private final Activity activity;
    private final LinearLayout root;
    private int generation;

    PrivacyBoard(Activity activity) {
        this.activity = activity;
        root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
    }

    View view() {
        return root;
    }

    void load() {
        int token = ++generation;
        root.removeAllViews();
        root.addView(line("नीति लोड हुँदैछ…", 15, 0xFF78716C, false, 0));
        new Thread(() -> {
            String html = Net.text(PAGE);
            if (!activity.isDestroyed()) activity.runOnUiThread(() -> {
                if (token == generation) render(html);
            });
        }).start();
    }

    private void render(String html) {
        root.removeAllViews();
        Document doc = Jsoup.parse(html == null ? "" : html);
        Element body = doc.selectFirst(".content-page-body");
        if (body == null || body.children().isEmpty()) {
            root.addView(line("गोपनीयता नीति आएन। नेट जाँच गरेर फेरि खोल्नुहोस्।", 15, 0xFFB42318, false, 0));
            return;
        }
        boolean dark = Night.on(activity);
        LinearLayout card = new LinearLayout(activity);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(16), dp(16), dp(8));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(dark ? 0xFF1A2420 : 0xFFFFFFFF);
        bg.setCornerRadius(dp(16));
        bg.setStroke(dp(1), dark ? 0xFF2C3A34 : 0xFFE6EBE8);
        card.setBackground(bg);
        boolean skippedTitle = false;
        for (Element child : body.children()) {
            String tag = child.tagName();
            String plain = child.text().replaceAll("\\s+", " ").trim();
            if (!skippedTitle && ("p".equals(tag) || "h1".equals(tag)) && "गोपनीयता नीति".equals(plain)) {
                skippedTitle = true;
                continue;
            }
            if ("h2".equals(tag) || "h3".equals(tag) || "h4".equals(tag)) {
                card.addView(line(plain, 18, dark ? 0xFFF4F7F5 : 0xFF142033, true, dp(14)));
            } else if ("ul".equals(tag) || "ol".equals(tag)) {
                for (Element item : child.children()) {
                    if (!"li".equals(item.tagName())) continue;
                    card.addView(bullet(item, dark));
                }
            } else if (!plain.isEmpty()) {
                card.addView(rich(child, 16, dark ? 0xFFE7EEEA : 0xFF292524, false, dp(8)));
            }
        }
        root.addView(card);
    }

    private View bullet(Element item, boolean dark) {
        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.HORIZONTAL);
        TextView mark = line("•", 16, 0xFF0F8A3C, true, 0);
        mark.setPadding(0, 0, dp(8), 0);
        row.addView(mark);
        TextView text = rich(item, 16, dark ? 0xFFE7EEEA : 0xFF292524, false, 0);
        row.addView(text, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(6);
        row.setLayoutParams(lp);
        return row;
    }

    private TextView rich(Element element, int size, int color, boolean bold, int gap) {
        String html = element.html().replace("<strong>", "<b>").replace("</strong>", "</b>");
        Spanned spanned = Build.VERSION.SDK_INT >= 24
                ? Html.fromHtml(html, Html.FROM_HTML_MODE_COMPACT)
                : Html.fromHtml(html);
        TextView view = line("", size, color, bold, gap);
        view.setText(spanned);
        if (Build.VERSION.SDK_INT >= 26) view.setJustificationMode(Layout.JUSTIFICATION_MODE_INTER_WORD);
        return view;
    }

    private TextView line(String value, int size, int color, boolean bold, int gap) {
        TextView view = new TextView(activity);
        view.setText(value);
        view.setTextColor(color);
        view.setTextSize(TypedValue.COMPLEX_UNIT_DIP, size);
        Typeface face = ResourcesCompat.getFont(activity, bold ? R.font.mukta_bold : R.font.mukta_regular);
        if (face != null) view.setTypeface(face);
        view.setLineSpacing(dp(2), 1f);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = gap;
        view.setLayoutParams(lp);
        return view;
    }

    private int dp(int value) {
        return Math.round(value * activity.getResources().getDisplayMetrics().density);
    }
}
