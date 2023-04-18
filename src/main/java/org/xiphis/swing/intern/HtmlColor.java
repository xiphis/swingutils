package org.xiphis.swing.intern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.TreeMap;

public final class HtmlColor {

    private static final Logger LOG = LoggerFactory.getLogger(HtmlColor.class);
    private static final TreeMap<String, Color> COLOR_MAP;

    private HtmlColor() {
    }

    public static Color getColor(String nm) {
        return getColor(nm, null);
    }
    public static Color getColor(String nm, Color c) {
        int start = 0;
        int end = nm.length();
        if (nm.endsWith(";")) {
            end--;
        }

        if (nm.startsWith("rgb(")) {
            end = nm.indexOf(')');
            if (end > 0) {
                String rgbString = nm.substring(4, end);
                String[] rgb = rgbString.split("\\s*,\\s*");
                if (rgb.length != 3) {
                    LOG.warn("bad rgb: {} .. {}", nm, rgbString);
                } else {
                    c = new Color(Integer.parseUnsignedInt(rgb[0]), Integer.parseUnsignedInt(rgb[1]), Integer.parseUnsignedInt(rgb[2]));
                }
            }
        } else if (nm.startsWith("#")) {
            start++;
            if (end - start == 3) {
                int cs = Integer.parseUnsignedInt(nm, start, end, 16);
                float r = ((cs >> 8) & 15) / 15f;
                float g = ((cs >> 4) & 15) / 15f;
                float b = ((cs >> 0) & 15) / 15f;
                c = new Color(r, g, b);
            } else if (end - start == 6) {
                c = Color.getColor(nm.substring(start, end), c);
            } else {
                LOG.warn("Bad color: {}", nm);
            }
        } else {
            c = COLOR_MAP.getOrDefault(nm.substring(start, end), c);
        }

        return c;
    }

    public static final Color transparent = new Color(0, 0, 0, 0);
    public static final Color aliceblue = new Color(0xf0f8ff);
    public static final Color antiquewhite = new Color(0xfaebd7);
    public static final Color aqua = new Color(0x00ffff);
    public static final Color aquamarine = new Color(0x7fffd4);
    public static final Color azure = new Color(0xf0ffff);
    public static final Color beige = new Color(0xf5f5dc);
    public static final Color bisque = new Color(0xffe4c4);
    public static final Color black = Color.black;
    public static final Color blanchedalmond = new Color(0xffebcd);
    public static final Color blue = new Color(0x0000ff);
    public static final Color blueviolet = new Color(0x8a2be2);
    public static final Color brown = new Color(0xa52a2a);
    public static final Color burlywood = new Color(0xdeb887);
    public static final Color cadetblue = new Color(0x5f9ea0);
    public static final Color chartreuse = new Color(0x7fff00);
    public static final Color chocolate = new Color(0xd2691e);
    public static final Color coral = new Color(0xff7f50);
    public static final Color cornflowerblue = new Color(0x6495ed);
    public static final Color cornsilk = new Color(0xfff8dc);
    public static final Color crimson = new Color(0xdc143c);
    public static final Color cyan = new Color(0x00ffff);
    public static final Color darkblue = new Color(0x00008b);
    public static final Color darkcyan = new Color(0x008b8b);
    public static final Color darkgoldenrod = new Color(0xb8860b);
    public static final Color darkgray = new Color(0xa9a9a9);
    public static final Color darkgreen = new Color(0x006400);
    public static final Color darkkhaki = new Color(0xbdb76b);
    public static final Color darkmagenta = new Color(0x8b008b);
    public static final Color darkolivegreen = new Color(0x556b2f);
    public static final Color darkorange = new Color(0xff8c00);
    public static final Color darkorchid = new Color(0x9932cc);
    public static final Color darkred = new Color(0x8b0000);
    public static final Color darksalmon = new Color(0xe9967a);
    public static final Color darkseagreen = new Color(0x8fbc8f);
    public static final Color darkslateblue = new Color(0x483d8b);
    public static final Color darkslategray = new Color(0x2f4f4f);
    public static final Color darkturquoise = new Color(0x00ced1);
    public static final Color darkviolet = new Color(0x9400d3);
    public static final Color deeppink = new Color(0xff1493);
    public static final Color deepskyblue = new Color(0x00bfff);
    public static final Color dimgray = new Color(0x696969);
    public static final Color dodgerblue = new Color(0x1e90ff);
    public static final Color firebrick = new Color(0xb22222);
    public static final Color floralwhite = new Color(0xfffaf0);
    public static final Color forestgreen = new Color(0x228b22);
    public static final Color fuchsia = new Color(0xff00ff);
    public static final Color gainsboro = new Color(0xdcdcdc);
    public static final Color ghostwhite = new Color(0xf8f8ff);
    public static final Color gold = new Color(0xffd700);
    public static final Color goldenrod = new Color(0xdaa520);
    public static final Color gray = Color.gray;
    public static final Color green = new Color(0x008000);
    public static final Color greenyellow = new Color(0xadff2f);
    public static final Color honeydew = new Color(0xf0fff0);
    public static final Color hotpink = new Color(0xff69b4);
    public static final Color indianred = new Color(0xcd5c5c);
    public static final Color indigo = new Color(0x4b0082);
    public static final Color ivory = new Color(0xfffff0);
    public static final Color khaki = new Color(0xf0e68c);
    public static final Color lavender = new Color(0xe6e6fa);
    public static final Color lavenderblush = new Color(0xfff0f5);
    public static final Color lawngreen = new Color(0x7cfc00);
    public static final Color lemonchiffon = new Color(0xfffacd);
    public static final Color lightblue = new Color(0xadd8e6);
    public static final Color lightcoral = new Color(0xf08080);
    public static final Color lightcyan = new Color(0xe0ffff);
    public static final Color lightgoldenrodyellow = new Color(0xfafad2);
    public static final Color lightgreen = new Color(0x90ee90);
    public static final Color lightgrey = new Color(0xd3d3d3);
    public static final Color lightgray = lightgrey;
    public static final Color lightpink = new Color(0xffb6c1);
    public static final Color lightsalmon = new Color(0xffa07a);
    public static final Color lightseagreen = new Color(0x20b2aa);
    public static final Color lightskyblue = new Color(0x87cefa);
    public static final Color lightslategray = new Color(0x778899);
    public static final Color lightsteelblue = new Color(0xb0c4de);
    public static final Color lightyellow = new Color(0xffffe0);
    public static final Color lime = Color.green;
    public static final Color limegreen = new Color(0x32cd32);
    public static final Color linen = new Color(0xfaf0e6);
    public static final Color magenta = Color.magenta;
    public static final Color maroon = new Color(0x800000);
    public static final Color mediumaquamarine = new Color(0x66cdaa);
    public static final Color mediumblue = new Color(0x0000cd);
    public static final Color mediumorchid = new Color(0xba55d3);
    public static final Color mediumpurple = new Color(0x9370db);
    public static final Color mediumseagreen = new Color(0x3cb371);
    public static final Color mediumslateblue = new Color(0x7b68ee);
    public static final Color mediumspringgreen = new Color(0x00fa9a);
    public static final Color mediumturquoise = new Color(0x48d1cc);
    public static final Color mediumvioletred = new Color(0xc71585);
    public static final Color midnightblue = new Color(0x191970);
    public static final Color mintcream = new Color(0xf5fffa);
    public static final Color mistyrose = new Color(0xffe4e1);
    public static final Color moccasin = new Color(0xffe4b5);
    public static final Color navajowhite = new Color(0xffdead);
    public static final Color navy = new Color(0x000080);
    public static final Color oldlace = new Color(0xfdf5e6);
    public static final Color olive = new Color(0x808000);
    public static final Color olivedrab = new Color(0x6b8e23);
    public static final Color orange = new Color(0xffa500);
    public static final Color orangered = new Color(0xff4500);
    public static final Color orchid = new Color(0xda70d6);
    public static final Color palegoldenrod = new Color(0xeee8aa);
    public static final Color palegreen = new Color(0x98fb98);
    public static final Color paleturquoise = new Color(0xafeeee);
    public static final Color palevioletred = new Color(0xdb7093);
    public static final Color papayawhip = new Color(0xffefd5);
    public static final Color peachpuff = new Color(0xffdab9);
    public static final Color peru = new Color(0xcd853f);
    public static final Color pink = new Color(0xffc0cb);
    public static final Color plum = new Color(0xdda0dd);
    public static final Color powderblue = new Color(0xb0e0e6);
    public static final Color purple = new Color(0x800080);
    public static final Color red = Color.red;
    public static final Color rosybrown = new Color(0xbc8f8f);
    public static final Color royalblue = new Color(0x4169e1);
    public static final Color saddlebrown = new Color(0x8b4513);
    public static final Color salmon = new Color(0xfa8072);
    public static final Color sandybrown = new Color(0xf4a460);
    public static final Color seagreen = new Color(0x2e8b57);
    public static final Color seashell = new Color(0xfff5ee);
    public static final Color sienna = new Color(0xa0522d);
    public static final Color silver = new Color(0xc0c0c0);
    public static final Color skyblue = new Color(0x87ceeb);
    public static final Color slateblue = new Color(0x6a5acd);
    public static final Color slategray = new Color(0x708090);
    public static final Color snow = new Color(0xfffafa);
    public static final Color springgreen = new Color(0x00ff7f);
    public static final Color steelblue = new Color(0x4682b4);
    public static final Color tan = new Color(0xd2b48c);
    public static final Color teal = new Color(0x008080);
    public static final Color thistle = new Color(0xd8bfd8);
    public static final Color tomato = new Color(0xff6347);
    public static final Color turquoise = new Color(0x40e0d0);
    public static final Color violet = new Color(0xee82ee);
    public static final Color wheat = new Color(0xf5deb3);
    public static final Color white = Color.white;
    public static final Color whitesmoke = new Color(0xf5f5f5);
    public static final Color yellow = Color.yellow;
    public static final Color yellowgreen = new Color(0x9acd32);

    static {
        COLOR_MAP = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

        try {
            for (Field f : HtmlColor.class.getDeclaredFields()) {
                if (!Modifier.isStatic(f.getModifiers())) {
                    continue;
                }
                if (Color.class.isAssignableFrom(f.getType())) {
                    COLOR_MAP.put(f.getName(), (Color) f.get(null));
                }
            }
        } catch (Exception e) {
            throw new Error(e);
        }
    }
}
