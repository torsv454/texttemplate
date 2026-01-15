package se.callcc.texttemplate;

import java.text.DecimalFormat;
import java.util.Set;
import java.util.regex.Pattern;

public class NumberFormatter implements ValueFormatter {

    private static final Set<String> BASE_SUPPORTED_FORMATS = Set.of(
        "0", "#,##0", "+0;-0", "0.00", "#,##0.00", "0.###", "0.00E0"
    );

    private static final Pattern LEADING_ZERO_PATTERN = Pattern.compile("0+");

    @Override
    public String format(Object value, String format) {
        if (!(value instanceof Number)) {
            throw new IllegalArgumentException("Value must be a subtype of Number");
        }

        if (!isSupportedFormat(format)) {
            throw new IllegalArgumentException("Unsupported format: " + format);
        }

        final var decimalFormat = getFormatter(format);
        return decimalFormat.format(value);
    }

	private DecimalFormat getFormatter(String format) {
		return new DecimalFormat(format);
	}

    public boolean isSupportedFormat(String format) {
        if (BASE_SUPPORTED_FORMATS.contains(format)) {
            return true;
        }
        // Check for variable leading zeros
        return LEADING_ZERO_PATTERN.matcher(format).matches();
    }
}
