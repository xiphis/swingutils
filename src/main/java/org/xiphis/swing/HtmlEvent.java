package org.xiphis.swing;

import org.jsoup.nodes.Element;

import javax.swing.*;
import java.awt.*;
import java.util.EventObject;

public class HtmlEvent {
    private final Element element;
    private final EventObject event;
    private final Window window;
    private final Action action;
    private final String id;

    public HtmlEvent(HtmlAction action, EventObject event) {
        this.action = action;
        this.event = event;
        this.element = action.element();
        this.id = action.element().attr("id");
        if (event.getSource() instanceof Component) {
            Component c = (Component) event.getSource();
            for (;;) {
                if (c instanceof Window) {
                    window = (Window) c;
                    break;
                }
                c = c.getParent();
            }
        } else {
            window = null;
        }
    }

    public Element getElement() {
        return element;
    }

    public EventObject getEvent() {
        return event;
    }

    public Action getAction() {
        return action;
    }

    public String getId() {
        return id;
    }

    public Window window() {
        return window;
    }
}
