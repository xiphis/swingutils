package org.xiphis.swing;

import org.xiphis.swing.intern.HtmlContext;
import org.xiphis.swing.intern.HtmlEvent;
import org.xiphis.swing.intern.HtmlPanel;

import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class HtmlJDialog extends JDialog {

    private HtmlJPanel panel;

    public HtmlJDialog(Frame owner, String html, boolean modal) {
        this(owner, new HtmlContext(html), modal);
        panel.context().init();
    }

    public HtmlJDialog(Frame owner, HtmlContext context, boolean modal) {
        super(owner, context.document().title(), modal);

        panel = new HtmlJPanel(context, context.document().body());
        getContentPane().add(panel, BorderLayout.CENTER);

        // TODO need to fix the bottom inset
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 45, 10));

        setResizable(context.document().body().hasAttr("resizable"));

        pack();
        doLayout();
    }

    public HtmlJDialog onSubmit(Predicate<HtmlEvent> handler) {
        panel.onSubmit(handler);
        return this;
    }

    public HtmlJDialog onClicked(String id, Consumer<HtmlEvent> handler) {
        panel.onClicked(id, handler);
        return this;
    }

    public HtmlJDialog onReset(Predicate<HtmlEvent> handler) {
        panel.onReset(handler);
        return this;
    }
}
