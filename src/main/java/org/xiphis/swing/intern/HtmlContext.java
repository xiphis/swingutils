package org.xiphis.swing.intern;
/*
Copyright 2023 Xiphis and A. T. Curtis

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice,
   this list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice,
   this list of conditions and the following disclaimer in the documentation
   and/or other materials provided with the distribution.

3. Neither the name of the copyright holder nor the names of its contributors
   may be used to endorse or promote products derived from this software
   without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS “AS IS” AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.steadystate.css.dom.CSSStyleSheetImpl;
import com.steadystate.css.parser.CSSOMParser;
import com.steadystate.css.parser.SACParserCSS3;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.css.sac.CSSException;
import org.w3c.css.sac.CSSParseException;
import org.w3c.css.sac.ErrorHandler;
import org.w3c.css.sac.InputSource;
import org.w3c.dom.css.*;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.font.TextAttribute;
import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;

public class HtmlContext {
    private final Logger log = LoggerFactory.getLogger(getClass());
    private final Document document;
    private final CSSOMParser parser;
    private CSSStyleSheet sheet;

    private final Map<String, Object> nameMap = new HashMap<>();
    private final Map<String, Component> idMap = new HashMap<>();
    private final Map<JLabel, String> labelFor = new IdentityHashMap<>();

    private Predicate<HtmlEvent> submit = ignore -> true;
    private Predicate<HtmlEvent> reset = ignore -> true;
    private Predicate<HtmlEvent> click = ignore -> false;


    public HtmlContext(String html) {
        this(html, false);
    }
    public HtmlContext(String html, boolean partial) {
        document = partial ? Jsoup.parseBodyFragment(html) : Jsoup.parse(html);

        StringBuilder sb = new StringBuilder();

        for (Element link : document.getElementsByTag("link")) {
            if ("stylesheet".equals(link.attr("rel"))) {
                String src = link.attr("src");
                URL url = forResource(src);
                if (url != null) {
                    try {
                        appendStyleSheet(url, sb);
                    } catch (Exception ex) {
                        log.warn("Failed trying to read from: {}", link.html(), ex);
                    }
                } else {
                    log.warn("Resource not found: {}", src);
                }
            }
        }

        for (Element style : document.getElementsByTag("style")) {
            sb.append(style.wholeText()).append('\n');
        }

        parser = new CSSOMParser(new SACParserCSS3());
        ErrorHandler errorHandler = new ErrorHandler() {
            @Override
            public void warning(CSSParseException e) throws CSSException {
                log.warn("WARN: {}", e.getLocalizedMessage(), e);
            }

            @Override
            public void error(CSSParseException e) throws CSSException {
                log.error("ERROR: {}", e.getLocalizedMessage(), e);
            }

            @Override
            public void fatalError(CSSParseException e) throws CSSException {
                log.error("FATAL: {}", e.getLocalizedMessage(), e);
            }
        };
        parser.setErrorHandler(errorHandler);
        try {
            sheet = parser.parseStyleSheet(new InputSource(new StringReader(sb.toString())), null, null);

            parser.setParentStyleSheet((CSSStyleSheetImpl) sheet);
        } catch (Exception e) {
            log.warn("Failed to parse style sheet", e);
        }
    }

    URL forResource(String src) {
        ClassLoader ccl = Thread.currentThread().getContextClassLoader();
        URL url = null;
        if (ccl != null) {
            url = ccl.getResource(src);
        }
        if (url == null) {
            url = getClass().getResource(src);
        }
        if (url == null) {
            url = ClassLoader.getSystemResource(src);
        }
        return url;
    }

    private void appendStyleSheet(URL url, StringBuilder sb) throws IOException {
        try (InputStream is = url.openStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(is,StandardCharsets.UTF_8))) {
            for (;;) {
                String line = reader.readLine();
                if (line == null) {
                    break;
                }
                sb.append(line).append('\n');
            }
        }
    }

    public CSSStyleSheet stylesheet() {
        return sheet;
    }

    public CSSStyleDeclaration style(String declaration) {
        try {
            return parser.parseStyleDeclaration(new InputSource(new StringReader(declaration)));
        } catch (IOException e) {
            log.warn("Failed to parse style declaration: {}", declaration, e);
            return null;
        }
    }

    public Document document() {
        return document;
    }
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    private boolean checkAttribute(Element el, String attribute) {
        switch (attribute) {
            case "":
                return true;
            case "empty":
                return el.childNodeSize() == 0;
            case "first-child":
                return el.previousElementSibling() == null;
            case "last-child":
                return el.nextElementSibling() == null;
            case "only-child":
                return el.previousElementSibling() == null && el.nextElementSibling() == null;
            default:
                if (attribute.startsWith("not(") && attribute.endsWith(")")) {
                    return checkRule0(el, attribute.substring(4, attribute.length() - 1));
                }
                if (attribute.startsWith("nth-child(") && attribute.endsWith(")")) {
                    return el.parent().children().indexOf(el) == Integer.parseUnsignedInt(attribute.substring(10, attribute.length() - 1)) - 1;
                }
                if (attribute.startsWith("nth-last-child(") && attribute.endsWith(")")) {
                    return el.parent().children().indexOf(el) == el.parent().childrenSize() - Integer.parseUnsignedInt(attribute.substring(15, attribute.length() - 1));
                }
        }
        return false;
    }

    private boolean checkRule0(Element el, String selector) {
        for (String sel : selector.split(",")) {
            String attr = "";
            int pos = sel.indexOf(':');
            if (pos == 0) {
                attr = sel;
                sel = "";
            } else if (pos > 0) {
                attr = sel.substring(pos);
                sel = sel.substring(0, pos);
            }

            if ("*".equals(sel)) {
                if (checkAttribute(el, attr)) {
                    return true;
                }
                continue;
            }

            if (sel.startsWith("#")) {
                if (el.hasAttr("id")) {
                    if (sel.substring(1).equals(el.attr("id"))) {
                        if (checkAttribute(el, attr)) {
                            return true;
                        }
                        continue;
                    }
                }
            }

            if (sel.startsWith(".")) {
                if (el.hasAttr("class")) {
                    List<String> classes = Arrays.asList(WHITESPACE.split(el.attr("class")));
                    List<String> test = Arrays.asList(selector.split("\\."));
                    if (classes.containsAll(test)) {
                        if (checkAttribute(el, attr)) {
                            return true;
                        }
                    }
                }
            }

            if (sel.equals(el.tagName())) {
                if (checkAttribute(el, attr)) {
                    return true;
                }
                continue;
            }

            if (sel.startsWith(el.tagName()) && sel.length() > el.tagName().length()
                    && sel.charAt(el.tagName().length()) == '.') {
                if (checkRule0(el, sel.substring(el.tagName().length()))) {
                    if (checkAttribute(el, attr)) {
                        return true;
                    }
                    continue;
                }
            }

            if (sel.endsWith(el.tagName())) {
                pos = sel.length() - el.tagName().length() - 1;
                char ch = sel.charAt(pos);
                boolean match = false;
                if (ch == '>') {
                    if (checkRule0(el.parent(), sel.substring(0, pos))) {
                        match = true;
                    }
                } else if (ch == '+') {
                    if (el.previousElementSibling() == null && checkRule0(el.parent(), sel.substring(0, pos))) {
                        match = true;
                    } else if (el.previousElementSibling() != null && checkRule0(el.previousElementSibling(), sel.substring(0, pos))) {
                        match = true;
                    }
                } else if (ch == '~') {
                    if (checkRule0(el.previousElementSibling(), sel.substring(0, pos))) {
                        match = true;
                    }
                }
                if (match) {
                    if (checkAttribute(el, attr)) {
                        return true;
                    }
                    continue;
                }
            }
            log.debug("no match");
        }
        return false;
    }

    private boolean checkRule(Element el, CSSStyleRule styleRule) {
        String selectorString = styleRule.getSelectorText();
        selectorString = selectorString.replaceAll("\\s*,\\s*", ",");
        selectorString = selectorString.replaceAll("\\s*>\\s*", ">");
        selectorString = selectorString.replaceAll("\\s*\\+\\s*", "+");
        selectorString = selectorString.replaceAll("\\s*~\\s*", "~");

        String[] selector = WHITESPACE.split(selectorString);
        for (int i = selector.length - 1; i >= 0;) {
            if (checkRule0(el, selector[i])) {
                i--;
                el = el.parent();
                continue;
            }
            if (i == selector.length - 1) {
                return false;
            }
            el = el.parent();
            if (el == null) {
                return false;
            }
        }
        return true;
    }

    public void applyStyle(JComponent component, Node n) {
        if (n instanceof Element) {
            if (n.hasAttr("id")) {
                idMap.putIfAbsent(n.attr("id"), component);
            }
            if (component instanceof JLabel && n.hasAttr("for")) {
                labelFor.put((JLabel) component, n.attr("for"));
            }
        }
        while (!(n instanceof Element)) {
            n = n.parentNode();
        }
        Element el = (Element) n;
        CSSRuleList cssRules = stylesheet().getCssRules();
        List<CSSStyleDeclaration> styleDeclarations = null;
        for (int i = 0; i < cssRules.getLength(); i++) {
            CSSRule rule = cssRules.item(i);
            switch (rule.getType()) {
                case CSSRule.STYLE_RULE: {
                    CSSStyleRule styleRule = (CSSStyleRule) rule;
                    if (checkRule(el, styleRule)) {
                        if (styleDeclarations == null) {
                            styleDeclarations = new LinkedList<>();
                        }
                        styleDeclarations.add(styleRule.getStyle());
                    }
                    break;
                }
                default:
                    break;
            }
        }
        if (el.hasAttr("style")) {
            CSSStyleDeclaration declaration = style(el.attr("style"));
            if (styleDeclarations == null) {
                styleDeclarations = Collections.singletonList(declaration);
            } else {
                styleDeclarations.add(declaration);
            }
        }
        if (el.hasAttr("align")) {
            switch (el.attr("align")) {
                case "left":
                case "right":
                case "center":
                    CSSStyleDeclaration declaration = style("float:" + el.attr("align"));
                    if (styleDeclarations == null) {
                        styleDeclarations = Collections.singletonList(declaration);
                    } else {
                        styleDeclarations.add(declaration);
                    }
                default:
            }

        }

        if (styleDeclarations == null) {
            return;
        }

        for (CSSStyleDeclaration style : styleDeclarations) {
            for (int i = 0; i < style.getLength(); i++) {
                String property = style.item(i);
                switch (property) {
                    case "float":
                        switch (style.getPropertyValue(property)) {
                            case "left":
                                component.setAlignmentX(JComponent.LEFT_ALIGNMENT);
                                continue;
                            case "center":
                                component.setAlignmentX(JComponent.CENTER_ALIGNMENT);
                                continue;
                            case "right":
                                component.setAlignmentX(JComponent.RIGHT_ALIGNMENT);
                                continue;
                            default:
                                log.info("unknown alignment: {} {} {}", el.tagName(), component.getClass().getSimpleName(), style.getPropertyValue(property));
                                continue;
                        }
                    case "overflow":
                        if (component.getParent() instanceof JScrollPane) {
                            JScrollPane scrollPane = (JScrollPane) component.getParent();
                            switch (style.getPropertyValue(property)) {
                                case "hidden":
                                    scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
                                    scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
                                    continue;
                                case "scroll":
                                    scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
                                    scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
                                    continue;
                                case "auto":
                                    scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
                                    scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
                                    continue;
                                default:
                                    break;
                            }
                            log.info("unknown overflow: {} {} {}", el.tagName(), component.getClass().getSimpleName(), style.getPropertyValue(property));
                            continue;
                        }
                    case "overflow-x":
                        if (component.getParent() instanceof JScrollPane) {
                            JScrollPane scrollPane = (JScrollPane) component.getParent();
                            switch (style.getPropertyValue(property)) {
                                case "hidden":
                                    scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
                                    continue;
                                case "scroll":
                                    scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
                                    continue;
                                case "auto":
                                    scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
                                    continue;
                                default:
                                    break;
                            }
                            log.info("unknown overflow-x: {} {} {}", el.tagName(), component.getClass().getSimpleName(), style.getPropertyValue(property));
                            continue;
                        }
                    case "overflow-y":
                        if (component.getParent() instanceof JScrollPane) {
                            JScrollPane scrollPane = (JScrollPane) component.getParent();
                            switch (style.getPropertyValue(property)) {
                                case "hidden":
                                    scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
                                    continue;
                                case "scroll":
                                    scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
                                    continue;
                                case "auto":
                                    scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
                                    continue;
                                default:
                                    break;
                            }
                            log.info("unknown overflow-y: {} {} {}", el.tagName(), component.getClass().getSimpleName(), style.getPropertyValue(property));
                            continue;
                        }
                    case "margin":
                        if (component instanceof HtmlPanel) {
                            log.debug("margin: {}", style.getPropertyValue(property));
                        }
                        continue;
                    case "padding":
                        if (component instanceof HtmlPanel) {
                            log.info("padding: {}", style.getPropertyValue(property));
                        }
                        continue;
                    case "border":
                        if ("0".equals(style.getPropertyValue(property))) {
                            component.setBorder(BorderFactory.createEmptyBorder());
                            continue;
                        }
                        log.info("border: {} {} {}", el.tagName(), component.getClass().getSimpleName(), style.getPropertyValue(property));
                        continue;
                    case "outline":
                        log.debug("not supported: {}", property);
                        continue;
                    case "line-height":
                        log.debug("not supported: {}", property);
                        continue;
                    case "vertical-align":
                        switch (style.getPropertyValue(property)) {
                            case "baseline":
                                continue;
                            default:
                                log.debug("vertical-align mot supported: {} {} {}", el.tagName(), component.getClass().getSimpleName(), style.getPropertyValue(property));
                                continue;
                        }
                    case "background":
                        if ("transparent".equals(style.getPropertyValue(property))) {
                            continue;
                        }
                        log.info("unhandled background: {} {} {}", el.tagName(), component.getClass().getSimpleName(), style.getPropertyValue(property));
                        continue;
                    case "background-color":
                        Optional.ofNullable(HtmlColor.getColor(style.getPropertyValue(property)))
                                .ifPresent(component::setBackground);
                        continue;
                    case "color":
                        Optional.ofNullable(HtmlColor.getColor(style.getPropertyValue(property)))
                                .ifPresent(component::setForeground);
                        continue;
                    case "font-family":
                        for (String family : style.getPropertyValue(property).split(",")) {
                            Font font = component.getFont().deriveFont(Collections.singletonMap(TextAttribute.FAMILY, family));
                            if (family.equals(font.getFamily())) {
                                component.setFont(font);
                                break;
                            }
                        }
                        continue;
                    case "font-size":
                        try {
                            String size = style.getPropertyValue(property);
                            if (size.endsWith("%")) {
                                float scale = Float.parseFloat(size.substring(0, size.length() - 1)) / 100f;
                                Font font = component.getFont();
                                component.setFont(font.deriveFont(scale * font.getSize2D()));
                                continue;
                            }
                            if (size.endsWith("px")) {
                                float px = Float.parseFloat(size.substring(0, size.length() - 2));
                                float pt = 72f * px / component.getToolkit().getScreenResolution();
                                component.setFont(component.getFont().deriveFont(pt));
                                continue;
                            }
                            if (size.endsWith("em")) {
                                float em = Float.parseFloat(size.substring(0, size.length() - 2));
                                float px = em * 16f;
                                float pt = 72f * px / component.getToolkit().getScreenResolution();
                                component.setFont(component.getFont().deriveFont(pt));
                                continue;
                            }
                            if (size.endsWith("pt")) {
                                size = size.substring(0, size.length() - 2);
                            }
                            component.setFont(component.getFont().deriveFont(Float.parseFloat(size)));
                        } catch (Exception ex) {
                            log.warn("Unable to parse font-size: {}", style.getPropertyValue(property), ex);
                        }
                        continue;

                    case "font-style":
                        switch (style.getPropertyValue(property)) {
                            case "normal":
                                component.setFont(component.getFont().deriveFont(
                                        Collections.singletonMap(TextAttribute.POSTURE, TextAttribute.POSTURE_REGULAR)));
                                continue;
                            case "italic":
                            case "oblique":
                                component.setFont(component.getFont().deriveFont(
                                        Collections.singletonMap(TextAttribute.POSTURE, TextAttribute.POSTURE_OBLIQUE)));
                                continue;
                            default:
                                try {
                                    float posture = Float.parseFloat(style.getPropertyValue(property));
                                    component.setFont(component.getFont().deriveFont(
                                            Collections.singletonMap(TextAttribute.POSTURE, posture)));
                                } catch (Exception ex) {
                                    log.warn("unknown font-style: {}", style.getPropertyValue(property));
                                }
                        }
                        continue;
                    case "font-weight":
                        switch (style.getPropertyValue(property)) {
                            case "normal":
                                component.setFont(component.getFont().deriveFont(
                                        Collections.singletonMap(TextAttribute.WEIGHT, TextAttribute.WEIGHT_REGULAR)));
                                continue;
                            case "bold":
                                component.setFont(component.getFont().deriveFont(
                                        Collections.singletonMap(TextAttribute.WEIGHT, TextAttribute.WEIGHT_BOLD)));
                                continue;
                            case "bolder":
                                component.setFont(component.getFont().deriveFont(
                                        Collections.singletonMap(TextAttribute.WEIGHT, TextAttribute.WEIGHT_EXTRABOLD)));
                                continue;
                            default:
                                try {
                                    float weight = Float.parseFloat(style.getPropertyValue(property));
                                    component.setFont(component.getFont().deriveFont(
                                            Collections.singletonMap(TextAttribute.WEIGHT, weight)));
                                } catch (Exception ex) {
                                    log.warn("unknown font-weight: {}", style.getPropertyValue(property));
                                }
                                continue;
                        }
                    case "text-decoration":
                        switch (style.getPropertyValue(property)) {
                            case "line-through":
                                component.setFont(component.getFont().deriveFont(
                                        Collections.singletonMap(TextAttribute.STRIKETHROUGH, TextAttribute.STRIKETHROUGH_ON)));
                                continue;
                            default:
                                log.warn("unknown text-decoration: {}", style.getPropertyValue(property));
                                continue;
                        }
                    default:
                        log.warn("unknown style attribute: {} {} {} {}",
                                el.tagName(), component.getClass().getSimpleName(), property,
                                style.getPropertyValue(property));
                }
            }
        }
    }

    public Border parseBorder(String style, Border border) {
        switch (style) {
            case "dotted":
                border = BorderFactory.createDashedBorder(Color.BLACK, 1f, 1f, 1f, true);
                break;
            case "dashed":
                border = BorderFactory.createDashedBorder(Color.BLACK);
                break;
            case "solid":
                border = BorderFactory.createLineBorder(Color.BLACK);
                break;
            case "double":
                border = BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(Color.BLACK),
                        BorderFactory.createLineBorder(Color.BLACK));
                break;
            case "groove":
                border = BorderFactory.createEtchedBorder(EtchedBorder.LOWERED);
                break;
            case "ridge":
                border = BorderFactory.createEtchedBorder(EtchedBorder.RAISED);
                break;
            case "inset":
                border = BorderFactory.createLoweredSoftBevelBorder();
                break;
            case "outset":
                border = BorderFactory.createRaisedSoftBevelBorder();
                break;
            case "none":
            case "hidden":
                border = BorderFactory.createEmptyBorder();
                break;
            default:
                break;
        }
        return border;
    }

    public HtmlAction.Button newButtonAction(Element el) {
        return new HtmlAction.Button(el, newAction(el));
    }

    public HtmlAction.Checkbox newCheckboxAction(Element el) {
        return new HtmlAction.Checkbox(el, newAction(el));
    }

    public HtmlAction.Radiobox newRadioboxAction(Element el) {
        return new HtmlAction.Radiobox(el, newAction(el));
    }

    public HtmlAction.Textarea newTextAreaModel(Element el) {
        return new HtmlAction.Textarea(el, newAction(el), text -> text);
    }
    public HtmlAction.Textarea newTextAreaModel(Element el, TextAreaParser<?> transform) {
        return new HtmlAction.Textarea(el, newAction(el), Objects.requireNonNull(transform));
    }

    public interface TextAreaParser<V> {
        V parse(String text) throws ParseException;
    }

    public HtmlAction.Progress newProgressModel(Element el) {
        return new HtmlAction.Progress(el, newAction(el));
    }

    public HtmlAction newAction(Element el) {
        if (el.hasAttr("name")) {

            return (HtmlAction) nameMap.computeIfAbsent(el.attr("name"), name -> {
                log.debug("Created action for {}", name);
                return new HtmlAction(this, name);
            });
        }
        log.warn("No name for element: {}", el);
        return new HtmlAction(this, "");
    }

    public void actionPerformed(HtmlAction htmlAction, ActionEvent e) {
        Component comp = (Component) e.getSource();
        Element element = htmlAction.element(comp);
        log.info("actionPerformed({}, {})", htmlAction, e);
        switch (element.tagName()) {
            case "a":
                clickAction(htmlAction, e);
                break;
            case "button":
                switch (element.attr("type")) {
                    case "submit":
                    case "image":
                        submitAction(htmlAction, e);
                        break;
                    case "reset":
                        resetAction(htmlAction, e);
                        break;
                    default:
                        clickAction(htmlAction, e);
                        break;
                }
            case "input":
                switch (element.attr("type")) {
                    case "submit":
                        submitAction(htmlAction, e);
                        break;
                    case "checkbox":
                    case "radio":
                        break;
                }
        }
    }

    private void submitAction(HtmlAction action, EventObject event) {
        HtmlEvent htmlEvent = new HtmlEvent(action, event);
        if (submit.test(htmlEvent)) {
            htmlEvent.window().setVisible(false);
        }
    }

    private void resetAction(HtmlAction action, EventObject event) {
        HtmlEvent htmlEvent = new HtmlEvent(action, event);
        if (reset.test(htmlEvent)) {
            // clear all data to defaults?
        }
    }

    private void clickAction(HtmlAction action, EventObject event) {
        HtmlEvent htmlEvent = new HtmlEvent(action, event);
        if (click.test(htmlEvent)) {
            log.debug("clicked");
        } else {
            log.warn("unhandled click: {} {}", action, event);
        }
    }

    public void onSubmit(Predicate<HtmlEvent> submitAction) {
        submit = submit.and(submitAction);
    }

    public void onClicked(String id, Consumer<HtmlEvent> clickAction) {
        Objects.requireNonNull(id);
        Objects.requireNonNull(clickAction);
        click = click.or(htmlEvent -> {
            if (id.equals(htmlEvent.getId())) {
                clickAction.accept(htmlEvent);
                return true;
            } else {
                return false;
            }
        });
    }

    public void onReset(Predicate<HtmlEvent> resetAction) {
        reset = reset.and(resetAction);
    }

    public void init() {
        labelFor.forEach((jLabel, id) -> {
            Component c = getComponentById(id);
            if (c != null) {
                jLabel.setLabelFor(c);
            }
        });
    }

    public <T extends Component> T getComponentById(String id) {
        //noinspection unchecked
        return (T) idMap.get(id);
    }

    public void setValues(JsonObject jsonObject) {
        nameMap.forEach((name, val) -> {
           if (val instanceof Action) {
               Action action = (Action) val;
               JsonPrimitive value = jsonObject.getAsJsonPrimitive(name);
               if (value.isBoolean()) {
                   action.putValue(Action.SELECTED_KEY, value.getAsBoolean());
               } else if (value.isNumber()) {
                   action.putValue(Action.SELECTED_KEY, value.getAsNumber());
               } else if (value.isString()) {
                   action.putValue(Action.SELECTED_KEY, value.getAsString());
               }
           }
        });
    }

    public JsonObject toJson() {
        JsonObject result = new JsonObject();
        nameMap.forEach((name, val) -> {
            if (val instanceof Action) {
                Action action = (Action) val;
                Object value = action.getValue(Action.SELECTED_KEY);
                if (value instanceof Boolean) {
                    result.addProperty(name, (Boolean) value);
                } else if (value instanceof Number) {
                    result.addProperty(name, (Number) value);
                } else if (value instanceof String) {
                    result.addProperty(name, (String) value);
                }
            }
        });
        return result;
    }
}
