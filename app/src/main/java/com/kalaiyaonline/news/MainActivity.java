package com.kalaiyaonline.news;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.net.Uri;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Outline;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.ViewOutlineProvider;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import android.util.TypedValue;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.gms.ads.AdView;
import com.google.android.material.card.MaterialCardView;

import org.json.JSONObject;

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
    private final ExecutorService images = Executors.newFixedThreadPool(2);
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
    private final Handler infoHandler = new Handler(Looper.getMainLooper());
    private ViewPager2 pager;
    private ObjectAnimator tickerAnim;
    private FrameLayout tickerHost;
    private TextView weatherFlip;
    private TextView weatherTemp;
    private String[] weatherLines = new String[0];
    private int weatherIndex;
    private int homeGen;
    private int homeTab;
    private String catSlug = "";
    private String catLabel = "";
    private int catPage = 1;
    private int catGen;
    private int catSiteFetched;
    private int catSitePages = 1;
    private final List<Story> catStories = new ArrayList<>();
    private View drawer;
    private View searchPanel;
    private LinearLayout searchResults;
    private EditText searchBox;
    private boolean drawerOpen;
    private boolean searchOpen;
    private boolean bellOpen;
    private View bellPanel;
    private LinearLayout bellList;
    private SiteClient.Home latestHome;

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
    private DockBar nav;
    private boolean moreOpen;
    private boolean restoringNav;
    private int currentNav = R.id.nav_home;
    private AdView shellAd;
    private AdView feedAd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FrameLayout screen = new FrameLayout(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Night.on(this) ? 0xFF0E1210 : 0xFFF6F7F4);
        root.addView(header());
        tickerHost = new FrameLayout(this);
        tickerHost.setVisibility(View.GONE);
        root.addView(tickerHost, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(12), dp(8), dp(12), dp(16));
        android.widget.ScrollView scroll = new android.widget.ScrollView(this);
        scroll.addView(page);
        Night.allow(scroll, Night.on(this));
        root.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        root.setPadding(0, 0, 0, DockBar.inset(this));
        shellAd = Ads.attach(this, root);
        screen.addView(root, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        dim = new View(this);
        dim.setBackgroundColor(0x73000000);
        dim.setVisibility(View.GONE);
        dim.setOnClickListener(v -> closeOverlays());
        FrameLayout.LayoutParams dimLp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        dimLp.bottomMargin = DockBar.inset(this);
        screen.addView(dim, dimLp);

        searchPanel = searchPanel();
        searchPanel.setVisibility(View.GONE);
        FrameLayout.LayoutParams searchLp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        searchLp.topMargin = dp(68);
        searchLp.leftMargin = dp(12);
        searchLp.rightMargin = dp(12);
        screen.addView(searchPanel, searchLp);

        bellPanel = bellPanel();
        bellPanel.setVisibility(View.GONE);
        FrameLayout.LayoutParams bellLp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        bellLp.topMargin = dp(68);
        bellLp.leftMargin = dp(12);
        bellLp.rightMargin = dp(12);
        screen.addView(bellPanel, bellLp);

        drawer = drawer();
        drawer.setVisibility(View.GONE);
        FrameLayout.LayoutParams drawerLp = new FrameLayout.LayoutParams(dp(300), ViewGroup.LayoutParams.MATCH_PARENT, Gravity.START);
        drawerLp.bottomMargin = DockBar.inset(this);
        screen.addView(drawer, drawerLp);

        morePanel = morePanel();
        morePanel.setVisibility(View.GONE);
        FrameLayout.LayoutParams sheetLp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM);
        sheetLp.bottomMargin = DockBar.inset(this);
        screen.addView(morePanel, sheetLp);

        DockBar nav = new DockBar(this);
        this.nav = nav;
        nav.setListener(new DockBar.Listener() {
            @Override
            public void onSelect(int id) {
                if (restoringNav) return;
                if (id == R.id.nav_more) {
                    setMore(true);
                    return;
                }
                setMore(false);
                currentNav = id;
                if (id == R.id.nav_home) showHome();
                else if (id == R.id.nav_gallery) showList("gallery");
                else if (id == R.id.nav_directory) showDirectory();
                else if (id == R.id.nav_saved) showSaved();
            }

            @Override
            public void onReselect(int id) {
                if (restoringNav) return;
                if (id == R.id.nav_more) {
                    setMore(false);
                    restoreNav();
                }
            }
        });
        screen.addView(nav, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, DockBar.inset(this), Gravity.BOTTOM));
        setContentView(screen);
        Night.sync(this);
        showHome();
        consumeOpen(getIntent());
        updates = new UpdateChecker(this);
        updates.check();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        consumeOpen(intent);
    }

    private void consumeOpen(Intent intent) {
        if (intent == null) return;
        String open = intent.getStringExtra("open");
        if (open == null || open.isEmpty() || nav == null) return;
        intent.removeExtra("open");
        if ("search".equals(open)) openSearch();
        else if ("bell".equals(open)) openBell();
        else if ("menu".equals(open)) openDrawer();
        else if ("more".equals(open)) setMore(true);
        else if ("gallery".equals(open)) nav.setSelectedItemId(R.id.nav_gallery);
        else if ("directory".equals(open)) nav.setSelectedItemId(R.id.nav_directory);
        else if ("saved".equals(open)) nav.setSelectedItemId(R.id.nav_saved);
        else nav.setSelectedItemId(R.id.nav_home);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (shellAd != null) shellAd.resume();
        if (feedAd != null) feedAd.resume();
        if (updates != null) updates.installPending();
    }

    @Override
    protected void onPause() {
        if (shellAd != null) shellAd.pause();
        if (feedAd != null) feedAd.pause();
        super.onPause();
    }

    private View header() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setBackgroundColor(Night.on(this) ? 0xFF101412 : 0xFFFFFFFF);
        Night.allow(box, false);
        box.setElevation(dp(2));
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(16), dp(10), dp(12), dp(10));
        ImageView logo = new ImageView(this);
        logo.setAdjustViewBounds(true);
        logo.setScaleType(ImageView.ScaleType.FIT_START);
        logo.setImageResource(R.drawable.wordmark);
        Night.allow(logo, false);
        row.addView(logo, new LinearLayout.LayoutParams(0, dp(38), 1f));
        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setGravity(Gravity.CENTER_VERTICAL);
        actions.setPadding(dp(3), dp(3), dp(3), dp(3));
        GradientDrawable pill = new GradientDrawable();
        pill.setColor(Night.on(this) ? 0xFF24302A : 0xFFF3F6F4);
        pill.setCornerRadius(dp(22));
        actions.setBackground(pill);
        actions.addView(headerButton(R.drawable.ic_bell, this::openBell));
        actions.addView(headerButton(R.drawable.ic_search, this::openSearch));
        actions.addView(headerButton(R.drawable.ic_menu, this::openDrawer));
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
        view.setPadding(dp(8), dp(8), dp(8), dp(8));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(36), dp(36));
        lp.leftMargin = dp(1);
        lp.rightMargin = dp(1);
        view.setLayoutParams(lp);
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(18));
        bg.setColor(Night.on(this) ? 0xFF24302A : 0xFFF3F6F4);
        view.setBackground(bg);
        view.setOnClickListener(v -> action.run());
        return view;
    }

    private void openSearch() {
        setMore(false);
        closeDrawer(false);
        closeBell(false);
        searchOpen = true;
        searchPanel.setVisibility(View.VISIBLE);
        dim.setVisibility(View.VISIBLE);
        searchBox.requestFocus();
    }

    private void openDrawer() {
        setMore(false);
        closeSearch(false);
        closeBell(false);
        drawerOpen = true;
        drawer.setVisibility(View.VISIBLE);
        drawer.setTranslationX(-dp(300));
        drawer.animate().translationX(0).setDuration(220).start();
        dim.setVisibility(View.VISIBLE);
    }

    private void closeOverlays() {
        setMore(false);
        restoreNav();
        closeDrawer(true);
        closeSearch(true);
        closeBell(true);
    }

    private void closeDrawer(boolean hideDim) {
        if (!drawerOpen) return;
        drawerOpen = false;
        drawer.animate().translationX(-dp(300)).setDuration(180).withEndAction(() -> drawer.setVisibility(View.GONE)).start();
        if (hideDim && !searchOpen && !moreOpen && !bellOpen) dim.setVisibility(View.GONE);
    }

    private void closeSearch(boolean hideDim) {
        if (!searchOpen) return;
        searchOpen = false;
        searchPanel.setVisibility(View.GONE);
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(searchBox.getWindowToken(), 0);
        if (hideDim && !drawerOpen && !moreOpen && !bellOpen) dim.setVisibility(View.GONE);
    }

    private void openBell() {
        setMore(false);
        closeDrawer(false);
        closeSearch(false);
        bellOpen = true;
        bellPanel.setVisibility(View.VISIBLE);
        dim.setVisibility(View.VISIBLE);
        fillBell();
    }

    private void closeBell(boolean hideDim) {
        if (!bellOpen) return;
        bellOpen = false;
        bellPanel.setVisibility(View.GONE);
        if (hideDim && !drawerOpen && !searchOpen && !moreOpen) dim.setVisibility(View.GONE);
    }

    private View bellPanel() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(16), dp(14), dp(16), dp(8));
        GradientDrawable sheet = new GradientDrawable();
        sheet.setColor(0xFFFFFFFF);
        sheet.setCornerRadius(dp(22));
        box.setBackground(sheet);
        box.setElevation(dp(12));
        box.setOnClickListener(v -> { });
        TextView title = new TextView(this);
        title.setText("ताजा समाचार");
        title.setTextColor(0xFF0B5C28);
        style(title, 20, R.font.mukta_extrabold);
        box.addView(title);
        bellList = new LinearLayout(this);
        bellList.setOrientation(LinearLayout.VERTICAL);
        android.widget.ScrollView scroll = new android.widget.ScrollView(this);
        scroll.addView(bellList);
        LinearLayout.LayoutParams listLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(420));
        listLp.topMargin = dp(8);
        box.addView(scroll, listLp);
        return box;
    }

    private void fillBell() {
        bellList.removeAllViews();
        if (latestHome == null) {
            bellList.addView(message("लोड हुँदैछ…"));
            io.execute(() -> {
                try {
                    SiteClient.Home home = client.home();
                    runOnUiThread(() -> {
                        latestHome = home;
                        if (bellOpen) showBell(latestSeven(home));
                    });
                } catch (Exception error) {
                    runOnUiThread(() -> {
                        if (!bellOpen) return;
                        bellList.removeAllViews();
                        bellList.addView(message("समाचार ल्याउन सकिएन।"));
                    });
                }
            });
            return;
        }
        showBell(latestSeven(latestHome));
    }

    private List<Story> latestSeven(SiteClient.Home home) {
        List<Story> list = new ArrayList<>();
        for (Story story : home.stories) {
            if (list.size() >= 7) break;
            list.add(story);
        }
        if (list.size() < 7) {
            for (Story story : home.ticker) {
                if (list.size() >= 7) break;
                boolean seen = false;
                for (Story item : list) if (item.slug.equals(story.slug)) seen = true;
                if (!seen) list.add(story);
            }
        }
        return list;
    }

    private void showBell(List<Story> stories) {
        bellList.removeAllViews();
        if (stories.isEmpty()) {
            bellList.addView(message("समाचार भेटिएन।"));
            return;
        }
        String digits = "०१२३४५६७८९";
        for (int i = 0; i < stories.size(); i++) {
            Story story = stories.get(i);
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(0, dp(8), 0, dp(8));
            TextView number = new TextView(this);
            number.setText(String.valueOf(digits.charAt(i + 1)));
            number.setTextColor(0xFFFFFFFF);
            number.setGravity(Gravity.CENTER);
            style(number, 16, R.font.mukta_bold);
            GradientDrawable badge = new GradientDrawable();
            badge.setCornerRadius(dp(16));
            badge.setColor(0xFFF15A22);
            number.setBackground(badge);
            row.addView(number, new LinearLayout.LayoutParams(dp(32), dp(32)));
            TextView title = new TextView(this);
            title.setText(story.title);
            title.setTextColor(0xFF1C1917);
            title.setMaxLines(2);
            style(title, 16, R.font.mukta_semibold);
            title.setPadding(dp(10), 0, 0, 0);
            row.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            row.setOnClickListener(v -> {
                closeBell(true);
                openStory(story);
            });
            bellList.addView(row);
            if (i < stories.size() - 1) {
                View line = new View(this);
                line.setBackgroundColor(0xFFF1F1F1);
                bellList.addView(line, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1)));
            }
        }
    }

    private View searchPanel() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(14), dp(14), dp(14), dp(8));
        GradientDrawable sheet = new GradientDrawable();
        sheet.setColor(0xFFFFFFFF);
        sheet.setCornerRadius(dp(22));
        box.setBackground(sheet);
        box.setElevation(dp(10));
        box.setOnClickListener(v -> { });
        TextView title = new TextView(this);
        title.setText("समाचार खोज्नुहोस्");
        title.setTextColor(0xFF0B5C28);
        style(title, 20, R.font.mukta_extrabold);
        box.addView(title);
        searchBox = new EditText(this);
        searchBox.setHint("शीर्षक वा शब्द लेख्नुहोस्");
        searchBox.setSingleLine(true);
        searchBox.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        searchBox.setTextColor(0xFF1C1917);
        searchBox.setHintTextColor(0xFFA8A29E);
        style(searchBox, 16, R.font.mukta_regular);
        GradientDrawable field = new GradientDrawable();
        field.setCornerRadius(dp(14));
        field.setColor(0xFFFFFFFF);
        field.setStroke(dp(1), 0xFFD6E8DC);
        searchBox.setBackground(field);
        searchBox.setPadding(dp(14), dp(12), dp(14), dp(12));
        LinearLayout.LayoutParams fieldLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        fieldLp.topMargin = dp(10);
        box.addView(searchBox, fieldLp);
        searchBox.setOnEditorActionListener((v, actionId, event) -> {
            runSearch();
            return true;
        });
        TextView go = new TextView(this);
        go.setText("खोज्नुहोस्");
        go.setGravity(Gravity.CENTER);
        go.setTextColor(0xFFFFFFFF);
        style(go, 16, R.font.mukta_bold);
        GradientDrawable goBg = new GradientDrawable();
        goBg.setCornerRadius(dp(14));
        goBg.setColor(0xFF0F8A3C);
        go.setBackground(goBg);
        go.setPadding(dp(12), dp(10), dp(12), dp(10));
        LinearLayout.LayoutParams goLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        goLp.topMargin = dp(10);
        go.setOnClickListener(v -> runSearch());
        box.addView(go, goLp);
        searchResults = new LinearLayout(this);
        searchResults.setOrientation(LinearLayout.VERTICAL);
        android.widget.ScrollView scroll = new android.widget.ScrollView(this);
        scroll.addView(searchResults);
        box.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(340)));
        return box;
    }

    private void runSearch() {
        String query = searchBox.getText().toString().trim();
        searchResults.removeAllViews();
        if (query.length() < 2) {
            searchResults.addView(message("कम्तीमा २ अक्षर लेख्नुहोस्।"));
            return;
        }
        searchResults.addView(message("खोज्दैछ…"));
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(searchBox.getWindowToken(), 0);
        io.execute(() -> {
            try {
                List<Story> list = client.search(query);
                runOnUiThread(() -> {
                    if (!searchOpen) return;
                    searchResults.removeAllViews();
                    if (list.isEmpty()) searchResults.addView(message("केही भेटिएन।"));
                    for (Story story : list) searchResults.addView(compact(story));
                });
            } catch (Exception error) {
                runOnUiThread(() -> {
                    searchResults.removeAllViews();
                    searchResults.addView(message("खोज हुन सकेन।"));
                });
            }
        });
    }

    private View drawer() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setBackgroundColor(0xFFFFFFFF);
        box.setElevation(dp(16));
        Night.allow(box, false);
        boolean dark = Night.on(this);
        if (dark) box.setBackgroundColor(0xFF121816);
        LinearLayout brand = new LinearLayout(this);
        brand.setOrientation(LinearLayout.VERTICAL);
        brand.setBackgroundColor(0xFF0F8A3C);
        brand.setPadding(dp(16), dp(16), dp(16), dp(14));
        ImageView mark = new ImageView(this);
        mark.setImageResource(R.drawable.wordmark);
        Night.allow(mark, false);
        mark.setAdjustViewBounds(true);
        mark.setScaleType(ImageView.ScaleType.FIT_CENTER);
        mark.setPadding(dp(8), dp(4), dp(8), dp(4));
        GradientDrawable chip = new GradientDrawable();
        chip.setColor(0xFFFFFFFF);
        chip.setCornerRadius(dp(10));
        mark.setBackground(chip);
        brand.addView(mark, new LinearLayout.LayoutParams(dp(150), dp(36)));
        brand.addView(themeSwitch());
        TextView sub = new TextView(this);
        sub.setText("समाचार श्रेणी");
        sub.setTextColor(0xFFD1FAE5);
        style(sub, 12, R.font.mukta_regular);
        sub.setPadding(0, dp(10), 0, 0);
        brand.addView(sub);
        box.addView(brand);
        android.widget.ScrollView scroll = new android.widget.ScrollView(this);
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(dp(8), dp(6), dp(8), dp(8));
        String[][] items = {
                {"news", "समाचार"},
                {"local", "स्थानीय"},
                {"international", "अन्तर्राष्ट्रिय"},
                {"politics", "राजनीति"},
                {"artha", "अर्थ"},
                {"khelkud", "खेलकुद"},
                {"entertainment", "मनोरंजन"},
                {"lekhabichar", "लेख / बिचार"},
                {"gallery", "फोटो ग्यालरी"},
                {"videos", "भिडियो"},
                {"directory", "डाइरेक्टरी"}
        };
        for (String[] item : items) list.addView(drawerRow(item[0], item[1], dark));
        scroll.addView(list);
        box.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        TextView version = new TextView(this);
        version.setText("संस्करण " + versionName());
        version.setGravity(Gravity.CENTER);
        version.setTextColor(dark ? 0xFF9AA89F : 0xFF78716C);
        style(version, 12, R.font.mukta_regular);
        version.setPadding(dp(12), dp(10), dp(12), dp(14));
        box.addView(version);
        return box;
    }

    private View themeSwitch() {
        boolean dark = Night.on(this);
        LinearLayout track = new LinearLayout(this);
        track.setOrientation(LinearLayout.HORIZONTAL);
        track.setPadding(dp(3), dp(3), dp(3), dp(3));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xFFFFFFFF);
        bg.setCornerRadius(dp(16));
        track.setBackground(bg);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(12);
        track.setLayoutParams(lp);
        track.addView(themeChoice("उज्यालो", !dark, false), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        track.addView(themeChoice("अँध्यारो", dark, true), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        return track;
    }

    private View themeChoice(String label, boolean selected, boolean darkChoice) {
        TextView view = new TextView(this);
        view.setText(label);
        view.setGravity(Gravity.CENTER);
        view.setTextColor(selected ? 0xFFFFFFFF : 0xFF0F8A3C);
        style(view, 13, R.font.mukta_bold);
        view.setPadding(dp(8), dp(6), dp(8), dp(6));
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(13));
        bg.setColor(selected ? 0xFF0F8A3C : 0x00000000);
        view.setBackground(bg);
        view.setOnClickListener(v -> {
            if (Night.on(this) == darkChoice) return;
            Night.set(this, darkChoice);
            AppCompatDelegate.setDefaultNightMode(darkChoice ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
        });
        return view;
    }

    private String versionName() {
        try {
            return getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (Exception ignored) {
            return "";
        }
    }

    private View drawerRow(String slug, String label, boolean dark) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(6), dp(5), dp(6), dp(5));
        ImageView icon = new ImageView(this);
        int res = "gallery".equals(slug) ? R.drawable.ic_photo : "videos".equals(slug) ? R.drawable.ic_play : "directory".equals(slug) ? R.drawable.ic_pin : iconFor(slug);
        icon.setImageResource(res);
        icon.setPadding(dp(6), dp(6), dp(6), dp(6));
        GradientDrawable bubble = new GradientDrawable();
        bubble.setCornerRadius(dp(12));
        bubble.setColor(dark ? 0xFF1E3328 : 0xFFE8F6EE);
        icon.setBackground(bubble);
        Drawable drawable = DrawableCompat.wrap(icon.getDrawable()).mutate();
        DrawableCompat.setTint(drawable, 0xFF0B5C28);
        icon.setImageDrawable(drawable);
        row.addView(icon, new LinearLayout.LayoutParams(dp(32), dp(32)));
        TextView text = new TextView(this);
        text.setText(label);
        text.setTextColor(dark ? 0xFFF3F6F4 : 0xFF1C1917);
        style(text, 14, R.font.mukta_semibold);
        text.setPadding(dp(10), 0, 0, 0);
        row.addView(text, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView chevron = new TextView(this);
        chevron.setText("›");
        chevron.setTextColor(0xFF0F8A3C);
        chevron.setTextSize(16);
        row.addView(chevron);
        View rule = new View(this);
        rule.setBackgroundColor(dark ? 0xFF2A332E : 0xFFF1F5F3);
        LinearLayout wrap = new LinearLayout(this);
        wrap.setOrientation(LinearLayout.VERTICAL);
        wrap.addView(row);
        wrap.addView(rule, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1)));
        row.setOnClickListener(v -> {
            closeDrawer(true);
            if ("gallery".equals(slug)) {
                selectNav(R.id.nav_gallery);
                showList("gallery");
            } else if ("videos".equals(slug)) showVideos();
            else if ("directory".equals(slug)) {
                selectNav(R.id.nav_directory);
                showDirectory();
            } else {
                selectNav(R.id.nav_home);
                openCategory(slug, label);
            }
        });
        return wrap;
    }

    private void selectNav(int id) {
        currentNav = id;
        restoringNav = true;
        nav.setSelectedItemId(id);
        restoringNav = false;
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
        modules.addView(moduleTile("अ", "प्रीति «» युनिकोड", 0xFFE7F6EC, this::showPreeti));
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
        page.addView(heading("प्रीति «» युनिकोड"));
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

    private void showSaved() {
        section = "saved";
        stopMotion();
        setMore(false);
        page.removeAllViews();
        page.addView(heading("सेभ"));
        List<Story> saved = SavedStore.list(this);
        if (saved.isEmpty()) {
            page.addView(message("सेभ गरिएको समाचार छैन।"));
            return;
        }
        for (Story story : saved) page.addView(card(story, false));
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
        weather.load(27.033, 84.983);
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
                    if (gen != homeGen) return;
                    latestHome = home;
                    if ("home".equals(section)) renderHome(home, gen);
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
        if (feedAd != null) {
            feedAd.destroy();
            feedAd = null;
        }
        page.removeAllViews();
        mountTicker(home.ticker);
        page.addView(infoBar(home.dateLine));
        List<Story> heroes = home.headlines.size() > 2 ? home.headlines.subList(0, 2) : home.headlines;
        for (Story story : heroes) page.addView(headlineCard(story));
        page.addView(homeTabs(home));
        feedAd = Ads.attach(this, page);
        if (!home.politics.isEmpty()) page.addView(politicsGrid(home.politics));
        for (SiteClient.Block block : home.blocks) page.addView(categoryBlock(block.label, block.slug, block.stories));
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
                runOnUiThread(() -> {
                    if (kind.equals(section)) setStatus("लोड हुन सकेन।");
                });
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
        catSlug = slug;
        catLabel = label;
        catPage = 1;
        catStories.clear();
        catSiteFetched = 0;
        catSitePages = 1;
        loadCategory();
    }

    private void loadCategory() {
        final int gen = ++catGen;
        final String slug = catSlug;
        final String label = catLabel;
        final int wanted = catPage;
        int need = wanted * 7;
        if (catSiteFetched > 0 && (catStories.size() >= need || catSiteFetched >= catSitePages)) {
            renderCategory();
            return;
        }
        section = "cat:" + slug;
        stopMotion();
        setStatus(label + " लोड हुँदैछ…");
        final List<Story> start = new ArrayList<>(catStories);
        final int fetchedStart = catSiteFetched;
        final int pagesStart = catSitePages;
        io.execute(() -> {
            try {
                List<Story> accumulated = new ArrayList<>(start);
                int fetched = fetchedStart;
                int pages = pagesStart;
                while (accumulated.size() < need && fetched < pages && fetched < 40) {
                    SiteClient.Feed feed = client.categoryPage(slug, fetched + 1);
                    pages = Math.max(pages, feed.pages);
                    for (Story story : feed.stories) {
                        boolean duplicate = false;
                        for (Story existing : accumulated) {
                            if (existing.slug.equals(story.slug)) {
                                duplicate = true;
                                break;
                            }
                        }
                        if (!duplicate) accumulated.add(story);
                    }
                    fetched++;
                    if (feed.stories.isEmpty()) break;
                }
                List<Story> done = accumulated;
                int fetchedDone = fetched;
                int pagesDone = pages;
                runOnUiThread(() -> {
                    if (gen != catGen) return;
                    catStories.clear();
                    catStories.addAll(done);
                    catSiteFetched = fetchedDone;
                    catSitePages = pagesDone;
                    renderCategory();
                });
            } catch (Exception error) {
                runOnUiThread(() -> {
                    if (gen == catGen) setStatus("लोड हुन सकेन।");
                });
            }
        });
    }

    private void renderCategory() {
        section = "cat:" + catSlug;
        page.removeAllViews();
        page.addView(chips());
        page.addView(heading(catLabel));
        int from = (catPage - 1) * 7;
        int to = Math.min(from + 7, catStories.size());
        if (from >= to) page.addView(message("समाचार भेटिएन।"));
        for (int i = from; i < to; i++) page.addView(card(catStories.get(i), false));
        boolean next = to < catStories.size() || catSiteFetched < catSitePages;
        page.addView(categoryPager(catPage > 1, next));
        if (page.getParent() instanceof android.widget.ScrollView) {
            ((android.widget.ScrollView) page.getParent()).scrollTo(0, 0);
        }
    }

    private View categoryPager(boolean prev, boolean next) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER);
        row.setPadding(0, dp(6), 0, dp(18));
        row.addView(pageStep("अघिल्लो", prev, () -> {
            catPage--;
            loadCategory();
        }));
        TextView mark = new TextView(this);
        mark.setText("पृष्ठ " + catPage);
        mark.setTextColor(0xFF0B5C28);
        mark.setGravity(Gravity.CENTER);
        style(mark, 15, R.font.mukta_bold);
        mark.setPadding(dp(14), dp(8), dp(14), dp(8));
        row.addView(mark);
        row.addView(pageStep("अर्को", next, () -> {
            catPage++;
            loadCategory();
        }));
        return row;
    }

    private View pageStep(String label, boolean enabled, Runnable action) {
        TextView view = new TextView(this);
        view.setText(label);
        view.setGravity(Gravity.CENTER);
        style(view, 15, R.font.mukta_bold);
        view.setTextColor(enabled ? 0xFFFFFFFF : 0xFFA8A29E);
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(14));
        bg.setColor(enabled ? 0xFF0F8A3C : 0xFFE7E5E4);
        view.setBackground(bg);
        view.setPadding(dp(16), dp(8), dp(16), dp(8));
        if (enabled) view.setOnClickListener(v -> action.run());
        return view;
    }

    private View chips() {
        HorizontalScrollView scroll = new HorizontalScrollView(this);
        scroll.setHorizontalScrollBarEnabled(false);
        LinearLayout row = new LinearLayout(this);
        row.setPadding(dp(2), dp(4), dp(2), dp(10));
        for (String[] cat : SECTIONS) {
            boolean on = cat[0].equals(catSlug);
            TextView chip = new TextView(this);
            chip.setText(cat[1]);
            chip.setTextColor(on ? 0xFFFFFFFF : 0xFF1C1917);
            style(chip, 15, R.font.mukta_semibold);
            chip.setGravity(Gravity.CENTER_VERTICAL);
            Drawable icon = DrawableCompat.wrap(getDrawable(iconFor(cat[0]))).mutate();
            DrawableCompat.setTint(icon, on ? 0xFFFFFFFF : 0xFF0F8A3C);
            icon.setBounds(0, 0, dp(16), dp(16));
            chip.setCompoundDrawables(icon, null, null, null);
            chip.setCompoundDrawablePadding(dp(6));
            GradientDrawable bg = new GradientDrawable();
            bg.setCornerRadius(dp(22));
            if (on) {
                bg.setColor(0xFF0F8A3C);
            } else {
                bg.setColor(0xFFFFFFFF);
                bg.setStroke(dp(1), 0xFFE5E7EB);
            }
            chip.setBackground(bg);
            chip.setElevation(on ? dp(4) : dp(1));
            chip.setPadding(dp(14), dp(8), dp(16), dp(8));
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

    private View infoBar(String dateLine) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rowLp.topMargin = dp(8);
        rowLp.bottomMargin = dp(8);
        row.setLayoutParams(rowLp);
        row.addView(dateChip(dateLine), new LinearLayout.LayoutParams(0, dp(58), 1.15f));
        View gap = new View(this);
        row.addView(gap, new LinearLayout.LayoutParams(dp(8), 1));
        row.addView(weatherChip(), new LinearLayout.LayoutParams(0, dp(58), 1f));
        loadStripWeather();
        return row;
    }

    private View dateChip(String dateLine) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.HORIZONTAL);
        box.setGravity(Gravity.CENTER_VERTICAL);
        box.setPadding(dp(10), dp(6), dp(10), dp(6));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xFFFFFFFF);
        bg.setCornerRadius(dp(18));
        box.setBackground(bg);
        box.setElevation(dp(2));
        TextView day = new TextView(this);
        day.setTextColor(0xFF0F8A3C);
        style(day, 22, R.font.mukta_extrabold);
        day.setIncludeFontPadding(false);
        String[] parts = nepaliDate(dateLine);
        day.setText(parts[0]);
        box.addView(day);
        LinearLayout words = new LinearLayout(this);
        words.setOrientation(LinearLayout.VERTICAL);
        words.setPadding(dp(8), 0, 0, 0);
        TextView week = new TextView(this);
        week.setText(weekday());
        week.setTextColor(0xFFF15A22);
        style(week, 11, R.font.mukta_bold);
        week.setIncludeFontPadding(false);
        words.addView(week);
        TextView rest = new TextView(this);
        rest.setText(parts[1]);
        rest.setTextColor(0xFF142033);
        rest.setMaxLines(1);
        style(rest, 13, R.font.mukta_semibold);
        rest.setIncludeFontPadding(false);
        words.addView(rest);
        box.addView(words);
        box.setOnClickListener(v -> showPatro());
        return box;
    }

    private View weatherChip() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.HORIZONTAL);
        box.setGravity(Gravity.CENTER_VERTICAL);
        box.setPadding(dp(10), dp(6), dp(10), dp(6));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xFFFFFFFF);
        bg.setCornerRadius(dp(18));
        box.setBackground(bg);
        box.setElevation(dp(2));
        weatherTemp = new TextView(this);
        weatherTemp.setText("—");
        weatherTemp.setTextColor(0xFF0F8A3C);
        style(weatherTemp, 22, R.font.mukta_extrabold);
        weatherTemp.setIncludeFontPadding(false);
        box.addView(weatherTemp);
        LinearLayout words = new LinearLayout(this);
        words.setOrientation(LinearLayout.VERTICAL);
        words.setPadding(dp(8), 0, 0, 0);
        TextView place = new TextView(this);
        place.setText("कलैया");
        place.setTextColor(0xFFF15A22);
        style(place, 11, R.font.mukta_bold);
        place.setIncludeFontPadding(false);
        words.addView(place);
        weatherFlip = new TextView(this);
        weatherFlip.setText("लोड हुँदैछ");
        weatherFlip.setTextColor(0xFF142033);
        weatherFlip.setMaxLines(1);
        style(weatherFlip, 13, R.font.mukta_semibold);
        weatherFlip.setIncludeFontPadding(false);
        words.addView(weatherFlip);
        box.addView(words);
        box.setOnClickListener(v -> showWeather());
        return box;
    }

    private void loadStripWeather() {
        io.execute(() -> {
            String body = fetchText("https://api.open-meteo.com/v1/forecast?latitude=27.033&longitude=84.983&current=temperature_2m,weather_code,is_day&daily=temperature_2m_max,temperature_2m_min&timezone=Asia%2FKathmandu&forecast_days=1");
            String[] lines = weatherCopy(body);
            String temp = weatherTempText(body);
            runOnUiThread(() -> {
                if (!"home".equals(section) || weatherFlip == null || lines.length == 0) return;
                if (weatherTemp != null) weatherTemp.setText(temp);
                weatherLines = lines;
                weatherIndex = 0;
                weatherFlip.setAlpha(1f);
                weatherFlip.setText(lines[0]);
                infoHandler.removeCallbacksAndMessages(null);
                infoHandler.postDelayed(this::advanceWeather, 2600);
            });
        });
    }

    private void advanceWeather() {
        if (!"home".equals(section) || weatherFlip == null || weatherLines.length < 2) return;
        weatherFlip.animate().alpha(0f).setDuration(240).withEndAction(() -> {
            if (weatherFlip == null || weatherLines.length == 0) return;
            weatherIndex = (weatherIndex + 1) % weatherLines.length;
            weatherFlip.setText(weatherLines[weatherIndex]);
            weatherFlip.animate().alpha(1f).setDuration(240).start();
            infoHandler.postDelayed(this::advanceWeather, 2600);
        }).start();
    }

    private String[] weatherCopy(String body) {
        try {
            JSONObject json = new JSONObject(body);
            JSONObject current = json.getJSONObject("current");
            boolean day = current.optInt("is_day", 1) == 1;
            String sky = skyLabel(current.optInt("weather_code"), day);
            JSONObject daily = json.getJSONObject("daily");
            int max = (int) Math.round(daily.getJSONArray("temperature_2m_max").getDouble(0));
            int min = (int) Math.round(daily.getJSONArray("temperature_2m_min").getDouble(0));
            return new String[]{sky, "अधिकतम " + nepaliNumber(max) + "°", "न्यूनतम " + nepaliNumber(min) + "°"};
        } catch (Exception ignored) {
            return new String[0];
        }
    }

    private String weatherTempText(String body) {
        try {
            return nepaliNumber((int) Math.round(new JSONObject(body).getJSONObject("current").getDouble("temperature_2m"))) + "°";
        } catch (Exception ignored) {
            return "—";
        }
    }

    private String skyLabel(int code, boolean day) {
        if (code == 0) return day ? "घमाइलो" : "सफा रात";
        if (code <= 3) return day ? "आंशिक बादल" : "हल्का बादल";
        if (code == 45 || code == 48) return "हुस्सु";
        if (code <= 67) return "वर्षा";
        if (code <= 77) return "हिउँ";
        if (code <= 82) return "झरी";
        if (code >= 95) return "चट्याङ";
        return "बादल";
    }

    private String[] nepaliDate(String dateLine) {
        String value = dateLine == null ? "" : dateLine;
        int cut = value.indexOf('|');
        if (cut > 0) value = value.substring(0, cut).trim();
        String[] parts = value.split("\\s+");
        if (parts.length >= 3) return new String[]{parts[parts.length - 1], parts[parts.length - 2] + "  " + parts[0]};
        if (!value.isEmpty()) return new String[]{"आज", value};
        return new String[]{"आज", weekday()};
    }

    private String weekday() {
        String[] names = {"", "आइतबार", "सोमबार", "मंगलबार", "बुधबार", "बिहिबार", "शुक्रबार", "शनिबार"};
        return names[java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_WEEK)];
    }

    private String nepaliNumber(int number) {
        String digits = "०१२३४५६७८९";
        String raw = String.valueOf(Math.abs(number));
        StringBuilder out = new StringBuilder();
        if (number < 0) out.append('-');
        for (int i = 0; i < raw.length(); i++) out.append(digits.charAt(raw.charAt(i) - '0'));
        return out.toString();
    }

    private String fetchText(String address) {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(address).openConnection();
            connection.setConnectTimeout(8000);
            connection.setReadTimeout(8000);
            connection.setRequestProperty("User-Agent", "KalaiyaOnline");
            InputStream input = connection.getInputStream();
            StringBuilder body = new StringBuilder();
            byte[] buffer = new byte[4096];
            int read;
            while ((read = input.read(buffer)) != -1) body.append(new String(buffer, 0, read, "UTF-8"));
            input.close();
            return body.toString();
        } catch (Exception ignored) {
            return "";
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    private void mountTicker(List<Story> items) {
        if (tickerHost == null) return;
        tickerHost.removeAllViews();
        if (items == null || items.isEmpty()) {
            tickerHost.setVisibility(View.GONE);
            return;
        }
        tickerHost.addView(ticker(items));
        tickerHost.setVisibility(View.VISIBLE);
    }

    private View ticker(List<Story> items) {
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        int paper = Night.on(this) ? 0xFF171D19 : 0xFFFFFFFF;
        bar.setBackgroundColor(paper);
        bar.setMinimumHeight(dp(42));
        LinearLayout badge = new LinearLayout(this);
        badge.setOrientation(LinearLayout.HORIZONTAL);
        badge.setGravity(Gravity.CENTER);
        GradientDrawable badgeBg = new GradientDrawable();
        badgeBg.setColor(0xFF0F8A3C);
        float r = dp(18);
        badgeBg.setCornerRadii(new float[]{0, 0, r, r, r, r, 0, 0});
        badge.setBackground(badgeBg);
        badge.setPadding(dp(12), 0, dp(12), 0);
        View dot = new View(this);
        GradientDrawable dotBg = new GradientDrawable();
        dotBg.setShape(GradientDrawable.OVAL);
        dotBg.setColor(0xFFF15A22);
        dot.setBackground(dotBg);
        badge.addView(dot, new LinearLayout.LayoutParams(dp(7), dp(7)));
        TextView label = new TextView(this);
        label.setText("ताजा");
        label.setTextColor(0xFFFFFFFF);
        label.setPadding(dp(6), 0, 0, 0);
        style(label, 13, R.font.mukta_bold);
        label.setIncludeFontPadding(false);
        badge.addView(label);
        bar.addView(badge, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT));
        FrameLayout clip = new FrameLayout(this);
        clip.setClipChildren(true);
        TextView moving = new TextView(this);
        StringBuilder text = new StringBuilder();
        for (int pass = 0; pass < 2; pass++) {
            for (Story item : items) text.append(item.title).append("     ·     ");
        }
        moving.setText(text.toString().trim());
        moving.setTextColor(Night.on(this) ? 0xFFF3F6F4 : 0xFF142033);
        moving.setSingleLine(true);
        style(moving, 15, R.font.mukta_semibold);
        moving.setIncludeFontPadding(false);
        clip.addView(moving, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER_VERTICAL));
        View fade = new View(this);
        fade.setBackground(new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, new int[]{0x00000000, paper}));
        clip.addView(fade, new FrameLayout.LayoutParams(dp(28), ViewGroup.LayoutParams.MATCH_PARENT, Gravity.END));
        LinearLayout.LayoutParams clipLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f);
        clipLp.leftMargin = dp(12);
        clipLp.rightMargin = dp(12);
        bar.addView(clip, clipLp);
        View line = new View(this);
        line.setBackgroundColor(0xFFE7E5E4);
        LinearLayout wrap = new LinearLayout(this);
        wrap.setOrientation(LinearLayout.VERTICAL);
        wrap.addView(bar, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(42)));
        wrap.addView(line, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1)));
        Night.allow(wrap, false);
        moving.post(() -> {
            moving.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
            ViewGroup.LayoutParams params = moving.getLayoutParams();
            params.width = Math.max(moving.getMeasuredWidth(), clip.getWidth() + dp(40));
            moving.setLayoutParams(params);
            moving.setTranslationX(clip.getWidth());
            if (tickerAnim != null) tickerAnim.cancel();
            tickerAnim = ObjectAnimator.ofFloat(moving, "translationX", clip.getWidth(), -params.width);
            float speed = Math.max(dp(46), 1);
            tickerAnim.setDuration(Math.max(14000L, (long) ((clip.getWidth() + params.width) / speed * 1000f)));
            tickerAnim.setRepeatCount(ValueAnimator.INFINITE);
            tickerAnim.setInterpolator(new LinearInterpolator());
            tickerAnim.start();
        });
        bar.setOnClickListener(v -> openStory(items.get(0)));
        return wrap;
    }

    private View headlineCard(Story story) {
        MaterialCardView card = new MaterialCardView(this);
        card.setRadius(dp(22));
        card.setCardElevation(dp(3));
        card.setStrokeWidth(0);
        card.setCardBackgroundColor(0xFFFFFFFF);
        card.setUseCompatPadding(false);
        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cardLp.topMargin = dp(4);
        cardLp.bottomMargin = dp(14);
        card.setLayoutParams(cardLp);
        LinearLayout inner = new LinearLayout(this);
        inner.setOrientation(LinearLayout.VERTICAL);
        inner.setGravity(Gravity.CENTER_HORIZONTAL);
        LinearLayout copy = new LinearLayout(this);
        copy.setOrientation(LinearLayout.VERTICAL);
        copy.setGravity(Gravity.CENTER_HORIZONTAL);
        copy.setPadding(dp(16), dp(14), dp(16), dp(4));

        View accent = new View(this);
        GradientDrawable accentBg = new GradientDrawable();
        accentBg.setCornerRadius(dp(4));
        accentBg.setColor(0xFFF15A22);
        accent.setBackground(accentBg);
        LinearLayout.LayoutParams accentLp = new LinearLayout.LayoutParams(dp(42), dp(5));
        accentLp.gravity = Gravity.CENTER_HORIZONTAL;
        accentLp.bottomMargin = dp(14);
        copy.addView(accent, accentLp);

        TextView title = new TextView(this);
        title.setText(accentTitle(story.title));
        title.setGravity(Gravity.CENTER);
        style(title, 30, R.font.mukta_extrabold);
        title.setIncludeFontPadding(false);
        title.setLineSpacing(dp(-10), 0.85f);
        if (Build.VERSION.SDK_INT >= 28) {
            title.setFallbackLineSpacing(false);
            title.setLineHeight(dp(38));
        }
        copy.addView(title);

        LinearLayout meta = new LinearLayout(this);
        meta.setOrientation(LinearLayout.HORIZONTAL);
        meta.setGravity(Gravity.CENTER);
        meta.setPadding(0, dp(12), 0, dp(10));
        ImageView logo = new ImageView(this);
        logo.setImageResource(R.mipmap.ic_launcher);
        logo.setScaleType(ImageView.ScaleType.CENTER_CROP);
        logo.setClipToOutline(true);
        logo.setOutlineProvider(new ViewOutlineProvider() {
            public void getOutline(View view, Outline outline) {
                outline.setOval(0, 0, view.getWidth(), view.getHeight());
            }
        });
        meta.addView(logo, new LinearLayout.LayoutParams(dp(28), dp(28)));
        TextView source = new TextView(this);
        source.setText("कलैया अनलाइन");
        source.setTextColor(0xFF142033);
        style(source, 15, R.font.mukta_bold);
        source.setPadding(dp(8), 0, dp(8), 0);
        meta.addView(source);
        TextView bar = new TextView(this);
        bar.setText("|");
        bar.setTextColor(0xFFD6D3D1);
        bar.setTextSize(16);
        meta.addView(bar);
        if (!story.date.isEmpty()) {
            ImageView calendar = new ImageView(this);
            calendar.setImageResource(R.drawable.ic_calendar);
            LinearLayout.LayoutParams calLp = new LinearLayout.LayoutParams(dp(16), dp(16));
            calLp.leftMargin = dp(8);
            calLp.rightMargin = dp(4);
            meta.addView(calendar, calLp);
            TextView date = new TextView(this);
            date.setText(story.date);
            date.setTextColor(0xFF1C1917);
            style(date, 14, R.font.mukta_semibold);
            meta.addView(date);
        }
        copy.addView(meta);
        inner.addView(copy);

        View line = new View(this);
        line.setBackgroundColor(0xFFE7E5E4);
        LinearLayout.LayoutParams lineLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1));
        lineLp.leftMargin = dp(16);
        lineLp.rightMargin = dp(16);
        lineLp.bottomMargin = dp(12);
        inner.addView(line, lineLp);

        FrameLayout photo = new FrameLayout(this);
        photo.setBackgroundColor(0xFFE8F6EE);
        photo.setClipToOutline(true);
        photo.setOutlineProvider(new ViewOutlineProvider() {
            public void getOutline(View view, Outline outline) {
                outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), dp(16));
            }
        });
        ImageView image = new ImageView(this);
        image.setScaleType(ImageView.ScaleType.CENTER_CROP);
        photo.addView(image, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(210)));
        if (!story.image.isEmpty()) loadImage(story.image, image);
        ImageView save = new ImageView(this);
        save.setImageResource(R.drawable.ic_bookmark);
        save.setPadding(dp(8), dp(8), dp(8), dp(8));
        GradientDrawable saveBg = new GradientDrawable();
        saveBg.setCornerRadius(dp(14));
        saveBg.setColor(0xFFFFFFFF);
        save.setBackground(saveBg);
        paintSave(save, story.slug);
        save.setOnClickListener(v -> {
            boolean now = !isSaved(story.slug);
            SavedStore.set(this, story, now);
            paintSave(save, story.slug);
        });
        FrameLayout.LayoutParams saveLp = new FrameLayout.LayoutParams(dp(40), dp(40), Gravity.END | Gravity.TOP);
        saveLp.setMargins(0, dp(10), dp(10), 0);
        photo.addView(save, saveLp);
        LinearLayout.LayoutParams photoLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(210));
        photoLp.leftMargin = dp(12);
        photoLp.rightMargin = dp(12);
        photoLp.bottomMargin = dp(12);
        inner.addView(photo, photoLp);
        card.addView(inner);
        card.setOnClickListener(v -> openStory(story));
        return card;
    }

    private CharSequence accentTitle(String title) {
        SpannableString text = new SpannableString(title);
        int split = title.indexOf(' ');
        if (split < 1) split = Math.min(title.length(), 4);
        text.setSpan(new ForegroundColorSpan(0xFFF15A22), 0, split, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        if (split < title.length()) {
            text.setSpan(new ForegroundColorSpan(0xFF1B2A4A), split, title.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return text;
    }

    private boolean isSaved(String slug) {
        return SavedStore.isSaved(this, slug);
    }

    private void paintSave(ImageView save, String slug) {
        Drawable icon = DrawableCompat.wrap(save.getDrawable()).mutate();
        DrawableCompat.setTint(icon, isSaved(slug) ? 0xFF0F8A3C : 0xFF44403C);
        save.setImageDrawable(icon);
    }

    private View heroCard(Story story, boolean lead) {
        MaterialCardView card = new MaterialCardView(this);
        card.setRadius(dp(18));
        card.setCardElevation(dp(2));
        card.setCardBackgroundColor(0xFFFFFFFF);
        card.setUseCompatPadding(true);
        LinearLayout inner = new LinearLayout(this);
        inner.setOrientation(LinearLayout.VERTICAL);
        if (!story.image.isEmpty()) {
            ImageView image = new ImageView(this);
            image.setScaleType(ImageView.ScaleType.CENTER_CROP);
            image.setBackgroundColor(0xFFE8F6EE);
            inner.addView(image, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, lead ? dp(210) : dp(156)));
            loadImage(story.image, image);
        }
        LinearLayout pad = new LinearLayout(this);
        pad.setOrientation(LinearLayout.VERTICAL);
        pad.setPadding(dp(14), dp(12), dp(14), dp(14));
        TextView kicker = new TextView(this);
        kicker.setText(story.category.isEmpty() ? (lead ? "हेडलाइन" : "") : story.category);
        kicker.setTextColor(0xFFF15A22);
        style(kicker, 13, R.font.mukta_bold);
        if (kicker.getText().length() == 0) kicker.setVisibility(View.GONE);
        pad.addView(kicker);
        TextView title = new TextView(this);
        title.setText(story.title);
        title.setTextColor(0xFF1C1917);
        style(title, lead ? 25 : 20, R.font.mukta_bold);
        title.setPadding(0, dp(4), 0, 0);
        pad.addView(title);
        inner.addView(pad);
        card.addView(inner);
        card.setOnClickListener(v -> openStory(story));
        return card;
    }

    private View homeTabs(SiteClient.Home home) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(0, dp(6), 0, 0);
        box.addView(sectionHead("समाचार", null, null));
        LinearLayout track = new LinearLayout(this);
        track.setOrientation(LinearLayout.HORIZONTAL);
        track.setPadding(dp(4), dp(4), dp(4), dp(4));
        GradientDrawable trackBg = new GradientDrawable();
        trackBg.setCornerRadius(dp(16));
        trackBg.setColor(0xFFFFFFFF);
        track.setBackground(trackBg);
        track.setElevation(dp(2));
        String[] labels = {"सबै", "स्थानिय", "देश", "अन्तरास्ट्रिय"};
        TextView[] buttons = new TextView[4];
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(0, dp(8), 0, 0);
        Runnable paint = () -> {
            for (int i = 0; i < buttons.length; i++) {
                boolean on = i == homeTab;
                buttons[i].setTextColor(on ? 0xFFFFFFFF : 0xFF3F4A45);
                GradientDrawable chip = new GradientDrawable();
                chip.setCornerRadius(dp(12));
                chip.setColor(on ? 0xFF0F8A3C : 0x00000000);
                buttons[i].setBackground(chip);
            }
            body.removeAllViews();
            List<Story> list = homeTab == 1 ? home.tabLocal : homeTab == 2 ? home.tabNational : homeTab == 3 ? home.tabWorld : home.tabAll;
            if (list.isEmpty()) {
                body.addView(message("यो ट्याबमा समाचार भेटिएन।"));
                return;
            }
            int count = Math.min(list.size(), 5);
            body.addView(featureCard(list.get(0)));
            for (int i = 1; i < count; i++) body.addView(compact(list.get(i)));
        };
        for (int i = 0; i < labels.length; i++) {
            TextView tab = new TextView(this);
            tab.setText(labels[i]);
            tab.setGravity(Gravity.CENTER);
            style(tab, 14, R.font.mukta_bold);
            tab.setPadding(dp(2), dp(9), dp(2), dp(9));
            tab.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            int index = i;
            tab.setOnClickListener(v -> {
                homeTab = index;
                paint.run();
            });
            buttons[i] = tab;
            track.addView(tab);
        }
        box.addView(track);
        box.addView(body);
        paint.run();
        return box;
    }

    private View featureCard(Story story) {
        MaterialCardView card = new MaterialCardView(this);
        card.setRadius(dp(18));
        card.setCardElevation(dp(2));
        card.setCardBackgroundColor(0xFF102418);
        card.setUseCompatPadding(false);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(8);
        card.setLayoutParams(lp);
        FrameLayout frame = new FrameLayout(this);
        ImageView image = new ImageView(this);
        image.setScaleType(ImageView.ScaleType.CENTER_CROP);
        image.setBackgroundColor(0xFF14532D);
        frame.addView(image, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(196)));
        if (!story.image.isEmpty()) loadImage(story.image, image);
        View shade = new View(this);
        shade.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[]{0x00000000, 0xE6100000}));
        frame.addView(shade, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(120), Gravity.BOTTOM));
        LinearLayout copy = new LinearLayout(this);
        copy.setOrientation(LinearLayout.VERTICAL);
        copy.setPadding(dp(12), dp(10), dp(12), dp(12));
        TextView kicker = new TextView(this);
        kicker.setText(story.category.isEmpty() ? "समाचार" : story.category);
        kicker.setTextColor(0xFFF7C56A);
        style(kicker, 12, R.font.mukta_bold);
        copy.addView(kicker);
        TextView title = new TextView(this);
        title.setText(story.title);
        title.setTextColor(0xFFFFFFFF);
        title.setMaxLines(2);
        style(title, 18, R.font.mukta_bold);
        title.setPadding(0, dp(2), 0, 0);
        copy.addView(title);
        frame.addView(copy, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM));
        card.addView(frame);
        card.setOnClickListener(v -> openStory(story));
        return card;
    }

    private View sectionHead(String label, String slug, String more) {
        LinearLayout head = new LinearLayout(this);
        head.setGravity(Gravity.CENTER_VERTICAL);
        head.setPadding(0, dp(16), 0, dp(8));
        View mark = new View(this);
        GradientDrawable markBg = new GradientDrawable();
        markBg.setCornerRadius(dp(3));
        markBg.setColor(0xFF0F8A3C);
        mark.setBackground(markBg);
        LinearLayout.LayoutParams markLp = new LinearLayout.LayoutParams(dp(4), dp(18));
        markLp.rightMargin = dp(8);
        head.addView(mark, markLp);
        TextView title = new TextView(this);
        title.setText(label);
        title.setTextColor(0xFF102418);
        style(title, 20, R.font.mukta_extrabold);
        title.setIncludeFontPadding(false);
        head.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        if (slug != null) {
            TextView link = new TextView(this);
            link.setText(more == null ? "सबै" : more);
            link.setTextColor(0xFF0F8A3C);
            style(link, 13, R.font.mukta_bold);
            link.setBackground(pill(0xFFE7F6EC));
            link.setPadding(dp(10), dp(4), dp(10), dp(4));
            link.setOnClickListener(v -> openCategory(slug, label));
            head.addView(link);
        }
        return head;
    }

    private GradientDrawable pill(int color) {
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(color);
        bg.setCornerRadius(dp(14));
        return bg;
    }

    private View politicsGrid(List<Story> stories) {
        LinearLayout block = new LinearLayout(this);
        block.setOrientation(LinearLayout.VERTICAL);
        block.addView(sectionHead("राजनीति", "politics", "सबै"));
        LinearLayout row = null;
        int count = Math.min(stories.size(), 4);
        for (int i = 0; i < count; i++) {
            if (i % 2 == 0) {
                row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                block.addView(row);
            }
            LinearLayout.LayoutParams cell = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            cell.leftMargin = i % 2 == 0 ? 0 : dp(4);
            cell.rightMargin = i % 2 == 0 ? dp(4) : 0;
            row.addView(overlayCell(stories.get(i)), cell);
        }
        return block;
    }

    private View overlayCell(Story story) {
        MaterialCardView card = new MaterialCardView(this);
        card.setRadius(dp(16));
        card.setCardElevation(0);
        card.setStrokeWidth(0);
        card.setCardBackgroundColor(0xFF102418);
        card.setUseCompatPadding(false);
        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cardLp.bottomMargin = dp(8);
        card.setLayoutParams(cardLp);
        FrameLayout frame = new FrameLayout(this);
        ImageView image = new ImageView(this);
        image.setScaleType(ImageView.ScaleType.CENTER_CROP);
        image.setBackgroundColor(0xFF14532D);
        frame.addView(image, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(156)));
        if (!story.image.isEmpty()) loadImage(story.image, image);
        View shade = new View(this);
        GradientDrawable fade = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[]{0x00000000, 0xF0000000});
        shade.setBackground(fade);
        FrameLayout.LayoutParams shadeLp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(96), Gravity.BOTTOM);
        frame.addView(shade, shadeLp);
        TextView title = new TextView(this);
        title.setText(story.title);
        title.setTextColor(0xFFFFFFFF);
        title.setMaxLines(3);
        style(title, 15, R.font.mukta_bold);
        title.setPadding(dp(10), dp(8), dp(10), dp(10));
        frame.addView(title, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM));
        card.addView(frame);
        card.setOnClickListener(v -> openStory(story));
        return card;
    }

    private View categoryBlock(String label, String slug, List<Story> stories) {
        LinearLayout block = new LinearLayout(this);
        block.setOrientation(LinearLayout.VERTICAL);
        block.addView(sectionHead(label, slug, "सबै"));
        if (stories.isEmpty()) return block;
        block.addView(featureCard(stories.get(0)));
        int count = Math.min(stories.size(), 4);
        for (int i = 1; i < count; i++) block.addView(compact(stories.get(i)));
        return block;
    }

    private View compact(Story story) {
        MaterialCardView card = new MaterialCardView(this);
        card.setRadius(dp(16));
        card.setCardElevation(0);
        card.setStrokeWidth(dp(1));
        card.setStrokeColor(0xFFE7E5E4);
        card.setCardBackgroundColor(0xFFFFFFFF);
        card.setUseCompatPadding(false);
        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cardLp.bottomMargin = dp(8);
        card.setLayoutParams(cardLp);
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(8), dp(8), dp(12), dp(8));
        if (!story.image.isEmpty()) {
            ImageView image = new ImageView(this);
            image.setScaleType(ImageView.ScaleType.CENTER_CROP);
            image.setBackgroundColor(0xFFE8F6EE);
            image.setClipToOutline(true);
            image.setOutlineProvider(new ViewOutlineProvider() {
                public void getOutline(View view, Outline outline) {
                    outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), dp(12));
                }
            });
            row.addView(image, new LinearLayout.LayoutParams(dp(84), dp(68)));
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
        infoHandler.removeCallbacksAndMessages(null);
        if (weatherFlip != null) weatherFlip.animate().cancel();
        if (tickerHost != null && !"home".equals(section)) tickerHost.setVisibility(View.GONE);
        pager = null;
    }

    @Override
    protected void onDestroy() {
        stopMotion();
        images.shutdownNow();
        if (shellAd != null) shellAd.destroy();
        if (feedAd != null) feedAd.destroy();
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
        if (hero) title.setTextSize(20);
        else style(title, 18, R.font.mukta_semibold);
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
        if (story.slug.contains("/gallery/") && !story.slug.contains("gallery-category")) {
            Intent intent = new Intent(this, GalleryActivity.class);
            intent.putExtra("url", story.slug);
            intent.putExtra("title", story.title);
            intent.putExtra("image", story.image);
            intent.putExtra("category", story.category);
            startActivity(intent);
            return;
        }
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
        images.execute(() -> {
            Bitmap bitmap = Bitmaps.load(url, 720);
            if (bitmap == null) return;
            runOnUiThread(() -> {
                if (!isDestroyed()) {
                    Night.allow(target, false);
                    target.setImageBitmap(bitmap);
                }
            });
        });
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
