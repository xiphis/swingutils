package org.xiphis.swing;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import javax.swing.*;
import java.awt.*;

public class HtmlDialog extends JDialog {

    private transient boolean packed;

    public HtmlDialog(Frame owner, String html, boolean modal) {
        this(owner, new HtmlContext(html), modal);
    }

    public HtmlDialog(Frame owner, HtmlContext context, boolean modal) {
        super(owner, context.document().title(), modal);

        HtmlPanel panel = new HtmlPanel(context, context.document().body(), BorderFactory.createEmptyBorder());
        getContentPane().add(panel, BorderLayout.CENTER);

        //setResizable(context.document().body().hasAttr("resizable"));

        if (context.document().body().hasAttr("width")
            || context.document().body().hasAttr("height")) {
            Dimension max = panel.getMaximumSize();
            if (context.document().body().hasAttr("width")) {
                max.width = Integer.parseUnsignedInt(context.document().body().attr("width"));
            }
            if (context.document().body().hasAttr("height")) {
                max.height = Integer.parseUnsignedInt(context.document().body().attr("height"));
            }
            panel.setMaximumSize(max);
            panel.setSize(max);
            System.out.println("max size = " + max);
        }


        pack();
    }

    @Override
    public void setVisible(boolean b) {
        if (b && !packed) {
            Window owner = getOwner();
            pack();
            Dimension dialogDimension = getSize();
            Dimension ownerDimension = owner.getSize();
            int x = Math.max((ownerDimension.width - dialogDimension.width) / 2, 0);
            int y = Math.max((ownerDimension.height - dialogDimension.height) / 2, 0);
            setBounds(owner.getX() + x, owner.getY() + y, dialogDimension.width, dialogDimension.height);
        }
        super.setVisible(b);
    }

    /* public HtmlDialog(Window owner, String html, boolean modal) {
        this(owner, Jsoup.parse(html), modal);
    }

    public HtmlDialog(Window owner, Document doc, boolean modal) {
        super(owner, doc.title(), modal);

        setContentPane(new HtmlPanel(doc.body()));
    }*/
}
