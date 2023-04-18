package org.xiphis.swing.intern;

import java.awt.font.TextAttribute;
import java.util.Collections;
import java.util.Map;

public final class HtmlStyle {
    static final Map<TextAttribute, String> TEXT_ATTRIBUTE_MONOSPACE = Collections.singletonMap(TextAttribute.FAMILY, "Monospaced");
    static final Map<TextAttribute, Float> TEXT_ATTRIBUTE_WEIGHT_BOLD = Collections.singletonMap(TextAttribute.WEIGHT, TextAttribute.WEIGHT_BOLD);
    static final Map<TextAttribute, Float> TEXT_ATTRIBUTE_WEIGHT_EXTRABOLD = Collections.singletonMap(TextAttribute.WEIGHT, TextAttribute.WEIGHT_EXTRABOLD);
    static final Map<TextAttribute, Float> TEXT_ATTRIBUTE_POSTURE_OBLIQUE = Collections.singletonMap(TextAttribute.POSTURE, TextAttribute.POSTURE_OBLIQUE);
    static final Map<TextAttribute, Integer> TEXT_ATTRIBUTE_SUPERSCRIPT_SUB = Collections.singletonMap(TextAttribute.SUPERSCRIPT, TextAttribute.SUPERSCRIPT_SUB);
    static final Map<TextAttribute, Integer> TEXT_ATTRIBUTE_SUPERSCRIPT_SUPER = Collections.singletonMap(TextAttribute.SUPERSCRIPT, TextAttribute.SUPERSCRIPT_SUPER);
    static final Map<TextAttribute, Boolean> TEXT_ATTRIBUTE_STRIKETHROUGH_ON = Collections.singletonMap(TextAttribute.STRIKETHROUGH, TextAttribute.STRIKETHROUGH_ON);
    static final Map<TextAttribute, Integer> TEXT_ATTRIBUTE_UNDERLINE_ON = Collections.singletonMap(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON);

    private HtmlStyle() {
    }
}
