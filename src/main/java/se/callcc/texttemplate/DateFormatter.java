package se.callcc.texttemplate;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Set;
import java.util.TimeZone;

public class DateFormatter implements ValueFormatter {

	private static final Set<String> SUPPORTED_FORMATS = Set.of("yyyy", "MM", "dd", "HH", "mm", "ss", "E", "MMM",
			"MMMM", "dd/MM/yyyy", "MM/dd/yyyy", "yyyy-MM-dd", "dd.MM.yyyy", "EEEE, MMMM dd, yyyy",
			"MM/dd/yyyy HH:mm:ss", "dd/MM/yyyy HH:mm:ss");

	private final TimeZone timeZone;

	public DateFormatter(TimeZone timeZone) {
		this.timeZone = timeZone;
	}

	@Override
	public String format(Object value, String format) {
		if (value instanceof Date date) {
			return DateTimeFormatter.ofPattern(format).withZone(timeZone.toZoneId()).format(date.toInstant());
		} else if (value instanceof LocalDate localDate) {
			return (localDate).format(DateTimeFormatter.ofPattern(format));
		} else if (value instanceof LocalDateTime localDateTime) {
			return (localDateTime).format(DateTimeFormatter.ofPattern(format));
		} else if (value instanceof ZonedDateTime zonedDateTime) {
			return (zonedDateTime).format(DateTimeFormatter.ofPattern(format));
		} else if (value instanceof Instant instant) {
			return DateTimeFormatter.ofPattern(format).withZone(timeZone.toZoneId()).format(instant);
		} else {
			throw new IllegalArgumentException(
					"Value must be a Date, LocalDate, LocalDateTime, ZonedDateTime, or Instant");
		}
	}

	public boolean isSupportedFormat(String format) {
		if (SUPPORTED_FORMATS.contains(format)) {
			return true;
		}
		return false;
	}

}
