package org.xiphis.swing;

import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class HtmlDialog extends JDialog {

    private HtmlPanel panel;

    public HtmlDialog(Frame owner, String html, boolean modal) {
        this(owner, new HtmlContext(html), modal);
    }

    public HtmlDialog(Frame owner, HtmlContext context, boolean modal) {
        super(owner, context.document().title(), modal);

        panel = new HtmlPanel(context, context.document().body());
        getContentPane().add(panel, BorderLayout.CENTER);

        // TODO need to fix the bottom inset
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 45, 10));

        //setResizable(context.document().body().hasAttr("resizable"));

        pack();
    }

    public HtmlDialog onSubmit(Predicate<HtmlEvent> handler) {
        panel.onSubmit(handler);
        return this;
    }

    public HtmlDialog onClicked(String id, Consumer<HtmlEvent> handler) {
        panel.onClicked(id, handler);
        return this;
    }

    public HtmlDialog onReset(Predicate<HtmlEvent> handler) {
        panel.onReset(handler);
        return this;
    }
}
