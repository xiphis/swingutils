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

        //getContentPane().add(new JScrollPane(panel,
        //        JScrollPane.VERTICAL_SCROLLBAR_NEVER, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER),
        //        BorderLayout.CENTER);

        // TODO need to fix the bottom inset
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 45, 10));

        panel.context().applyStyle(panel, context.document().body());
        //setResizable(context.document().body().hasAttr("resizable"));

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
