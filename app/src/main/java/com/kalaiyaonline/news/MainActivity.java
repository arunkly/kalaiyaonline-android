package com.kalaiyaonline.news;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends Activity {
    private WebView web;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        web = new WebView(this);
        setContentView(web);
        WebSettings settings = web.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
        web.addJavascriptInterface(new Bridge(), "KalaiyaNative");
        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();
                if (uri == null) return true;
                String host = uri.getHost() == null ? "" : uri.getHost();
                if ("file".equals(uri.getScheme())
                        || host.equals("kalaiyaonline.com")
                        || host.endsWith(".kalaiyaonline.com")
                        || host.equals("ik.imagekit.io")
                        || host.endsWith(".imagekit.io")
                        || host.equals("fonts.googleapis.com")
                        || host.equals("fonts.gstatic.com")) {
                    return false;
                }
                startActivity(new Intent(Intent.ACTION_VIEW, uri));
                return true;
            }
        });
        web.loadUrl("file:///android_asset/www/index.html");
    }

    @Override
    public void onBackPressed() {
        web.evaluateJavascript("window.appBack()", value -> {
            if ("\"exit\"".equals(value)) finish();
        });
    }

    private final class Bridge {
        @JavascriptInterface
        public String fetchText(String raw) {
            HttpURLConnection connection = null;
            try {
                URL url = new URL(raw);
                if (!"https".equals(url.getProtocol())) return "";
                String host = url.getHost();
                if (host == null || !(host.equals("kalaiyaonline.com") || host.endsWith(".kalaiyaonline.com"))) {
                    return "";
                }
                connection = (HttpURLConnection) url.openConnection();
                connection.setConnectTimeout(15000);
                connection.setReadTimeout(20000);
                connection.setInstanceFollowRedirects(true);
                connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36");
                connection.setRequestProperty("Accept", "text/html,application/xhtml+xml");
                connection.setRequestProperty("Accept-Language", "ne,en;q=0.8");
                int code = connection.getResponseCode();
                InputStream stream = code >= 400 ? connection.getErrorStream() : connection.getInputStream();
                if (stream == null) return "";
                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                byte[] chunk = new byte[8192];
                int read;
                int total = 0;
                while ((read = stream.read(chunk)) != -1 && total < 1_500_000) {
                    buffer.write(chunk, 0, read);
                    total += read;
                }
                stream.close();
                return buffer.toString("UTF-8");
            } catch (Exception error) {
                return "";
            } finally {
                if (connection != null) connection.disconnect();
            }
        }

        @JavascriptInterface
        public void share(String title, String text) {
            runOnUiThread(() -> {
                Intent send = new Intent(Intent.ACTION_SEND);
                send.setType("text/plain");
                send.putExtra(Intent.EXTRA_SUBJECT, title);
                send.putExtra(Intent.EXTRA_TEXT, text);
                startActivity(Intent.createChooser(send, title));
            });
        }
    }
}
