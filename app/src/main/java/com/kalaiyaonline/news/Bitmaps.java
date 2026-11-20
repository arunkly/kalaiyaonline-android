package com.kalaiyaonline.news;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

final class Bitmaps {
    private static File folder;

    private Bitmaps() {}

    static void use(File cache) {
        folder = new File(cache, "pics");
        if (!folder.exists()) folder.mkdirs();
    }

    static Bitmap load(String url, int maxSide) {
        if (url == null || url.isEmpty()) return null;
        String sized = sized(url, maxSide);
        File file = fileFor(sized);
        if (file != null && file.exists() && file.length() > 32) {
            Bitmap cached = decode(read(file), maxSide);
            if (cached != null) return cached;
        }
        byte[] data = download(sized);
        if (data == null && !sized.equals(url)) data = download(url);
        if (data == null) return null;
        if (file != null) write(file, data);
        return decode(data, maxSide);
    }

    private static byte[] download(String url) {
        for (int attempt = 0; attempt < 3; attempt++) {
            HttpURLConnection connection = null;
            try {
                connection = (HttpURLConnection) new URL(url).openConnection();
                connection.setInstanceFollowRedirects(true);
                connection.setRequestProperty("User-Agent", "Mozilla/5.0");
                connection.setConnectTimeout(attempt == 0 ? 20000 : 35000);
                connection.setReadTimeout(attempt == 0 ? 35000 : attempt == 1 ? 60000 : 90000);
                InputStream input = connection.getInputStream();
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                byte[] buffer = new byte[8192];
                int read;
                while ((read = input.read(buffer)) != -1) bytes.write(buffer, 0, read);
                input.close();
                if (bytes.size() > 32) return bytes.toByteArray();
            } catch (Exception ignored) {
            } finally {
                if (connection != null) connection.disconnect();
            }
        }
        return null;
    }

    private static Bitmap decode(byte[] data, int maxSide) {
        if (data == null || data.length == 0) return null;
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(data, 0, data.length, bounds);
        int sample = 1;
        int side = Math.max(bounds.outWidth, bounds.outHeight);
        if (side > 0) while (side / sample > maxSide) sample *= 2;
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = Math.max(1, sample);
        options.inPreferredConfig = maxSide > 1000 ? Bitmap.Config.ARGB_8888 : Bitmap.Config.RGB_565;
        return BitmapFactory.decodeByteArray(data, 0, data.length, options);
    }

    private static File fileFor(String url) {
        if (folder == null) return null;
        return new File(folder, Integer.toHexString(url.hashCode()) + ".img");
    }

    private static byte[] read(File file) {
        try (InputStream input = new java.io.FileInputStream(file)) {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int read;
            while ((read = input.read(buffer)) != -1) bytes.write(buffer, 0, read);
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

    private static String sized(String url, int maxSide) {
        if (url == null || !url.contains("imagekit.io") || url.contains("tr=") || url.contains("/tr:")) return url;
        int width = Math.max(280, Math.min(maxSide, 640));
        return url + (url.contains("?") ? "&" : "?") + "tr=w-" + width + ",q-40";
    }
}
