package org.xiphis.swing;

import org.jsoup.nodes.Element;
import org.xiphis.swing.intern.HtmlContext;
import org.xiphis.swing.intern.HtmlPanel;

public class HtmlJPanel extends HtmlPanel {
    public HtmlJPanel(String html) {
        this(new HtmlContext(html, true));
        context().init();
    }

    private HtmlJPanel(HtmlContext context) {
        this(context, context.document());
    }

    HtmlJPanel(HtmlContext context, Element body) {
        super(context, body);
    }

}
