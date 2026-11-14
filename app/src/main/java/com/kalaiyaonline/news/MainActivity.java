package com.kalaiyaonline.news;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.net.Uri;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import android.util.TypedValue;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.card.MaterialCardView;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {
    private final SiteClient client = new SiteClient();
    private final ExecutorService io = Executors.newFixedThreadPool(3);
    private LinearLayout page;
    private TextView status;
    private String section = "home";

    private static final String[][] SECTIONS = {
            {"news", "समाचार"},
            {"local", "स्थानीय"},
            {"international", "अन्तर्राष्ट्रिय"},
            {"politics", "राजनीति"},
            {"artha", "अर्थ"},
            {"khelkud", "खेलकुद"},
            {"entertainment", "मनोरंजन"},
            {"lekhabichar", "लेख / बिचार"},
    };

    private final Handler sliderHandler = new Handler(Looper.getMainLooper());
    private ViewPager2 pager;
    private ObjectAnimator tickerAnim;
    private int homeGen;

    private UpdateChecker updates;
    private View morePanel;
    private View dim;
    private WeatherBoard weather;
    private AirBoard air;
    private PatroBoard patro;
    private ConverterBoard converter;
    private CurrencyBoard fx;
    private PreetiBoard preeti;
    private BloodBoard blood;
    private VideoBoard videos;
    private DirectoryBoard directory;
    private BottomNavigationView nav;
    private boolean moreOpen;
    private boolean restoringNav;
    private boolean askedLocation;
    private int currentNav = R.id.nav_home;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FrameLayout screen = new FrameLayout(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFFF6F7F4);
        root.addView(header());

        page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(12), dp(8), dp(12), dp(68));
        android.widget.ScrollView scroll = new android.widget.ScrollView(this);
        scroll.addView(page);
        root.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        screen.addView(root, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        dim = new View(this);
        dim.setBackgroundColor(0x73000000);
        dim.setVisibility(View.GONE);
        dim.setOnClickListener(v -> {
            setMore(false);
            restoreNav();
        });
        FrameLayout.LayoutParams dimLp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        dimLp.bottomMargin = dp(56);
        screen.addView(dim, dimLp);

        morePanel = morePanel();
        morePanel.setVisibility(View.GONE);
        FrameLayout.LayoutParams sheetLp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM);
        sheetLp.bottomMargin = dp(56);
        screen.addView(morePanel, sheetLp);

        BottomNavigationView nav = new BottomNavigationView(this);
        this.nav = nav;
        nav.inflateMenu(R.menu.bottom_nav);
        nav.setBackgroundColor(0xFFFFFFFF);
        nav.setItemIconTintList(getColorStateList(R.color.nav_tint));
        nav.setItemTextColor(getColorStateList(R.color.nav_tint));
        nav.setLabelVisibilityMode(BottomNavigationView.LABEL_VISIBILITY_LABELED);
        nav.setItemIconSize(dp(22));
        nav.setItemPaddingTop(dp(4));
        nav.setItemPaddingBottom(dp(6));
        nav.setItemActiveIndicatorEnabled(false);
        nav.setItemTextAppearanceActive(R.style.NavLabel);
        nav.setItemTextAppearanceInactive(R.style.NavLabel);
        nav.setElevation(dp(8));
        nav.setOnItemSelectedListener(item -> {
            if (restoringNav) return true;
            int id = item.getItemId();
            if (id == R.id.nav_more) {
                setMore(true);
                return true;
            }
            setMore(false);
            currentNav = id;
            if (id == R.id.nav_home) showHome();
            else if (id == R.id.nav_gallery) showList("gallery");
            else if (id == R.id.nav_directory) showDirectory();
            return true;
        });
        nav.setOnItemReselectedListener(item -> {
            if (restoringNav) return;
            if (item.getItemId() == R.id.nav_more) {
                setMore(false);
                restoreNav();
            }
        });
        View hairline = new View(this);
        hairline.setBackgroundColor(0xFFE7E5E4);
        FrameLayout.LayoutParams lineLp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1), Gravity.BOTTOM);
        lineLp.bottomMargin = dp(56);
        screen.addView(hairline, lineLp);
        FrameLayout.LayoutParams navLp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(56), Gravity.BOTTOM);
        screen.addView(nav, navLp);
        setContentView(screen);
        showHome();
        updates = new UpdateChecker(this);
        updates.check();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (updates != null) updates.installPending();
    }

    private View header() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setBackgroundColor(0xFFFFFFFF);
        ImageView logo = new ImageView(this);
        logo.setAdjustViewBounds(true);
        logo.setImageResource(R.drawable.wordmark);
        int pad = dp(10);
        logo.setPadding(pad, pad, pad, dp(6));
        box.addView(logo, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(58)));
        View green = new View(this);
        green.setBackgroundColor(0xFF0F8A3C);
        box.addView(green, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(4)));
        View orange = new View(this);
        orange.setBackgroundColor(0xFFF15A22);
        box.addView(orange, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(2)));
        return box;
    }

    private View morePanel() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable sheet = new GradientDrawable();
        sheet.setColor(0xFFF8FBFA);
        float radius = dp(28);
        sheet.setCornerRadii(new float[]{radius, radius, radius, radius, 0, 0, 0, 0});
        box.setBackground(sheet);
        box.setElevation(dp(18));
        box.setPadding(dp(14), dp(8), dp(14), dp(12));

        View handle = new View(this);
        GradientDrawable grip = new GradientDrawable();
        grip.setColor(0xFFD6D3D1);
        grip.setCornerRadius(dp(4));
        handle.setBackground(grip);
        LinearLayout.LayoutParams handleLp = new LinearLayout.LayoutParams(dp(42), dp(4));
        handleLp.gravity = Gravity.CENTER_HORIZONTAL;
        handleLp.bottomMargin = dp(10);
        box.addView(handle, handleLp);

        LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout badge = new LinearLayout(this);
        badge.setGravity(Gravity.CENTER);
        GradientDrawable badgeBg = new GradientDrawable();
        badgeBg.setColor(0xFFE5F6EC);
        badgeBg.setCornerRadius(dp(14));
        badge.setBackground(badgeBg);
        ImageView grid = new ImageView(this);
        grid.setImageResource(R.drawable.ic_more);
        DrawableCompat.setTint(grid.getDrawable().mutate(), 0xFF0F8A3C);
        badge.addView(grid, new LinearLayout.LayoutParams(dp(22), dp(22)));
        head.addView(badge, new LinearLayout.LayoutParams(dp(42), dp(42)));

        LinearLayout titles = new LinearLayout(this);
        titles.setOrientation(LinearLayout.VERTICAL);
        titles.setPadding(dp(10), 0, 0, 0);
        TextView title = new TextView(this);
        title.setText("मोड्युलहरू");
        title.setTextColor(0xFF142033);
        style(title, 22, R.font.mukta_extrabold);
        titles.addView(title);
        TextView subtitle = new TextView(this);
        subtitle.setText("तपाईंको पहुँचका सबै सुविधाहरू");
        subtitle.setTextColor(0xFF78716C);
        style(subtitle, 13, R.font.mukta_regular);
        titles.addView(subtitle);
        head.addView(titles, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView close = new TextView(this);
        close.setText("×");
        close.setGravity(Gravity.CENTER);
        close.setTextColor(0xFF44403C);
        close.setTextSize(22);
        GradientDrawable closeBg = new GradientDrawable();
        closeBg.setColor(0xFFF4F4F5);
        closeBg.setCornerRadius(dp(20));
        close.setBackground(closeBg);
        close.setOnClickListener(v -> {
            setMore(false);
            restoreNav();
        });
        head.addView(close, new LinearLayout.LayoutParams(dp(40), dp(40)));
        box.addView(head);

        weather = new WeatherBoard(this);
        air = new AirBoard(this);
        patro = new PatroBoard(this);
        converter = new ConverterBoard(this);
        fx = new CurrencyBoard(this);
        preeti = new PreetiBoard(this);
        blood = new BloodBoard(this);
        videos = new VideoBoard(this);
        directory = new DirectoryBoard(this);
        GridLayout modules = new GridLayout(this);
        modules.setColumnCount(4);
        LinearLayout.LayoutParams gridLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        gridLp.topMargin = dp(12);
        box.addView(modules, gridLp);
        modules.addView(moduleTile("🎬", "भिडियो", 0xFFE7F6EC, this::showVideos));
        modules.addView(moduleTile("🔴", "लाइभ", 0xFFF3E9FB, () -> showSoon("लाइभ")));
        modules.addView(moduleTile("⛅", "मौसम", 0xFFFFF4E5, this::showWeather));
        modules.addView(moduleTile("☁", "वायु गुणस्तर", 0xFFE5F6F1, this::showAir));
        modules.addView(moduleTile("📅", "नेपाली पात्रो", 0xFFFDECEC, this::showPatro));
        modules.addView(moduleTile("🗓", "मिति रूपान्तरण", 0xFFF4EEF8, this::showConverter));
        modules.addView(moduleTile("💱", "मुद्रा रूपान्तरण", 0xFFFFF6EA, this::showCurrency));
        modules.addView(moduleTile("अ", "प्रीति ↔ युनिकोड", 0xFFE7F6EC, this::showPreeti));
        modules.addView(moduleTile("🩸", "रक्तदान", 0xFFFDECEC, this::showBlood));
        modules.addView(moduleTile("📄", "गोपनीयता नीति", 0xFFF3EEF8, () -> openWeb("https://kalaiyaonline.com/page/privacy%20policy")));
        modules.addView(moduleTile("🔐", "लगइन / खाता", 0xFFFFF6EA, () -> openWeb("https://kalaiyaonline.com/account.php")));
        box.addView(languageBar());
        return box;
    }

    private View moduleTile(String emoji, String label, int color, Runnable action) {
        LinearLayout tile = new LinearLayout(this);
        tile.setOrientation(LinearLayout.VERTICAL);
        tile.setGravity(Gravity.CENTER);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(color);
        bg.setCornerRadius(dp(18));
        tile.setBackground(bg);
        tile.setPadding(dp(4), dp(12), dp(4), dp(10));
        TextView icon = new TextView(this);
        icon.setText(emoji);
        icon.setTextSize(24);
        icon.setGravity(Gravity.CENTER);
        icon.setTextColor(0xFF0F8A3C);
        tile.addView(icon);
        TextView name = new TextView(this);
        name.setText(label);
        name.setTextColor(0xFF1C1917);
        name.setGravity(Gravity.CENTER);
        name.setMaxLines(2);
        style(name, 12, R.font.mukta_semibold);
        name.setPadding(dp(2), dp(4), dp(2), 0);
        tile.addView(name);
        tile.setOnClickListener(v -> action.run());
        GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
        lp.width = 0;
        lp.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        lp.setMargins(dp(4), dp(4), dp(4), dp(4));
        tile.setMinimumHeight(dp(92));
        tile.setLayoutParams(lp);
        return tile;
    }

    private View languageBar() {
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(dp(4), dp(12), 0, dp(2));
        TextView globe = new TextView(this);
        globe.setText("🌐");
        globe.setTextSize(16);
        bar.addView(globe);
        TextView label = new TextView(this);
        label.setText("भाषा परिवर्तन");
        label.setTextColor(0xFF1C1917);
        style(label, 14, R.font.mukta_bold);
        label.setPadding(dp(8), 0, dp(8), 0);
        bar.addView(label);
        bar.addView(langChip("नेपाली", true));
        bar.addView(langChip("English", false));
        bar.addView(langChip("English साइट ›", false));
        return bar;
    }

    private TextView langChip(String label, boolean selected) {
        TextView chip = new TextView(this);
        chip.setText(label);
        chip.setTextSize(12);
        chip.setTextColor(selected ? 0xFF0B5C28 : 0xFF44403C);
        chip.setPadding(dp(10), dp(6), dp(10), dp(6));
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(16));
        bg.setColor(selected ? 0xFFE5F6EC : 0xFFFFFFFF);
        bg.setStroke(dp(1), selected ? 0xFF8FCBAA : 0xFFE7E5E4);
        chip.setBackground(bg);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.leftMargin = dp(4);
        chip.setLayoutParams(lp);
        return chip;
    }

    private void openTab(int id) {
        nav.setSelectedItemId(id);
    }

    private void openWeb(String url) {
        setMore(false);
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
    }

    private void showSoon(String name) {
        section = "module";
        stopMotion();
        setMore(false);
        restoreNav();
        page.removeAllViews();
        page.addView(heading(name));
        page.addView(message(name + " छिट्टै थपिनेछ।"));
    }

    private void showWeather() {
        section = "weather";
        stopMotion();
        setMore(false);
        restoreNav();
        page.removeAllViews();
        page.addView(heading("मौसम"));
        View board = weather.view();
        if (board.getParent() instanceof ViewGroup) ((ViewGroup) board.getParent()).removeView(board);
        page.addView(board);
        refreshWeather();
    }

    private void showAir() {
        section = "air";
        stopMotion();
        setMore(false);
        restoreNav();
        page.removeAllViews();
        page.addView(heading("वायु गुणस्तर"));
        View board = air.view();
        if (board.getParent() instanceof ViewGroup) ((ViewGroup) board.getParent()).removeView(board);
        page.addView(board);
        air.load();
    }

    private void showPatro() {
        section = "patro";
        stopMotion();
        setMore(false);
        restoreNav();
        page.removeAllViews();
        page.addView(heading("नेपाली पात्रो"));
        View board = patro.view();
        if (board.getParent() instanceof ViewGroup) ((ViewGroup) board.getParent()).removeView(board);
        page.addView(board);
        patro.loadToday();
    }

    private void showConverter() {
        section = "converter";
        stopMotion();
        setMore(false);
        restoreNav();
        page.removeAllViews();
        page.addView(heading("मिति रूपान्तरण"));
        View board = converter.view();
        if (board.getParent() instanceof ViewGroup) ((ViewGroup) board.getParent()).removeView(board);
        page.addView(board);
    }

    private void showCurrency() {
        section = "currency";
        stopMotion();
        setMore(false);
        restoreNav();
        page.removeAllViews();
        page.addView(heading("मुद्रा रूपान्तरण"));
        View board = fx.view();
        if (board.getParent() instanceof ViewGroup) ((ViewGroup) board.getParent()).removeView(board);
        page.addView(board);
        fx.load();
    }

    private void showPreeti() {
        section = "preeti";
        stopMotion();
        setMore(false);
        restoreNav();
        page.removeAllViews();
        page.addView(heading("प्रीति ↔ युनिकोड"));
        View board = preeti.view();
        if (board.getParent() instanceof ViewGroup) ((ViewGroup) board.getParent()).removeView(board);
        page.addView(board);
    }

    private void showBlood() {
        section = "blood";
        stopMotion();
        setMore(false);
        restoreNav();
        page.removeAllViews();
        page.addView(heading("रक्तदान"));
        View board = blood.view();
        if (board.getParent() instanceof ViewGroup) ((ViewGroup) board.getParent()).removeView(board);
        page.addView(board);
        blood.load();
    }

    private void showVideos() {
        section = "videos";
        stopMotion();
        setMore(false);
        restoreNav();
        page.removeAllViews();
        page.addView(heading("भिडियो"));
        View board = videos.view();
        if (board.getParent() instanceof ViewGroup) ((ViewGroup) board.getParent()).removeView(board);
        page.addView(board);
        videos.load();
    }

    private void showDirectory() {
        section = "directory";
        stopMotion();
        setMore(false);
        page.removeAllViews();
        page.addView(heading("डाइरेक्टरी"));
        View board = directory.view();
        if (board.getParent() instanceof ViewGroup) ((ViewGroup) board.getParent()).removeView(board);
        page.addView(board);
        directory.load();
    }

    private void setMore(boolean open) {
        moreOpen = open;
        morePanel.setVisibility(open ? View.VISIBLE : View.GONE);
        dim.setVisibility(open ? View.VISIBLE : View.GONE);
    }

    private void restoreNav() {
        if (nav.getSelectedItemId() == currentNav) return;
        restoringNav = true;
        nav.setSelectedItemId(currentNav);
        restoringNav = false;
    }

    private void refreshWeather() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            Location location = latestLocation();
            if (location != null) {
                weather.load(location.getLatitude(), location.getLongitude());
                return;
            }
        } else if (!askedLocation) {
            askedLocation = true;
            requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 41);
        }
        weather.load(27.033, 84.983);
    }

    private Location latestLocation() {
        LocationManager manager = (LocationManager) getSystemService(LOCATION_SERVICE);
        Location best = null;
        for (String provider : manager.getProviders(true)) {
            Location location = manager.getLastKnownLocation(provider);
            if (location != null && (best == null || location.getTime() > best.getTime())) best = location;
        }
        return best;
    }

    @Override
    public void onRequestPermissionsResult(int code, @NonNull String[] permissions, @NonNull int[] results) {
        super.onRequestPermissionsResult(code, permissions, results);
        if (code == 41 && weather != null) refreshWeather();
    }

    private void showHome() {
        section = "home";
        stopMotion();
        int gen = homeGen;
        setStatus("समाचार लोड हुँदैछ…");
        io.execute(() -> {
            try {
                SiteClient.Home home = client.home();
                runOnUiThread(() -> {
                    if (gen == homeGen) renderHome(home, gen);
                });
            } catch (Exception error) {
                runOnUiThread(() -> {
                    if (gen == homeGen) setStatus("समाचार ल्याउन सकिएन। इन्टरनेट जाँच्नुहोस्।");
                });
            }
        });
    }

    private void renderHome(SiteClient.Home home, int gen) {
        if (!"home".equals(section) || gen != homeGen) return;
        page.removeAllViews();
        if (!home.headlines.isEmpty()) page.addView(slider(home.headlines));
        if (!home.dateLine.isEmpty()) page.addView(meta(home.dateLine));
        if (!home.ticker.isEmpty()) page.addView(ticker(home.ticker));
        page.addView(chips());
        LinearLayout sections = new LinearLayout(this);
        sections.setOrientation(LinearLayout.VERTICAL);
        page.addView(sections);
        io.execute(() -> {
            for (String[] cat : SECTIONS) {
                List<Story> list;
                try {
                    list = client.category(cat[0]);
                } catch (Exception error) {
                    list = new ArrayList<>();
                }
                if (list.isEmpty()) continue;
                List<Story> top = list.subList(0, Math.min(3, list.size()));
                String slug = cat[0];
                String label = cat[1];
                runOnUiThread(() -> {
                    if (gen != homeGen || !"home".equals(section)) return;
                    sections.addView(categoryBlock(label, slug, top));
                });
            }
        });
    }

    private void showList(String kind) {
        section = kind;
        stopMotion();
        setStatus("लोड हुँदैछ…");
        io.execute(() -> {
            try {
                List<Story> list = "gallery".equals(kind) ? client.gallery()
                        : "directory".equals(kind) ? client.directory() : client.reels();
                runOnUiThread(() -> renderList(kind, list));
            } catch (Exception error) {
                runOnUiThread(() -> setStatus("लोड हुन सकेन।"));
            }
        });
    }

    private void renderList(String kind, List<Story> list) {
        if (!kind.equals(section)) return;
        page.removeAllViews();
        String title = "gallery".equals(kind) ? "फोटो ग्यालरी" : "directory".equals(kind) ? "डाइरेक्टरी" : "रिल्स";
        page.addView(heading(title));
        if (list.isEmpty()) {
            page.addView(message("केही भेटिएन।"));
            return;
        }
        for (Story story : list) page.addView(card(story, false));
    }

    private void openCategory(String slug, String label) {
        section = "cat:" + slug;
        stopMotion();
        setStatus(label + " लोड हुँदैछ…");
        io.execute(() -> {
            try {
                List<Story> list = slug.isEmpty() ? client.home().stories : client.category(slug);
                runOnUiThread(() -> {
                    if (!section.equals("cat:" + slug)) return;
                    page.removeAllViews();
                    page.addView(chips());
                    page.addView(heading(label));
                    for (Story story : list) page.addView(card(story, false));
                    if (list.isEmpty()) page.addView(message("समाचार भेटिएन।"));
                });
            } catch (Exception error) {
                runOnUiThread(() -> setStatus("लोड हुन सकेन।"));
            }
        });
    }

    private View chips() {
        HorizontalScrollView scroll = new HorizontalScrollView(this);
        scroll.setHorizontalScrollBarEnabled(false);
        LinearLayout row = new LinearLayout(this);
        row.setPadding(0, dp(8), 0, dp(8));
        for (String[] cat : SECTIONS) {
            TextView chip = new TextView(this);
            chip.setText(cat[1]);
            chip.setTextColor(0xFF0B5C28);
            style(chip, 18, R.font.mukta_bold);
            chip.setGravity(Gravity.CENTER_VERTICAL);
            Drawable icon = DrawableCompat.wrap(getDrawable(iconFor(cat[0]))).mutate();
            DrawableCompat.setTint(icon, 0xFF0B5C28);
            icon.setBounds(0, 0, dp(18), dp(18));
            chip.setCompoundDrawables(icon, null, null, null);
            chip.setCompoundDrawablePadding(dp(6));
            chip.setBackgroundResource(R.drawable.bg_chip);
            chip.setPadding(dp(14), dp(8), dp(14), dp(8));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.rightMargin = dp(8);
            chip.setOnClickListener(v -> openCategory(cat[0], cat[1]));
            row.addView(chip, lp);
        }
        scroll.addView(row);
        return scroll;
    }

    private View slider(List<Story> stories) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        pager = new ViewPager2(this);
        pager.setAdapter(new HeroAdapter(stories));
        pager.setOffscreenPageLimit(stories.size());
        LinearLayout.LayoutParams pagerLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(268));
        pagerLp.bottomMargin = dp(8);
        box.addView(pager, pagerLp);
        LinearLayout dots = new LinearLayout(this);
        dots.setGravity(Gravity.CENTER);
        View[] marks = new View[stories.size()];
        for (int i = 0; i < stories.size(); i++) {
            View dot = new View(this);
            dot.setBackgroundResource(i == 0 ? R.drawable.bg_dot : R.drawable.bg_dot_off);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(i == 0 ? 18 : 8), dp(8));
            lp.leftMargin = dp(4);
            lp.rightMargin = dp(4);
            dots.addView(dot, lp);
            marks[i] = dot;
        }
        box.addView(dots);
        pager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                for (int i = 0; i < marks.length; i++) {
                    marks[i].setBackgroundResource(i == position ? R.drawable.bg_dot : R.drawable.bg_dot_off);
                    ViewGroup.LayoutParams lp = marks[i].getLayoutParams();
                    lp.width = dp(i == position ? 18 : 8);
                    marks[i].setLayoutParams(lp);
                }
                scheduleSlide(stories.size());
            }
        });
        scheduleSlide(stories.size());
        return box;
    }

    private void scheduleSlide(int count) {
        sliderHandler.removeCallbacksAndMessages(null);
        if (count < 2 || pager == null) return;
        sliderHandler.postDelayed(() -> {
            if (!"home".equals(section) || pager == null) return;
            int next = (pager.getCurrentItem() + 1) % count;
            pager.setCurrentItem(next, true);
        }, 3500);
    }

    private View ticker(List<Story> items) {
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setBackgroundResource(R.drawable.bg_ticker);
        bar.setPadding(dp(8), dp(8), dp(8), dp(8));
        TextView badge = new TextView(this);
        badge.setText("ताजा");
        badge.setTextColor(0xFFFFFFFF);
        badge.setTextSize(12);
        badge.setBackgroundResource(R.drawable.bg_badge);
        badge.setPadding(dp(10), dp(4), dp(10), dp(4));
        bar.addView(badge);
        FrameLayout clip = new FrameLayout(this);
        clip.setClipChildren(true);
        TextView moving = new TextView(this);
        StringBuilder text = new StringBuilder();
        for (int pass = 0; pass < 2; pass++) {
            for (Story item : items) {
                text.append(item.title).append("     •     ");
            }
        }
        moving.setText(text.toString());
        moving.setTextColor(0xFFFFFFFF);
        moving.setTextSize(14);
        moving.setSingleLine(true);
        clip.addView(moving, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER_VERTICAL));
        bar.addView(clip, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(8);
        lp.bottomMargin = dp(4);
        bar.setLayoutParams(lp);
        bar.setOnClickListener(v -> openStory(items.get(0)));
        moving.post(() -> {
            int travel = clip.getWidth() + moving.getWidth();
            if (travel <= 0) return;
            moving.setTranslationX(clip.getWidth());
            if (tickerAnim != null) tickerAnim.cancel();
            tickerAnim = ObjectAnimator.ofFloat(moving, "translationX", clip.getWidth(), -moving.getWidth());
            tickerAnim.setDuration(Math.max(12000, travel * 14L));
            tickerAnim.setRepeatCount(ValueAnimator.INFINITE);
            tickerAnim.setInterpolator(new LinearInterpolator());
            tickerAnim.start();
        });
        return bar;
    }

    private View categoryBlock(String label, String slug, List<Story> stories) {
        LinearLayout block = new LinearLayout(this);
        block.setOrientation(LinearLayout.VERTICAL);
        block.setPadding(0, dp(16), 0, dp(4));
        LinearLayout head = new LinearLayout(this);
        head.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = new TextView(this);
        title.setText(label);
        title.setTextColor(0xFF0B5C28);
        style(title, 22, R.font.mukta_extrabold);
        head.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView more = new TextView(this);
        more.setText("सबै");
        more.setTextColor(0xFFF15A22);
        more.setTextSize(14);
        more.setOnClickListener(v -> openCategory(slug, label));
        head.addView(more);
        block.addView(head);
        for (Story story : stories) block.addView(compact(story));
        return block;
    }

    private View compact(Story story) {
        MaterialCardView card = new MaterialCardView(this);
        card.setRadius(dp(14));
        card.setCardElevation(dp(1));
        card.setCardBackgroundColor(0xFFFFFFFF);
        card.setUseCompatPadding(true);
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(8), dp(8), dp(10), dp(8));
        if (!story.image.isEmpty()) {
            ImageView image = new ImageView(this);
            image.setScaleType(ImageView.ScaleType.CENTER_CROP);
            image.setBackgroundColor(0xFFE8F6EE);
            row.addView(image, new LinearLayout.LayoutParams(dp(92), dp(72)));
            loadImage(story.image, image);
        }
        TextView title = new TextView(this);
        title.setText(story.title);
        title.setTextColor(0xFF1C1917);
        style(title, 17, R.font.mukta_semibold);
        title.setMaxLines(3);
        title.setPadding(dp(10), 0, 0, 0);
        row.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        card.addView(row);
        card.setOnClickListener(v -> openStory(story));
        return card;
    }

    private void stopMotion() {
        homeGen++;
        sliderHandler.removeCallbacksAndMessages(null);
        if (tickerAnim != null) tickerAnim.cancel();
        pager = null;
    }

    @Override
    protected void onDestroy() {
        stopMotion();
        super.onDestroy();
    }

    private View card(Story story, boolean hero) {
        MaterialCardView card = new MaterialCardView(this);
        card.setRadius(dp(16));
        card.setCardElevation(dp(2));
        card.setCardBackgroundColor(0xFFFFFFFF);
        card.setUseCompatPadding(true);
        LinearLayout inner = new LinearLayout(this);
        inner.setOrientation(LinearLayout.VERTICAL);
        if (!story.image.isEmpty()) {
            ImageView image = new ImageView(this);
            image.setScaleType(ImageView.ScaleType.CENTER_CROP);
            image.setBackgroundColor(0xFFE8F6EE);
            inner.addView(image, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, hero ? dp(210) : dp(150)));
            loadImage(story.image, image);
        }
        LinearLayout pad = new LinearLayout(this);
        pad.setOrientation(LinearLayout.VERTICAL);
        pad.setPadding(dp(14), dp(12), dp(14), dp(14));
        TextView kicker = new TextView(this);
        kicker.setText(story.category.isEmpty() ? (hero ? "हेडलाइन" : "समाचार") : story.category);
        kicker.setTextColor(0xFFF15A22);
        kicker.setTextSize(12);
        pad.addView(kicker);
        TextView title = new TextView(this);
        title.setText(story.title);
        title.setTextColor(0xFF1C1917);
        title.setTextSize(hero ? 20 : 16);
        title.setPadding(0, dp(4), 0, 0);
        pad.addView(title);
        String meta = (story.author.isEmpty() ? "कलैया अनलाइन" : story.author) + (story.date.isEmpty() ? "" : " · " + story.date);
        TextView line = new TextView(this);
        line.setText(meta);
        line.setTextColor(0xFF78716C);
        line.setTextSize(12);
        line.setPadding(0, dp(6), 0, 0);
        pad.addView(line);
        inner.addView(pad);
        card.addView(inner);
        card.setOnClickListener(v -> openStory(story));
        return card;
    }

    private void openStory(Story story) {
        if (story.slug.startsWith("http") && !story.slug.contains("/news/")) return;
        Intent intent = new Intent(this, ArticleActivity.class);
        intent.putExtra("slug", story.slug);
        intent.putExtra("title", story.title);
        intent.putExtra("image", story.image);
        intent.putExtra("category", story.category);
        intent.putExtra("date", story.date);
        startActivity(intent);
    }

    private void loadImage(String url, ImageView target) {
        io.execute(() -> {
            Bitmap bitmap = fetchBitmap(url);
            if (bitmap == null) return;
            runOnUiThread(() -> target.setImageBitmap(bitmap));
        });
    }

    private Bitmap fetchBitmap(String url) {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setConnectTimeout(12000);
            connection.setReadTimeout(12000);
            connection.setRequestProperty("User-Agent", "Mozilla/5.0");
            InputStream stream = connection.getInputStream();
            return BitmapFactory.decodeStream(stream);
        } catch (Exception ignored) {
            return null;
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    private void setStatus(String text) {
        page.removeAllViews();
        status = message(text);
        page.addView(status);
    }

    private TextView message(String text) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextColor(0xFF78716C);
        view.setTextSize(16);
        view.setGravity(Gravity.CENTER);
        view.setPadding(dp(12), dp(28), dp(12), dp(12));
        return view;
    }

    private TextView heading(String text) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextColor(0xFF1C1917);
        view.setTextSize(22);
        view.setPadding(0, dp(8), 0, dp(12));
        return view;
    }

    private TextView meta(String text) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextColor(0xFF0F8A3C);
        view.setTextSize(15);
        view.setBackgroundColor(0xFFE8F6EE);
        view.setPadding(dp(16), dp(10), dp(16), dp(10));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(-dp(12), -dp(8), -dp(12), dp(8));
        view.setLayoutParams(lp);
        return view;
    }

    private int iconFor(String slug) {
        if ("local".equals(slug)) return R.drawable.ic_pin;
        if ("international".equals(slug)) return R.drawable.ic_cat_globe;
        if ("politics".equals(slug)) return R.drawable.ic_cat_politics;
        if ("artha".equals(slug)) return R.drawable.ic_cat_wallet;
        if ("khelkud".equals(slug)) return R.drawable.ic_cat_trophy;
        if ("entertainment".equals(slug)) return R.drawable.ic_cat_film;
        if ("lekhabichar".equals(slug)) return R.drawable.ic_cat_pen;
        return R.drawable.ic_cat_news;
    }

    private void style(TextView view, int sizePx, int font) {
        view.setTextSize(TypedValue.COMPLEX_UNIT_DIP, sizePx);
        view.setTypeface(ResourcesCompat.getFont(this, font));
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private final class HeroAdapter extends RecyclerView.Adapter<HeroAdapter.Holder> {
        private final List<Story> stories;

        HeroAdapter(List<Story> stories) {
            this.stories = stories;
        }

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            FrameLayout frame = new FrameLayout(MainActivity.this);
            frame.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            return new Holder(frame);
        }

        @Override
        public void onBindViewHolder(@NonNull Holder holder, int position) {
            holder.bind(stories.get(position));
        }

        @Override
        public int getItemCount() {
            return stories.size();
        }

        final class Holder extends RecyclerView.ViewHolder {
            Holder(FrameLayout frame) {
                super(frame);
            }

            void bind(Story story) {
                FrameLayout frame = (FrameLayout) itemView;
                frame.removeAllViews();
                ImageView image = new ImageView(MainActivity.this);
                image.setScaleType(ImageView.ScaleType.CENTER_CROP);
                image.setBackgroundColor(0xFF0B5C28);
                frame.addView(image, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                View shade = new View(MainActivity.this);
                shade.setBackgroundResource(R.drawable.bg_shade);
                frame.addView(shade, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(140), Gravity.BOTTOM));
                TextView badge = new TextView(MainActivity.this);
                badge.setText("हेडलाइन");
                badge.setTextColor(0xFFFFFFFF);
                badge.setTextSize(12);
                badge.setBackgroundResource(R.drawable.bg_badge);
                badge.setPadding(dp(10), dp(4), dp(10), dp(4));
                FrameLayout.LayoutParams badgeLp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP | Gravity.START);
                badgeLp.setMargins(dp(14), dp(14), 0, 0);
                frame.addView(badge, badgeLp);
                TextView title = new TextView(MainActivity.this);
                title.setText(story.title);
                title.setTextColor(0xFFFFFFFF);
                style(title, 25, R.font.mukta_bold);
                title.setPadding(dp(14), 0, dp(14), dp(16));
                frame.addView(title, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM));
                frame.setOnClickListener(v -> openStory(story));
                if (!story.image.isEmpty()) loadImage(story.image, image);
            }
        }
    }
}
