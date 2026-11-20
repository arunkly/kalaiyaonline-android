package com.kalaiyaonline.news;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

final class Net {
    static volatile int lastKbps;
    private static File folder;

    private Net() {}

    static void use(File cache) {
        folder = new File(cache, "pages");
        if (!folder.exists()) folder.mkdirs();
    }

    static String text(String url) {
        return text(url, "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36");
    }

    static String text(String url, String agent) {
        if (url == null || url.isEmpty()) return null;
        File file = fileFor(url);
        boolean saved = file != null && file.exists() && file.length() > 40;
        int tries = saved ? 2 : 3;
        int[] connect = {20000, 30000, 40000};
        int[] read = {40000, 60000, 90000};
        for (int attempt = 0; attempt < tries; attempt++) {
            byte[] data = fetch(url, agent, connect[attempt], read[attempt]);
            if (data == null || data.length < 40) continue;
            if (file != null) write(file, data);
            return new String(data, StandardCharsets.UTF_8);
        }
        if (!saved) return null;
        byte[] cached = read(file);
        return cached == null ? null : new String(cached, StandardCharsets.UTF_8);
    }

    private static byte[] fetch(String url, String agent, int connect, int readTimeout) {
        HttpURLConnection connection = null;
        long start = System.nanoTime();
        try {
            connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setInstanceFollowRedirects(true);
            connection.setConnectTimeout(connect);
            connection.setReadTimeout(readTimeout);
            connection.setRequestProperty("User-Agent", agent);
            connection.setRequestProperty("Accept-Language", "ne,en;q=0.8");
            int code = connection.getResponseCode();
            if (code >= 400) return null;
            InputStream input = connection.getInputStream();
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int read;
            while ((read = input.read(buffer)) != -1) bytes.write(buffer, 0, read);
            input.close();
            long ms = Math.max(1L, (System.nanoTime() - start) / 1_000_000L);
            lastKbps = (int) Math.min(Integer.MAX_VALUE, (bytes.size() * 8L) / ms);
            return bytes.toByteArray();
        } catch (Exception ignored) {
            return null;
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    private static File fileFor(String url) {
        if (folder == null) return null;
        return new File(folder, Integer.toHexString(url.hashCode()) + ".html");
    }

    private static byte[] read(File file) {
        try (InputStream input = new FileInputStream(file)) {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int n;
            while ((n = input.read(buffer)) != -1) bytes.write(buffer, 0, n);
            return bytes.toByteArray();
        } catch (Exception ignored) {
            return null;
        }
    }

    private static void write(File file, byte[] data) {
        try (FileOutputStream output = new FileOutputStream(file)) {
            output.write(data);
        } catch (Exception ignored) {
        }
    }
}
