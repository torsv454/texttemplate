package se.callcc.texttemplate;

import java.util.ArrayList;
import java.util.List;

public class Formatters {

	private final List<ValueFormatter> formatters = new ArrayList<>();

	public Formatters( List<ValueFormatter> formatters) {
		this.formatters.addAll(formatters);
	}


	public ValueFormatter find(String format) {
		for(final var formatter : formatters) {
			if(formatter.isSupportedFormat(format)) {
				return formatter;
			}
		}
		throw new IllegalArgumentException("Unsupported format %s".formatted(format));
	}
}
