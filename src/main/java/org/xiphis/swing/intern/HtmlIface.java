package org.xiphis.swing.intern;

import com.google.gson.JsonObject;

import java.awt.*;

public interface HtmlIface {

    HtmlContext context();

    default  <T extends Component> T componentById(String id) {
        return context().getComponentById(id);
    }

    default void setValues(JsonObject jsonObject) {
        context().setValues(jsonObject);
    }
    default JsonObject toJson() {
        return context().toJson();
    }

}
