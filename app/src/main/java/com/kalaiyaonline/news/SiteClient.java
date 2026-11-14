package com.kalaiyaonline.news;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class SiteClient {
    public static final String ORIGIN = "https://kalaiyaonline.com";
    private static final String UA = "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36";

    public static final class Block {
        public final String label;
        public final String slug;
        public final List<Story> stories;

        Block(String label, String slug, List<Story> stories) {
            this.label = label;
            this.slug = slug;
            this.stories = stories;
        }
    }

    public static final class Home {
        public final String dateLine;
        public final List<Story> ticker;
        public final List<Story> headlines;
        public final List<Story> stories;
        public final List<Story> tabAll;
        public final List<Story> tabLocal;
        public final List<Story> tabNational;
        public final List<Story> tabWorld;
        public final List<Story> politics;
        public final List<Block> blocks;

        Home(String dateLine, List<Story> ticker, List<Story> headlines, List<Story> stories,
             List<Story> tabAll, List<Story> tabLocal, List<Story> tabNational, List<Story> tabWorld,
             List<Story> politics, List<Block> blocks) {
            this.dateLine = dateLine;
            this.ticker = ticker;
            this.headlines = headlines;
            this.stories = stories;
            this.tabAll = tabAll;
            this.tabLocal = tabLocal;
            this.tabNational = tabNational;
            this.tabWorld = tabWorld;
            this.politics = politics;
            this.blocks = blocks;
        }
    }

    public Home home() throws Exception {
        Document doc = page("/");
        String date = text(doc.selectFirst("#searchBsDateText"));
        List<Story> ticker = new ArrayList<>();
        Set<String> seenTicker = new LinkedHashSet<>();
        for (Element item : doc.select(".aks-ticker-item")) {
            Story story = fromLink(item, "ताजा");
            if (story == null || !seenTicker.add(story.slug) || ticker.size() >= 4) continue;
            ticker.add(story);
        }
        List<Story> stories = storiesIn(doc);
        if (ticker.size() < 4) {
            for (Story story : stories) {
                if (ticker.size() >= 4) break;
                if (!containsSlug(ticker, story.slug)) ticker.add(story);
            }
        }
        List<Story> headlines = new ArrayList<>();
        Element primary = doc.selectFirst("article.master-feature-card.is-primary");
        Story lead = storyFromElement(primary);
        if (lead != null) headlines.add(lead);
        for (Element article : doc.select("article.master-feature-card.is-secondary")) {
            Story story = storyFromElement(article);
            if (story == null || containsSlug(headlines, story.slug) || headlines.size() >= 2) continue;
            headlines.add(story);
        }
        for (Story story : stories) {
            if (headlines.size() >= 2) break;
            if (!"हेडलाइन".equals(story.category) && !headlines.isEmpty()) continue;
            if (containsSlug(headlines, story.slug)) continue;
            headlines.add(story);
        }
        for (Story story : stories) {
            if (headlines.size() >= 2) break;
            if (containsSlug(headlines, story.slug)) continue;
            headlines.add(story);
        }
        List<Story> tabAll = storiesFrom(doc.getElementById("master-tab-all"), 6);
        List<Story> tabLocal = storiesFrom(doc.getElementById("master-tab-local"), 6);
        List<Story> tabNational = storiesFrom(doc.getElementById("master-tab-national"), 6);
        List<Story> tabWorld = storiesFrom(doc.getElementById("master-tab-international"), 6);
        List<Story> politics = new ArrayList<>();
        List<Block> blocks = new ArrayList<>();
        for (Element section : doc.select("section.master-section")) {
            String aria = section.attr("aria-label");
            if (aria.startsWith("राजनीति")) {
                politics = storiesFrom(section, 4);
            } else if (aria.startsWith("अर्थ")) {
                addBlock(blocks, "अर्थ", "artha", section, 3);
            } else if (aria.startsWith("खेल")) {
                addBlock(blocks, "खेलकुद", "khelkud", section, 3);
            } else if (aria.contains("मनोरंजन")) {
                addBlock(blocks, "मनोरंजन", "entertainment", section, 3);
            } else if (aria.contains("लेख")) {
                addBlock(blocks, "लेख / बिचार", "lekhabichar", section, 3);
            }
        }
        return new Home(date, ticker, headlines, stories, tabAll, tabLocal, tabNational, tabWorld, politics, blocks);
    }

    private void addBlock(List<Block> blocks, String label, String slug, Element section, int limit) {
        List<Story> list = storiesFrom(section, limit);
        if (!list.isEmpty()) blocks.add(new Block(label, slug, list));
    }

    private List<Story> storiesFrom(Element scope, int limit) {
        List<Story> stories = new ArrayList<>();
        if (scope == null) return stories;
        Set<String> seen = new LinkedHashSet<>();
        for (Element article : scope.select("article")) {
            Story story = storyFromElement(article);
            if (story == null || !seen.add(story.slug)) continue;
            stories.add(story);
            if (stories.size() >= limit) break;
        }
        return stories;
    }

    public List<Story> search(String query) throws Exception {
        String q = java.net.URLEncoder.encode(query, "UTF-8");
        Document doc = page("/?search=1&q=" + q);
        List<Story> list = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        for (Element card : doc.select("a.search-result-card")) {
            String slug = slug(card.attr("href"));
            String title = text(card.selectFirst("h2, h3, b"));
            if (slug == null || title.length() < 4 || !seen.add(slug)) continue;
            String meta = text(card.selectFirst("small"));
            String category = meta;
            String date = "";
            int dot = meta.indexOf('·');
            if (dot > 0) {
                category = meta.substring(0, dot).trim();
                date = meta.substring(dot + 1).trim();
            }
            list.add(new Story(slug, title, imageFrom(card), category, date, "", ""));
            if (list.size() >= 20) break;
        }
        return list;
    }

    public List<Story> category(String slug) throws Exception {
        return storiesIn(page("/category/" + slug));
    }

    public Story article(String slug) throws Exception {
        Document doc = page("/news/" + slug);
        String title = text(doc.selectFirst("h1"));
        String image = imageFrom(doc.selectFirst("figure.article-hero-image"));
        if (image.isEmpty()) image = imageFrom(doc.body());
        String date = text(doc.selectFirst(".article-meta-date"));
        String category = text(doc.selectFirst(".article-category, .master-category, .nb1-cat"));
        if (category.isEmpty()) category = text(doc.selectFirst("h1") == null ? null : doc.selectFirst("h1").parent() == null ? null : doc.selectFirst("h1").parent().selectFirst("a[href*=/category/]"));
        if (category.isEmpty()) category = "समाचार";
        String author = text(doc.selectFirst(".article-reporter-name, .reporter-meta-name"));
        if (author.isEmpty()) author = "कलैया अनलाइन";
        StringBuilder body = new StringBuilder();
        for (Element p : doc.select(".article-content p")) {
            String line = p.text().replaceAll("\\s+", " ").trim();
            if (line.length() < 25) continue;
            if (body.length() > 0) body.append("\n\n");
            body.append(line);
        }
        return new Story(slug, title, image, category, date, author, body.toString());
    }

    public List<Story> gallery() throws Exception {
        Document doc = page("/gallery");
        List<Story> list = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        for (Element card : doc.select("a.gallery-card")) {
            String href = card.attr("href");
            String title = text(card.selectFirst("b"));
            if (title.isEmpty() || !seen.add(href)) continue;
            list.add(new Story(abs(href), title, imageFrom(card), "फोटो", "", "", ""));
        }
        return list;
    }

    public List<Story> directory() throws Exception {
        Document doc = page("/?directory=all");
        List<Story> list = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        for (Element card : doc.select("a[href*=/directory/]")) {
            String title = text(card.selectFirst("h2, h3, b"));
            if (title.length() < 2 || !seen.add(title)) continue;
            list.add(new Story(abs(card.attr("href")), title, imageFrom(card), "डाइरेक्टरी", "", "", ""));
        }
        return list;
    }

    public List<Story> reels() throws Exception {
        Document doc = page("/");
        List<Story> list = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        for (Element card : doc.select("a.web-story-card")) {
            Story story = fromLink(card, "रिल्स");
            if (story == null || !seen.add(story.slug)) continue;
            String image = imageFrom(card);
            list.add(new Story(story.slug, story.title, image, "रिल्स", "", "", ""));
            if (list.size() >= 12) break;
        }
        return list;
    }

    public static final class GalleryPost {
        public final String title;
        public final String category;
        public final String date;
        public final String body;
        public final String cover;
        public final List<String> photos;

        GalleryPost(String title, String category, String date, String body, String cover, List<String> photos) {
            this.title = title;
            this.category = category;
            this.date = date;
            this.body = body;
            this.cover = cover;
            this.photos = photos;
        }
    }

    public GalleryPost galleryPost(String url) throws Exception {
        Document doc = page(url);
        String title = text(doc.selectFirst("h1"));
        String category = text(doc.selectFirst(".gallery-breadcrumb b"));
        Element meta = doc.selectFirst(".gallery-meta-modern span");
        String date = text(meta);
        String body = text(doc.selectFirst(".gallery-single-head p"));
        String cover = imageFrom(doc.selectFirst(".gallery-single-cover"));
        List<String> photos = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        for (Element button : doc.select("[data-lightbox-src]")) {
            String src = abs(button.attr("data-lightbox-src"));
            if (!src.isEmpty() && seen.add(src)) photos.add(src);
        }
        if (photos.isEmpty() && !cover.isEmpty()) photos.add(cover);
        return new GalleryPost(title, category, date, body, cover, photos);
    }

    private List<Story> storiesIn(Document doc) {
        List<Story> stories = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        for (Element article : doc.select("article")) {
            Story story = storyFromElement(article);
            if (story == null || !seen.add(story.slug)) continue;
            stories.add(story);
            if (stories.size() >= 24) break;
        }
        return stories;
    }

    private Story storyFromElement(Element article) {
        if (article == null) return null;
        Element link = article.selectFirst("a[href*=/news/]");
        String href = link == null ? "" : link.attr("href");
        String slug = slug(href);
        if (slug == null) return null;
        String title = text(article.selectFirst("h1, h2, h3, h4"));
        if (title.length() < 8) return null;
        String category = text(article.selectFirst(".master-category, .category-card-badge, .ctnb-kicker, .nb1-cat"));
        String date = text(article.selectFirst(".master-meta-date, .category-card-meta, .ctnb-meta, .hlg-date"));
        String author = text(article.selectFirst(".reporter-meta-name"));
        return new Story(slug, title, imageFrom(article), category, date, author, "");
    }

    private static boolean containsSlug(List<Story> list, String slug) {
        for (Story story : list) if (story.slug.equals(slug)) return true;
        return false;
    }

    private Story fromLink(Element link, String category) {
        String slug = slug(link.attr("href"));
        String title = text(link.selectFirst(".aks-ticker-title"));
        if (title.isEmpty()) title = link.text().replaceAll("\\s+", " ").trim();
        if (slug == null || title.length() < 8) return null;
        return new Story(slug, title, imageFrom(link), category, "", "", "");
    }

    private String imageFrom(Element scope) {
        if (scope == null) return "";
        String best = "";
        int bestScore = -1;
        for (Element img : scope.select("img")) {
            String src = img.attr("src");
            if (src.isEmpty()) src = img.attr("data-src");
            if (src.isEmpty()) continue;
            String low = src.toLowerCase();
            if (low.contains("logo") || low.contains("favicon") || low.contains("/reporters/") || low.contains("/ads/")) continue;
            int score = 0;
            if (low.contains("imagekit.io")) score += 5;
            if (img.parent() != null && img.parent().className().contains("media")) score += 3;
            if (score >= bestScore) {
                bestScore = score;
                best = abs(src);
            }
        }
        return best;
    }

    private Document page(String path) throws Exception {
        return Jsoup.connect(path.startsWith("http") ? path : ORIGIN + path)
                .userAgent(UA)
                .header("Accept-Language", "ne,en;q=0.8")
                .referrer(ORIGIN + "/")
                .timeout(20000)
                .followRedirects(true)
                .get();
    }

    private static String text(Element element) {
        if (element == null) return "";
        return element.text().replaceAll("\\s+", " ").trim();
    }

    private static String slug(String href) {
        if (href == null) return null;
        int at = href.indexOf("/news/");
        if (at < 0) return null;
        String rest = href.substring(at + 6);
        int cut = rest.indexOf('/');
        if (cut >= 0) rest = rest.substring(0, cut);
        int q = rest.indexOf('?');
        if (q >= 0) rest = rest.substring(0, q);
        try {
            rest = java.net.URLDecoder.decode(rest, "UTF-8");
        } catch (Exception ignored) {
        }
        return rest.isEmpty() ? null : rest;
    }

    private static String abs(String href) {
        if (href == null || href.isEmpty()) return "";
        if (href.startsWith("http")) return href;
        if (href.startsWith("//")) return "https:" + href;
        if (!href.startsWith("/")) href = "/" + href;
        return ORIGIN + href;
    }
}
