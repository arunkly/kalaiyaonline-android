package com.kalaiyaonline.news;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

final class PreetiConverter {
    private static final Map<Character, String> P2U = new HashMap<>();
    private static final Map<Character, String> U2P = new HashMap<>();
    private static final Map<Character, String> HAL = new HashMap<>();
    private static final List<Pattern> PATTERNS = new ArrayList<>();
    private static final List<String> REPLACEMENTS = new ArrayList<>();

    static {
        P2U.put("0".charAt(0), "ण्");
        P2U.put("1".charAt(0), "ज्ञ");
        P2U.put("2".charAt(0), "द्द");
        P2U.put("3".charAt(0), "घ");
        P2U.put("4".charAt(0), "द्ध");
        P2U.put("5".charAt(0), "छ");
        P2U.put("6".charAt(0), "ट");
        P2U.put("7".charAt(0), "ठ");
        P2U.put("8".charAt(0), "ड");
        P2U.put("9".charAt(0), "ढ");
        P2U.put("÷".charAt(0), "/");
        P2U.put("v".charAt(0), "ख");
        P2U.put("r".charAt(0), "च");
        P2U.put("\"".charAt(0), "ू");
        P2U.put("~".charAt(0), "ञ्");
        P2U.put("z".charAt(0), "श");
        P2U.put("ç".charAt(0), "ॐ");
        P2U.put("f".charAt(0), "ा");
        P2U.put("b".charAt(0), "द");
        P2U.put("n".charAt(0), "ल");
        P2U.put("j".charAt(0), "व");
        P2U.put("×".charAt(0), "×");
        P2U.put("V".charAt(0), "ख्");
        P2U.put("R".charAt(0), "च्");
        P2U.put("ß".charAt(0), "द्म");
        P2U.put("^".charAt(0), "६");
        P2U.put("Û".charAt(0), "!");
        P2U.put("Z".charAt(0), "श्");
        P2U.put("F".charAt(0), "ँ");
        P2U.put("B".charAt(0), "द्य");
        P2U.put("N".charAt(0), "ल्");
        P2U.put("Ë".charAt(0), "ङ्ग");
        P2U.put("J".charAt(0), "व्");
        P2U.put("¿".charAt(0), "रू");
        P2U.put(">".charAt(0), "श्र");
        P2U.put(":".charAt(0), "स्");
        P2U.put("§".charAt(0), "ट्ट");
        P2U.put("&".charAt(0), "७");
        P2U.put("£".charAt(0), "घ्");
        P2U.put("•".charAt(0), "ड्ड");
        P2U.put(".".charAt(0), "।");
        P2U.put("«".charAt(0), "्र");
        P2U.put("*".charAt(0), "८");
        P2U.put("„".charAt(0), "ध्र");
        P2U.put("w".charAt(0), "ध");
        P2U.put("s".charAt(0), "क");
        P2U.put("g".charAt(0), "न");
        P2U.put("æ".charAt(0), "“");
        P2U.put("c".charAt(0), "अ");
        P2U.put("o".charAt(0), "य");
        P2U.put("k".charAt(0), "प");
        P2U.put("W".charAt(0), "ध्");
        P2U.put("S".charAt(0), "क्");
        P2U.put("Ò".charAt(0), "¨");
        P2U.put("_".charAt(0), ")");
        P2U.put("[".charAt(0), "ृ");
        P2U.put("Ú".charAt(0), "’");
        P2U.put("G".charAt(0), "न्");
        P2U.put("ˆ".charAt(0), "फ्");
        P2U.put("C".charAt(0), "ऋ");
        P2U.put("O".charAt(0), "इ");
        P2U.put("Î".charAt(0), "ङ्ख");
        P2U.put("K".charAt(0), "प्");
        P2U.put("¶".charAt(0), "ठ्ठ");
        P2U.put("?".charAt(0), "रु");
        P2U.put(";".charAt(0), "स");
        P2U.put("'".charAt(0), "ु");
        P2U.put("#".charAt(0), "३");
        P2U.put("¢".charAt(0), "द्घ");
        P2U.put("/".charAt(0), "र");
        P2U.put("+".charAt(0), "ं");
        P2U.put("ª".charAt(0), "ङ");
        P2U.put("t".charAt(0), "त");
        P2U.put("p".charAt(0), "उ");
        P2U.put("|".charAt(0), "्र");
        P2U.put("x".charAt(0), "ह");
        P2U.put("å".charAt(0), "द्व");
        P2U.put("d".charAt(0), "म");
        P2U.put("`".charAt(0), "ञ");
        P2U.put("l".charAt(0), "ि");
        P2U.put("h".charAt(0), "ज");
        P2U.put("T".charAt(0), "त्");
        P2U.put("P".charAt(0), "ए");
        P2U.put("Ý".charAt(0), "ट्ठ");
        P2U.put("\\".charAt(0), "्");
        P2U.put("Ù".charAt(0), ";");
        P2U.put("X".charAt(0), "ह्");
        P2U.put("Å".charAt(0), "हृ");
        P2U.put("D".charAt(0), "म्");
        P2U.put("@".charAt(0), "२");
        P2U.put("Í".charAt(0), "ङ्क");
        P2U.put("L".charAt(0), "ी");
        P2U.put("H".charAt(0), "ज्");
        P2U.put("±".charAt(0), "+");
        P2U.put("<".charAt(0), "?");
        P2U.put("¥".charAt(0), "र्‍");
        P2U.put("$".charAt(0), "४");
        P2U.put("¡".charAt(0), "ज्ञ्");
        P2U.put(",".charAt(0), ",");
        P2U.put("©".charAt(0), "र");
        P2U.put("(".charAt(0), "९");
        P2U.put("‘".charAt(0), "ॅ");
        P2U.put("u".charAt(0), "ग");
        P2U.put("q".charAt(0), "त्र");
        P2U.put("}".charAt(0), "ै");
        P2U.put("y".charAt(0), "थ");
        P2U.put("e".charAt(0), "भ");
        P2U.put("a".charAt(0), "ब");
        P2U.put("i".charAt(0), "ष्");
        P2U.put("U".charAt(0), "ग्");
        P2U.put("Q".charAt(0), "त्त");
        P2U.put("]".charAt(0), "े");
        P2U.put("˜".charAt(0), "ऽ");
        P2U.put("Y".charAt(0), "थ्");
        P2U.put("Ø".charAt(0), "्य");
        P2U.put("E".charAt(0), "भ्");
        P2U.put("A".charAt(0), "ब्");
        P2U.put("M".charAt(0), "ः");
        P2U.put("Ì".charAt(0), "न्न");
        P2U.put("I".charAt(0), "क्ष्");
        P2U.put("´".charAt(0), "झ");
        P2U.put("°".charAt(0), "ङ्ढ");
        P2U.put("=".charAt(0), ".");
        P2U.put("Æ".charAt(0), "”");
        P2U.put("‹".charAt(0), "ङ्घ");
        P2U.put("%".charAt(0), "५");
        P2U.put("¤".charAt(0), "झ्");
        P2U.put("!".charAt(0), "१");
        P2U.put("-".charAt(0), "(");
        P2U.put("›".charAt(0), "द्र");
        P2U.put(")".charAt(0), "०");
        P2U.put("…".charAt(0), "‘");
        P2U.put("Ü".charAt(0), "%");
        U2P.put("अ".charAt(0), "c");
        U2P.put("आ".charAt(0), "cf");
        U2P.put("ा".charAt(0), "f");
        U2P.put("इ".charAt(0), "O");
        U2P.put("ई".charAt(0), "O{");
        U2P.put("उ".charAt(0), "p");
        U2P.put("ए".charAt(0), "P");
        U2P.put("े".charAt(0), "]");
        U2P.put("ै".charAt(0), "}");
        U2P.put("ो".charAt(0), "f]");
        U2P.put("ौ".charAt(0), "f}");
        U2P.put("ओ".charAt(0), "cf]");
        U2P.put("औ".charAt(0), "cf}");
        U2P.put("ं".charAt(0), "+");
        U2P.put("ँ".charAt(0), "F");
        U2P.put("ि".charAt(0), "l");
        U2P.put("ी".charAt(0), "L");
        U2P.put("ु".charAt(0), "'");
        U2P.put("ू".charAt(0), "\"");
        U2P.put("क".charAt(0), "s");
        U2P.put("ख".charAt(0), "v");
        U2P.put("ग".charAt(0), "u");
        U2P.put("घ".charAt(0), "3");
        U2P.put("ङ".charAt(0), "ª");
        U2P.put("च".charAt(0), "r");
        U2P.put("छ".charAt(0), "5");
        U2P.put("ज".charAt(0), "h");
        U2P.put("झ".charAt(0), "´");
        U2P.put("ञ".charAt(0), "`");
        U2P.put("ट".charAt(0), "6");
        U2P.put("ठ".charAt(0), "7");
        U2P.put("ड".charAt(0), "8");
        U2P.put("ढ".charAt(0), "9");
        U2P.put("ण".charAt(0), "0f");
        U2P.put("त".charAt(0), "t");
        U2P.put("थ".charAt(0), "y");
        U2P.put("द".charAt(0), "b");
        U2P.put("ध".charAt(0), "w");
        U2P.put("न".charAt(0), "g");
        U2P.put("प".charAt(0), "k");
        U2P.put("फ".charAt(0), "km");
        U2P.put("ब".charAt(0), "a");
        U2P.put("भ".charAt(0), "e");
        U2P.put("म".charAt(0), "d");
        U2P.put("य".charAt(0), "o");
        U2P.put("र".charAt(0), "/");
        U2P.put("ृ".charAt(0), "[");
        U2P.put("ल".charAt(0), "n");
        U2P.put("व".charAt(0), "j");
        U2P.put("स".charAt(0), ";");
        U2P.put("श".charAt(0), "z");
        U2P.put("ष".charAt(0), "if");
        U2P.put("ह".charAt(0), "x");
        U2P.put("१".charAt(0), "!");
        U2P.put("२".charAt(0), "@");
        U2P.put("३".charAt(0), "#");
        U2P.put("४".charAt(0), "$");
        U2P.put("५".charAt(0), "%");
        U2P.put("६".charAt(0), "^");
        U2P.put("७".charAt(0), "&");
        U2P.put("८".charAt(0), "*");
        U2P.put("९".charAt(0), "(");
        U2P.put("०".charAt(0), ")");
        U2P.put("।".charAt(0), ".");
        U2P.put("्".charAt(0), "\\");
        U2P.put("ऊ".charAt(0), "pm");
        U2P.put("(".charAt(0), "- ");
        U2P.put(")".charAt(0), "_");
        HAL.put("क".charAt(0), "s");
        HAL.put("ख".charAt(0), "v");
        HAL.put("ग".charAt(0), "u");
        HAL.put("घ".charAt(0), "3");
        HAL.put("ङ".charAt(0), "ª");
        HAL.put("च".charAt(0), "r");
        HAL.put("छ".charAt(0), "5");
        HAL.put("ज".charAt(0), "h");
        HAL.put("झ".charAt(0), "´");
        HAL.put("ञ".charAt(0), "`");
        HAL.put("ट".charAt(0), "6");
        HAL.put("ठ".charAt(0), "7");
        HAL.put("ड".charAt(0), "8");
        HAL.put("ढ".charAt(0), "9");
        HAL.put("ण".charAt(0), "0f");
        HAL.put("त".charAt(0), "t");
        HAL.put("थ".charAt(0), "y");
        HAL.put("द".charAt(0), "b");
        HAL.put("ध".charAt(0), "w");
        HAL.put("न".charAt(0), "g");
        HAL.put("प".charAt(0), "k");
        HAL.put("फ".charAt(0), "k");
        HAL.put("ब".charAt(0), "a");
        HAL.put("भ".charAt(0), "e");
        HAL.put("म".charAt(0), "d");
        HAL.put("य".charAt(0), "o");
        HAL.put("ल".charAt(0), "n");
        HAL.put("व".charAt(0), "j");
        HAL.put("श".charAt(0), "z");
        HAL.put("ष".charAt(0), "i");
        HAL.put("स".charAt(0), ";");
        HAL.put("ह".charAt(0), "x");

        rule("्ा", "");
        rule("((त्र|त्त)[^उभप]+?)m", "$1m$2");
        rule("त्रm", "क्र");
        rule("त्तm", "क्त");
        rule("([^उभप]+?)m", "m$1");
        rule("उm", "ऊ");
        rule("भm", "झ");
        rule("पm", "फ");
        rule("इ\\{", "ई");
        rule("ि((.्)*[^्])", "$1ि");
        rule("(.[ािीुूृेैोौंःँ]*?)\\{", "{$1");
        rule("((.्)*)\\{", "{$1");
        rule("\\{", "र्");
        rule("([ाीुूृेैोौंःँ]+?)(्(.्)*[^्])", "$2$1");
        rule("्([ाीुूृेैोौः]*?)((.्)*[^्])", "्$2$1");
        rule("([ंँ])([ािीुूृेैोौः]*)", "$2$1");
        rule("ँँ", "ँ");
        rule("ंं", "ं");
        rule("ेे", "े");
        rule("ैै", "ै");
        rule("ुु", "ु");
        rule("ूू", "ू");
        rule("^ः", ":");
        rule("टृ", "ट्ट");
        rule("ेा", "ाे");
        rule("ैा", "ाै");
        rule("अाे", "ओ");
        rule("अाै", "औ");
        rule("अा", "आ");
        rule("एे", "ऐ");
        rule("ाे", "ो");
        rule("ाै", "ौ");

    }

    private static void rule(String source, String replacement) {
        PATTERNS.add(Pattern.compile(source));
        REPLACEMENTS.add(replacement);
    }

    private PreetiConverter() {}

    static String preetiToUnicode(String text) {
        if (text == null) return "";
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            String mapped = P2U.get(ch);
            out.append(mapped == null ? String.valueOf(ch) : mapped);
        }
        String value = out.toString();
        for (int i = 0; i < PATTERNS.size(); i++) value = PATTERNS.get(i).matcher(value).replaceAll(REPLACEMENTS.get(i));
        return value;
    }

    static String unicodeToPreeti(String text) {
        text = normalize(text == null ? "" : text);
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (ch == '\uFEFF') continue;
            if (ch == 'र' && at(text, i + 1) == '्' && i + 2 < text.length()) {
                char next = text.charAt(i + 2);
                if (next == 'ि' && i + 3 < text.length()) {
                    out.append(map(next)).append(map(text.charAt(i + 3))).append('{');
                    i += 3;
                    continue;
                }
                if (i + 3 < text.length() && isMatra(text.charAt(i + 3))) {
                    out.append(map(next)).append(map(text.charAt(i + 3))).append('{');
                    i += 3;
                    continue;
                }
                out.append(map(next)).append('{');
                i += 2;
                continue;
            }
            if (at(text, i + 1) == 'ि') {
                if (ch == 'त' && at(text, i + 2) == '्' && at(text, i + 3) == 'त') {
                    out.append("lTt");
                    i += 3;
                    continue;
                }
                out.append('l').append(map(ch));
                i++;
                continue;
            }
            String pair = i + 1 < text.length() ? text.substring(i, i + 2) : "";
            String glyph = glyph(pair);
            if (glyph != null) {
                out.append(glyph);
                i++;
                continue;
            }
            if (ch == '्' && i + 1 < text.length()) {
                char prev = at(text, i - 1);
                if (text.charAt(i + 1) == 'र' && HAL.containsKey(prev)) {
                    String rb = HAL.get(prev);
                    trim(out, rb.length());
                    out.append(rb).append(prev == 'ट' || prev == 'ठ' || prev == 'ड' ? '«' : '|');
                    i++;
                    continue;
                }
                if (prev == 'ण') {
                    trim(out, 2);
                    out.append('0');
                    continue;
                }
                if (HAL.containsKey(prev)) {
                    String base = HAL.get(prev);
                    String upper = base.length() == 1 ? base.toUpperCase(Locale.ROOT) : base;
                    trim(out, base.length());
                    out.append(upper);
                    continue;
                }
            }
            out.append(map(ch));
        }
        return out.toString()
                .replace("Si", "I")
                .replace("H`", "1")
                .replace("b\\w", "4")
                .replace("z|", ">")
                .replace("/'", "?")
                .replace("/\"", "¿")
                .replace("Tt", "Q")
                .replace("b\\lj", "lå")
                .replace("b\\j", "å")
                .replace("0f\\", "0")
                .replace("`\\", "~");
    }

    private static String glyph(String pair) {
        if ("त्र".equals(pair)) return "q";
        if ("त्त".equals(pair)) return "Q";
        if ("ज्ञ".equals(pair)) return "1";
        if ("क्ष".equals(pair)) return "I";
        if ("श्र".equals(pair)) return ">";
        if ("द्व".equals(pair)) return "å";
        if ("द्म".equals(pair)) return "ß";
        if ("रु".equals(pair)) return "?";
        if ("रू".equals(pair)) return "¿";
        return null;
    }

    private static String normalize(String text) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (ch == 'र' && at(text, i + 1) == '्' && i + 2 < text.length() && text.charAt(i + 2) != ' ') {
                out.append("र्");
                i++;
                continue;
            }
            out.append(ch);
        }
        return out.toString();
    }

    private static boolean isMatra(char ch) {
        return "ािीुूृेैोौंःँ".indexOf(ch) >= 0;
    }

    private static char at(String text, int index) {
        return index >= 0 && index < text.length() ? text.charAt(index) : 0;
    }

    private static String map(char ch) {
        String value = U2P.get(ch);
        return value == null ? String.valueOf(ch) : value;
    }

    private static void trim(StringBuilder out, int length) {
        if (length > 0 && out.length() >= length) out.setLength(out.length() - length);
    }
}
