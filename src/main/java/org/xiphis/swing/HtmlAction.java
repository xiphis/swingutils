package org.xiphis.swing;

import org.jsoup.nodes.Element;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class HtmlAction extends AbstractAction {

    private final HtmlContext context;
    private final Element element;

    public HtmlAction(HtmlContext context, Element element, String name) {
        super(name);
        this.context = context;
        this.element = element;

        if (element.hasAttr("alt")) {
            putValue(Action.SHORT_DESCRIPTION, element.attr("alt"));
        }
        if (element.hasAttr("disabled")) {
            setEnabled(false);
        }
    }

    public Element element() {
        return element;
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
}
