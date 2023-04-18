package org.xiphis.swing.intern;

import java.util.Objects;

public final class ComboBoxOption {
    private final String value;
    private final String text;

    private transient int hashCode;

    public ComboBoxOption(String value, String text) {
        this.value = Objects.requireNonNull(value);
        this.text = Objects.requireNonNull(text);
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ComboBoxOption that = (ComboBoxOption) o;
        return Objects.equals(value, that.value) && Objects.equals(text, that.text);
    }

    @Override
    public int hashCode() {
        if (hashCode == 0) {
            hashCode = Objects.hash(value, text);
        }
        return hashCode;
    }

    @Override
    public String toString() {
        return text;
    }
}
