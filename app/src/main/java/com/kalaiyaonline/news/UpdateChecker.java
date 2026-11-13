package com.kalaiyaonline.news;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

final class UpdateChecker {
    private static final String RELEASE = "https://api.github.com/repos/arunkly/kalaiyaonline-android/releases/latest";
    private final AppCompatActivity activity;
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private File pending;
    private boolean waitingForPermission;

    UpdateChecker(AppCompatActivity activity) {
        this.activity = activity;
    }

    void check() {
        io.execute(() -> {
            try {
                String body = get(RELEASE);
                JSONObject json = new JSONObject(body);
                String tag = json.optString("tag_name", "");
                String current = activity.getPackageManager().getPackageInfo(activity.getPackageName(), 0).versionName;
                if (compare(tag, current) <= 0) return;
                String apk = assetUrl(json);
                if (apk.isEmpty()) return;
                String version = tag.startsWith("v") ? tag.substring(1) : tag;
                activity.runOnUiThread(() -> offer(version, apk));
            } catch (Exception ignored) {
            }
        });
    }

    private void offer(String version, String apk) {
        if (activity.isFinishing()) return;
        new AlertDialog.Builder(activity)
                .setTitle("नयाँ अपडेट")
                .setMessage("कलैया अनलाइन " + version + " उपलब्ध छ। अहिले अपडेट गर्नुहुन्छ?")
                .setPositiveButton("अहिले अपडेट", (dialog, which) -> download(apk))
                .setNegativeButton("पछि", null)
                .show();
    }

    private void download(String apk) {
        ProgressDialog progress = new ProgressDialog(activity);
        progress.setMessage("अपडेट डाउनलोड हुँदैछ…");
        progress.setIndeterminate(true);
        progress.setCancelable(false);
        progress.show();
        io.execute(() -> {
            try {
                File dir = new File(activity.getCacheDir(), "updates");
                if (!dir.exists()) dir.mkdirs();
                File file = new File(dir, "KalaiyaOnline.apk");
                HttpURLConnection connection = (HttpURLConnection) new URL(apk).openConnection();
                connection.setInstanceFollowRedirects(true);
                connection.setRequestProperty("User-Agent", "KalaiyaOnline");
                connection.setConnectTimeout(20000);
                connection.setReadTimeout(60000);
                InputStream input = connection.getInputStream();
                FileOutputStream output = new FileOutputStream(file);
                byte[] buffer = new byte[8192];
                int read;
                while ((read = input.read(buffer)) != -1) output.write(buffer, 0, read);
                output.close();
                input.close();
                connection.disconnect();
                activity.runOnUiThread(() -> {
                    progress.dismiss();
                    pending = file;
                    install(file);
                });
            } catch (Exception error) {
                activity.runOnUiThread(() -> {
                    progress.dismiss();
                    new AlertDialog.Builder(activity)
                            .setMessage("अपडेट डाउनलोड हुन सकेन। पछि फेरि प्रयास गर्नुहोस्।")
                            .setPositiveButton("ठीक छ", null)
                            .show();
                });
            }
        });
    }

    void installPending() {
        if (waitingForPermission && pending != null && canInstall()) install(pending);
    }

    private void install(File file) {
        if (Build.VERSION.SDK_INT >= 26 && !activity.getPackageManager().canRequestPackageInstalls()) {
            waitingForPermission = true;
            Intent settings = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:" + activity.getPackageName()));
            activity.startActivity(settings);
            return;
        }
        waitingForPermission = false;
        pending = null;
        Uri uri = FileProvider.getUriForFile(activity, activity.getPackageName() + ".fileprovider", file);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, "application/vnd.android.package-archive");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
        activity.startActivity(intent);
    }

    private boolean canInstall() {
        return Build.VERSION.SDK_INT < 26 || activity.getPackageManager().canRequestPackageInstalls();
    }

    private String assetUrl(JSONObject json) throws Exception {
        JSONArray assets = json.optJSONArray("assets");
        if (assets == null) return "";
        for (int i = 0; i < assets.length(); i++) {
            JSONObject asset = assets.getJSONObject(i);
            if ("KalaiyaOnline.apk".equals(asset.optString("name"))) return asset.optString("browser_download_url");
        }
        return "";
    }

    private String get(String address) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(address).openConnection();
        connection.setRequestProperty("User-Agent", "KalaiyaOnline");
        connection.setRequestProperty("Accept", "application/vnd.github+json");
        connection.setConnectTimeout(12000);
        connection.setReadTimeout(12000);
        InputStream input = connection.getInputStream();
        StringBuilder body = new StringBuilder();
        byte[] buffer = new byte[4096];
        int read;
        while ((read = input.read(buffer)) != -1) body.append(new String(buffer, 0, read));
        input.close();
        connection.disconnect();
        return body.toString();
    }

    static int compare(String remote, String local) {
        String[] left = remote.replaceFirst("^[vV]", "").split("\\.");
        String[] right = local.replaceFirst("^[vV]", "").split("\\.");
        int length = Math.max(left.length, right.length);
        for (int i = 0; i < length; i++) {
            int a = i < left.length ? number(left[i]) : 0;
            int b = i < right.length ? number(right[i]) : 0;
            if (a != b) return Integer.compare(a, b);
        }
        return 0;
    }

    private static int number(String value) {
        String digits = value.replaceAll("[^0-9].*$", "");
        if (digits.isEmpty()) return 0;
        return Integer.parseInt(digits);
    }
}
