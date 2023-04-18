package org.xiphis.swing.intern;
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
        this.element = action.element((Component) event.getSource());
        this.id = element.attr("id");
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

    public JsonObject getState() {
        if (action instanceof HtmlAction) {
            return ((HtmlAction) action).context().toJson();
        }
        return null;
    }

    public void setState(JsonObject state) {
        ((HtmlAction) action).context().setValues(state);
    }

    public String getId() {
        return id;
    }

    public Window window() {
        return window;
    }
}
