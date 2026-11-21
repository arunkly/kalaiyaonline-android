package com.kalaiyaonline.news;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.res.ResourcesCompat;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

final class SocialBoard {
    private static final String HOME = "https://kalaiyaonline.com/social.php";
    private static final String[] EMOJI = {"😀", "😍", "😂", "❤️", "👍", "🙏", "🎉", "🔥", "😢", "😮", "👏", "🇳🇵"};
    private final Activity activity;
    private final LinearLayout root;
    private int generation;
    private String pageUrl = HOME;
    private boolean gated;

    SocialBoard(Activity activity) {
        this.activity = activity;
        root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFFF0F2F5);
    }

    View view() {
        return root;
    }

    boolean waitingForLogin() {
        return gated;
    }

    void load() {
        open(HOME);
    }

    void open(String url) {
        int token = ++generation;
        pageUrl = url == null || url.isEmpty() ? HOME : url;
        root.removeAllViews();
        root.addView(note("समुदाय लोड हुँदैछ…", 15, 0xFF78716C, false));
        new Thread(() -> {
            String[] base = {pageUrl};
            String html = fetch("GET", pageUrl, null, base);
            if (!activity.isDestroyed()) activity.runOnUiThread(() -> {
                if (token != generation || activity.isDestroyed()) return;
                try {
                    render(html, base[0] == null || base[0].isEmpty() ? HOME : base[0]);
                } catch (RuntimeException ignored) {
                    root.removeAllViews();
                    root.addView(card(loginCard(true)));
                }
            });
        }).start();
    }

    private void render(String html, String url) {
        root.removeAllViews();
        pageUrl = url;
        Document doc = Jsoup.parse(html == null ? "" : html, url);
        doc.select("script, style, header, footer, nav, .aks-mobile-bottom-bar, .ads-slot").remove();
        boolean login = doc.selectFirst("input[name=mode][value=login]") != null;
        gated = html == null || (login && doc.selectFirst("textarea") == null);
        if (gated) {
            root.addView(card(loginCard(html == null)));
            return;
        }
        Element alert = doc.selectFirst(".social-alert, .koc-alert, .alert");
        if (alert != null && !alert.text().trim().isEmpty()) root.addView(note(alert.text(), 14, 0xFF0F8A3C, true));
        if (!HOME.equals(url)) root.addView(textButton("फिड", () -> open(HOME), false));
        root.addView(newsStrip(doc));
        Element area = composerArea(doc);
        if (area != null) root.addView(card(composer(area)));
        List<Element> posts = posts(doc);
        for (Element post : posts) root.addView(card(postCard(post)));
        View members = members(doc);
        if (members != null) root.addView(card(members));
        if (area == null && posts.isEmpty() && members == null) root.addView(note("समुदायमा अहिले केही छैन।", 15, 0xFF78716C, false));
    }

    private Element composerArea(Document doc) {
        for (Element area : doc.select("textarea")) {
            String hint = area.attr("placeholder");
            if (hint.contains("status") || hint.contains("स्टाटस") || hint.contains("आजको") || area.closest(".social-composer") != null) return area;
        }
        return doc.selectFirst("textarea");
    }

    private View composer(Element area) {
        Element form = area.closest("form");
        if (form == null) form = area.parent();
        Element scope = form;
        for (int i = 0; i < 4 && scope.parent() != null; i++) {
            if (scope.text().contains("प्रकाशित") || scope.text().contains("Poll") || scope.text().contains("Emoji")) break;
            scope = scope.parent();
        }
        LinearLayout box = new LinearLayout(activity);
        box.setOrientation(LinearLayout.VERTICAL);
        LinearLayout head = new LinearLayout(activity);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);
        head.addView(avatar(firstImage(form), Account.name(), dp(40)));
        EditText status = input(area.attr("placeholder").isEmpty() ? "तपाईंको आजको स्टाटस के छ?" : area.attr("placeholder"), true);
        status.setBackground(pill(0xFFF0F2F5));
        status.setMinLines(2);
        LinearLayout.LayoutParams statusLp = new LinearLayout.LayoutParams(0, dp(76), 1f);
        statusLp.leftMargin = dp(10);
        status.setLayoutParams(statusLp);
        head.addView(status);
        box.addView(head);

        LinkedHashMap<String, String> fields = fieldsOf(form);
        String key = area.attr("name");
        LinearLayout extras = new LinearLayout(activity);
        extras.setOrientation(LinearLayout.VERTICAL);
        extras.setVisibility(View.GONE);
        EditText link = input("समाचारको लिंक", false);
        link.setVisibility(View.GONE);
        LinearLayout polls = new LinearLayout(activity);
        polls.setOrientation(LinearLayout.VERTICAL);
        polls.setVisibility(View.GONE);
        List<Element> pollInputs = new ArrayList<>();
        for (Element input : form.select("input[name], textarea[name]")) {
            if (input == area) continue;
            String type = input.attr("type").toLowerCase();
            if ("hidden".equals(type) || "submit".equals(type) || "file".equals(type)) continue;
            String name = input.attr("name").toLowerCase();
            if (name.contains("link") || name.contains("url")) {
                link.setHint(input.attr("placeholder").isEmpty() ? "समाचारको लिंक" : input.attr("placeholder"));
                link.setTag(input.attr("name"));
            } else if (name.contains("poll") || name.contains("option") || name.contains("choice")) {
                pollInputs.add(input);
            }
        }
        for (Element input : pollInputs) {
            EditText field = input(input.attr("placeholder").isEmpty() ? "विकल्प" : input.attr("placeholder"), false);
            field.setTag(input.attr("name"));
            polls.addView(field);
        }
        LinearLayout emoji = new LinearLayout(activity);
        emoji.setOrientation(LinearLayout.VERTICAL);
        emoji.setVisibility(View.GONE);
        box.addView(modes(status, link, polls, emoji));
        box.addView(emojiGrid(emoji, status, scope));
        box.addView(link);
        box.addView(polls);
        Element select = form.selectFirst("select");
        if (select != null) box.addView(privacy(select, fields));
        Element submit = form.selectFirst("button[type=submit], input[type=submit], button");
        String label = "प्रकाशित गर्नुहोस्";
        if (submit != null && submit.text().contains("प्रकाशित")) label = submit.text();
        Element button = submit;
        Element formEl = form;
        box.addView(wideButton(label, 0xFF0F8A3C, () -> {
            if (!key.isEmpty()) fields.put(key, status.getText().toString().trim());
            if (link.getVisibility() == View.VISIBLE && link.getTag() instanceof String) fields.put((String) link.getTag(), link.getText().toString().trim());
            else if (link.getVisibility() == View.VISIBLE && !link.getText().toString().trim().isEmpty() && !key.isEmpty()) {
                String current = fields.get(key);
                fields.put(key, ((current == null ? "" : current) + "\n" + link.getText().toString().trim()).trim());
            }
            for (int i = 0; i < polls.getChildCount(); i++) {
                View child = polls.getChildAt(i);
                if (child instanceof EditText && child.getTag() instanceof String) {
                    fields.put((String) child.getTag(), ((EditText) child).getText().toString().trim());
                }
            }
            if (key.isEmpty() || fields.get(key) == null || fields.get(key).isEmpty()) return;
            send(formEl, button, fields);
        }));
        return box;
    }

    private View modes(EditText status, EditText link, LinearLayout polls, LinearLayout emoji) {
        LinearLayout line = new LinearLayout(activity);
        line.setOrientation(LinearLayout.HORIZONTAL);
        line.setGravity(Gravity.CENTER_VERTICAL);
        line.setPadding(dp(4), dp(4), dp(4), dp(4));
        line.setBackground(pill(0xFFF3F6F4));
        LinearLayout.LayoutParams lineLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lineLp.topMargin = dp(12);
        line.setLayoutParams(lineLp);
        String[] labels = {"स्टाटस", "लिंक", "पोल", "इमोजी"};
        TextView[] chips = new TextView[4];
        Runnable[] apply = new Runnable[4];
        apply[0] = () -> { link.setVisibility(View.GONE); polls.setVisibility(View.GONE); emoji.setVisibility(View.GONE); };
        apply[1] = () -> { link.setVisibility(View.VISIBLE); polls.setVisibility(View.GONE); emoji.setVisibility(View.GONE); };
        apply[2] = () -> { link.setVisibility(View.GONE); polls.setVisibility(polls.getChildCount() == 0 ? View.GONE : View.VISIBLE); emoji.setVisibility(View.GONE); };
        apply[3] = () -> { link.setVisibility(View.GONE); polls.setVisibility(View.GONE); emoji.setVisibility(View.VISIBLE); };
        Runnable[] paint = new Runnable[1];
        int[] picked = {0};
        paint[0] = () -> {
            for (int n = 0; n < chips.length; n++) {
                boolean on = n == picked[0];
                chips[n].setTextColor(on ? 0xFFFFFFFF : 0xFF3F4A45);
                GradientDrawable bg = new GradientDrawable();
                bg.setCornerRadius(dp(12));
                bg.setColor(on ? 0xFF0F8A3C : 0x00000000);
                chips[n].setBackground(bg);
            }
        };
        for (int index = 0; index < labels.length; index++) {
            TextView chip = note(labels[index], 13, 0xFF3F4A45, true);
            chip.setGravity(Gravity.CENTER);
            chip.setIncludeFontPadding(false);
            chip.setPadding(dp(4), dp(5), dp(4), dp(5));
            chip.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            chips[index] = chip;
            int pick = index;
            chip.setOnClickListener(v -> {
                picked[0] = pick;
                paint[0].run();
                apply[pick].run();
            });
            line.addView(chip);
        }
        paint[0].run();
        return line;
    }

    private View emojiGrid(LinearLayout host, EditText status, Element scope) {
        host.setVisibility(View.GONE);
        List<String> faces = new ArrayList<>();
        for (Element button : scope.select("button, span")) {
            String text = button.ownText().trim();
            if (text.length() >= 1 && text.length() <= 3 && text.codePointAt(0) > 255 && !text.contains("स्टाटस")) faces.add(text);
        }
        if (faces.isEmpty()) for (String face : EMOJI) faces.add(face);
        int columns = 6;
        for (int i = 0; i < faces.size(); i += columns) {
            LinearLayout row = new LinearLayout(activity);
            row.setOrientation(LinearLayout.HORIZONTAL);
            for (int c = 0; c < columns && i + c < faces.size(); c++) {
                String face = faces.get(i + c);
                TextView cell = note(face, 22, 0xFF142033, false);
                cell.setGravity(Gravity.CENTER);
                cell.setBackground(stroke(0xFFFFFFFF, 0xFFE5E7EB));
                cell.setPadding(dp(4), dp(8), dp(4), dp(8));
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(48), 1f);
                lp.setMargins(dp(3), dp(3), dp(3), dp(3));
                cell.setLayoutParams(lp);
                cell.setOnClickListener(v -> status.getText().append(face));
                row.addView(cell);
            }
            host.addView(row);
        }
        return host;
    }

    private View privacy(Element select, LinkedHashMap<String, String> fields) {
        String name = select.attr("name");
        List<Element> options = select.select("option");
        int[] index = {0};
        for (int i = 0; i < options.size(); i++) if (options.get(i).hasAttr("selected")) index[0] = i;
        TextView view = note(options.isEmpty() ? "सबैले देख्ने ▾" : options.get(index[0]).text() + " ▾", 13, 0xFF3F4A45, true);
        view.setPadding(dp(10), dp(6), dp(10), dp(6));
        view.setBackground(stroke(0xFFF8FAFC, 0xFFE5E7EB));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(10);
        view.setLayoutParams(lp);
        if (!options.isEmpty() && !name.isEmpty()) fields.put(name, options.get(index[0]).hasAttr("value") ? options.get(index[0]).attr("value") : options.get(index[0]).text());
        view.setOnClickListener(v -> {
            if (options.isEmpty()) return;
            index[0] = (index[0] + 1) % options.size();
            Element option = options.get(index[0]);
            view.setText(option.text() + " ▾");
            if (!name.isEmpty()) fields.put(name, option.hasAttr("value") ? option.attr("value") : option.text());
        });
        return view;
    }

    private View newsStrip(Document doc) {
        HorizontalScrollView scroll = new HorizontalScrollView(activity);
        scroll.setHorizontalScrollBarEnabled(false);
        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, 0, 0, dp(8));
        int count = 0;
        for (Element link : doc.select("a[href*=/news/]")) {
            if (link.selectFirst("img") == null) continue;
            if (count >= 8) break;
            String href = link.attr("abs:href");
            String title = link.text().replaceAll("\\s+", " ").trim();
            if (title.length() < 8) title = link.attr("title");
            if (title.isEmpty()) continue;
            String image = link.selectFirst("img").attr("abs:src");
            FrameLayout tile = new FrameLayout(activity);
            ImageView photo = new ImageView(activity);
            photo.setScaleType(ImageView.ScaleType.CENTER_CROP);
            tile.addView(photo, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            TextView caption = note(title, 12, 0xFFFFFFFF, true);
            caption.setMaxLines(4);
            caption.setPadding(dp(8), dp(28), dp(8), dp(8));
            caption.setGravity(Gravity.BOTTOM);
            caption.setBackgroundColor(0x66000000);
            tile.addView(caption, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, Gravity.BOTTOM));
            GradientDrawable bg = new GradientDrawable();
            bg.setCornerRadius(dp(14));
            bg.setColor(0xFFE5E7EB);
            tile.setBackground(bg);
            tile.setClipToOutline(true);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(132), dp(176));
            lp.rightMargin = dp(8);
            tile.setLayoutParams(lp);
            String slug = slug(href);
            String storyTitle = title;
            tile.setOnClickListener(v -> {
                if (slug == null) return;
                try {
                    Intent intent = new Intent(activity, ArticleActivity.class);
                    intent.putExtra("slug", slug);
                    intent.putExtra("title", storyTitle);
                    intent.putExtra("image", image);
                    activity.startActivity(intent);
                } catch (Exception ignored) {
                }
            });
            row.addView(tile);
            loadImage(image, photo);
            count++;
        }
        if (count == 0) return new View(activity);
        scroll.addView(row);
        return scroll;
    }

    private List<Element> posts(Document doc) {
        List<Element> cards = new ArrayList<>();
        for (Element input : doc.select("input[placeholder], textarea[placeholder]")) {
            String hint = input.attr("placeholder");
            if (!hint.contains("टिप्पणी") && !hint.toLowerCase().contains("comment")) continue;
            Element card = input.parent();
            if (card == null) continue;
            while (card.parent() != null && !"body".equals(card.parent().tagName())) {
                Element parent = card.parent();
                if (parent.select("textarea[placeholder*=आजको], textarea[placeholder*=status]").size() > 0) break;
                if (parent.select("input[placeholder*=टिप्पणी], textarea[placeholder*=टिप्पणी]").size() > 1) break;
                card = parent;
                if ("article".equals(card.tagName()) || card.className().contains("post")) break;
            }
            if (card == null || cards.contains(card)) continue;
            if (card.selectFirst("textarea[placeholder*=status], textarea[placeholder*=आजको]") != null) continue;
            cards.add(card);
        }
        if (cards.isEmpty()) for (Element post : doc.select(".social-post")) cards.add(post);
        return cards;
    }

    private View postCard(Element post) {
        LinearLayout box = new LinearLayout(activity);
        box.setOrientation(LinearLayout.VERTICAL);
        LinearLayout head = new LinearLayout(activity);
        head.setGravity(Gravity.CENTER_VERTICAL);
        String photo = firstImage(post);
        String author = shortText(post.selectFirst("b, strong, h3, h2"));
        head.addView(avatar(photo, author, dp(42)));
        LinearLayout who = new LinearLayout(activity);
        who.setOrientation(LinearLayout.VERTICAL);
        who.setPadding(dp(10), 0, 0, 0);
        if (!author.isEmpty()) who.addView(note(author, 15, 0xFF050505, true));
        String meta = meta(post, author);
        if (!meta.isEmpty()) who.addView(note(meta, 12, 0xFF65676B, false));
        head.addView(who, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        for (Element form : post.select("form")) {
            Element button = form.selectFirst("button, input[type=submit]");
            if (button == null || !button.text().contains("हटाउनुहोस्")) continue;
            TextView danger = note("हटाउनुहोस्", 12, 0xFFBE123C, true);
            danger.setPadding(dp(8), dp(4), 0, dp(4));
            danger.setOnClickListener(v -> send(form, button, fieldsOf(form)));
            head.addView(danger);
            break;
        }
        box.addView(head);
        String body = body(post, author, meta);
        if (!body.isEmpty()) {
            TextView copy = note(body, 16, 0xFF050505, false);
            copy.setPadding(0, dp(10), 0, dp(4));
            box.addView(copy);
        }
        for (Element image : post.select("img")) {
            String src = image.attr("abs:src");
            if (src.isEmpty() || src.equals(photo)) continue;
            ImageView view = new ImageView(activity);
            view.setAdjustViewBounds(true);
            view.setScaleType(ImageView.ScaleType.CENTER_CROP);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(180));
            lp.topMargin = dp(8);
            view.setLayoutParams(lp);
            box.addView(view);
            loadImage(src, view);
        }
        LinearLayout tools = new LinearLayout(activity);
        tools.setOrientation(LinearLayout.HORIZONTAL);
        tools.setGravity(Gravity.CENTER_VERTICAL);
        tools.setPadding(0, dp(4), 0, dp(2));
        View rule = new View(activity);
        rule.setBackgroundColor(0xFFE4E6EB);
        LinearLayout.LayoutParams ruleLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1));
        ruleLp.topMargin = dp(8);
        rule.setLayoutParams(ruleLp);
        box.addView(rule);
        for (Element form : post.select("form")) {
            String text = form.text();
            if (text.contains("हटाउनुहोस्") || text.contains("टिप्पणी") || text.contains("पठाउनुहोस्") || text.contains("साथी")) continue;
            Element button = form.selectFirst("button, input[type=submit]");
            if (button == null) continue;
            TextView action = note(button.text().isEmpty() ? "मन" : button.text(), 14, 0xFF65676B, true);
            action.setGravity(Gravity.CENTER);
            action.setPadding(dp(4), dp(8), dp(4), dp(8));
            action.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            action.setOnClickListener(v -> send(form, button, fieldsOf(form)));
            tools.addView(action);
        }
        TextView comment = note("टिप्पणी", 14, 0xFF65676B, true);
        comment.setGravity(Gravity.CENTER);
        comment.setPadding(dp(4), dp(8), dp(4), dp(8));
        comment.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        tools.addView(comment);
        if (tools.getChildCount() > 0) box.addView(tools);
        box.addView(shares(post));
        Element reply = null;
        for (Element form : post.select("form")) {
            if (form.selectFirst("input[placeholder*=टिप्पणी], textarea[placeholder*=टिप्पणी], input[placeholder*=comment]") != null) reply = form;
        }
        if (reply != null) box.addView(replyRow(reply));
        return box;
    }

    private View shares(Element post) {
        LinearLayout grid = new LinearLayout(activity);
        grid.setOrientation(LinearLayout.VERTICAL);
        String[][] targets = {{"facebook", "Facebook"}, {"x.com", "X"}, {"twitter", "X"}, {"whatsapp", "WhatsApp"}};
        List<Element> links = new ArrayList<>();
        for (Element link : post.select("a[href]")) {
            String href = link.attr("href").toLowerCase();
            String text = link.text().toLowerCase();
            if (href.contains("facebook") || href.contains("twitter") || href.contains("x.com") || href.contains("whatsapp") || text.contains("link") || text.contains("facebook") || text.contains("whatsapp")) links.add(link);
        }
        if (links.isEmpty()) return new View(activity);
        LinearLayout row = null;
        int n = 0;
        for (Element link : links) {
            if (n % 2 == 0) {
                row = new LinearLayout(activity);
                row.setOrientation(LinearLayout.HORIZONTAL);
                grid.addView(row);
            }
            TextView chip = note(link.text().trim().isEmpty() ? "सेयर" : link.text().trim(), 13, 0xFF65676B, true);
            chip.setGravity(Gravity.CENTER);
            chip.setPadding(dp(6), dp(6), dp(6), dp(6));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            chip.setLayoutParams(lp);
            String href = link.attr("abs:href");
            String label = link.text().toLowerCase();
            chip.setOnClickListener(v -> {
                try {
                    if (href == null || href.isEmpty()) return;
                    if (label.contains("link")) {
                        ClipboardManager clipboard = (ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
                        if (clipboard != null) clipboard.setPrimaryClip(ClipData.newPlainText("link", href));
                        chip.setText("कपी भयो");
                    } else activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(href)));
                } catch (Exception ignored) {
                }
            });
            row.addView(chip);
            n++;
        }
        return grid;
    }

    private View replyRow(Element form) {
        Element field = form.selectFirst("input[placeholder], textarea[placeholder]");
        LinearLayout row = new LinearLayout(activity);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(8), 0, 0);
        EditText input = input(field == null || field.attr("placeholder").isEmpty() ? "टिप्पणी लेख्नुहोस्..." : field.attr("placeholder"), false);
        row.addView(input, new LinearLayout.LayoutParams(0, dp(44), 1f));
        Element button = form.selectFirst("button, input[type=submit]");
        TextView send = note(button == null || button.text().isEmpty() ? "पठाउनुहोस्" : button.text(), 14, 0xFFFFFFFF, true);
        send.setGravity(Gravity.CENTER);
        send.setIncludeFontPadding(false);
        send.setBackground(pill(0xFF0F8A3C));
        send.setPadding(dp(12), dp(7), dp(12), dp(7));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.leftMargin = dp(8);
        send.setLayoutParams(lp);
        send.setOnClickListener(v -> {
            LinkedHashMap<String, String> fields = fieldsOf(form);
            if (field != null && !field.attr("name").isEmpty()) fields.put(field.attr("name"), input.getText().toString().trim());
            String typed = field == null || field.attr("name").isEmpty() ? "" : fields.get(field.attr("name"));
            if (field != null && !field.attr("name").isEmpty() && (typed == null || typed.isEmpty())) return;
            send(form, button, fields);
        });
        row.addView(send);
        return row;
    }

    private View members(Document doc) {
        List<Element> buttons = new ArrayList<>();
        for (Element button : doc.select("button, a, input[type=submit]")) if (friendButton(button)) buttons.add(button);
        if (buttons.isEmpty()) return null;
        LinearLayout box = new LinearLayout(activity);
        box.setOrientation(LinearLayout.VERTICAL);
        LinearLayout titleRow = new LinearLayout(activity);
        titleRow.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = note("दर्ता भएका सदस्यहरू", 16, 0xFF050505, true);
        titleRow.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        for (Element link : doc.select("a")) {
            if (!link.text().contains("सबै हेर्नुहोस्")) continue;
            TextView more = note("सबै", 14, 0xFF0F8A3C, true);
            more.setOnClickListener(v -> open(link.attr("abs:href")));
            titleRow.addView(more);
            break;
        }
        box.addView(titleRow);
        HorizontalScrollView scroll = new HorizontalScrollView(activity);
        scroll.setHorizontalScrollBarEnabled(false);
        LinearLayout row = new LinearLayout(activity);
        row.setPadding(0, dp(10), 0, 0);
        List<Element> seen = new ArrayList<>();
        java.util.LinkedHashSet<String> names = new java.util.LinkedHashSet<>();
        for (Element button : buttons) {
            Element card = personCard(button);
            if (card == null || seen.contains(card)) continue;
            String name = personName(card, button);
            String photo = "";
            Element image = card.selectFirst("img");
            if (image != null) photo = image.attr("abs:src");
            String key = name.toLowerCase() + "|" + photo;
            if (name.isEmpty() || names.contains(key)) continue;
            names.add(key);
            seen.add(card);
            LinearLayout person = new LinearLayout(activity);
            person.setOrientation(LinearLayout.VERTICAL);
            person.setGravity(Gravity.CENTER_HORIZONTAL);
            person.setBackground(stroke(0xFFFFFFFF, 0xFFE4E6EB));
            person.setPadding(dp(8), dp(12), dp(8), dp(10));
            person.addView(avatar(photo, name, dp(72)));
            TextView label = note(name, 13, 0xFF050505, true);
            label.setGravity(Gravity.CENTER);
            label.setMaxLines(2);
            label.setPadding(0, dp(8), 0, dp(8));
            person.addView(label);
            TextView add = note("+ साथी", 13, 0xFF0F8A3C, true);
            add.setGravity(Gravity.CENTER);
            add.setBackground(pill(0xFFE7F6EE));
            add.setPadding(dp(8), dp(6), dp(8), dp(6));
            LinearLayout.LayoutParams addLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            add.setLayoutParams(addLp);
            Element form = button.closest("form");
            add.setOnClickListener(v -> {
                if (form != null) send(form, button, fieldsOf(form));
                else if (button.hasAttr("href")) open(button.attr("abs:href"));
            });
            person.addView(add);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(124), ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.rightMargin = dp(8);
            person.setLayoutParams(lp);
            row.addView(person);
        }
        if (row.getChildCount() == 0) return null;
        scroll.addView(row);
        box.addView(scroll);
        return box;
    }

    private boolean friendButton(Element button) {
        String own = button.ownText().replaceAll("\\s+", " ").trim();
        if (own.isEmpty() && button.children().isEmpty()) own = button.text().replaceAll("\\s+", " ").trim();
        if (own.length() > 18 || own.contains("दर्ता") || own.contains("हेर्नु")) return false;
        return own.equals("+ साथी") || own.equals("साथी") || own.endsWith("साथी");
    }

    private Element personCard(Element button) {
        Element card = button.parent() == null ? button : button.parent();
        while (card.parent() != null && friendCount(card.parent()) <= 1) card = card.parent();
        return card;
    }

    private int friendCount(Element scope) {
        int count = 0;
        for (Element button : scope.select("button, a, input[type=submit]")) if (friendButton(button)) count++;
        return count;
    }

    private String personName(Element card, Element button) {
        Element image = card.selectFirst("img");
        String alt = image == null ? "" : image.attr("alt").replaceAll("\\s+", " ").trim();
        if (alt.length() >= 2 && alt.length() <= 40 && !alt.toLowerCase().contains("logo")) return alt;
        for (Element el : card.getAllElements()) {
            if (el == button || friendButton(el) || !el.children().isEmpty()) continue;
            String text = el.ownText().replaceAll("\\s+", " ").trim();
            if (text.length() >= 2 && text.length() <= 32 && !text.contains("साथी") && !text.contains("+")) return text;
        }
        return "";
    }

    private View loginCard(boolean offline) {
        LinearLayout box = new LinearLayout(activity);
        box.setOrientation(LinearLayout.VERTICAL);
        box.addView(note(offline ? "समुदाय आएन। नेट जाँच गर्नुहोस्।" : "समुदाय हेर्न आफ्नो खातामा लगइन गर्नुहोस्।", 16, 0xFF142033, true));
        box.addView(wideButton(offline ? "फेरि प्रयास" : "लगइन / खाता", 0xFF0F8A3C, () -> {
            if (offline) load();
            else activity.startActivity(new Intent(activity, AccountActivity.class));
        }));
        return box;
    }

    private void send(Element form, Element button, LinkedHashMap<String, String> fields) {
        if (button != null && button.hasAttr("name") && !button.attr("name").isEmpty()) fields.put(button.attr("name"), button.attr("value"));
        String action = form.hasAttr("action") && !form.attr("action").isEmpty() ? form.attr("abs:action") : pageUrl;
        String method = form.attr("method").toUpperCase();
        if (!"GET".equals(method)) method = "POST";
        int token = ++generation;
        root.removeAllViews();
        root.addView(note("पठाउँदैछ…", 15, 0xFF78716C, false));
        String verb = method;
        new Thread(() -> {
            String body = "GET".equals(verb) ? null : encode(fields);
            String target = "GET".equals(verb) ? action + (action.contains("?") ? "&" : "?") + encode(fields) : action;
            String[] base = {target};
            String html = fetch(verb, target, body, base);
            if (!activity.isDestroyed()) activity.runOnUiThread(() -> {
                if (token != generation || activity.isDestroyed()) return;
                try {
                    render(html, base[0] == null || base[0].isEmpty() ? pageUrl : base[0]);
                } catch (RuntimeException ignored) {
                    root.removeAllViews();
                    root.addView(note("पठाउन सकिएन। फेरि प्रयास गर्नुहोस्।", 15, 0xFF78716C, false));
                }
            });
        }).start();
    }

    private LinkedHashMap<String, String> fieldsOf(Element form) {
        LinkedHashMap<String, String> fields = new LinkedHashMap<>();
        if (form == null) return fields;
        for (Element input : form.select("input[name]")) {
            String type = input.attr("type").toLowerCase();
            if ("submit".equals(type) || "button".equals(type) || "file".equals(type)) continue;
            if (("checkbox".equals(type) || "radio".equals(type)) && !input.hasAttr("checked")) continue;
            fields.put(input.attr("name"), input.attr("value"));
        }
        for (Element select : form.select("select[name]")) {
            Element chosen = select.selectFirst("option[selected]");
            if (chosen == null) chosen = select.selectFirst("option");
            if (chosen != null) fields.put(select.attr("name"), chosen.hasAttr("value") ? chosen.attr("value") : chosen.text());
        }
        return fields;
    }

    private String fetch(String method, String url, String body, String[] base) {
        String current = url;
        String verb = method;
        String send = body;
        for (int hop = 0; hop < 5; hop++) {
            HttpURLConnection connection = null;
            try {
                connection = (HttpURLConnection) new URL(current).openConnection();
                connection.setInstanceFollowRedirects(false);
                connection.setConnectTimeout(30000);
                connection.setReadTimeout(hop == 0 ? 45000 : 70000);
                connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 Chrome/131.0.0.0 Mobile Safari/537.36");
                connection.setRequestProperty("Referer", HOME);
                Account.apply(connection);
                if ("POST".equals(verb) && send != null) {
                    byte[] bytes = send.getBytes(StandardCharsets.UTF_8);
                    connection.setRequestMethod("POST");
                    connection.setDoOutput(true);
                    connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
                    connection.setFixedLengthStreamingMode(bytes.length);
                    OutputStream output = connection.getOutputStream();
                    output.write(bytes);
                    output.close();
                }
                int code = connection.getResponseCode();
                Account.capture(connection);
                if (code >= 300 && code < 400) {
                    String location = connection.getHeaderField("Location");
                    if (location == null || location.isEmpty()) return null;
                    current = location.startsWith("http") ? location : "https://kalaiyaonline.com" + (location.startsWith("/") ? location : "/" + location);
                    verb = "GET";
                    send = null;
                    continue;
                }
                if (base != null) base[0] = current;
                if (code >= 400) return null;
                String html = read(connection.getInputStream());
                return html == null || html.isEmpty() ? null : html;
            } catch (Exception ignored) {
                if (hop == 0) continue;
                return null;
            } finally {
                if (connection != null) connection.disconnect();
            }
        }
        return null;
    }

    private View avatar(String url, String name, int size) {
        FrameLayout frame = new FrameLayout(activity);
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.OVAL);
        bg.setColor(0xFF7C3AED);
        frame.setBackground(bg);
        TextView letter = note(name.isEmpty() ? "स" : new String(Character.toChars(name.codePointAt(0))).toUpperCase(java.util.Locale.ROOT), 16, 0xFFFFFFFF, true);
        letter.setGravity(Gravity.CENTER);
        frame.addView(letter, new FrameLayout.LayoutParams(size, size, Gravity.CENTER));
        if (url != null && !url.isEmpty()) {
            ImageView image = new ImageView(activity);
            image.setScaleType(ImageView.ScaleType.CENTER_CROP);
            GradientDrawable clip = new GradientDrawable();
            clip.setShape(GradientDrawable.OVAL);
            clip.setColor(0xFFFFFFFF);
            image.setBackground(clip);
            image.setClipToOutline(true);
            frame.addView(image, new FrameLayout.LayoutParams(size, size));
            loadImage(url, image);
        }
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(size, size);
        frame.setLayoutParams(lp);
        return frame;
    }

    private View card(View child) {
        if (child == null || child.getParent() != null) return new View(activity);
        LinearLayout card = new LinearLayout(activity);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(12), dp(10), dp(12), dp(10));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Night.on(activity) ? 0xFF242526 : 0xFFFFFFFF);
        bg.setCornerRadius(dp(12));
        card.setBackground(bg);
        card.addView(child);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(8);
        lp.leftMargin = dp(8);
        lp.rightMargin = dp(8);
        card.setLayoutParams(lp);
        return card;
    }

    private View wideButton(String label, int color, Runnable action) {
        TextView view = note(label, 15, 0xFFFFFFFF, true);
        view.setGravity(Gravity.CENTER);
        view.setIncludeFontPadding(false);
        view.setBackground(pill(color));
        view.setPadding(dp(12), dp(8), dp(12), dp(8));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(10);
        view.setLayoutParams(lp);
        view.setOnClickListener(v -> action.run());
        return view;
    }

    private TextView textButton(String label, Runnable action, boolean fill) {
        TextView view = note(label, 14, fill ? 0xFFFFFFFF : 0xFF0F8A3C, true);
        view.setOnClickListener(v -> action.run());
        return view;
    }

    private EditText input(String hint, boolean multi) {
        EditText field = new EditText(activity);
        field.setHint(hint);
        field.setMinLines(multi ? 3 : 1);
        field.setSingleLine(!multi);
        field.setTextColor(0xFF1C1917);
        field.setHintTextColor(0xFF9AA89F);
        style(field, 15, false);
        field.setPadding(dp(12), dp(10), dp(12), dp(10));
        field.setBackground(stroke(0xFFF8FAFC, 0xFFE5E7EB));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(8);
        field.setLayoutParams(lp);
        return field;
    }

    private TextView note(String value, int size, int color, boolean bold) {
        TextView view = new TextView(activity);
        view.setText(value);
        view.setTextColor(color);
        style(view, size, bold);
        return view;
    }

    private void style(TextView view, int size, boolean bold) {
        view.setTextSize(TypedValue.COMPLEX_UNIT_DIP, size);
        Typeface face = ResourcesCompat.getFont(activity, bold ? R.font.mukta_bold : R.font.mukta_regular);
        if (face != null) view.setTypeface(face);
    }

    private GradientDrawable pill(int color) {
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(18));
        bg.setColor(color);
        return bg;
    }

    private GradientDrawable stroke(int fill, int line) {
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(16));
        bg.setColor(fill);
        bg.setStroke(dp(1), line);
        return bg;
    }

    private void loadImage(String url, ImageView image) {
        if (url == null || url.isEmpty()) return;
        new Thread(() -> {
            android.graphics.Bitmap bitmap = Bitmaps.load(url, 800);
            if (bitmap != null && !activity.isDestroyed()) activity.runOnUiThread(() -> image.setImageBitmap(bitmap));
        }).start();
    }

    private String firstImage(Element scope) {
        Element image = scope == null ? null : scope.selectFirst("img");
        return image == null ? "" : image.attr("abs:src");
    }

    private String shortText(Element element) {
        if (element == null) return "";
        String text = element.text().replaceAll("\\s+", " ").trim();
        return text.length() > 40 ? text.substring(0, 40) : text;
    }

    private String meta(Element post, String author) {
        for (Element small : post.select("small, time, span")) {
            String text = small.text().replaceAll("\\s+", " ").trim();
            if (text.isEmpty() || text.equals(author) || text.length() > 80) continue;
            if (text.contains("सार्वजनिक") || text.contains("स्टाटस") || text.contains("मंसिर") || text.contains("२०")) return text;
        }
        return "";
    }

    private String body(Element post, String author, String meta) {
        Element known = post.selectFirst(".social-post-body, .post-body, .status-body");
        if (known != null) return known.text().replaceAll("\\s+", " ").trim();
        for (Element p : post.select("p")) {
            String text = p.text().replaceAll("\\s+", " ").trim();
            if (text.isEmpty() || text.equals(author) || text.equals(meta) || text.contains("सम्मानजनक")) continue;
            if (!text.contains("हटाउनुहोस्") && !text.contains("टिप्पणी") && text.length() < 500) return text;
        }
        return "";
    }

    private String slug(String href) {
        int at = href.indexOf("/news/");
        if (at < 0) return null;
        String rest = href.substring(at + 6);
        int cut = rest.indexOf('/');
        if (cut >= 0) rest = rest.substring(0, cut);
        cut = rest.indexOf('?');
        if (cut >= 0) rest = rest.substring(0, cut);
        return rest.isEmpty() ? null : rest;
    }

    private String encode(LinkedHashMap<String, String> fields) {
        StringBuilder body = new StringBuilder();
        for (java.util.Map.Entry<String, String> entry : fields.entrySet()) {
            if (body.length() > 0) body.append('&');
            body.append(url(entry.getKey())).append('=').append(url(entry.getValue()));
        }
        return body.toString();
    }

    private String url(String value) {
        try {
            return URLEncoder.encode(value == null ? "" : value, "UTF-8");
        } catch (Exception ignored) {
            return "";
        }
    }

    private String read(InputStream input) throws Exception {
        if (input == null) return "";
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int n;
        while ((n = input.read(buffer)) != -1) bytes.write(buffer, 0, n);
        input.close();
        return bytes.toString("UTF-8");
    }

    private int dp(int value) {
        return Math.round(value * activity.getResources().getDisplayMetrics().density);
    }
}
