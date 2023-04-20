package org.xiphis.swing;
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

import com.google.gson.JsonObject;
import org.xiphis.swing.intern.HtmlContext;
import org.xiphis.swing.intern.HtmlEvent;

import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class HtmlJFrame extends JFrame {
    private HtmlJPanel panel;

    /**
     * Constructs a new frame that is initially invisible.
     * <p>
     * This constructor sets the component's locale property to the value
     * returned by <code>JComponent.getDefaultLocale</code>.
     *
     * @throws HeadlessException if GraphicsEnvironment.isHeadless()
     *                           returns true.
     * @see GraphicsEnvironment#isHeadless
     * @see Component#setSize
     * @see Component#setVisible
     * @see JComponent#getDefaultLocale
     */
    public HtmlJFrame(String html) throws HeadlessException {
        this(null, html);
    }

    /**
     * Creates a <code>Frame</code> in the specified
     * <code>GraphicsConfiguration</code> of
     * a screen device and a blank title.
     * <p>
     * This constructor sets the component's locale property to the value
     * returned by <code>JComponent.getDefaultLocale</code>.
     *
     * @param gc the <code>GraphicsConfiguration</code> that is used
     *           to construct the new <code>Frame</code>;
     *           if <code>gc</code> is <code>null</code>, the system
     *           default <code>GraphicsConfiguration</code> is assumed
     * @throws IllegalArgumentException if <code>gc</code> is not from
     *                                  a screen device.  This exception is always thrown when
     *                                  GraphicsEnvironment.isHeadless() returns true.
     * @see GraphicsEnvironment#isHeadless
     * @see JComponent#getDefaultLocale
     * @since 1.3
     */
    public HtmlJFrame(GraphicsConfiguration gc, String html) {
        this(gc, new HtmlContext(html));
    }

    public HtmlJFrame(GraphicsConfiguration gc, HtmlContext context) {
        super(context.document().title(), gc);
        init(context);
    }

    private void init(HtmlContext context) {

        panel = new HtmlJPanel(context, context.document().body());
        getContentPane().add(new JScrollPane(panel), BorderLayout.CENTER);

        // TODO need to fix the bottom inset
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 45, 10));

        //setResizable(context.document().body().hasAttr("resizable"));

        pack();
        doLayout();
    }

    public HtmlJFrame onSubmit(Predicate<HtmlEvent> handler) {
        panel.onSubmit(handler);
        return this;
    }

    public HtmlJFrame onClicked(String id, Consumer<HtmlEvent> handler) {
        panel.onClicked(id, handler);
        return this;
    }

    public HtmlJFrame onReset(Predicate<HtmlEvent> handler) {
        panel.onReset(handler);
        return this;
    }

    public Component componentById(String id) {
        return panel.context().getComponentById(id);
    }

    public void setValues(JsonObject jsonObject) {
        panel.context().setValues(jsonObject);
    }
    public JsonObject toJson() {
        return panel.context().toJson();
    }

}
