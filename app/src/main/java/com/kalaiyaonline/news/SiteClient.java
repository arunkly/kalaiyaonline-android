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
        public final List<Story> popular;
        public final List<Block> blocks;

        Home(String dateLine, List<Story> ticker, List<Story> headlines, List<Story> stories,
             List<Story> tabAll, List<Story> tabLocal, List<Story> tabNational, List<Story> tabWorld,
             List<Story> politics, List<Story> popular, List<Block> blocks) {
            this.dateLine = dateLine;
            this.ticker = ticker;
            this.headlines = headlines;
            this.stories = stories;
            this.tabAll = tabAll;
            this.tabLocal = tabLocal;
            this.tabNational = tabNational;
            this.tabWorld = tabWorld;
            this.politics = politics;
            this.popular = popular;
            this.blocks = blocks;
        }
    }

    public int homeKbps = -1;
    public String homeSource = "";

    public Home home() throws Exception {
        return parsedHome(fetchHome());
    }

    public Home homeFrom(String html) {
        return parsedHome(Jsoup.parse(html == null ? "" : html, ORIGIN + "/"));
    }

    private Document fetchHome() throws Exception {
        Exception last = new Exception("home");
        for (int timeout : new int[]{35000, 60000}) {
            try {
                long start = System.nanoTime();
                org.jsoup.Connection.Response response = Jsoup.connect(ORIGIN + "/")
                        .userAgent(UA)
                        .header("Accept-Language", "ne,en;q=0.8")
                        .referrer(ORIGIN + "/")
                        .timeout(timeout)
                        .followRedirects(true)
                        .execute();
                long ms = Math.max(1L, (System.nanoTime() - start) / 1_000_000L);
                homeKbps = (int) Math.min(Integer.MAX_VALUE, (response.bodyAsBytes().length * 8L) / ms);
                homeSource = response.body();
                return response.parse();
            } catch (Exception error) {
                last = error;
            }
        }
        throw last;
    }

    private Home parsedHome(Document doc) {
        String date = text(doc.selectFirst("#searchBsDateText"));
        List<Story> stories = storiesIn(doc);
        List<Story> ticker = new ArrayList<>();
        for (Story story : stories) {
            if (ticker.size() >= 4) break;
            ticker.add(story);
        }
        if (ticker.size() < 4) {
            for (Element item : doc.select(".aks-ticker-item, .aks-ticker a")) {
                Story story = fromLink(item, "ताजा");
                if (story == null || containsSlug(ticker, story.slug) || ticker.size() >= 4) continue;
                ticker.add(story);
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
        return new Home(date, ticker, headlines, stories, tabAll, tabLocal, tabNational, tabWorld, politics, popularNear(doc, ""), blocks);
    }

    public static final class Sign {
        public final String name;
        public final String letters;
        public final String text;
        public final String color;
        public final String number;

        public final String time;

        Sign(String name, String letters, String text, String color, String number, String time) {
            this.name = name;
            this.letters = letters;
            this.text = text;
            this.color = color;
            this.number = number;
            this.time = time == null ? "" : time;
        }
    }

    public static final class Rashifal {
        public final String date;
        public final List<Sign> signs;

        Rashifal(String date, List<Sign> signs) {
            this.date = date;
            this.signs = signs;
        }
    }

    private static Rashifal rashifalCache;
    private static String rashifalDay;

    public Rashifal rashifal() throws Exception {
        String today = kathmanduDate();
        if (rashifalCache != null && today.equals(rashifalDay)) return rashifalCache;
        Document doc = page("https://kalaiyaonline.com/?rashifal=all");
        if (doc.title().contains("One moment")) throw new Exception("rashifal");
        String[] order = {"मेष", "वृष", "मिथुन", "कर्कट", "सिंह", "कन्या", "तुला", "वृश्चिक", "धनु", "मकर", "कुम्भ", "मीन"};
        String[] letters = {
                "चु, चे, चो, ला, लि, लु, ले, लो, अ",
                "इ, उ, ए, ओ, वा, वि, वु, वे, वो",
                "का, कि, कु, घ, ङ, छ, के, को, हा",
                "हि, हु, हे, हो, डा, डि, डु, डे, डो",
                "मा, मि, मु, मे, मो, टा, टि, टु, टे",
                "टो, पा, पि, पु, ष, ण, ठ, पे, पो",
                "रा, रि, रु, रे, रो, ता, ति, तु, ते",
                "तो, ना, नि, नु, ने, नो, या, यि, यु",
                "ये, यो, भा, भि, भु, धा, फा, ढा, भे",
                "भो, जा, जि, जु, जे, जो, ख, खि, खु",
                "गु, गे, गो, सा, सि, सु, से, सो, दा",
                "दि, दु, थ, झ, ञ, दे, दो, चा, चि"
        };
        java.util.Map<String, Sign> found = new java.util.LinkedHashMap<>();
        for (Element card : rashifalCards(doc)) {
            String blob = text(card);
            String name = rashiIn(blob);
            if (name.isEmpty() || found.containsKey(name)) continue;
            String reading = rashifalReading(blob, name);
            if (reading.length() < 12) continue;
            int index = java.util.Arrays.asList(order).indexOf(name);
            found.put(name, new Sign(name, index < 0 ? "" : letters[index], reading,
                    rashifalColor(blob), nepaliDigits(rashifalNumber(blob)), rashifalTime(blob)));
        }
        List<Sign> signs = new ArrayList<>();
        for (String name : order) if (found.containsKey(name)) signs.add(found.get(name));
        if (signs.size() < 8) throw new Exception("rashifal");
        rashifalCache = new Rashifal(today, signs);
        rashifalDay = today;
        return rashifalCache;
    }

    private List<Element> rashifalCards(Document doc) {
        List<Element> cards = new ArrayList<>();
        for (Element el : doc.select("[id^=rashi-]")) {
            Element card = el;
            for (int i = 0; i < 4 && text(card).length() < 50 && card.parent() != null; i++) card = card.parent();
            if (text(card).contains("राशि") && text(card).length() < 1200) cards.add(card);
        }
        for (Element el : doc.select("div, section, article, li")) {
            String body = text(el);
            if (body.length() < 40 || body.length() > 900 || !body.contains("राशि")) continue;
            if (!body.contains("भाग्य") && !body.contains("शुभ रंग") && !body.contains("भविष्यफल")) continue;
            boolean nested = false;
            for (Element child : el.children()) {
                String childText = text(child);
                if (childText.length() > 40 && childText.contains("राशि") && (childText.contains("भाग्य") || childText.contains("शुभ रंग"))) {
                    nested = true;
                    break;
                }
            }
            if (!nested) cards.add(el);
        }
        return cards;
    }

    private static String rashiIn(String blob) {
        String[] keys = {"वृश्चिक", "मिथुन", "कर्कट", "कन्या", "मेष", "वृष", "सिंह", "तुला", "धनु", "मकर", "कुम्भ", "मीन"};
        for (String key : keys) if (blob.contains(key)) return key;
        return "";
    }

    private static String rashifalReading(String blob, String name) {
        int cut = blob.length();
        for (String mark : new String[]{"भाग्यशाली", "शुभ रंग", "शुभ समय", "यो राशिफल", "सेयर गर्नु"}) {
            int at = blob.indexOf(mark);
            if (at > 8 && at < cut) cut = at;
        }
        String reading = blob.substring(0, cut)
                .replace(name + " राशि", "")
                .replace("आजको भविष्यफल", "")
                .replace(name, "")
                .replaceAll("[\\u2648-\\u2653]", "")
                .replaceAll("^[\\s\\S]{0,24}?सेयर\\s*", "")
                .replaceAll("^[\\s↗↑➤►▶]+", "");
        return reading.replaceAll("\\s+", " ").replaceAll("[\\s🍀☘🌿]+$", "").trim();
    }

    private static String rashifalNumber(String blob) {
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("अंक\\s*[:：]?\\s*([०-९0-9]+)").matcher(blob);
        return matcher.find() ? matcher.group(1) : "";
    }

    private static String rashifalColor(String blob) {
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("रंग\\s*[:：]?\\s*([^\\s०-९0-9,।*]+)").matcher(blob);
        if (!matcher.find()) return "";
        String color = matcher.group(1);
        if (color.equals("शुभ") || color.length() > 18) return "";
        return color;
    }

    private static String rashifalTime(String blob) {
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("समय\\s*[:：]?\\s*([0-9०-९]{1,2}[:ः.][0-9०-९]{2}\\s*[–—\\-]\\s*[0-9०-९]{1,2}[:ः.][0-9०-९]{2})").matcher(blob);
        if (!matcher.find()) return "";
        String digits = "०१२३४५६७८९";
        String raw = matcher.group(1);
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (c >= '0' && c <= '9') out.append(digits.charAt(c - '0'));
            else out.append(c);
        }
        return out.toString().replaceAll("\\s+", " ").trim();
    }

    public static String today() {
        return kathmanduDate();
    }

    private static String kathmanduDate() {
        java.util.Calendar calendar = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("Asia/Kathmandu"));
        return String.format(java.util.Locale.US, "%04d-%02d-%02d",
                calendar.get(java.util.Calendar.YEAR),
                calendar.get(java.util.Calendar.MONTH) + 1,
                calendar.get(java.util.Calendar.DAY_OF_MONTH));
    }

    private static String colorName(String key) {
        String id = key == null ? "" : key.toLowerCase();
        if (id.contains("red")) return "रातो";
        if (id.contains("blue")) return "निलो";
        if (id.contains("green")) return "हरियो";
        if (id.contains("yellow")) return "पहेंलो";
        if (id.contains("orange")) return "सुन्तला";
        if (id.contains("purple") || id.contains("violet")) return "बैजनी";
        if (id.contains("pink")) return "गुलाबी";
        if (id.contains("white")) return "सेतो";
        if (id.contains("black")) return "कालो";
        if (id.contains("gold")) return "सुनौलो";
        if (id.contains("silver") || id.contains("grey") || id.contains("gray")) return "खरानी";
        if (id.contains("brown")) return "खैरो";
        return key == null ? "" : key;
    }

    private static String nepaliDigits(String raw) {
        String digits = "०१२३४५६७८९";
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (c >= '0' && c <= '9') out.append(digits.charAt(c - '0'));
        }
        return out.toString();
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

    public static final class Feed {
        public final List<Story> stories;
        public final int pages;

        Feed(List<Story> stories, int pages) {
            this.stories = stories;
            this.pages = pages;
        }
    }

    public List<Story> category(String slug) throws Exception {
        return categoryPage(slug, 1).stories;
    }

    public Feed categoryPage(String slug, int pageNo) throws Exception {
        String path = "/category/" + slug + "?category=" + java.net.URLEncoder.encode(slug, "UTF-8") + "&view=&page_no=" + Math.max(1, pageNo);
        Document doc = page(path);
        int pages = 1;
        java.util.regex.Matcher marker = java.util.regex.Pattern.compile("पृष्ठ\\s*(\\d+)\\s*/\\s*(\\d+)").matcher(doc.body().text());
        List<Story> stories = storiesIn(doc);
        if (marker.find()) pages = Math.max(1, Integer.parseInt(marker.group(2)));
        else if (stories.size() >= 8) pages = pageNo + 1;
        return new Feed(stories, pages);
    }

    public List<Story> popular(String slug) throws Exception {
        return popularNear(page("/news/" + slug), slug);
    }

    private List<Story> popularNear(Document doc, String current) {
        for (Element section : doc.select("section, aside")) {
            String label = section.attr("aria-label") + " " + text(section.selectFirst("h2, h3, h4"));
            if (label.contains("लोकप्रिय") && !label.contains("ताजा")) {
                List<Story> scoped = storiesFrom(section, 7);
                if (scoped.size() >= 2) return scoped;
            }
        }
        Element mark = null;
        for (Element el : doc.getAllElements()) {
            String own = el.ownText().replaceAll("\\s+", " ").trim();
            if (own.equals("लोकप्रिय समाचार") || own.equals("लोकप्रिय")) {
                mark = el;
                break;
            }
        }
        if (mark == null) return new ArrayList<>();
        Element node = mark;
        List<Story> only = null;
        for (int depth = 0; depth < 5 && node != null; depth++) {
            List<List<Story>> groups = new ArrayList<>();
            Element sib = node.nextElementSibling();
            while (sib != null && groups.size() < 3) {
                List<List<Story>> nested = new ArrayList<>();
                for (Element child : sib.children()) {
                    List<Story> block = linksIn(child, current);
                    if (block.size() >= 2) nested.add(block);
                }
                if (nested.size() >= 2) return cap(nested.get(1));
                if (nested.size() == 1) groups.add(nested.get(0));
                else {
                    List<Story> block = linksIn(sib, current);
                    if (block.size() >= 2) groups.add(block);
                }
                sib = sib.nextElementSibling();
            }
            if (groups.size() >= 2) return cap(groups.get(1));
            if (groups.size() == 1 && only == null) only = groups.get(0);
            node = node.parent();
        }
        return trim(only);
    }

    private List<Story> cap(List<Story> list) {
        if (list == null || list.size() <= 7) return list == null ? new ArrayList<>() : list;
        return new ArrayList<>(list.subList(0, 7));
    }

    private List<Story> trim(List<Story> list) {
        if (list == null || list.isEmpty()) return new ArrayList<>();
        if (list.size() > 8) return new ArrayList<>(list.subList(7, Math.min(list.size(), 14)));
        if (list.size() > 7) return new ArrayList<>(list.subList(0, 7));
        return list;
    }

    private List<Story> linksIn(Element scope, String current) {
        List<Story> found = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        for (Element link : scope.select("a[href*=/news/]")) {
            String next = slug(link.attr("href"));
            if (next == null || next.equals(current) || !seen.add(next)) continue;
            String title = text(link.selectFirst("h2, h3, h4, b"));
            if (title.length() < 8) title = text(link);
            if (title.length() < 8) continue;
            String image = imageFrom(link);
            if (image.isEmpty() && link.parent() != null) image = imageFrom(link.parent());
            found.add(new Story(next, title, image, "", "", "", ""));
            if (found.size() >= 16) break;
        }
        return found;
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
        if (author.isEmpty()) author = text(doc.selectFirst("a[href*=reporter]"));
        if (author.isEmpty()) author = "कलैया अनलाइन";
        String views = "";
        java.util.regex.Matcher readers = java.util.regex.Pattern.compile("([०-९0-9][०-९0-9,]*)\\s*पाठक").matcher(doc.body().text());
        if (readers.find()) views = readers.group(1);
        StringBuilder body = new StringBuilder();
        List<Story.Piece> pieces = new ArrayList<>();
        readArticle(doc, body, pieces);
        Story story = new Story(slug, title, image, category, date, author, body.toString());
        story.pieces.addAll(pieces);
        story.views = views;
        story.authorImage = authorImage(doc);
        story.related.addAll(relatedFrom(doc, slug));
        tagsFrom(doc, story);
        feelingsFrom(doc, story);
        return story;
    }

    private void readArticle(Document doc, StringBuilder body, List<Story.Piece> pieces) {
        Element content = doc.selectFirst(".article-content");
        if (content == null) return;
        Element parent = content.parent();
        if (parent != null) {
            for (Element sibling : parent.children()) {
                if (sibling == content) break;
                Story.Piece ad = directAd(sibling);
                if (ad != null) pieces.add(ad);
            }
        }
        boolean lead = true;
        for (Element node : content.children()) {
            if ("p".equals(node.tagName())) {
                int before = pieces.size();
                addParagraph(node, body, pieces, lead);
                if (pieces.size() > before) lead = false;
            } else {
                Story.Piece ad = adPiece(node);
                if (ad != null) pieces.add(ad);
            }
        }
        if (parent == null) return;
        boolean passed = false;
        for (Element sibling : parent.children()) {
            if (sibling == content) {
                passed = true;
                continue;
            }
            if (!passed) continue;
            Story.Piece ad = adPiece(sibling);
            if (ad != null) pieces.add(ad);
        }
    }

    private void addParagraph(Element node, StringBuilder body, List<Story.Piece> pieces, boolean lead) {
        String line = node.text().replaceAll("\\s+", " ").trim();
        Element badge = node.selectFirst(".aks-first-word-badge");
        String word = badge == null ? "" : text(badge);
        if (line.length() < 25 && word.isEmpty()) return;
        String leadWord = "";
        if (lead) {
            leadWord = !word.isEmpty() ? word : firstWord(line);
            if (!leadWord.isEmpty() && !line.startsWith(leadWord)) leadWord = "";
        }
        pieces.add(new Story.Piece(line, leadWord, "", ""));
        if (line.isEmpty()) return;
        if (body.length() > 0) body.append("\n\n");
        body.append(line);
    }

    private Story.Piece directAd(Element node) {
        if (node == null || (!node.hasClass("ads-slot") && !node.hasClass("site-ad"))) return null;
        return adPiece(node);
    }

    private Story.Piece adPiece(Element node) {
        if (node == null) return null;
        Element slot = node.hasClass("ads-slot") || node.hasClass("site-ad") ? node : node.selectFirst(".ads-slot, .site-ad");
        if (slot == null) return null;
        Element image = slot.selectFirst("img");
        if (image == null) return null;
        String src = abs(image.attr("src"));
        if (src.isEmpty()) return null;
        Element link = slot.selectFirst("a[href]");
        String href = link == null ? "" : abs(link.attr("href"));
        return new Story.Piece("", "", src, href);
    }

    private static String firstWord(String line) {
        int space = line.indexOf(' ');
        if (space < 2 || space > 18) return "";
        return line.substring(0, space);
    }

    private void tagsFrom(Document doc, Story story) {
        Set<String> seen = new LinkedHashSet<>();
        for (Element link : doc.select("a[rel=tag], a[href*=/tag/], a[href*=/tags/], a[href*=?tag=], a[href*=&tag=], a[href*=/topic/], a[href*=keyword], a[class*=keyword], a[class*=chip], .article-tags a, .post-tags a, .news-tags a, .tag-list a, .tags a, a.tag, a.tag-chip, a.article-tag")) {
            addTag(story, seen, text(link), link.attr("href"));
        }
        if (!story.tags.isEmpty()) return;
        for (Element box : doc.select("[class*=tag]")) {
            String kind = box.className().toLowerCase();
            if (!kind.contains("tag")) continue;
            if (kind.contains("stage") || kind.contains("percentage")) continue;
            for (Element child : box.select("a, span")) {
                addTag(story, seen, child.ownText(), child.hasAttr("href") ? child.attr("href") : "");
            }
        }
        if (!story.tags.isEmpty()) return;
        for (Element head : doc.select("h2, h3, h4, h5, strong")) {
            String label = head.ownText().replaceAll("\\s+", " ").trim();
            if (!label.equals("ट्याग") && !label.equals("ट्यागहरू") && !label.equalsIgnoreCase("tags")) continue;
            Element box = head.parent() == null ? head : head.parent();
            for (Element link : box.select("a, span")) {
                if (link == head) continue;
                addTag(story, seen, text(link), link.hasAttr("href") ? link.attr("href") : "");
            }
        }
    }

    private void addTag(Story story, Set<String> seen, String label, String href) {
        String name = label == null ? "" : label.replaceAll("\\s+", " ").trim();
        if (name.length() < 2 || name.length() > 28) return;
        if (name.contains("सेयर") || name.contains("प्रतिक्रिया") || name.contains("टिप्पणी")) return;
        if (name.contains("%") || name.contains("महसुस")) return;
        if (!seen.add(name)) return;
        story.tags.add(name);
        story.tagUrls.add(href == null ? "" : abs(href));
        if (story.tags.size() > 8) {
            story.tags.remove(story.tags.size() - 1);
            story.tagUrls.remove(story.tagUrls.size() - 1);
        }
    }

    private void feelingsFrom(Document doc, Story story) {
        String[][] words = {
                {"मन पर्यो", "मन पऱ्यो"},
                {"माया लाग्यो"},
                {"हाँस्यो", "हांस्यो"},
                {"अचम्मित"},
                {"दुखी", "दुःखी"},
                {"रिस उठ्यो"}
        };
        Element box = null;
        int best = Integer.MAX_VALUE;
        for (Element el : doc.select("section, div, form, article")) {
            String blob = flat(text(el));
            if (!blob.contains("महसुस") && !blob.contains("माया लाग्यो")) continue;
            int hits = 0;
            for (String[] group : words) for (String word : group) if (blob.contains(flat(word))) { hits++; break; }
            if (hits >= 4 && blob.length() < best && blob.length() < 1600) {
                best = blob.length();
                box = el;
            }
        }
        String blob = box == null ? "" : flat(text(box));
        if (blob.isEmpty() && doc.body() != null) {
            String all = flat(doc.body().text());
            int at = all.indexOf("महसुस");
            if (at >= 0) blob = all.substring(Math.max(0, at - 10), Math.min(all.length(), at + 360));
        }
        for (int i = 0; i < words.length; i++) story.feelings[i] = feelingPercent(blob, words[i]);
        story.feelingNote = blob.contains("कुनै प्रतिक्रिया छैन") || feelingTotal(story) == 0
                ? "अहिलेसम्म कुनै प्रतिक्रिया छैन।" : "";
        if (box == null) return;
        for (Element button : box.select("button, a, [role=button]")) {
            String label = flat(text(button));
            if (label.length() > 40) continue;
            for (int i = 0; i < words.length; i++) {
                boolean hit = false;
                for (String word : words[i]) if (label.contains(flat(word))) hit = true;
                if (!hit || !story.feelingUrls[i].isEmpty()) continue;
                story.feelingUrls[i] = feelingTarget(button);
            }
        }
    }

    private static int feelingTotal(Story story) {
        int total = 0;
        for (int value : story.feelings) total += value;
        return total;
    }

    private String feelingTarget(Element button) {
        String href = button.attr("href");
        if (!href.isEmpty() && !href.startsWith("#") && !href.startsWith("javascript:")) return abs(href);
        Element form = button;
        while (form != null && !"form".equals(form.tagName())) form = form.parent();
        if (form != null && !form.attr("action").isEmpty()) return abs(form.attr("action"));
        String onclick = button.attr("onclick");
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("['\"]([^'\"]*(?:react|emoji|feel)[^'\"]*)['\"]").matcher(onclick);
        if (matcher.find()) return abs(matcher.group(1));
        return "";
    }

    private static int feelingPercent(String blob, String[] words) {
        for (String word : words) {
            String key = flat(word);
            int at = blob.indexOf(key);
            if (at < 0) continue;
            String tail = blob.substring(at + key.length(), Math.min(blob.length(), at + key.length() + 14));
            java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("([0-9०-९]+)").matcher(tail);
            if (matcher.find()) return toInt(matcher.group(1));
        }
        return 0;
    }

    private static int toInt(String raw) {
        String digits = "०१२३४५६७८९";
        StringBuilder number = new StringBuilder();
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            int at = digits.indexOf(c);
            if (c >= '0' && c <= '9') number.append(c);
            else if (at >= 0) number.append(at);
        }
        try {
            return Integer.parseInt(number.toString());
        } catch (Exception ignored) {
            return 0;
        }
    }

    private static String flat(String value) {
        return value == null ? "" : value.replace("\u200c", "").replace("\u200d", "").replaceAll("\\s+", " ").trim();
    }

    public void ping(String url) {
        try {
            if (url == null || url.isEmpty()) return;
            Jsoup.connect(url).userAgent(UA).ignoreContentType(true).timeout(20000).followRedirects(true).execute();
        } catch (Exception ignored) {
        }
    }

    private List<Story> relatedFrom(Document doc, String current) {
        List<Story> list = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        boolean after = false;
        for (Element el : doc.getAllElements()) {
            if (!after) {
                String label = el.ownText().replaceAll("\\s+", " ").trim();
                if (label.contains("सम्बन्धित समाचार")) after = true;
                continue;
            }
            if (!el.tagName().equals("a")) continue;
            String next = slug(el.attr("href"));
            if (next == null || next.equals(current) || !seen.add(next)) continue;
            String title = text(el.selectFirst("h2, h3, h4, b"));
            if (title.length() < 8) title = text(el);
            if (title.length() < 8) continue;
            String image = imageFrom(el);
            if (image.isEmpty() && el.parent() != null) image = imageFrom(el.parent());
            list.add(new Story(next, title, image, "", "", "", ""));
            if (list.size() >= 4) break;
        }
        return list;
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

    private String authorImage(Document doc) {
        Element img = doc.selectFirst("img[src*=/reporters/], img[data-src*=/reporters/], .article-reporter img, .reporter-meta img, .reporter-card img, a[href*=reporter] img");
        if (img == null) return "";
        String src = img.attr("src");
        if (src.isEmpty()) src = img.attr("data-src");
        return abs(src);
    }

    private static boolean containsSlug(List<Story> list, String slug) {
        for (Story story : list) if (story.slug.equals(slug)) return true;
        return false;
    }

    private Story fromLink(Element link, String category) {
        Element anchor = "a".equals(link.tagName()) ? link : link.selectFirst("a[href]");
        String href = anchor == null ? link.attr("href") : anchor.attr("href");
        String slug = slug(href);
        String title = text(link.selectFirst(".aks-ticker-title"));
        if (title.isEmpty() && anchor != null) title = text(anchor.selectFirst(".aks-ticker-title"));
        if (title.isEmpty()) title = (anchor == null ? link : anchor).text().replaceAll("\\s+", " ").trim();
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
        Exception last = new Exception(path);
        for (int timeout : new int[]{30000, 55000}) {
            try {
                return Jsoup.connect(path.startsWith("http") ? path : ORIGIN + path)
                        .userAgent(UA)
                        .header("Accept-Language", "ne,en;q=0.8")
                        .referrer(ORIGIN + "/")
                        .timeout(timeout)
                        .followRedirects(true)
                        .get();
            } catch (Exception error) {
                last = error;
            }
        }
        throw last;
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
