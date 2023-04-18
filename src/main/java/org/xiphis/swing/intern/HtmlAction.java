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

import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.UndoableEditEvent;
import javax.swing.text.PlainDocument;
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

            if ("input".equals(element.tagName()) && "image".equals(element.attr("type"))) {
                String src = element.attr("src");
                try {
                    putValue(Action.SMALL_ICON, new ImageIcon(htmlAction.context().forResource(src)));
                } catch (Exception ex) {
                    //TODO
                }
            }
        }

        static String submitText(Element el) {
            if ("input".equals(el.tagName())) {
                if ("image".equals(el.attr("type"))) {
                    return null;
                }
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

            if (element.hasAttr("checked")) {
                htmlAction.putValue(SELECTED_KEY, true);
            }
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

    public static class Textarea extends PlainDocument {

        private static final Logger LOG = LoggerFactory.getLogger(Textarea.class);
        private final HtmlAction htmlAction;
        private final HtmlContext.TextAreaParser<?> transform;
        public Textarea(Element el, HtmlAction htmlAction, HtmlContext.TextAreaParser<?> transform) {
            this.htmlAction = htmlAction;
            this.transform = transform;
            htmlAction.addPropertyChangeListener(this::htmlListener);
        }

        private void htmlListener(PropertyChangeEvent propertyChangeEvent) {
            if (SELECTED_KEY.equals(propertyChangeEvent.getPropertyName())) {
                try {
                    if (propertyChangeEvent.getNewValue() == null) {
                        replace(0, getLength(), "", null);
                    } else {
                        String newText = String.valueOf(propertyChangeEvent.getNewValue());
                        if (newText.length() == getLength()) {
                            if (getText(0, getLength()).equals(newText)) {
                                return;
                            }
                        }
                        replace(0, getLength(), newText, null);
                    }
                } catch (Exception ex) {
                    LOG.warn("Failed to set content", ex);
                }
            }
        }

        private void updateContent() {
            try {
                String text = getText(0, getLength());
                htmlAction.putValue(SELECTED_KEY, !text.isBlank() ? transform.parse(text) : null);
            } catch (Exception ex) {
                LOG.warn("Failed to update content", ex);
            }
        }

        /**
         * Notifies all listeners that have registered interest for
         * notification on this event type.  The event instance
         * is lazily created using the parameters passed into
         * the fire method.
         *
         * @param e the event
         * @see EventListenerList
         */
        @Override
        protected void fireInsertUpdate(DocumentEvent e) {
            super.fireInsertUpdate(e);
            updateContent();
        }

        /**
         * Notifies all listeners that have registered interest for
         * notification on this event type.  The event instance
         * is lazily created using the parameters passed into
         * the fire method.
         *
         * @param e the event
         * @see EventListenerList
         */
        @Override
        protected void fireChangedUpdate(DocumentEvent e) {
            super.fireChangedUpdate(e);
            updateContent();
        }

        /**
         * Notifies all listeners that have registered interest for
         * notification on this event type.  The event instance
         * is lazily created using the parameters passed into
         * the fire method.
         *
         * @param e the event
         * @see EventListenerList
         */
        @Override
        protected void fireRemoveUpdate(DocumentEvent e) {
            super.fireRemoveUpdate(e);
            updateContent();
        }

        /**
         * Notifies all listeners that have registered interest for
         * notification on this event type.  The event instance
         * is lazily created using the parameters passed into
         * the fire method.
         *
         * @param e the event
         * @see EventListenerList
         */
        @Override
        protected void fireUndoableEditUpdate(UndoableEditEvent e) {
            super.fireUndoableEditUpdate(e);
            updateContent();
        }
    }

    static class Progress extends DefaultBoundedRangeModel {
        private final HtmlAction htmlAction;

        public Progress(Element el, HtmlAction htmlAction) {
            this(el, htmlAction, minValue(el), maxValue(el));
        }

        private Progress(Element el, HtmlAction htmlAction, int min, int max) {
            this(el, htmlAction, curValue(el, min), 0, min, max);
        }

        private Progress(Element el, HtmlAction htmlAction, int value, int extent, int min, int max) {
            super(value, extent, min, max);
            this.htmlAction = htmlAction;
            htmlAction.addPropertyChangeListener(this::htmlListener);
        }

        private void htmlListener(PropertyChangeEvent propertyChangeEvent) {
            if (SELECTED_KEY.equals(propertyChangeEvent.getPropertyName())) {
                if (propertyChangeEvent.getNewValue() instanceof Number) {
                    setValue(((Number) propertyChangeEvent.getNewValue()).intValue());
                } else if (propertyChangeEvent.getNewValue() instanceof String) {
                    setValue(Integer.parseInt(propertyChangeEvent.getNewValue().toString()));
                }
            }
        }

        /**
         * Runs each <code>ChangeListener</code>'s <code>stateChanged</code> method.
         *
         * @see #setRangeProperties
         * @see EventListenerList
         */
        @Override
        protected void fireStateChanged() {
            super.fireStateChanged();
            htmlAction.putValue(SELECTED_KEY, getValue());
        }

        static int curValue(Element el, int dflt) {
            if (el.hasAttr("value")) {
                return Integer.parseInt(el.attr("value"));
            }
            return dflt;
        }
        static int minValue(Element el) {
            if (el.hasAttr("min")) {
                return Integer.parseInt(el.attr("min"));
            }
            return 0;
        }
        static int maxValue(Element el) {
            if (el.hasAttr("max")) {
                return Integer.parseInt(el.attr("max"));
            }
            return 100;
        }

    }
}
