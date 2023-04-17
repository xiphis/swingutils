package org.xiphis.swing;

import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.css.CSSStyleDeclaration;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;
import java.awt.*;
import java.awt.font.TextAttribute;
import java.net.URL;
import java.util.Collections;
import java.util.Objects;
import java.util.Vector;
import java.util.function.IntBinaryOperator;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HtmlPanel extends JPanel {
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

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
                    JLabel b = new JLabel(text);
                    add(b, c);
                    context.applyStyle(el, b);
                    c = new GridBagConstraints();
                    c.gridx = 1;
                    c.gridy = y;
                    c.fill = GridBagConstraints.HORIZONTAL;
                    new HtmlPanel(context, el).addAndApply(this, c);
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
                    new HtmlPanel(context, el).addAndApply(this, c);
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

                setLayout(new GridBagLayout());
                renderContent(this, body, getFont(), getForeground(), null);
                break;
        }
        if (border != null) {
            setBorder(border);
        }
    }

    void addAndApply(Container container) {
        container.add(this);
        applyStyle();
    }

    void addAndApply(Container container, Object constraint) {
        container.add(this, constraint);
        applyStyle();
    }

    void applyStyle() {
        context.applyStyle(body, this);
        if (body.hasAttr("bgcolor")) {
            String text = body.attr("bgcolor");
            Color color = HtmlColor.getColor(text);
            if (color != null) {
                setBackground(color);
            }
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

            new HtmlPanel(context, el).addAndApply(this, c);
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
                nav = new JPanel();
                nav.setLayout(new FlowLayout());
                add(nav, c);
                context.applyStyle(el, nav);
            }
            new HtmlPanel(context, el).addAndApply(nav);
        }

        int asidey = y;

        for (Element el : elementsByTag(body, "section")) {
            GridBagConstraints c = new GridBagConstraints();
            c.gridx = 0;
            c.gridwidth = 1;
            c.gridy = y++;
            c.gridheight = 1;
            c.fill = GridBagConstraints.HORIZONTAL;

            new HtmlPanel(context, el).addAndApply(this, c);
        }

        for (Element el : elementsByTag(body, "article")) {
            GridBagConstraints c = new GridBagConstraints();
            c.gridx = 0;
            c.gridwidth = 1;
            c.gridy = y++;
            c.gridheight = 1;
            c.fill = GridBagConstraints.HORIZONTAL;
            new HtmlPanel(context, el).addAndApply(this, c);
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
                add(aside, c);
                context.applyStyle(el, aside);
            }
            new HtmlPanel(context, el).addAndApply(aside);
        }
        for (Element el : elementsByTag(body, "footer")) {
            GridBagConstraints c = new GridBagConstraints();
            c.gridx = 0;
            c.gridwidth = 2;
            c.gridy = y++;
            c.gridheight = 1;
            c.fill = GridBagConstraints.HORIZONTAL;
            new HtmlPanel(context, el).addAndApply(this, c);
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
                    new HtmlPanel(context, d, border).addAndApply(this, c);
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
                    new HtmlPanel(context, d, border).addAndApply(this, c);
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
                    new HtmlPanel(context, d, border).addAndApply(this, c);
                }
                y++;
            }
        }
    }

    private void renderContent(JPanel panel, Element body, Font f, Color fg, Color bg) {
        try {
            renderContent(panel, body, f, fg, bg, null);
        } catch (Exception ex) {
            log.atError().setCause(ex).log("Exception rendering element: {}", body.html());
        }
    }

    private JPanel newParagraph(final JPanel panel, final Element body, final Font f, final Color fg, final Color bg) {
        JPanel paragraph = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
        if (f != null) {
            paragraph.setFont(f);
        }
        if (fg != null) {
            paragraph.setForeground(fg);
        }
        if (bg != null) {
            paragraph.setBackground(bg);
        }
        paragraph.setBorder(BorderFactory.createEmptyBorder());
        context.applyStyle(body, paragraph);
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0; c.gridwidth = 1;
        c.gridy = panel.getComponentCount(); c.gridheight = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        panel.add(paragraph, c);
        return paragraph;
    }
    private JPanel renderContent(final JPanel panel, final Element body, final Font f, final Color fg, final Color bg,
                                 JPanel paragraph) {
        for (Node n : body.childNodes()) {
            if (paragraph == null) {
                paragraph = newParagraph(panel, body, f, fg, bg);
            }

            if (n instanceof Element) {
                Element el = (Element) n;
                switch (el.tagName()) {
                    case "br":
                        if (paragraph.getComponentCount() > 0) {
                            paragraph = null;
                        }
                        continue;
                    case "h1":
                    case "h2":
                    case "h3":
                    case "h4":
                    case "h5":
                    case "h6":
                        paragraph = newParagraph(panel, el, f, fg, bg);
                        renderContent(panel, el, paragraph.getFont(), paragraph.getForeground(), paragraph.getBackground(), paragraph);
                        paragraph = null;
                        continue;
                    case "hr": {
                        JSeparator separator = new JSeparator();
                        panel.add(separator);
                        context.applyStyle(el, separator);
                        paragraph = null;
                        continue;
                    }
                    case "p":
                    case "div":
                        renderContent(panel, el, f, fg, bg);
                        paragraph = null;
                        continue;
                    case "table":
                        paragraph = newParagraph(panel, el, f, fg, bg);
                        new HtmlPanel(context, el).addAndApply(paragraph);
                        paragraph = null;
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
                                context.applyStyle(el, label);
                                paragraph.add(label);
                            }
                        } catch (Exception ex) {
                            log.atError().setCause(ex)
                                    .log("Exception rendering image for {}", el.html());
                        }
                        continue;
                    }

                    case "span": {
                        paragraph = renderContent(panel, el, f, fg, bg, paragraph);
                        continue;
                    }
                    case "tt":
                    case "code":
                        paragraph = renderContent(panel, el, f.deriveFont(Collections.singletonMap(TextAttribute.FAMILY, "Monospaced")), fg, bg, paragraph);
                        continue;
                    case "b":
                        paragraph = renderContent(panel, el, f.deriveFont(Collections.singletonMap(TextAttribute.WEIGHT, TextAttribute.WEIGHT_BOLD)), fg, bg, paragraph);
                        continue;
                    case "i":
                    case "em":
                    case "cite":
                    case "dfn":
                        paragraph = renderContent(panel, el, f.deriveFont(Collections.singletonMap(TextAttribute.POSTURE, TextAttribute.POSTURE_OBLIQUE)), fg, bg, paragraph);
                        continue;
                    case "strong":
                        paragraph = renderContent(panel, el, f.deriveFont(Collections.singletonMap(TextAttribute.WEIGHT, TextAttribute.WEIGHT_EXTRABOLD)), fg, bg, paragraph);
                        continue;
                    case "sub":
                        paragraph = renderContent(panel, el, f.deriveFont(Collections.singletonMap(TextAttribute.SUPERSCRIPT, TextAttribute.SUPERSCRIPT_SUB)), fg, bg, paragraph);
                        continue;
                    case "sup":
                        paragraph = renderContent(panel, el, f.deriveFont(Collections.singletonMap(TextAttribute.SUPERSCRIPT, TextAttribute.SUPERSCRIPT_SUPER)), fg, bg, paragraph);
                        continue;
                    case "small":
                        paragraph = renderContent(panel, el, f.deriveFont(f.getSize2D() - 1f), fg, bg, paragraph);
                        continue;
                    case "big":
                        paragraph = renderContent(panel, el, f.deriveFont(f.getSize2D() + 1f), fg, bg, paragraph);
                        continue;
                    case "mark":
                        paragraph = renderContent(panel, el, f, fg, bg, paragraph);
                        continue;
                    case "del":
                        paragraph = renderContent(panel, el, f.deriveFont(Collections.singletonMap(TextAttribute.STRIKETHROUGH, TextAttribute.STRIKETHROUGH_ON)), fg, bg, paragraph);
                        continue;
                    case "ins":
                        paragraph = renderContent(panel, el, f.deriveFont(Collections.singletonMap(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON)), fg, bg, paragraph);
                        continue;

                    case "form":
                        panel.add(new HtmlForm(context, el));
                        paragraph = null;
                        continue;
                    case "label": {
                        String content = el.wholeText();
                        String plain = el.text();
                        JLabel label = new JLabel(plain.equals(content) ? plain : "<HTML>" + content);
                        paragraph.add(label);
                        if (el.hasAttr("for")) {
                            form().addLabel(el, label);
                        }
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

                        if (el.hasAttr("id")) {
                            form().addInput(el, textArea);
                        }
                        if (el.hasAttr("name")) {
                            textArea.setName(el.attr("name"));
                        }
                        if (el.hasAttr("disabled")) {
                            textArea.setEnabled(false);
                        }
                        paragraph.add(textArea);
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
                        if (el.hasAttr("id")) {
                            form().addInput(el, comboBox);
                        }
                        if (el.hasAttr("name")) {
                            comboBox.setName(el.attr("name"));
                        }
                        if (el.hasAttr("disabled")) {
                            comboBox.setEnabled(false);
                        }
                        paragraph.add(comboBox);
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
                        if (el.hasAttr("id")) {
                            form().addInput(el, progressBar);
                        }
                        if (el.hasAttr("name")) {
                            progressBar.setName(el.attr("name"));
                        }
                        if (el.hasAttr("disabled")) {
                            progressBar.setEnabled(false);
                        }
                        paragraph.add(progressBar);
                        continue;
                    }
                    case "button": {
                        JButton button;
                        switch (el.attr("type")) {
                            case "button":
                                button = new JButton(el.text());
                                break;
                            case "reset":
                                button = new JButton(el.text());
                                break;
                            case "submit":
                                button = new HtmlSubmit(new HtmlForm.SubmitAction(this, el.text()));
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
                        paragraph.add(button);
                    }
                    case "input": {
                        JComponent component;
                        switch (el.attr("type")) {
                            case "submit": {
                                component = new HtmlSubmit(new HtmlForm.SubmitAction(this, el.hasAttr("value") ? el.attr("value") : "Submit"));
                                break;
                            }
                            case "checkbox":
                                component = new JCheckBox();
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
                                component = new JRadioButton();
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
                        if (el.hasAttr("id")) {
                            form().addInput(el, component);
                        }
                        if (el.hasAttr("name")) {
                            component.setName(el.attr("name"));
                        }
                        if (el.hasAttr("disabled")) {
                            component.setEnabled(false);
                        }
                        paragraph.add(component);
                        continue;
                    }

                    default:
                        paragraph = renderContent(panel, el, f, fg, bg, paragraph);
                        break;
                }
            } else if (n instanceof TextNode) {
                TextNode t = (TextNode) n;

                String s = t.text();
                Matcher m = WHITESPACE.matcher(s);
                int prev = 0;
                while (m.find()) {
                    if (prev != m.start()) {
                        newTextLabel(s.substring(prev, m.start()), f, fg, bg, paragraph);
                    }
                    newTextLabel(" ", f, fg, bg, paragraph);
                    prev = m.end();
                }
                if (prev != s.length()) {
                    newTextLabel(s.substring(prev, s.length()), f, fg, bg, paragraph);
                }
            }
        }
        return paragraph;
    }

    protected HtmlForm form() {
        Container c = getParent();
        while (c != null) {
            if (c instanceof HtmlPanel) {
                return ((HtmlPanel) c).form();
            }
            c = c.getParent();
        }
        return null;
    }

    private JLabel newTextLabel(String text, Font f, Color fg, Color bg, JPanel paragraph) {
        JLabel label = new JLabel(text);
        label.setFont(f);
        label.setForeground(fg);
        if (bg != null) {
            label.setBackground(bg);
        }
        paragraph.add(label);
        context.applyStyle(body, label);
        label.setBorder(BorderFactory.createEmptyBorder());
        return label;
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
}
