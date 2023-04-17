package org.xiphis.swing;

import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.font.TextAttribute;
import java.net.URL;
import java.text.AttributedCharacterIterator;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Vector;
import java.util.function.IntBinaryOperator;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HtmlPanel extends JPanel {
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    private static final Map<TextAttribute, String> TEXT_ATTRIBUTE_MONOSPACE = Collections.singletonMap(TextAttribute.FAMILY, "Monospaced");
    private static final Map<TextAttribute, Float> TEXT_ATTRIBUTE_WEIGHT_BOLD = Collections.singletonMap(TextAttribute.WEIGHT, TextAttribute.WEIGHT_BOLD);
    private static final Map<TextAttribute, Float> TEXT_ATTRIBUTE_WEIGHT_EXTRABOLD = Collections.singletonMap(TextAttribute.WEIGHT, TextAttribute.WEIGHT_EXTRABOLD);
    private static final Map<TextAttribute, Float> TEXT_ATTRIBUTE_POSTURE_OBLIQUE = Collections.singletonMap(TextAttribute.POSTURE, TextAttribute.POSTURE_OBLIQUE);
    private static final Map<TextAttribute, Integer> TEXT_ATTRIBUTE_SUPERSCRIPT_SUB = Collections.singletonMap(TextAttribute.SUPERSCRIPT, TextAttribute.SUPERSCRIPT_SUB);
    private static final Map<TextAttribute, Integer> TEXT_ATTRIBUTE_SUPERSCRIPT_SUPER = Collections.singletonMap(TextAttribute.SUPERSCRIPT, TextAttribute.SUPERSCRIPT_SUPER);
    private static final Map<TextAttribute, Boolean> TEXT_ATTRIBUTE_STRIKETHROUGH_ON = Collections.singletonMap(TextAttribute.STRIKETHROUGH, TextAttribute.STRIKETHROUGH_ON);
    private static final Map<TextAttribute, Integer> TEXT_ATTRIBUTE_UNDERLINE_ON = Collections.singletonMap(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON);


    private final Logger log = LoggerFactory.getLogger(getClass());

    private final Element body;
    private final HtmlContext context;

    public HtmlPanel(String html) {
        this(new HtmlContext(html, true));
    }
    private HtmlPanel(HtmlContext context) {
        this(context, context.document());
    }
    public HtmlPanel(HtmlContext context, Element body) {
        this(context, body, null);
    }

    HtmlPanel(HtmlContext context, Element body, Border border) {
        this.body = Objects.requireNonNull(body);
        this.context = Objects.requireNonNull(context);

        String title = null;
        if (body.hasAttr("title")) {
            title = body.attr("title");
        }

        if (body.hasAttr("border")) {
            border = context().parseBorder(body.attr("border"), border);
            if (border != null) {
                if (title != null && !title.isBlank()) {
                    border = BorderFactory.createTitledBorder(border, title);
                    title = null;
                }
            }
        }

        if (title != null) {
            setToolTipText(title);
        }

        switch (body.tagName()) {
            case "table": {
                if (border == null) {
                    border = BorderFactory.createLineBorder(Color.BLACK);
                }
                renderTable(border);
                break;
            }
            case "ul":
            case "ol": {
                setLayout(new GridBagLayout());
                int y = 0;
                for (Element el : elementsByTag(body, "li")) {
                    GridBagConstraints c = new GridBagConstraints();
                    c.gridx = 0;
                    c.gridy = y;
                    String text = body.tagName().equals("ul") ? "\u2202" : String.valueOf(y+1);
                    add(this, new JLabel(text), c, el, null);

                    c = new GridBagConstraints();
                    c.gridx = 1;
                    c.gridy = y;
                    c.fill = GridBagConstraints.HORIZONTAL;
                    add(this, new HtmlPanel(context, el), c, el, null);
                    y++;
                }
                break;
            }

            case "dl": {
                setLayout(new GridBagLayout());
                int y = 0; boolean indent = false;
                for (Element el : elementsByTag(body, tag -> {
                    switch (tag) {
                        case "dt":
                        case "dd":
                            return true;
                        default:
                            return false;
                    }
                })) {
                    GridBagConstraints c = new GridBagConstraints();
                    switch (el.tagName()) {
                        case "dt":
                            c.gridwidth = 2;
                            c.gridx = 0;
                            break;
                        case "dd":
                            c.gridy = 1;
                            if (!indent) {
                                indent = true;
                                GridBagConstraints c2 = new GridBagConstraints();
                                c2.gridy = c.gridy;
                                c2.gridx = 0;
                                add(new JLabel(" "), c2);
                            }
                            break;
                        default:
                            continue;
                    }
                    c.gridy = y++;
                    add(this, new HtmlPanel(context, el), c, el, null);
                }
                break;
            }

            default:
                if (body.children().stream()
                        .map(Element::nodeName)
                        .anyMatch(name -> {
                            switch (name) {
                                case "header":
                                case "nav":
                                case "section":
                                case "article":
                                case "aside":
                                case "footer":
                                    return true;
                                default:
                                    return false;
                            }
                        })) {
                    renderArticles();
                    break;
                }

                setLayout(new HtmlLayout());
                renderContent(this, body, new Attr(getFont(), getForeground(), null));
                break;
        }
        if (border != null) {
            setBorder(border);
        }
    }

    private void renderArticles() {
        setLayout(new GridBagLayout());

        int y = 0;

        for (Element el : elementsByTag(body, "header")) {
            GridBagConstraints c = new GridBagConstraints();
            c.gridx = 0;
            c.gridwidth = 2;
            c.gridy = y++;
            c.gridheight = 1;
            c.fill = GridBagConstraints.HORIZONTAL;

            add(this, new HtmlPanel(context, el), c, el, null);
        }

        JPanel nav = null;
        for (Element el : elementsByTag(body, "nav")) {
            if (nav == null) {
                GridBagConstraints c = new GridBagConstraints();
                c.gridx = 0;
                c.gridwidth = 2;
                c.gridy = y++;
                c.gridheight = 1;
                //c.fill = GridBagConstraints.HORIZONTAL;
                c.anchor = GridBagConstraints.CENTER;
                nav = new JPanel(new FlowLayout());
                add(this, nav, c, el, null);
            }
            add(nav, new HtmlPanel(context, el), null, el, null);
        }

        int asidey = y;

        for (Element el : elementsByTag(body, "section")) {
            GridBagConstraints c = new GridBagConstraints();
            c.gridx = 0;
            c.gridwidth = 1;
            c.gridy = y++;
            c.gridheight = 1;
            c.fill = GridBagConstraints.HORIZONTAL;

            add(this, new HtmlPanel(context, el), c, el, null);
        }

        for (Element el : elementsByTag(body, "article")) {
            GridBagConstraints c = new GridBagConstraints();
            c.gridx = 0;
            c.gridwidth = 1;
            c.gridy = y++;
            c.gridheight = 1;
            c.fill = GridBagConstraints.HORIZONTAL;
            add(this, new HtmlPanel(context, el), c, el, null);
        }

        JPanel aside = null;
        for (Element el : elementsByTag(body, "aside")) {
            if (aside == null) {
                GridBagConstraints c = new GridBagConstraints();
                c.gridx = 1;
                c.gridwidth = 1;
                c.gridy = asidey;
                if (asidey == y) {
                    y++;
                }
                c.gridheight = y - asidey;
                c.fill = GridBagConstraints.BOTH;
                aside = new JPanel();
                aside.setLayout(new BoxLayout(aside, BoxLayout.Y_AXIS));
                add(this, aside, c, el, null);
            }
            add(aside, new HtmlPanel(context, el), null, el, null);
        }
        for (Element el : elementsByTag(body, "footer")) {
            GridBagConstraints c = new GridBagConstraints();
            c.gridx = 0;
            c.gridwidth = 2;
            c.gridy = y++;
            c.gridheight = 1;
            c.fill = GridBagConstraints.HORIZONTAL;
            add(this, new HtmlPanel(context, el), c, el, null);
        }
    }

    private void renderTable(Border border) {
        setLayout(new GridBagLayout());
        int y = 0;
        for (Element thead : elementsByTag(body, "thead")) {
            for (Element el : elementsByTag(thead, "tr")) {
                int x = 0;
                for (Element d : elementsByTag(el, "th")) {
                    GridBagConstraints c = new GridBagConstraints();
                    c.gridx = x++;
                    c.gridy = y;
                    c.fill = GridBagConstraints.BOTH;
                    if (d.hasAttr("colspan")) {
                        c.gridwidth = Integer.parseUnsignedInt(d.attr("colspan"));
                    }
                    if (d.hasAttr("rowspan")) {
                        c.gridheight = Integer.parseUnsignedInt(d.attr("rowspan"));
                    }
                    add(this, new HtmlPanel(context, d, border), c, d, null);
                }
            }
        }
        for (Element tbody : elementsByTag(body, "tbody")) {
            for (Element el : elementsByTag(tbody, "tr")) {
                int x = 0;
                for (Element d : elementsByTag(el, tag -> {
                    switch (tag) {
                        case "th":
                        case "td":
                            return true;
                        default:
                            return false;
                    }
                })) {
                    GridBagConstraints c = new GridBagConstraints();
                    c.gridx = x++;
                    c.gridy = y;
                    c.fill = GridBagConstraints.BOTH;
                    if (d.hasAttr("colspan")) {
                        c.gridwidth = Integer.parseUnsignedInt(d.attr("colspan"));
                    }
                    if (d.hasAttr("rowspan")) {
                        c.gridheight = Integer.parseUnsignedInt(d.attr("rowspan"));
                    }
                    add(this, new HtmlPanel(context, d, border), c, d, null);
                }
                y++;
            }
        }
        for (Element tfoot : elementsByTag(body, "tfoot")) {
            for (Element el : elementsByTag(tfoot, "tr")) {
                int x = 0;
                for (Element d : elementsByTag(el, tag -> {
                    switch (tag) {
                        case "th":
                        case "td":
                            return true;
                        default:
                            return false;
                    }
                })) {
                    GridBagConstraints c = new GridBagConstraints();
                    c.gridx = x++;
                    c.gridy = y;
                    c.fill = GridBagConstraints.BOTH;
                    if (d.hasAttr("colspan")) {
                        c.gridwidth = Integer.parseUnsignedInt(d.attr("colspan"));
                    }
                    if (d.hasAttr("rowspan")) {
                        c.gridheight = Integer.parseUnsignedInt(d.attr("rowspan"));
                    }
                    add(this, new HtmlPanel(context, d, border), c, d, null);
                }
                y++;
            }
        }
    }

    private void add(JPanel panel, JComponent comp, Node n, Attr attr) {
        add(panel, comp, n, n, attr);
    }

    private void add(JPanel panel, JComponent comp, Object constraint,
                     Node n, Attr attr) {
        if (attr != null) {
            attr.apply(comp);
        }
        panel.add(comp, constraint);
        context.applyStyle(comp, n);
        if (attr != null) {
            attr.font = comp.getFont();
            attr.fgColor = comp.getForeground();
            attr.bgColor = comp.getBackground();
        }
    }

    private void renderContent(JPanel panel, Element body, Attr attr) {
        for (Node n : body.childNodes()) {
            if (n instanceof Element) {
                Element el = (Element) n;
                switch (el.tagName()) {
                    case "br": {
                        add(panel, new JLabel(" "), el, attr.copy());
                        continue;
                    }
                    case "hr": {
                        add(panel, new JSeparator(), el, attr.copy());
                        continue;
                    }
                    case "table":
                        add(panel, new HtmlPanel(context, el), el,  attr.copy());
                        continue;
                    case "img": {
                        try {
                            URL imageUrl = getClass().getResource(el.attr("src"));
                            if (imageUrl == null) {
                                log.warn("Unable to find resource for {}", el.html());
                            } else {
                                ImageIcon imageIcon = new ImageIcon(imageUrl);

                                if (el.hasAttr("width") || el.hasAttr("height")) {
                                    // TODO
                                }

                                JLabel label = new JLabel(imageIcon);
                                if (el.hasAttr("alt")) {
                                    label.setToolTipText(el.attr("alt"));
                                }
                                add(panel, label, el, attr.copy());
                            }
                        } catch (Exception ex) {
                            log.atError().setCause(ex)
                                    .log("Exception rendering image for {}", el.html());
                        }
                        continue;
                    }

                    case "tt":
                    case "code":
                        renderContent(panel, el, attr.deriveFont(TEXT_ATTRIBUTE_MONOSPACE));
                        continue;
                    case "b":
                        renderContent(panel, el, attr.deriveFont(TEXT_ATTRIBUTE_WEIGHT_BOLD));
                        continue;
                    case "i":
                    case "em":
                    case "cite":
                    case "dfn":
                        renderContent(panel, el, attr.deriveFont(TEXT_ATTRIBUTE_POSTURE_OBLIQUE));
                        continue;
                    case "strong":
                        renderContent(panel, el, attr.deriveFont(TEXT_ATTRIBUTE_WEIGHT_EXTRABOLD));
                        continue;
                    case "sub":
                        renderContent(panel, el, attr.deriveFont(TEXT_ATTRIBUTE_SUPERSCRIPT_SUB));
                        continue;
                    case "sup":
                        renderContent(panel, el, attr.deriveFont(TEXT_ATTRIBUTE_SUPERSCRIPT_SUPER));
                        continue;
                    case "small":
                        renderContent(panel, el, attr.deriveFontDelta( - 1f));
                        continue;
                    case "big":
                        renderContent(panel, el, attr.deriveFontDelta( + 1f));
                        continue;
                    case "del":
                        renderContent(panel, el, attr.deriveFont(TEXT_ATTRIBUTE_STRIKETHROUGH_ON));
                        continue;
                    case "ins":
                        renderContent(panel, el, attr.deriveFont(TEXT_ATTRIBUTE_UNDERLINE_ON));
                        continue;
                    case "label": {
                        String content = el.wholeText();
                        String plain = el.text();
                        JLabel label = new JLabel(plain.equals(content) ? plain : "<HTML>" + content);
                        add(panel, label, el, attr.copy());
                        //if (el.hasAttr("for")) {
                        //    form().addLabel(el, label);
                        //}
                        continue;
                    }
                    case "textarea": {
                        int rows = 0;
                        int cols = 0;
                        try {
                            if (el.hasAttr("rows")) {
                                rows = Integer.parseUnsignedInt(el.attr("rows"));
                            }
                            if (el.hasAttr("cols")) {
                                cols = Integer.parseUnsignedInt(el.attr("cols"));
                            }
                        } catch (Exception ex) {
                            log.warn("Unable to parse rows/cols for {}", el.html(), ex);
                        }
                        JTextArea textArea = new JTextArea(null, el.text(), rows, cols);

                        //if (el.hasAttr("id")) {
                        //    form().addInput(el, textArea);
                        //}
                        if (el.hasAttr("name")) {
                            textArea.setName(el.attr("name"));
                        }
                        if (el.hasAttr("disabled")) {
                            textArea.setEnabled(false);
                        }
                        add(panel, textArea, el, attr.copy());
                        continue;
                    }
                    case "select": {
                        Vector<ComboBoxOption> options = new Vector<>();
                        for (Element o : elementsByTag(el, "option")) {
                            String value = o.attr("value");
                            String text = o.text();
                            if (value == null || value.isBlank()) {
                                value = text;
                            }
                            options.add(new ComboBoxOption(value, text));
                        }
                        JComboBox<ComboBoxOption> comboBox = new JComboBox<>(options);
                        //if (el.hasAttr("id")) {
                        //    form().addInput(el, comboBox);
                        //}
                        if (el.hasAttr("name")) {
                            comboBox.setName(el.attr("name"));
                        }
                        if (el.hasAttr("disabled")) {
                            comboBox.setEnabled(false);
                        }
                        add(panel, comboBox, el, attr.copy());
                        continue;
                    }
                    case "meter":
                    case "progress": {
                        JProgressBar progressBar = new JProgressBar();
                        if (el.hasAttr("max")) {
                            progressBar.setMaximum(Integer.parseInt(el.attr("max")));
                        }
                        if (el.hasAttr("min")) {
                            progressBar.setMinimum(Integer.parseInt(el.attr("min")));
                        }
                        if (el.hasAttr("value")) {
                            progressBar.setValue(Integer.parseInt(el.attr("value")));
                        }
                        //if (el.hasAttr("id")) {
                        //    form().addInput(el, progressBar);
                        //}
                        if (el.hasAttr("name")) {
                            progressBar.setName(el.attr("name"));
                        }
                        if (el.hasAttr("disabled")) {
                            progressBar.setEnabled(false);
                        }
                        add(panel, progressBar, el, attr.copy());
                        continue;
                    }
                    case "button": {
                        JButton button;
                        switch (el.attr("type")) {
                            case "button":
                                button = new JButton(context.newAction(el));
                                break;
                            case "reset":
                                button = new JButton(context.newAction(el));
                                break;
                            case "submit":
                                button = new JButton(context.newAction(el));
                                break;
                            default:
                                continue;
                        }
                        if (el.hasAttr("name")) {
                            button.setName(el.attr("name"));
                        }
                        if (el.hasAttr("disabled")) {
                            button.setEnabled(false);
                        }
                        add(panel, button, el, attr.copy());
                    }
                    case "input": {
                        JComponent component;
                        switch (el.attr("type")) {
                            case "submit": {
                                component = new JButton(context.newAction(el, el.hasAttr("value") ? el.attr("value") : "Submit"));
                                break;
                            }
                            case "checkbox":
                                component = new JCheckBox(context.newAction(el));
                                break;
                            case "color":
                                component = new JColorChooser();
                                break;
                            case "number":
                                continue;
                            case "password":
                                component = new JPasswordField();
                                break;
                            case "radio":
                                component = new JRadioButton(context.newAction(el));
                                break;
                            case "range":
                                component = new JSlider();
                                if (el.hasAttr("min")) {
                                    ((JSlider) component).setMinimum(Integer.parseInt(el.attr("min")));
                                }
                                if (el.hasAttr("max")) {
                                    ((JSlider) component).setMaximum(Integer.parseInt(el.attr("max")));
                                }
                                if (el.hasAttr("step")) {
                                    ((JSlider) component).setMinorTickSpacing(Integer.parseInt(el.attr("max")));
                                    ((JSlider) component).setSnapToTicks(true);
                                }
                                if (el.hasAttr("value")) {
                                    ((JSlider) component).setValue(Integer.parseInt(el.attr("value")));
                                }
                                break;
                            case "date":
                            case "datetime-local":
                            case "email":
                            case "file":
                            case "month":
                            case "search":
                            case "tel":
                            case "time":
                            case "url":
                            case "week":
                            case "text":
                                component = new JTextField();
                                break;
                            default:
                                continue;
                        }
                        //if (el.hasAttr("id")) {
                        //    form().addInput(el, component);
                        //}
                        if (el.hasAttr("name")) {
                            component.setName(el.attr("name"));
                        }
                        if (el.hasAttr("disabled")) {
                            component.setEnabled(false);
                        }
                        add(panel, component, el, attr.copy());
                        continue;
                    }

                    default:
                        renderContent(panel, el, attr.copy());
                        break;
                }
            } else if (n instanceof TextNode) {
                TextNode t = (TextNode) n;

                String s = t.text();
                Matcher m = WHITESPACE.matcher(s);
                int prev = 0;
                while (m.find()) {
                    if (prev != m.start()) {
                        add(panel, new JLabel(s.substring(prev, m.start())), t, attr);
                    }
                    add(panel, new JLabel(" "), t, attr);
                    prev = m.end();
                }
                if (prev != s.length()) {
                    add(panel, new JLabel(s.substring(prev, s.length())), t, attr);
                }
            }
        }
    }

    private static Element[] elementsByTag(Element body, String tag) {
        return elementsByTag(body, tag::equalsIgnoreCase);
    }

    private static Element[] elementsByTag(Element body, Predicate<String> tag) {
        return body.children().stream().filter(el -> tag.test(el.tagName())).toArray(Element[]::new);
    }

    public final HtmlContext context() {
        return context;
    }

    private Dimension adjustSize(Dimension dim, IntBinaryOperator operator) {
        if (body.hasAttr("width")) {
            dim.width = operator.applyAsInt(dim.width,
                    Integer.parseInt(body.attr("width")));
        }
        if (body.hasAttr("height")) {
            dim.height = operator.applyAsInt(dim.height,
                    Integer.parseInt(body.attr("height")));
        }
        return dim;
    }

    @Override
    public Dimension getPreferredSize() {
        return adjustSize(super.getPreferredSize(), Math::min);
    }

    @Override
    public Dimension getMaximumSize() {
        return adjustSize(super.getMaximumSize(), Math::min);
    }

    @Override
    public Dimension getMinimumSize() {
        return adjustSize(super.getMinimumSize(), Math::max);
    }

    static class Attr implements Cloneable {
        Font font;
        Color fgColor;
        Color bgColor;

        Attr() {
        }

        Attr(Font f, Color fg, Color bg) {
            font = f;
            fgColor = fg;
            bgColor = bg;
        }

        Attr copy() {
            try {
                return (Attr) clone();
            } catch (CloneNotSupportedException e) {
                throw new RuntimeException(e);
            }
        }

        Attr deriveFont(Map<? extends AttributedCharacterIterator.Attribute, ?> attributes) {
            return new Attr(
                    font.deriveFont(attributes),
                    fgColor,
                    bgColor
            );
        }
        Attr deriveFontDelta(float delta) {
            return new Attr(
                    font.deriveFont(font.getSize2D() + delta),
                    fgColor,
                    bgColor
            );
        }

        void apply(Component comp) {
            if (font != null) {
                comp.setFont(font);
            }
            if (fgColor != null) {
                comp.setForeground(fgColor);
            }
            if (bgColor != null) {
                comp.setBackground(bgColor);
            }
        }
    }
}
