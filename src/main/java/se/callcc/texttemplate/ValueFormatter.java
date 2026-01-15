package se.callcc.texttemplate;

public interface ValueFormatter {

	boolean isSupportedFormat(String format);
	String format(Object value, String format);
}
