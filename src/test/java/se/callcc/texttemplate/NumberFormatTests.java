package se.callcc.texttemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.junit.jupiter.api.Test;

public class NumberFormatTests {

    private final NumberFormatter formatter = new NumberFormatter();

    @Test
    void testIntegerFormatting() {
        assertThat(formatter.format(1234, "0")).isEqualTo("1234");
        assertThat(formatter.format(1234, "#,##0")).isEqualTo("1,234");
        assertThat(formatter.format(12, "0000")).isEqualTo("0012");
        assertThat(formatter.format(1234, "+0;-0")).isEqualTo("+1234");
        assertThat(formatter.format(-1234, "+0;-0")).isEqualTo("-1234");
    }

    @Test
    void testDecimalFormatting() {
        assertThat(formatter.format(1234.56, "0.00")).isEqualTo("1234.56");
        assertThat(formatter.format(1234.56, "#,##0.00")).isEqualTo("1,234.56");
        assertThat(formatter.format(1234.567, "0.###")).isEqualTo("1234.567");
        assertThat(formatter.format(1234.567, "0.00E0")).isEqualTo("1.23E3");
    }

    @Test
    void testVariableLeadingZeros() {
        assertThat(formatter.format(12, "00000")).isEqualTo("00012");
    }

    @Test
    void testUnsupportedFormat() {
        assertThatThrownBy(() -> formatter.format(1234, "unsupported"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Unsupported format");
    }

    @Test
    void testNonNumberValue() {
        assertThatThrownBy(() -> formatter.format("not a number", "0"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Value must be a subtype of Number");
    }


    @Test
    void testBigDecimalFormatting() {
        BigDecimal bigDecimal = new BigDecimal("1234.5678");
        assertThat(formatter.format(bigDecimal, "0.00")).isEqualTo("1234.57");
        assertThat(formatter.format(bigDecimal, "#,##0.00")).isEqualTo("1,234.57");
        assertThat(formatter.format(bigDecimal, "0.###")).isEqualTo("1234.568");
        assertThat(formatter.format(bigDecimal, "0.00E0")).isEqualTo("1.23E3");
    }


    @Test
    void testBigIntegerFormatting() {
        BigInteger bigInteger = new BigInteger("12345678901234567890");
        assertThat(formatter.format(bigInteger, "0")).isEqualTo("12345678901234567890");
        assertThat(formatter.format(bigInteger, "#,##0")).isEqualTo("12,345,678,901,234,567,890");
        assertThat(formatter.format(bigInteger, "00000000000000000000")).isEqualTo("12345678901234567890");
        assertThat(formatter.format(bigInteger, "+0;-0")).isEqualTo("+12345678901234567890");
    }
}
