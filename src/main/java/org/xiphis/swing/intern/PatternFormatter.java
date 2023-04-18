package org.xiphis.swing.intern;

import javax.swing.text.DefaultFormatter;
import java.text.ParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PatternFormatter extends DefaultFormatter {

    public static final Pattern EMAIL_PATTERN = Pattern.compile("^[\\w-.]+@([\\w-]+\\.)+[\\w-]{2,4}");

    private Pattern pattern;
    private transient Matcher matcher;

    public PatternFormatter() {

    }

    public PatternFormatter(String regex) {
        this();
        setPattern(Pattern.compile(regex));
    }

    public PatternFormatter(Pattern pattern) {
        this();
        setPattern(pattern);
    }

    public void setPattern(Pattern pattern) {
        this.pattern = pattern;
        this.matcher = null;
    }

    public Pattern getPattern() {
        return pattern;
    }

    protected void setMatcher(Matcher matcher) {
        if (matcher.pattern() != getPattern()) {
            throw new IllegalArgumentException();
        }
        this.matcher = matcher;
    }

    protected Matcher getMatcher() {
        return matcher;
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
        Pattern pattern = getPattern();

        if (pattern != null) {
            Matcher matcher = pattern.matcher(string);

            if (matcher.matches()) {
                setMatcher(matcher);
                return super.stringToValue(string);
            }
            throw new ParseException("Pattern does not match", 0);
        }
        return string;
    }
}
