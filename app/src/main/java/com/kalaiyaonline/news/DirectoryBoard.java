package com.kalaiyaonline.news;

import android.content.Intent;
import android.graphics.Outline;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.graphics.drawable.Drawable;
import android.widget.HorizontalScrollView;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.graphics.drawable.DrawableCompat;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class DirectoryBoard {
    private final AppCompatActivity activity;
    private final LinearLayout root;
    private final LinearLayout list;
    private final TextView status;
    private final EditText query;
    private final List<Listing> listings = new ArrayList<>();
    private final LinearLayout chips;
    private final List<String> siteCategories = new ArrayList<>();
    private String category = "";
    private boolean detailOpen;
    private int page;
    private int generation;

    DirectoryBoard(AppCompatActivity activity) {
        this.activity = activity;
        root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);

        chips = new LinearLayout(activity);
        chips.setOrientation(LinearLayout.HORIZONTAL);
        chips.setPadding(0, dp(2), 0, dp(8));
        HorizontalScrollView scroll = new HorizontalScrollView(activity);
        scroll.setHorizontalScrollBarEnabled(false);
        scroll.addView(chips);
        root.addView(scroll);

        query = new EditText(activity);
        query.setHint("डाइरेक्टरी खोज्नुहोस्…");
        query.setSingleLine(true);
        query.setBackground(field());
        query.setPadding(dp(12), dp(10), dp(12), dp(10));
        query.setTypeface(ResourcesCompat.getFont(activity, R.font.mukta_regular));
        query.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                page = 0;
                showPage();
            }
            public void afterTextChanged(Editable s) {}
        });
        root.addView(query);

        status = text("", 13, 0xFF57534E, false);
        status.setPadding(0, dp(8), 0, 0);
        root.addView(status);

        list = new LinearLayout(activity);
        list.setOrientation(LinearLayout.VERTICAL);
        root.addView(list);
    }

    View view() {
        return root;
    }

    void load() {
        if (!listings.isEmpty()) {
            renderChips();
            if (!detailOpen) showPage();
        } else {
            status.setText("डाइरेक्टरी लोड हुँदैछ…");
        }
        new Thread(() -> {
            String html = download("https://kalaiyaonline.com/?directory=all");
            List<Listing> parsed = html == null ? null : parse(html);
            activity.runOnUiThread(() -> {
                if (parsed == null) {
                    if (listings.isEmpty()) {
                        status.setText("डाइरेक्टरी आएन। फेरि थिच्नुहोस्।");
                        status.setOnClickListener(v -> load());
                    }
                    return;
                }
                status.setOnClickListener(null);
                listings.clear();
                listings.addAll(parsed);
                if (!category.isEmpty() && !siteCategories.contains(category) && !hasCategory(category)) category = "";
                renderChips();
                if (!detailOpen) showPage();
            });
        }).start();
    }

    private boolean hasCategory(String name) {
        for (Listing item : listings) if (name.equals(item.category)) return true;
        return false;
    }

    private List<Listing> parse(String html) {
        List<String> found = new ArrayList<>();
        Matcher cats = Pattern.compile("<a\\b[^>]*href=\"[^\"]*directory_category=[^\"]*\"[^>]*>([\\s\\S]*?)</a>").matcher(html);
        while (cats.find()) {
            String name = cleanCategory(cats.group(1));
            if (name.length() < 2 || "सबै".equals(name) || found.contains(name)) continue;
            found.add(name);
        }
        siteCategories.clear();
        siteCategories.addAll(found);
        List<Listing> rows = new ArrayList<>();
        Matcher cards = Pattern.compile("<article class=\"directory-card-v3\">([\\s\\S]*?)</article>").matcher(html);
        while (cards.find()) {
            String block = cards.group(1);
            Listing item = new Listing();
            item.title = textOf(block, "<h2><a [^>]*>([^<]+)</a>");
            item.page = textOf(block, "href=\"(https://kalaiyaonline.com/directory/[^\"]+)\"");
            item.image = textOf(block, "<img src=\"([^\"]+)\"");
            item.category = cleanCategory(textOf(block, "directory-card-kicker\">([^<]+)"));
            item.location = textOf(block, "directory-card-location\">([^<]+)").replace("⌖", "").trim();
            item.blurb = textOf(block, "<p>([^<]+)</p>");
            if (!item.title.isEmpty()) rows.add(item);
        }
        return rows;
    }

    private void renderChips() {
        chips.removeAllViews();
        chips.addView(chip("सबै", "", category.isEmpty()));
        LinkedHashSet<String> names = new LinkedHashSet<>(siteCategories);
        for (Listing item : listings) if (!item.category.isEmpty()) names.add(item.category);
        for (String name : names) chips.addView(chip(name, name, name.equals(category)));
    }

    private View chip(String label, String value, boolean on) {
        TextView view = text(label, 14, on ? 0xFFFFFFFF : 0xFF1C1917, true);
        view.setGravity(Gravity.CENTER_VERTICAL);
        Drawable icon = DrawableCompat.wrap(activity.getDrawable(iconFor(label))).mutate();
        DrawableCompat.setTint(icon, on ? 0xFFFFFFFF : 0xFF0F8A3C);
        icon.setBounds(0, 0, dp(16), dp(16));
        view.setCompoundDrawables(icon, null, null, null);
        view.setCompoundDrawablePadding(dp(6));
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(20));
        if (on) bg.setColor(0xFF0F8A3C);
        else {
            bg.setColor(0xFFFFFFFF);
            bg.setStroke(dp(1), 0xFFE5E7EB);
        }
        view.setBackground(bg);
        view.setPadding(dp(12), dp(7), dp(14), dp(7));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.rightMargin = dp(8);
        view.setLayoutParams(lp);
        view.setOnClickListener(v -> {
            category = value;
            page = 0;
            renderChips();
            showPage();
        });
        return view;
    }

    private int iconFor(String label) {
        if (label.contains("अस्पताल") || label.contains("स्वास्थ्य")) return R.drawable.ic_dir_health;
        if (label.contains("कम्प्युटर") || label.contains("आईटी") || label.contains("IT")) return R.drawable.ic_dir_laptop;
        if (label.contains("क्याफे") || label.contains("खाजा")) return R.drawable.ic_dir_cafe;
        if (label.contains("बैंक") || label.contains("वित्त")) return R.drawable.ic_cat_wallet;
        if (label.contains("विद्यालय") || label.contains("कलेज")) return R.drawable.ic_cat_pen;
        if (label.contains("व्यवसाय") || label.contains("कम्पनी")) return R.drawable.ic_dir_shop;
        if (label.contains("सरकारी")) return R.drawable.ic_cat_politics;
        if (label.contains("होटल") || label.contains("रेष्ट")) return R.drawable.ic_home;
        if ("सबै".equals(label)) return R.drawable.ic_menu;
        return R.drawable.ic_pin;
    }

    private void showPage() {
        detailOpen = false;
        if (listings.isEmpty()) return;
        generation++;
        String needle = query.getText().toString().trim();
        List<Listing> matches = new ArrayList<>();
        for (Listing item : listings) {
            if (!category.isEmpty() && !cleanCategory(category).equals(cleanCategory(item.category))) continue;
            String blob = item.title + " " + item.category + " " + item.location + " " + item.blurb;
            if (needle.isEmpty() || blob.contains(needle)) matches.add(item);
        }
        list.removeAllViews();
        if (matches.isEmpty()) {
            status.setText("सूची भेटिएन");
            list.addView(text("यो खोजमा केही भेटिएन।", 14, 0xFF57534E, false));
            return;
        }
        int pages = Math.max(1, (matches.size() + 6) / 7);
        if (page >= pages) page = pages - 1;
        int start = page * 7;
        int end = Math.min(matches.size(), start + 7);
        status.setText((start + 1) + "–" + end + " / " + matches.size());
        int token = generation;
        for (int i = start; i < end; i++) list.addView(card(matches.get(i), token));
        LinearLayout pager = new LinearLayout(activity);
        pager.setOrientation(LinearLayout.HORIZONTAL);
        pager.setGravity(Gravity.CENTER);
        pager.setPadding(0, dp(12), 0, dp(8));
        if (page > 0) pager.addView(action("अघिल्लो", () -> { page--; showPage(); }));
        TextView label = text("  " + (page + 1) + " / " + pages + "  ", 14, 0xFF142033, true);
        pager.addView(label);
        if (page + 1 < pages) pager.addView(action("पछिल्लो", () -> { page++; showPage(); }));
        list.addView(pager);
    }

    private View card(Listing item, int token) {
        LinearLayout card = new LinearLayout(activity);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setBackground(cardBg());
        card.setPadding(dp(8), dp(8), dp(8), dp(8));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(8);
        card.setLayoutParams(lp);

        ImageView image = new ImageView(activity);
        image.setScaleType(ImageView.ScaleType.CENTER_CROP);
        image.setBackgroundColor(0xFFE7E5E4);
        card.addView(image, new LinearLayout.LayoutParams(dp(64), dp(64)));
        if (!item.image.isEmpty()) loadImage(item.image, image, token, 280);

        LinearLayout copy = new LinearLayout(activity);
        copy.setOrientation(LinearLayout.VERTICAL);
        copy.setPadding(dp(10), 0, 0, 0);
        copy.addView(text(item.title, 16, 0xFF142033, true));
        if (!item.category.isEmpty()) copy.addView(text(item.category, 12, 0xFF0B5C28, false));
        if (!item.location.isEmpty()) copy.addView(text(item.location, 12, 0xFF78716C, false));
        card.addView(copy, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        card.setOnClickListener(v -> openDetail(item));
        return card;
    }

    private void openDetail(Listing item) {
        detailOpen = true;
        generation++;
        int token = generation;
        list.removeAllViews();
        status.setText("");

        TextView back = text("←  सूची", 14, 0xFF0B5C28, true);
        back.setPadding(dp(12), dp(8), dp(12), dp(8));
        GradientDrawable backBg = new GradientDrawable();
        backBg.setCornerRadius(dp(16));
        backBg.setColor(0xFFE5F6EC);
        back.setBackground(backBg);
        back.setOnClickListener(v -> showPage());
        LinearLayout.LayoutParams backLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        backLp.topMargin = dp(8);
        back.setLayoutParams(backLp);
        list.addView(back);

        FrameLayout hero = new FrameLayout(activity);
        hero.setBackgroundColor(0xFFE7E5E4);
        hero.setClipToOutline(true);
        hero.setOutlineProvider(new ViewOutlineProvider() {
            public void getOutline(View view, Outline outline) {
                outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), dp(18));
            }
        });
        LinearLayout.LayoutParams heroLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(220));
        heroLp.topMargin = dp(12);
        list.addView(hero, heroLp);
        ImageView image = new ImageView(activity);
        image.setScaleType(ImageView.ScaleType.CENTER_CROP);
        hero.addView(image, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        if (!item.image.isEmpty()) loadImage(item.image, image, token, 1280);

        LinearLayout sheet = new LinearLayout(activity);
        sheet.setOrientation(LinearLayout.VERTICAL);
        sheet.setBackground(cardBg());
        sheet.setPadding(dp(14), dp(14), dp(14), dp(16));
        LinearLayout.LayoutParams sheetLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        sheetLp.topMargin = dp(12);
        sheet.setLayoutParams(sheetLp);
        if (!item.category.isEmpty()) {
            TextView chip = text(item.category, 12, 0xFF0B5C28, true);
            chip.setPadding(dp(10), dp(4), dp(10), dp(4));
            GradientDrawable chipBg = new GradientDrawable();
            chipBg.setCornerRadius(dp(12));
            chipBg.setColor(0xFFE5F6EC);
            chip.setBackground(chipBg);
            LinearLayout.LayoutParams chipLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            chipLp.bottomMargin = dp(8);
            chip.setLayoutParams(chipLp);
            sheet.addView(chip);
        }
        sheet.addView(text(item.title, 22, 0xFF142033, true));
        if (!item.location.isEmpty()) {
            TextView place = text(item.location, 14, 0xFF57534E, false);
            place.setPadding(0, dp(4), 0, 0);
            sheet.addView(place);
        }
        TextView body = text(item.blurb.isEmpty() ? "विवरण लोड हुँदैछ…" : item.blurb, 15, 0xFF292524, false);
        body.setPadding(0, dp(10), 0, 0);
        body.setLineSpacing(dp(2), 1f);
        sheet.addView(body);
        LinearLayout actions = new LinearLayout(activity);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setPadding(0, dp(14), 0, 0);
        sheet.addView(actions);
        list.addView(sheet);

        new Thread(() -> {
            String html = item.page.isEmpty() ? null : download(item.page);
            activity.runOnUiThread(() -> {
                if (token != generation || html == null) return;
                String photo = textOf(html, "data-kp-lightbox-src=\"([^\"]+)\"");
                if (!photo.isEmpty() && !photo.equals(item.image)) loadImage(photo, image, token, 1280);
                String about = textOf(html, "kp-short-description\">([^<]+)");
                String address = textOf(html, "ठेगाना</strong><p>([^<]+)</p>");
                String phone = textOf(html, "सम्पर्क नम्बर</strong><p>([^<]+)</p>");
                String rating = textOf(html, "kp-rating-score\"><strong>([^<]+)</strong>");
                String map = textOf(html, "href=\"(https://maps\\.google\\.com/[^\"]+)\"");
                String whatsapp = textOf(html, "href=\"(https://wa\\.me/[^\"]+)\"");
                if (!about.isEmpty()) body.setText(about);
                if (!rating.isEmpty()) {
                    TextView score = text("★  " + rating + " / 5", 14, 0xFFB45309, true);
                    score.setPadding(0, dp(8), 0, 0);
                    sheet.addView(score, sheet.indexOfChild(body));
                }
                if (!address.isEmpty()) {
                    TextView line = text("ठेगाना\n" + address, 14, 0xFF142033, false);
                    line.setPadding(0, dp(12), 0, 0);
                    sheet.addView(line, sheet.indexOfChild(actions));
                }
                if (!phone.isEmpty()) actions.addView(action("फोन", () -> open("tel:" + phone.replace(" ", ""))));
                if (!whatsapp.isEmpty()) actions.addView(action("WhatsApp", () -> open(whatsapp)));
                if (!map.isEmpty()) actions.addView(action("नक्सा", () -> open(map)));
            });
        }).start();
    }

    private String download(String address) {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(address).openConnection();
            connection.setRequestProperty("User-Agent", "Mozilla/5.0");
            connection.setConnectTimeout(12000);
            connection.setReadTimeout(20000);
            InputStream input = connection.getInputStream();
            StringBuilder body = new StringBuilder();
            byte[] buffer = new byte[8192];
            int read;
            while ((read = input.read(buffer)) != -1) body.append(new String(buffer, 0, read, "UTF-8"));
            input.close();
            return body.toString();
        } catch (Exception ignored) {
            return null;
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    private void loadImage(String url, ImageView target, int token, int maxSide) {
        new Thread(() -> {
            Bitmap bitmap = fetchBitmap(url, maxSide);
            if (bitmap != null && token == generation) activity.runOnUiThread(() -> {
                if (token == generation) target.setImageBitmap(bitmap);
            });
        }).start();
    }

    private Bitmap fetchBitmap(String url, int maxSide) {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setInstanceFollowRedirects(true);
            connection.setRequestProperty("User-Agent", "Mozilla/5.0");
            connection.setRequestProperty("Accept", "image/*,*/*");
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
            Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length, options);
            return bitmap != null ? bitmap : BitmapFactory.decodeByteArray(data, 0, data.length);
        } catch (Exception ignored) {
            return null;
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    private String textOf(String html, String source) {
        Matcher matcher = Pattern.compile(source).matcher(html);
        return matcher.find() ? decode(matcher.group(1).trim()) : "";
    }

    private String cleanCategory(String value) {
        String raw = decode(value == null ? "" : value).replaceAll("<[^>]+>", " ");
        StringBuilder out = new StringBuilder();
        raw.codePoints().forEach(cp -> {
            int type = Character.getType(cp);
            if (type == Character.OTHER_SYMBOL || type == Character.FORMAT || type == Character.SURROGATE) return;
            out.appendCodePoint(cp);
        });
        return out.toString().replaceAll("\\s+", " ").trim();
    }

    private String decode(String value) {
        String text = value.replace("\u0026amp;", "\u0026")
                .replace("\u0026quot;", "\"")
                .replace("\u0026#039;", "'")
                .replace("\u0026nbsp;", " ");
        Matcher marks = Pattern.compile("\u0026#(x?[0-9A-Fa-f]+);").matcher(text);
        StringBuffer decoded = new StringBuffer();
        while (marks.find()) {
            try {
                String raw = marks.group(1);
                int point = raw.charAt(0) == 'x' || raw.charAt(0) == 'X' ? Integer.parseInt(raw.substring(1), 16) : Integer.parseInt(raw);
                marks.appendReplacement(decoded, Matcher.quoteReplacement(new String(Character.toChars(point))));
            } catch (Exception ignored) {
                marks.appendReplacement(decoded, "");
            }
        }
        marks.appendTail(decoded);
        return decoded.toString();
    }

    private View button(String label, Runnable action) {
        TextView view = text(label, 15, 0xFFFFFFFF, true);
        view.setGravity(Gravity.CENTER);
        view.setPadding(dp(12), dp(10), dp(12), dp(10));
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(12));
        bg.setColor(0xFF0F8A3C);
        view.setBackground(bg);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(8);
        view.setLayoutParams(lp);
        view.setOnClickListener(v -> action.run());
        return view;
    }

    private View action(String label, Runnable task) {
        TextView view = text(label, 14, 0xFF0B5C28, true);
        view.setPadding(dp(12), dp(8), dp(12), dp(8));
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(12));
        bg.setColor(0xFFE5F6EC);
        view.setBackground(bg);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.rightMargin = dp(8);
        view.setLayoutParams(lp);
        view.setOnClickListener(v -> task.run());
        return view;
    }

    private void open(String url) {
        try {
            activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (Exception ignored) {
        }
    }

    private GradientDrawable field() {
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xFFFFFFFF);
        bg.setCornerRadius(dp(12));
        bg.setStroke(dp(1), 0xFFE7E5E4);
        return bg;
    }

    private GradientDrawable cardBg() {
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xFFFFFFFF);
        bg.setCornerRadius(dp(14));
        return bg;
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

    private static final class Listing {
        String title = "";
        String category = "";
        String location = "";
        String blurb = "";
        String image = "";
        String page = "";
    }
}
