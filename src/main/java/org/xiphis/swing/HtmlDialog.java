package org.xiphis.swing;

import javax.swing.*;
import java.awt.*;

public class HtmlDialog extends JDialog {

    public HtmlDialog(Frame owner, String html, boolean modal) {
        this(owner, new HtmlContext(html), modal);
    }

    public HtmlDialog(Frame owner, HtmlContext context, boolean modal) {
        super(owner, context.document().title(), modal);

        HtmlPanel panel = new HtmlPanel(context, context.document().body());
        getContentPane().add(panel, BorderLayout.CENTER);

        // TODO need to fix the bottom inset
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 45, 10));

        //setResizable(context.document().body().hasAttr("resizable"));

        pack();
    }
}
