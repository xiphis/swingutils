package org.xiphis.swing;

import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.*;

public class HtmlForm extends HtmlPanel {

    private Map<String, JComponent> idMap;
    private transient LinkedList<Map.Entry<Element, JLabel>> labels;
    public HtmlForm(HtmlContext context, Element body) {
        super(context, body);

        if (idMap == null) {
            idMap = Collections.emptyMap();
        }

        if (labels != null) {
            for (Map.Entry<Element, JLabel> entry : labels) {
                if (entry.getKey().hasAttr("for")) {
                    JComponent component = idMap.get(entry.getKey().attr("for"));
                    if (component != null) {
                        entry.getValue().setLabelFor(component);
                    }
                }
            }
            labels = null;
        }
    }

    @Override
    protected HtmlForm form() {
        return this;
    }

    public void addLabel(Element el, JLabel label) {
        if (labels == null) {
            labels = new LinkedList<>();
        }
        labels.add(new AbstractMap.SimpleImmutableEntry<>(el, label));
    }

    public void addInput(Element el, JComponent input) {
        if (el.hasAttr("id")) {
            if (idMap == null) {
                idMap = new HashMap<>();
            }
            idMap.put(el.attr("id"), input);
        }
    }

    public void submit(SubmitAction action) {
        Container p = getParent();
        do {
            if (p instanceof JDialog) {
                JDialog dialog = (JDialog) p;
                if (context().submit(this, action)) {
                    dialog.setVisible(false);
                }
            }
            p = p.getParent();
        } while (p != null);
    }

    public static class SubmitAction extends AbstractAction {

        private static final Logger LOG = LoggerFactory.getLogger(SubmitAction.class);

        private final JPanel panel;
        public SubmitAction(JPanel panel, String name) {
            super(name);
            this.panel = panel;
        }

        public SubmitAction(JPanel panel, String name, Icon icon) {
            super(name, icon);
            this.panel = panel;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            Component p = panel;
            if (p == null) {
                LOG.warn("Action not in a form");
                return;
            }
            do {
                if (p instanceof HtmlForm) {
                    ((HtmlForm) p).submit(this);
                    return;
                }
                p = p.getParent();
            } while (p != null);
        }
    }
}
