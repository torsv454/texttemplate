package se.callcc.texttemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.TimeZone;

import org.junit.jupiter.api.Test;

public class DateFormatTests  {

	private final DateFormatter formatter = new DateFormatter(TimeZone.getTimeZone("CET"));

	private final Date date = new Date(1672527600000L); // 2023-01-01T00:00:00Z
	private final LocalDate localDate = LocalDate.of(2023, 1, 1);
	private final LocalDateTime localDateTime = LocalDateTime.of(2023, 1, 1, 0, 0, 0);
	private final ZonedDateTime zonedDateTime = ZonedDateTime.of(2023, 1, 1, 0, 0, 0, 0,
			TimeZone.getTimeZone("CET").toZoneId());
	private final Instant instant = Instant.ofEpochMilli(1672527600000L); // 2023-01-01T00:00:00Z

	@Test
	void testDateFormatting() {
		assertThat(formatter.format(date, "yyyy")).isEqualTo("2023");
		assertThat(formatter.format(date, "MM")).isEqualTo("01");
		assertThat(formatter.format(date, "dd")).isEqualTo("01");
		assertThat(formatter.format(date, "HH")).isEqualTo("00");
		assertThat(formatter.format(date, "mm")).isEqualTo("00");
		assertThat(formatter.format(date, "ss")).isEqualTo("00");
		assertThat(formatter.format(date, "E")).isEqualTo("Sun");
		assertThat(formatter.format(date, "MMM")).isEqualTo("Jan");
		assertThat(formatter.format(date, "MMMM")).isEqualTo("January");
		assertThat(formatter.format(date, "dd/MM/yyyy")).isEqualTo("01/01/2023");
		assertThat(formatter.format(date, "MM/dd/yyyy")).isEqualTo("01/01/2023");
		assertThat(formatter.format(date, "yyyy-MM-dd")).isEqualTo("2023-01-01");
		assertThat(formatter.format(date, "dd.MM.yyyy")).isEqualTo("01.01.2023");
		assertThat(formatter.format(date, "EEEE, MMMM dd, yyyy")).isEqualTo("Sunday, January 01, 2023");
		assertThat(formatter.format(date, "MM/dd/yyyy HH:mm:ss")).isEqualTo("01/01/2023 00:00:00");
		assertThat(formatter.format(date, "dd/MM/yyyy HH:mm:ss")).isEqualTo("01/01/2023 00:00:00");
	}

	@Test
	void testLocalDateFormatting() {
		assertThat(formatter.format(localDate, "yyyy")).isEqualTo("2023");
		assertThat(formatter.format(localDate, "MM")).isEqualTo("01");
		assertThat(formatter.format(localDate, "dd")).isEqualTo("01");
		assertThat(formatter.format(localDate, "E")).isEqualTo("Sun");
		assertThat(formatter.format(localDate, "MMM")).isEqualTo("Jan");
		assertThat(formatter.format(localDate, "MMMM")).isEqualTo("January");
		assertThat(formatter.format(localDate, "dd/MM/yyyy")).isEqualTo("01/01/2023");
		assertThat(formatter.format(localDate, "MM/dd/yyyy")).isEqualTo("01/01/2023");
		assertThat(formatter.format(localDate, "yyyy-MM-dd")).isEqualTo("2023-01-01");
		assertThat(formatter.format(localDate, "dd.MM.yyyy")).isEqualTo("01.01.2023");
		assertThat(formatter.format(localDate, "EEEE, MMMM dd, yyyy")).isEqualTo("Sunday, January 01, 2023");
	}

	@Test
	void testLocalDateTimeFormatting() {
		assertThat(formatter.format(localDateTime, "yyyy")).isEqualTo("2023");
		assertThat(formatter.format(localDateTime, "MM")).isEqualTo("01");
		assertThat(formatter.format(localDateTime, "dd")).isEqualTo("01");
		assertThat(formatter.format(localDateTime, "HH")).isEqualTo("00");
		assertThat(formatter.format(localDateTime, "mm")).isEqualTo("00");
		assertThat(formatter.format(localDateTime, "ss")).isEqualTo("00");
		assertThat(formatter.format(localDateTime, "E")).isEqualTo("Sun");
		assertThat(formatter.format(localDateTime, "MMM")).isEqualTo("Jan");
		assertThat(formatter.format(localDateTime, "MMMM")).isEqualTo("January");
		assertThat(formatter.format(localDateTime, "dd/MM/yyyy")).isEqualTo("01/01/2023");
		assertThat(formatter.format(localDateTime, "MM/dd/yyyy")).isEqualTo("01/01/2023");
		assertThat(formatter.format(localDateTime, "yyyy-MM-dd")).isEqualTo("2023-01-01");
		assertThat(formatter.format(localDateTime, "dd.MM.yyyy")).isEqualTo("01.01.2023");
		assertThat(formatter.format(localDateTime, "EEEE, MMMM dd, yyyy")).isEqualTo("Sunday, January 01, 2023");
		assertThat(formatter.format(localDateTime, "MM/dd/yyyy HH:mm:ss")).isEqualTo("01/01/2023 00:00:00");
		assertThat(formatter.format(localDateTime, "dd/MM/yyyy HH:mm:ss")).isEqualTo("01/01/2023 00:00:00");
	}

	@Test
	void testZonedDateTimeFormatting() {
		assertThat(formatter.format(zonedDateTime, "yyyy")).isEqualTo("2023");
		assertThat(formatter.format(zonedDateTime, "MM")).isEqualTo("01");
		assertThat(formatter.format(zonedDateTime, "dd")).isEqualTo("01");
		assertThat(formatter.format(zonedDateTime, "HH")).isEqualTo("00");
		assertThat(formatter.format(zonedDateTime, "mm")).isEqualTo("00");
		assertThat(formatter.format(zonedDateTime, "ss")).isEqualTo("00");
		assertThat(formatter.format(zonedDateTime, "E")).isEqualTo("Sun");
		assertThat(formatter.format(zonedDateTime, "MMM")).isEqualTo("Jan");
		assertThat(formatter.format(zonedDateTime, "MMMM")).isEqualTo("January");
		assertThat(formatter.format(zonedDateTime, "dd/MM/yyyy")).isEqualTo("01/01/2023");
		assertThat(formatter.format(zonedDateTime, "MM/dd/yyyy")).isEqualTo("01/01/2023");
		assertThat(formatter.format(zonedDateTime, "yyyy-MM-dd")).isEqualTo("2023-01-01");
		assertThat(formatter.format(zonedDateTime, "dd.MM.yyyy")).isEqualTo("01.01.2023");
		assertThat(formatter.format(zonedDateTime, "EEEE, MMMM dd, yyyy")).isEqualTo("Sunday, January 01, 2023");
		assertThat(formatter.format(zonedDateTime, "MM/dd/yyyy HH:mm:ss")).isEqualTo("01/01/2023 00:00:00");
		assertThat(formatter.format(zonedDateTime, "dd/MM/yyyy HH:mm:ss")).isEqualTo("01/01/2023 00:00:00");
	}

	@Test
	void testInstantFormatting() {
		assertThat(formatter.format(instant, "yyyy")).isEqualTo("2023");
		assertThat(formatter.format(instant, "MM")).isEqualTo("01");
		assertThat(formatter.format(instant, "dd")).isEqualTo("01");
		assertThat(formatter.format(instant, "HH")).isEqualTo("00");
		assertThat(formatter.format(instant, "mm")).isEqualTo("00");
		assertThat(formatter.format(instant, "ss")).isEqualTo("00");
		assertThat(formatter.format(instant, "E")).isEqualTo("Sun");
		assertThat(formatter.format(instant, "MMM")).isEqualTo("Jan");
		assertThat(formatter.format(instant, "MMMM")).isEqualTo("January");
		assertThat(formatter.format(instant, "dd/MM/yyyy")).isEqualTo("01/01/2023");
		assertThat(formatter.format(instant, "MM/dd/yyyy")).isEqualTo("01/01/2023");
		assertThat(formatter.format(instant, "yyyy-MM-dd")).isEqualTo("2023-01-01");
		assertThat(formatter.format(instant, "dd.MM.yyyy")).isEqualTo("01.01.2023");
		assertThat(formatter.format(instant, "EEEE, MMMM dd, yyyy")).isEqualTo("Sunday, January 01, 2023");
		assertThat(formatter.format(instant, "MM/dd/yyyy HH:mm:ss")).isEqualTo("01/01/2023 00:00:00");
		assertThat(formatter.format(instant, "dd/MM/yyyy HH:mm:ss")).isEqualTo("01/01/2023 00:00:00");
	}

	@Test
	void testUnsupportedValue() {
		assertThatThrownBy(() -> formatter.format("not a date", "yyyy-MM-dd"))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("Value must be a Date, LocalDate, LocalDateTime, ZonedDateTime, or Instant");
	}
}
