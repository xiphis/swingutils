package org.xiphis.swing;

import org.jsoup.nodes.Element;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.util.IdentityHashMap;
import java.util.Map;

public class HtmlAction extends AbstractAction {

    public static final String VALUE = "value";

    private final HtmlContext context;
    private final Map<Component, Element> elementMap;

    public HtmlAction(HtmlContext context, String name) {
        super(name);
        this.context = context;
        this.elementMap = new IdentityHashMap<>();
    }

    void add(Component comp, Element element) {
        elementMap.put(comp, element);
    }

    public HtmlContext context() {
        return context;
    }

    public Element element(Component comp) {
        return elementMap.get(comp);
    }


    /**
     * Invoked when an action occurs.
     *
     * @param e the event to be processed
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        context.actionPerformed(this, e);
    }

    static abstract class Abstract extends AbstractAction {
        final HtmlAction htmlAction;
        final Element element;

        Abstract(String name, Element element, HtmlAction htmlAction) {
            super(name);
            this.htmlAction = htmlAction;
            this.element = element;

            if (element.hasAttr("alt")) {
                putValue(Action.SHORT_DESCRIPTION, element.attr("alt"));
            }

            if (element.hasAttr("disabled")) {
                setEnabled(false);
            }
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            htmlAction.elementMap.put((Component) e.getSource(), element);
            htmlAction.actionPerformed(e);
        }
    }

    static class Button extends Abstract {
        Button(Element element, HtmlAction htmlAction) {
            super(submitText(element), element, htmlAction);
        }

        static String submitText(Element el) {
            if ("input".equals(el.tagName())) {
                return el.hasAttr("value") ? el.attr("value") : "Submit";
            } else {
                return textForElement(el);
            }
        }
    }

    static class Checkbox extends Abstract {
        Checkbox(Element element, HtmlAction htmlAction) {
            super(textForElement(element), element, htmlAction);

            htmlAction.addPropertyChangeListener(this::htmlListener);
        }

        public boolean isSelected() {
            return Boolean.TRUE.equals(getValue(SELECTED_KEY));
        }

        private void htmlListener(PropertyChangeEvent propertyChangeEvent) {
            if (SELECTED_KEY.equals(propertyChangeEvent.getPropertyName())) {
                putValue(SELECTED_KEY, propertyChangeEvent.getNewValue());
            }
        }

        /**
         * Invoked when an action occurs.
         *
         * @param e the event to be processed
         */
        @Override
        public void actionPerformed(ActionEvent e) {
            if (e.getSource() instanceof JCheckBox) {
                htmlAction.putValue(SELECTED_KEY, ((JCheckBox) e.getSource()).isSelected());
            }
            super.actionPerformed(e);
        }
    }

    static class Radiobox extends Abstract {

        private static final String BUTTON_GROUP = "ButtonGroup";

        Radiobox(Element element, HtmlAction htmlAction) {
            super(textForElement(element), element, htmlAction);

            if (htmlAction.getValue(BUTTON_GROUP) == null) {
                htmlAction.putValue(BUTTON_GROUP, new ButtonGroup());
            }
        }

        /**
         * Invoked when an action occurs.
         *
         * @param e the event to be processed
         */
        @Override
        public void actionPerformed(ActionEvent e) {
            if (e.getSource() instanceof JRadioButton) {
                if (getValue(BUTTON_GROUP) == null) {
                    ButtonGroup buttonGroup = (ButtonGroup) htmlAction.getValue(BUTTON_GROUP);
                    buttonGroup.add((JRadioButton) e.getSource());
                    putValue(BUTTON_GROUP, buttonGroup);
                }
            }
            super.actionPerformed(e);
        }
    }

    static String textForElement(Element el) {
        String text = el.text();
        String wholeText = el.wholeText();
        if (!text.equals(wholeText.trim())) {
            text = "<html>" + wholeText;
        }
        return text;
    }
}
