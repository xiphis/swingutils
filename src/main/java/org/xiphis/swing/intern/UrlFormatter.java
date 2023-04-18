package org.xiphis.swing.intern;

import javax.swing.text.DefaultFormatter;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UrlFormatter extends DefaultFormatter {

    public static final UrlFormatter INSTANCE = new UrlFormatter();

    public UrlFormatter() {

    }

    /**
     * Converts the passed in String into an instance of
     * <code>getValueClass</code> by way of the constructor that
     * takes a String argument. If <code>getValueClass</code>
     * returns null, the Class of the current value in the
     * <code>JFormattedTextField</code> will be used. If this is null, a
     * String will be returned. If the constructor throws an exception, a
     * <code>ParseException</code> will be thrown. If there is no single
     * argument String constructor, <code>string</code> will be returned.
     *
     * @param string String to convert
     * @return Object representation of text
     * @throws ParseException if there is an error in the conversion
     */
    @Override
    public Object stringToValue(String string) throws ParseException {
        try {
            new URL(string);
            return super.stringToValue(string);
        } catch (MalformedURLException e) {
            throw new ParseException("Pattern does not match", 0);
        }
    }
}
