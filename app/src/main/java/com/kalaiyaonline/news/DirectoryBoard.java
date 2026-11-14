package com.kalaiyaonline.news;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class DirectoryBoard {
    private final AppCompatActivity activity;
    private final LinearLayout root;
    private final LinearLayout chips;
    private final LinearLayout list;
    private final TextView status;
    private final EditText query;
    private final Spinner province;
    private final Spinner district;
    private final Spinner city;
    private final List<Place> places = new ArrayList<>();
    private final List<String> categories = new ArrayList<>();
    private final List<Listing> listings = new ArrayList<>();
    private boolean filling;
    private boolean loaded;
    private int category;
    private int provinceId;
    private int districtId;
    private int cityId;

    DirectoryBoard(AppCompatActivity activity) {
        this.activity = activity;
        root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);

        TextView hero = text("आफ्नो नजिकको सेवा खोज्नुहोस्", 20, 0xFF142033, true);
        root.addView(hero);
        TextView intro = text("प्रदेश, जिल्ला र शहर छानेर व्यवसाय खोज्नुहोस्।", 14, 0xFF57534E, false);
        intro.setPadding(0, 0, 0, dp(8));
        root.addView(intro);

        query = new EditText(activity);
        query.setHint("के खोज्दै हुनुहुन्छ? जस्तै: अस्पताल, होटल…");
        query.setSingleLine(true);
        query.setBackground(field());
        query.setPadding(dp(12), dp(10), dp(12), dp(10));
        query.setTypeface(ResourcesCompat.getFont(activity, R.font.mukta_regular));
        query.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) { apply(); }
            public void afterTextChanged(Editable s) {}
        });
        root.addView(query);

        province = spinner();
        district = spinner();
        city = spinner();
        root.addView(labeled("प्रदेश", province));
        root.addView(labeled("जिल्ला", district));
        root.addView(labeled("शहर / पालिका", city));
        province.setOnItemSelectedListener(pick(1));
        district.setOnItemSelectedListener(pick(2));
        city.setOnItemSelectedListener(pick(3));

        root.addView(button("आफ्नो व्यवसाय थप्नुहोस्", () -> open("https://kalaiyaonline.com/index.php?directory_submit=1")));

        chips = new LinearLayout(activity);
        chips.setOrientation(LinearLayout.HORIZONTAL);
        chips.setPadding(0, dp(8), 0, dp(4));
        HorizontalScrollView scroll = new HorizontalScrollView(activity);
        scroll.setHorizontalScrollBarEnabled(false);
        scroll.addView(chips);
        root.addView(scroll);

        status = text("", 13, 0xFF57534E, false);
        status.setPadding(0, dp(6), 0, 0);
        root.addView(status);
        list = new LinearLayout(activity);
        list.setOrientation(LinearLayout.VERTICAL);
        root.addView(list);
    }

    View view() { return root; }

    void load() {
        if (loaded) {
            apply();
            return;
        }
        status.setText("डाइरेक्टरी लोड हुँदैछ…");
        new Thread(() -> {
            String html = download("https://kalaiyaonline.com/?directory=all");
            activity.runOnUiThread(() -> ingest(html));
        }).start();
    }

    private void ingest(String html) {
        if (html == null) {
            status.setText("डाइरेक्टरी आएन। फेरि थिच्नुहोस्।");
            status.setOnClickListener(v -> { loaded = false; load(); });
            return;
        }
        status.setOnClickListener(null);
        places.clear();
        categories.clear();
        listings.clear();
        categories.add("सबै");
        Matcher loc = Pattern.compile("id=\"directoryPublicLocations\">(.*?)</script>").matcher(html);
        if (loc.find()) readPlaces(loc.group(1));
        Matcher cats = Pattern.compile("<div class=\"directory-category-scroll\">([\\s\\S]*?)</div>").matcher(html);
        if (cats.find()) {
            Matcher link = Pattern.compile("<a [^>]*>([^<]+)</a>").matcher(cats.group(1));
            while (link.find()) {
                String name = clean(decode(link.group(1)));
                if (!name.isEmpty() && !categories.contains(name)) categories.add(name);
            }
        }
        Matcher cards = Pattern.compile("<article class=\"directory-card-v3\">([\\s\\S]*?)</article>").matcher(html);
        while (cards.find()) {
            String block = cards.group(1);
            Listing item = new Listing();
            item.title = textOf(block, "<h2><a [^>]*>([^<]+)</a>");
            item.page = textOf(block, "href=\"(https://kalaiyaonline.com/directory/[^\"]+)\"");
            item.image = textOf(block, "<img src=\"([^\"]+)\"");
            item.category = textOf(block, "directory-card-kicker\">([^<]+)");
            item.location = textOf(block, "directory-card-location\">([^<]+)").replace("⌖", "").trim();
            item.blurb = textOf(block, "<p>([^<]+)</p>");
            item.stars = textOf(block, "directory-stars-v3\">([^<]+)");
            item.rating = textOf(block, "directory-card-bottom\">[\\s\\S]*?<b>([^<]+)</b>");
            item.featured = block.contains("Featured");
            if (!item.title.isEmpty()) listings.add(item);
        }
        loaded = true;
        paintChips();
        bindLocations();
        apply();
    }

    private void readPlaces(String json) {
        try {
            JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                JSONObject row = array.getJSONObject(i);
                Place place = new Place();
                place.id = row.getInt("id");
                place.parent = row.isNull("parent_id") ? 0 : row.getInt("parent_id");
                place.name = row.getString("name");
                places.add(place);
            }
        } catch (Exception ignored) {
        }
    }

    private void apply() {
        if (!loaded) return;
        String needle = query.getText().toString().trim();
        String provinceName = selectedName(province);
        String districtName = selectedName(district);
        String cityName = selectedName(city);
        String cat = category == 0 ? "" : categories.get(category);
        list.removeAllViews();
        int shown = 0;
        for (Listing item : listings) {
            if (!cat.isEmpty() && !item.category.equals(cat)) continue;
            if (!provinceName.isEmpty() && !item.location.contains(provinceName)) continue;
            if (!districtName.isEmpty() && !item.location.contains(districtName)) continue;
            if (!cityName.isEmpty() && !item.location.contains(cityName)) continue;
            String blob = item.title + " " + item.blurb + " " + item.category + " " + item.location;
            if (!needle.isEmpty() && !blob.contains(needle)) continue;
            list.addView(card(item));
            shown++;
        }
        status.setText(shown + " listing");
        if (shown == 0) list.addView(text("यो फिल्टरमा सूची भेटिएन।", 14, 0xFF57534E, false));
        TextView note = text("फोन नम्बर र ठेगाना परिवर्तन हुन सक्छ। जानुअघि पुष्टि गर्नुहोस्।", 12, 0xFF78716C, false);
        note.setPadding(0, dp(12), 0, dp(8));
        list.addView(note);
    }

    private View card(Listing item) {
        LinearLayout card = new LinearLayout(activity);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(cardBg());
        card.setPadding(dp(8), dp(8), dp(8), dp(10));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(10);
        card.setLayoutParams(lp);
        ImageView image = new ImageView(activity);
        image.setScaleType(ImageView.ScaleType.CENTER_CROP);
        card.addView(image, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(150)));
        if (!item.image.isEmpty()) loadImage(item.image, image);
        if (item.featured) {
            TextView badge = text("★ Featured", 12, 0xFFB45309, true);
            badge.setPadding(0, dp(6), 0, 0);
            card.addView(badge);
        }
        if (!item.category.isEmpty()) {
            TextView cat = text(item.category, 12, 0xFF0B5C28, true);
            cat.setPadding(0, dp(4), 0, 0);
            card.addView(cat);
        }
        card.addView(text(item.title, 17, 0xFF142033, true));
        if (!item.location.isEmpty()) card.addView(text(item.location, 13, 0xFF57534E, false));
        if (!item.blurb.isEmpty()) card.addView(text(item.blurb, 13, 0xFF44403C, false));
        String score = item.stars;
        if (!item.rating.isEmpty()) score = (score + "  " + item.rating).trim();
        if (!score.isEmpty()) card.addView(text(score, 13, 0xFFB45309, true));
        card.setOnClickListener(v -> openDetail(item));
        return card;
    }

    private void openDetail(Listing item) {
        list.removeAllViews();
        status.setText("");
        list.addView(button("← सूची", this::apply));
        TextView title = text(item.title, 22, 0xFF142033, true);
        title.setPadding(0, dp(8), 0, 0);
        list.addView(title);
        if (!item.category.isEmpty()) list.addView(text(item.category, 13, 0xFF0B5C28, true));
        if (!item.location.isEmpty()) list.addView(text(item.location, 14, 0xFF57534E, false));
        ImageView image = new ImageView(activity);
        image.setScaleType(ImageView.ScaleType.CENTER_CROP);
        LinearLayout.LayoutParams ip = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(180));
        ip.topMargin = dp(8);
        list.addView(image, ip);
        if (!item.image.isEmpty()) loadImage(item.image, image);
        TextView wait = text("विवरण लोड हुँदैछ…", 14, 0xFF57534E, false);
        list.addView(wait);
        new Thread(() -> {
            String html = download(item.page);
            activity.runOnUiThread(() -> fillDetail(item, html, wait));
        }).start();
    }

    private void fillDetail(Listing item, String html, TextView wait) {
        if (html == null) {
            wait.setText("विवरण आएन।");
            return;
        }
        String about = textOf(html, "kp-short-description\">([^<]+)");
        String address = textOf(html, "ठेगाना</strong><p>([^<]+)</p>");
        String phone = textOf(html, "सम्पर्क नम्बर</strong><p>([^<]+)</p>");
        String map = textOf(html, "href=\"(https://maps\\.google\\.com/[^\"]+)\"");
        String whatsapp = textOf(html, "href=\"(https://wa\\.me/[^\"]+)\"");
        wait.setText(about.isEmpty() ? item.blurb : about);
        if (!address.isEmpty()) list.addView(text("ठेगाना: " + address, 14, 0xFF142033, false));
        LinearLayout actions = new LinearLayout(activity);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setPadding(0, dp(8), 0, 0);
        if (!phone.isEmpty()) actions.addView(action("फोन", () -> open("tel:" + phone.replace(" ", ""))));
        if (!whatsapp.isEmpty()) actions.addView(action("WhatsApp", () -> open(whatsapp)));
        if (!map.isEmpty()) actions.addView(action("नक्सा", () -> open(map)));
        if (actions.getChildCount() > 0) list.addView(actions);
    }

    private void paintChips() {
        chips.removeAllViews();
        for (int i = 0; i < categories.size(); i++) {
            boolean on = i == category;
            TextView chip = text(categories.get(i), 13, on ? 0xFFFFFFFF : 0xFF0B5C28, true);
            chip.setPadding(dp(12), dp(8), dp(12), dp(8));
            GradientDrawable bg = new GradientDrawable();
            bg.setCornerRadius(dp(16));
            bg.setColor(on ? 0xFF0F8A3C : 0xFFE5F6EC);
            chip.setBackground(bg);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.rightMargin = dp(8);
            chip.setLayoutParams(lp);
            int index = i;
            chip.setOnClickListener(v -> {
                category = index;
                paintChips();
                apply();
            });
            chips.addView(chip);
        }
    }

    private void bindLocations() {
        filling = true;
        fill(province, children(0), provinceId, true);
        int selectedProvince = idOf(province);
        fill(district, children(selectedProvince), districtId, selectedProvince != 0);
        int selectedDistrict = idOf(district);
        fill(city, children(selectedDistrict), cityId, selectedDistrict != 0);
        filling = false;
    }

    private void refillChildren() {
        filling = true;
        fill(district, children(provinceId), districtId, provinceId != 0);
        districtId = idOf(district);
        fill(city, children(districtId), cityId, districtId != 0);
        filling = false;
    }

    private void fill(Spinner spinner, List<Place> rows, int selectedId, boolean enabled) {
        List<String> labels = new ArrayList<>();
        labels.add("सबै");
        int selection = 0;
        for (int i = 0; i < rows.size(); i++) {
            labels.add(rows.get(i).name);
            if (rows.get(i).id == selectedId) selection = i + 1;
        }
        spinner.setTag(rows);
        spinner.setAdapter(new ArrayAdapter<>(activity, android.R.layout.simple_spinner_dropdown_item, labels));
        spinner.setSelection(selection);
        spinner.setEnabled(enabled && !rows.isEmpty());
    }

    private List<Place> children(int parent) {
        List<Place> rows = new ArrayList<>();
        for (Place place : places) if (place.parent == parent) rows.add(place);
        return rows;
    }

    private int idOf(Spinner spinner) {
        int index = spinner.getSelectedItemPosition();
        if (index <= 0 || !(spinner.getTag() instanceof List)) return 0;
        List<?> rows = (List<?>) spinner.getTag();
        if (index - 1 >= rows.size()) return 0;
        return ((Place) rows.get(index - 1)).id;
    }

    private String selectedName(Spinner spinner) {
        int index = spinner.getSelectedItemPosition();
        if (index <= 0 || !(spinner.getTag() instanceof List)) return "";
        List<?> rows = (List<?>) spinner.getTag();
        if (index - 1 >= rows.size()) return "";
        return ((Place) rows.get(index - 1)).name;
    }

    private android.widget.AdapterView.OnItemSelectedListener pick(int level) {
        return new android.widget.AdapterView.OnItemSelectedListener() {
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                if (filling) return;
                if (level == 1) {
                    provinceId = idOf(province);
                    districtId = 0;
                    cityId = 0;
                    refillChildren();
                } else if (level == 2) {
                    districtId = idOf(district);
                    cityId = 0;
                    refillChildren();
                }
                apply();
            }
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        };
    }

    private View labeled(String label, Spinner spinner) {
        LinearLayout box = new LinearLayout(activity);
        box.setOrientation(LinearLayout.VERTICAL);
        TextView name = text(label, 12, 0xFF57534E, true);
        name.setPadding(0, dp(8), 0, dp(2));
        box.addView(name);
        box.addView(spinner);
        return box;
    }

    private Spinner spinner() {
        Spinner spinner = new Spinner(activity);
        spinner.setBackground(field());
        return spinner;
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
        lp.topMargin = dp(10);
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
        activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
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

    private void loadImage(String url, ImageView target) {
        new Thread(() -> {
            Bitmap bitmap = null;
            HttpURLConnection connection = null;
            try {
                connection = (HttpURLConnection) new URL(url).openConnection();
                connection.setRequestProperty("User-Agent", "Mozilla/5.0");
                connection.setConnectTimeout(12000);
                connection.setReadTimeout(12000);
                bitmap = BitmapFactory.decodeStream(connection.getInputStream());
            } catch (Exception ignored) {
            } finally {
                if (connection != null) connection.disconnect();
            }
            Bitmap image = bitmap;
            if (image != null) activity.runOnUiThread(() -> target.setImageBitmap(image));
        }).start();
    }

    private String textOf(String html, String source) {
        Matcher matcher = Pattern.compile(source).matcher(html);
        return matcher.find() ? decode(matcher.group(1).trim()) : "";
    }

    private String decode(String value) {
        return value.replace("\u0026amp;", "\u0026").replace("\u0026quot;", "\"").replace("\u0026#039;", "'");
    }

    private String clean(String value) {
        return value.replaceAll("^[^\\u0900-\\u097FA-Za-z0-9]+", "").trim();
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
        bg.setCornerRadius(dp(16));
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

    private static final class Place {
        int id;
        int parent;
        String name;
    }

    private static final class Listing {
        String title = "";
        String category = "";
        String location = "";
        String blurb = "";
        String image = "";
        String page = "";
        String stars = "";
        String rating = "";
        boolean featured;
    }
}
