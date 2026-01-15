package se.callcc.texttemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.function.Function;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import se.callcc.texttemplate.TextTemplate.DefaultRenderOptions;
import se.callcc.texttemplate.TextTemplate.RenderOptions;
import se.callcc.texttemplate.TextTemplate.Template;
import se.callcc.texttemplate.TextTemplate.TextTemplateMacro;
import se.callcc.texttemplate.TextTemplate.TextTemplateStringMacro;

public class TextTemplateTests {

	@Nested
	class VariableSubstitution {

		@Test
		void simpleVariable() {
			check("${name}", Map.of("name", "Alice"), "Alice");
			check("Hello ${name}!", Map.of("name", "Alice"), "Hello Alice!");
			check("${name} says hello!", Map.of("name", "Alice"), "Alice says hello!");
		}

		@Test
		void multipleVariables() {
			check("${name} says ${name}${name}", Map.of("name", "Alice"), "Alice says AliceAlice");
			check("${a} ${b} ${c}", Map.of("a", "1", "b", "2", "c", "3"), "1 2 3");
		}

		@Test
		void missingVariableRendersEmpty() {
			check("Hello ${unknown}!", Map.of(), "Hello !");
			check("${missing}", Map.of(), "");
		}

		@Test
		void dollarEscaping() {
			check("$$", Map.of(), "$");
			check("a$$b", Map.of(), "a$b");
			check("$$${name}$$", Map.of("name", "x"), "$x$");
		}

		@Test
		void emptyTemplate() {
			check("", Map.of(), "");
		}

		@Test
		void plainText() {
			check("Hello", Map.of(), "Hello");
			check("\nHel\nlo\n", Map.of(), "\nHel\nlo\n");
		}

		@Test
		void whitespacePreservation() {
			check("  ${name}  ", Map.of("name", "x"), "  x  ");
			check("\t${name}\t", Map.of("name", "x"), "\tx\t");
		}

		@Test
		void formatting() {
			check("${count|00000}", Map.of("count", 5), "00005");
		}
	}

	@Nested
	class Conditionals {

		@Test
		void ifTrue() {
			check("$if(hasName)Name: ${name}$end", Map.of("hasName", true, "name", "Alice"), "Name: Alice");
			check("$if(hasName)visible$end", Map.of("hasName", "yes"), "visible");
		}

		@Test
		void ifFalse() {
			check("$if(hasNoName)Name: ${name}$end", Map.of("name", "Alice"), "");
			check("$if(empty)visible$end", Map.of("empty", ""), "");
		}

		@Test
		void unlessTrue() {
			check("$unless(hasName)fallback$end", Map.of("hasName", true), "");
		}

		@Test
		void unlessFalse() {
			check("$unless(hasNoName)Name: ${name}$end", Map.of("name", "Alice"), "Name: Alice");
			check("$unless(missing)shown$end", Map.of(), "shown");
		}

		@Test
		void nestedConditionals() {
			var ctx = Map.<String, Object>of("a", true, "b", true);
			check("$if(a)$if(b)both$end$end", ctx, "both");
			// Note: whitespace after $end is trimmed by the parser
			check("$if(a)$if(missing)inner$end outer$end", ctx, "outer");
		}
	}

	@Nested
	class EqualityConditionals {

		@Test
		void ifEqMatches() {
			check("""
					$if_eq(name, "Alice")
					bananas
					$end""", Map.of("name", "Alice"), """
					bananas
					""");
		}

		@Test
		void ifEqNoMatch() {
			check("""
					$if_eq(name, "Frog")
					bananas
					$end""", Map.of("name", "Alice"), """
					""");
		}

		@Test
		void unlessEqMatches() {
			check("""
					$unless_eq(name, "Alice")
					bananas
					$end""", Map.of("name", "Alice"), """
					""");
		}

		@Test
		void unlessEqNoMatch() {
			check("""
					$unless_eq(name, "Frog")
					bananas
					$end""", Map.of("name", "Alice"), """
					bananas
					""");
		}
	}

	@Nested
	class ComparisonConditionals {

		@Test
		void greaterThanTrue() {
			check("""
					$greater_than(count, 3)
					bananas
					$end""", Map.of("count", 5), """
					bananas
					""");
		}

		@Test
		void greaterThanFalse() {
			check("""
					$greater_than(count, 6)
					bananas
					$end""", Map.of("count", 5), """
					""");
		}

		@Test
		void greaterThanBoundary() {
			// count == 5, not > 5, so should be false
			check("""
					$greater_than(count, 5)
					bananas
					$end""", Map.of("count", 5), """
					""");
		}

		@Test
		void greaterThanMissingVariable() {
			check("""
					$greater_than(num, 3)
					bananas
					$end""", Map.of(), """
					""");
		}

		@Test
		void lessThanTrue() {
			check("""
					$less_than(count, 7)
					bananas
					$end""", Map.of("count", 5), """
					bananas
					""");
		}

		@Test
		void lessThanFalse() {
			check("""
					$less_than(count, 4)
					bananas
					$end""", Map.of("count", 5), """
					""");
		}

		@Test
		void lessThanBoundary() {
			// count == 5, not < 5, so should be false
			check("""
					$less_than(count, 5)
					bananas
					$end""", Map.of("count", 5), """
					""");
		}

		@Test
		void lessThanMissingVariable() {
			check("""
					$less_than(num, 3)
					bananas
					$end""", Map.of(), """
					""");
		}
	}

	@Nested
	class HasManyConditionals {

		@Test
		void ifHasManyWithMultipleItems() {
			check("""
					$if_has_many(items)
					bananas
					$end""", Map.of("items", List.of(1, 2, 3)), """
					bananas
					""");
		}

		@Test
		void ifHasManyWithSingleItem() {
			check("""
					$if_has_many(items)
					bananas
					$end""", Map.of("items", List.of(1)), """
					""");
		}

		@Test
		void ifHasManyWithEmptyList() {
			check("""
					$if_has_many(emptyItems)
					bananas
					$end""", Map.of("emptyItems", List.of()), """
					""");
		}

		@Test
		void unlessHasManyWithMultipleItems() {
			check("""
					$unless_has_many(items)
					bananas
					$end""", Map.of("items", List.of(1, 2, 3)), """
					""");
		}

		@Test
		void unlessHasManyWithSingleItem() {
			check("""
					$unless_has_many(items)
					bananas
					$end""", Map.of("items", List.of(1)), """
					bananas
					""");
		}

		@Test
		void unlessHasManyWithEmptyList() {
			check("""
					$unless_has_many(emptyItems)
					bananas
					$end""", Map.of("emptyItems", List.of()), """
					bananas
					""");
		}
	}

	@Nested
	class Loops {

		@Test
		void eachOverList() {
			check("""
					$each(items)
					- ${it}
					$end""", Map.of("items", List.of("Item1", "Item2", "Item3")), """
					- Item1
					- Item2
					- Item3
					""");
		}

		@Test
		void eachOverEmptyList() {
			check("$each(emptyItems)- ${it}\n$end", Map.of("emptyItems", List.of()), "");
		}

		@Test
		void eachOverListOfMaps() {
			check("""
					$each(persons)
					- ${name}
					$end""", Map.of("persons", List.of(Map.of("name", "John"), Map.of("name", "Jane"))), """
					- John
					- Jane
					""");
		}

		@Test
		void eachWithParentAccess() {
			var ctx = Map.of("name", "Alice", "persons", List.of(Map.of("name", "John"), Map.of("name", "Jane")));
			check("""
					$each(persons)
					- ${name} but parent is ${../name}
					$end""", ctx, """
					- John but parent is Alice
					- Jane but parent is Alice
					""");
		}

		@Test
		void nestedLoops() {
			var ctx = Map.<String, Object>of(
					"persons", List.of(Map.of("name", "John"), Map.of("name", "Jane")),
					"items", List.of("Item1", "Item2", "Item3"));
			check("""
					Persons:
					$each(persons)
					- ${name}:
					$each(../items)
						- ${it}
					$end
					$end""", ctx, """
					Persons:
					- John:
						- Item1
						- Item2
						- Item3
					- Jane:
						- Item1
						- Item2
						- Item3
					""");
		}

		@Test
		void eachOverMap() {
			var ctx = Map.<String, Object>of("answers", new TreeMap<>(Map.of(
					"key1", Map.of("value", "value1"),
					"key2", Map.of("value", "value2"),
					"key3", Map.of("value", "value3"))));
			check("""
					$each(answers)
					${key} = ${value}
					$end
					""", ctx, """
					key1 = value1
					key2 = value2
					key3 = value3
					""");
		}
	}

	@Nested
	class FirstDirective {

		@Test
		void firstOnList() {
			check("""
					$first(persons)
					- ${name}
					$end
					""", Map.of("persons", List.of(Map.of("name", "John"), Map.of("name", "Jane"))), """
					- John
					""");
		}

		@Test
		void firstOnEmptyList() {
			check("""
					$first(emptyItems)
					- ${name}
					$end
					""", Map.of("emptyItems", List.of()), """
					""");
		}

		@Test
		void firstOnUnknownVariable() {
			check("""
					$first(unknown)
					- ${name}
					$end
					""", Map.of(), """
					""");
		}
	}

	@Nested
	class LengthDirective {

		@Test
		void lengthOfList() {
			check("$length(items)", Map.of("items", List.of("a", "b", "c")), "3");
		}

		@Test
		void lengthOfString() {
			check("$length(name)", Map.of("name", "Alice"), "5");
		}

		@Test
		void lengthOfEmptyList() {
			check("$length(items)", Map.of("items", List.of()), "0");
		}

		@Test
		void lengthOfUnknownVariable() {
			check("$length(unknown)", Map.of(), "0");
		}

		@Test
		void lengthOfMap() {
			check("$length(map)", Map.of("map", Map.of("a", 1, "b", 2)), "2");
		}

		@Test
		void lengthOfArray() {
			check("$length(arr)", Map.of("arr", new String[]{"a", "b", "c", "d"}), "4");
		}

		@Test
		void lengthOfPrimitiveArray() {
			check("$length(arr)", Map.of("arr", new int[]{1, 2, 3}), "3");
		}
	}

	@Nested
	class IndexDirective {

		@Test
		void indexOnList() {
			check("""
					$index(items, 2)
					$index(items, 1)
					$index(items, 0)
					""", Map.of("items", List.of("Item1", "Item2", "Item3")), """
					Item3
					Item2
					Item1
					""");
		}

		@Test
		void indexOnMap() {
			check("""
					$index(item, key1)
					$index(item, key2)
					$index(item, ${somekey})
					""", Map.of(
					"item", Map.of("key1", "value1", "key2", "value2", "key3", "value3"),
					"somekey", "key3"), """
					value1
					value2
					value3
					""");
		}

		@Test
		void indexOutOfBounds() {
			check("$index(items, 999)", Map.of("items", List.of("a", "b")), "");
		}

		@Test
		void indexNegative() {
			check("$index(items, -1)", Map.of("items", List.of("a", "b")), "");
		}

		@Test
		void indexInvalidNotANumber() {
			check("$index(items, notAnInt)", Map.of("items", List.of("a", "b")), "");
		}

		@Test
		void indexDynamicKeyInMapLoop() {
			var ctx = Map.<String, Object>of(
					"old", Map.of("joblevel", "junior", "jobtitle", "FE engineer"),
					"new", new TreeMap<>(Map.of("joblevel", "senior", "jobtitle", "senior FE engineer")));
			check("""
					|key|old|new|
					|---|---|---|
					$each(new)
					|${key}|$index(../old, ${key})|${it}|
					$end
					""", ctx, """
					|key|old|new|
					|---|---|---|
					|joblevel|junior|senior|
					|jobtitle|FE engineer|senior FE engineer|
					""");
		}
	}

	@Nested
	class IncludeDirective {

		@Test
		void includeNestedTemplates() {
			final var opts = DefaultRenderOptions.withContentIncluder(RenderOptions.RESOURCE_FILE_INCLUDER);
			var ctx = Map.<String, Object>of("name", "Alice");

			// foo.md includes bar.md
			final var expanded = TextTemplate.render("$include(templates/foo.md)", ctx, opts);
			assertThat(expanded).isEqualTo("""
					this is foo
					hello Alice
					this is bar
					hello Alice
					""");
		}

		@Test
		void includeMissingFileThrows() {
			final var opts = DefaultRenderOptions.withContentIncluder(RenderOptions.RESOURCE_FILE_INCLUDER);
			assertThatThrownBy(() -> TextTemplate.render("$include(nonexistent.md)", Map.of(), opts))
					.isInstanceOf(RuntimeException.class)
					.hasMessageContaining("Resource not found");
		}
	}

	@Nested
	class Macros {

		@Test
		void simpleMacro() {
			var opts = DefaultRenderOptions.builder()
					.macro(new TextTemplateMacro() {
						@Override
						public String name() {
							return "wrapper";
						}

						@Override
						public String apply(Map<String, String> arguments) {
							return "BEGIN" + arguments.get("body") + "END";
						}
					})
					.build();

			var result = TextTemplate.render("""
					$call(wrapper)
					$arg(body)

					hello
					$end
					$end

					""", Map.of(), opts);

			assertThat(result).isEqualTo("""
					BEGIN
					hello
					END
					""");
		}

		@Test
		void macroWithMultipleArgs() {
			var opts = DefaultRenderOptions.builder()
					.macro(new TextTemplateMacro() {
						@Override
						public String name() {
							return "link";
						}

						@Override
						public String apply(Map<String, String> arguments) {
							return "<a href=\"" + arguments.get("url").trim() + "\">" + arguments.get("text").trim() + "</a>";
						}
					})
					.build();

			var result = TextTemplate.render("""
					$call(link)
					$arg(url)https://example.com$end
					$arg(text)Click here$end
					$end
					""", Map.of(), opts);

			assertThat(result).isEqualTo("<a href=\"https://example.com\">Click here</a>");
		}

		@Test
		void textTemplateStringMacroFromText() {
			var opts = DefaultRenderOptions.builder()
					.macro(TextTemplateStringMacro.fromText("greeting", "Hello ${name}!"))
					.build();

			var result = TextTemplate.render("""
					$call(greeting)
					$arg(name)World$end
					$end
					""", Map.of(), opts);

			assertThat(result).isEqualTo("Hello World!");
		}

		@Test
		void textTemplateStringMacroWithConditional() {
			var opts = DefaultRenderOptions.builder()
					.macro(TextTemplateStringMacro.fromText("greet",
							"$if(formal)Dear ${name}$end$unless(formal)Hi ${name}$end"))
					.build();

			var formalResult = TextTemplate.render("""
					$call(greet)
					$arg(name)Alice$end
					$arg(formal)yes$end
					$end
					""", Map.of(), opts);

			var informalResult = TextTemplate.render("""
					$call(greet)
					$arg(name)Bob$end
					$end
					""", Map.of(), opts);

			assertThat(formalResult).isEqualTo("Dear Alice");
			assertThat(informalResult).isEqualTo("Hi Bob");
		}

		@Test
		void unknownMacroThrows() {
			var opts = DefaultRenderOptions.defaults();
			assertThatThrownBy(() -> TextTemplate.render("$call(unknown) $arg(x)y$end $end", Map.of(), opts))
					.isInstanceOf(IllegalArgumentException.class)
					.hasMessageContaining("No such macro");
		}
	}

	@Nested
	class TemplateCaching {

		@Test
		void parseOnceRenderMany() {
			Template template = TextTemplate.parse("Hello ${name}!");

			assertThat(TextTemplate.render(template, Map.of("name", "Alice"))).isEqualTo("Hello Alice!");
			assertThat(TextTemplate.render(template, Map.of("name", "Bob"))).isEqualTo("Hello Bob!");
			assertThat(TextTemplate.render(template, Map.of("name", "Charlie"))).isEqualTo("Hello Charlie!");
		}

		@Test
		void renderWithLookupFunction() {
			Template template = TextTemplate.parse("${greeting} ${name}!");
			Function<String, Object> lookup = name -> switch (name) {
				case "greeting" -> "Hello";
				case "name" -> "World";
				default -> null;
			};

			assertThat(TextTemplate.render(template, lookup)).isEqualTo("Hello World!");
		}
	}

	@Nested
	class CustomRenderOptions {

		@Test
		void customVariableNotFoundHandler() {
			var opts = DefaultRenderOptions.builder()
					.onVariableNotFound(variable -> "[MISSING: " + variable + "]")
					.build();

			var result = TextTemplate.render("Hello ${unknown}!", Map.of(), opts);
			assertThat(result).isEqualTo("Hello [MISSING: unknown]!");
		}

		@Test
		void customVariableNotFoundWithFallback() {
			var defaults = Map.of("name", "Guest", "greeting", "Hello");
			var opts = DefaultRenderOptions.builder()
					.onVariableNotFound(variable -> String.valueOf(defaults.getOrDefault(variable, "")))
					.build();

			var result = TextTemplate.render("${greeting} ${name}!", Map.of(), opts);
			assertThat(result).isEqualTo("Hello Guest!");
		}
	}

	@Nested
	class ParserErrors {

		@Test
		void unmatchedEndInIf() {
			assertThatThrownBy(() -> TextTemplate.parse("$if(condition) Some text"))
					.hasMessage("Expected '$end' at position: 24");
		}

		@Test
		void unmatchedEndInUnless() {
			assertThatThrownBy(() -> TextTemplate.parse("$unless(condition) Some text"))
					.hasMessage("Expected '$end' at position: 28");
		}

		@Test
		void unmatchedEndInEach() {
			assertThatThrownBy(() -> TextTemplate.parse("$each(items) Some text"))
					.hasMessage("Expected '$end' at position: 22");
		}

		@Test
		void unmatchedVariableBrace() {
			assertThatThrownBy(() -> TextTemplate.parse("Hello ${name"))
					.hasMessage("Expected '}' at position: 12");
		}

		@Test
		void unknownDirective() {
			assertThatThrownBy(() -> TextTemplate.parse("$unknown(x)"))
					.hasMessage("Unknown directive at position: 0");
		}

		@Test
		void malformedIfEqMissingComma() {
			assertThatThrownBy(() -> TextTemplate.parse("$if_eq(name) $end"))
					.hasMessage("Expected ',' at position: 17");
		}

		@Test
		void malformedIfEqMissingQuote() {
			assertThatThrownBy(() -> TextTemplate.parse("$if_eq(name, value) $end"))
					.hasMessage("Expected '\"' at position: 13");
		}

		@Test
		void malformedGreaterThanNotInteger() {
			assertThatThrownBy(() -> TextTemplate.parse("$greater_than(x, abc) $end"))
					.hasMessage("Expected integer literal at position: 17");
		}
	}

	@Nested
	class BuilderTests {

		@Test
		void timeZoneAffectsDateFormatting() {
			// Use Instant - timezone setting affects Date and Instant formatting
			var instant = java.time.Instant.parse("2024-01-15T12:00:00Z");

			var utcOpts = DefaultRenderOptions.builder()
					.timeZone(TimeZone.getTimeZone("UTC"))
					.build();
			var tokyoOpts = DefaultRenderOptions.builder()
					.timeZone(TimeZone.getTimeZone("Asia/Tokyo"))
					.build();

			var template = TextTemplate.parse("${date|MM/dd/yyyy HH:mm:ss}");
			var ctx = Map.<String, Object>of("date", instant);

			assertThat(TextTemplate.render(template, ctx, utcOpts)).isEqualTo("01/15/2024 12:00:00");
			assertThat(TextTemplate.render(template, ctx, tokyoOpts)).isEqualTo("01/15/2024 21:00:00");
		}

		@Test
		void addCustomFormatter() {
			var opts = DefaultRenderOptions.builder()
					.addFormatter(new ValueFormatter() {
						@Override
						public boolean isSupportedFormat(String format) {
							return format.equals("reverse");
						}

						@Override
						public String format(Object value, String format) {
							return new StringBuilder(String.valueOf(value)).reverse().toString();
						}
					})
					.build();

			var result = TextTemplate.render("${name|reverse}", Map.of("name", "hello"), opts);
			assertThat(result).isEqualTo("olleh");
		}

		@Test
		void macrosListAddition() {
			var opts = DefaultRenderOptions.builder()
					.macros(List.of(
							TextTemplateStringMacro.fromText("hello", "Hello ${name}!"),
							TextTemplateStringMacro.fromText("bye", "Goodbye ${name}!")
					))
					.build();

			var helloResult = TextTemplate.render("$call(hello) $arg(name)World$end $end", Map.of(), opts);
			var byeResult = TextTemplate.render("$call(bye) $arg(name)World$end $end", Map.of(), opts);

			assertThat(helloResult).isEqualTo("Hello World!");
			assertThat(byeResult).isEqualTo("Goodbye World!");
		}

		@Test
		void builderCombinesAllOptions() {
			var opts = DefaultRenderOptions.builder()
					.timeZone(TimeZone.getTimeZone("UTC"))
					.macro(TextTemplateStringMacro.fromText("greet", "Hi ${name}"))
					.contentIncluder(file -> "included: " + file)
					.onVariableNotFound(var -> "[" + var + "?]")
					.build();

			assertThat(TextTemplate.render("${missing}", Map.of(), opts)).isEqualTo("[missing?]");
			assertThat(TextTemplate.render("$include(test.txt)", Map.of(), opts)).isEqualTo("included: test.txt");
			assertThat(TextTemplate.render("$call(greet) $arg(name)You$end $end", Map.of(), opts)).isEqualTo("Hi You");
		}
	}

	@Nested
	class FormattingIntegration {

		@Test
		void dateFormattingInTemplate() {
			var ctx = Map.<String, Object>of("date", LocalDate.of(2024, 6, 15));
			check("Date: ${date|yyyy-MM-dd}", ctx, "Date: 2024-06-15");
			check("${date|MMMM}", ctx, "June");
			check("${date|dd/MM/yyyy}", ctx, "15/06/2024");
		}

		@Test
		void numberFormattingInTemplate() {
			check("${num|#,##0}", Map.of("num", 1234567), "1,234,567");
			check("${num|0.00}", Map.of("num", 3.14159), "3.14");
			check("${num|00000}", Map.of("num", 42), "00042");
		}

		@Test
		void formattingInLoop() {
			var ctx = Map.<String, Object>of("items", List.of(
					Map.of("name", "Item A", "price", 19.99),
					Map.of("name", "Item B", "price", 5.5)
			));
			var result = TextTemplate.render("""
					$each(items)
					${name}: $$${price|0.00}
					$end""", ctx);
			assertThat(result).isEqualTo("""
					Item A: $19.99
					Item B: $5.50
					""");
		}

		@Test
		void unsupportedFormatThrows() {
			assertThatThrownBy(() -> TextTemplate.render("${value|%%%invalid%%%}", Map.of("value", "test")))
					.isInstanceOf(IllegalArgumentException.class);
		}
	}

	@Nested
	class NullHandling {

		@Test
		void nullValueRendersEmpty() {
			var ctx = new java.util.HashMap<String, Object>();
			ctx.put("name", null);
			assertThat(TextTemplate.render("Hello ${name}!", ctx)).isEqualTo("Hello !");
		}

		@Test
		void eachOverNullRendersNothing() {
			var ctx = new java.util.HashMap<String, Object>();
			ctx.put("items", null);
			assertThat(TextTemplate.render("$each(items)${it}$end", ctx)).isEqualTo("");
		}

		@Test
		void ifWithNullIsFalsy() {
			var ctx = new java.util.HashMap<String, Object>();
			ctx.put("value", null);
			assertThat(TextTemplate.render("$if(value)yes$end", ctx)).isEqualTo("");
			assertThat(TextTemplate.render("$unless(value)no$end", ctx)).isEqualTo("no");
		}

		@Test
		void lengthOfNullIsZero() {
			var ctx = new java.util.HashMap<String, Object>();
			ctx.put("items", null);
			assertThat(TextTemplate.render("$length(items)", ctx)).isEqualTo("0");
		}

		@Test
		void firstOnNullRendersEmpty() {
			var ctx = new java.util.HashMap<String, Object>();
			ctx.put("items", null);
			assertThat(TextTemplate.render("$first(items)x$end", ctx)).isEqualTo("");
		}
	}

	@Nested
	class ValueTypes {

		@Test
		void booleanValues() {
			check("${flag}", Map.of("flag", true), "true");
			check("${flag}", Map.of("flag", false), "false");
		}

		@Test
		void numericValues() {
			check("${num}", Map.of("num", 42), "42");
			check("${num}", Map.of("num", 3.14), "3.14");
			check("${num}", Map.of("num", 100L), "100");
			check("${num}", Map.of("num", 2.5f), "2.5");
		}

		@Test
		void objectToString() {
			var obj = new Object() {
				@Override
				public String toString() {
					return "CustomObject";
				}
			};
			check("${obj}", Map.of("obj", obj), "CustomObject");
		}

		@Test
		void booleanInCondition() {
			// Note: $if checks for "not null and not empty string"
			// Boolean false is neither null nor empty string, so it's truthy!
			// This is different from JavaScript-style truthiness
			check("$if(flag)yes$end", Map.of("flag", false), "yes");
			check("$if(flag)yes$end", Map.of("flag", true), "yes");
			// Use $if_eq for boolean checks
			check("$if_eq(flag, \"true\")yes$end", Map.of("flag", true), "yes");
			check("$if_eq(flag, \"true\")yes$end", Map.of("flag", false), "");
		}

		@Test
		void numericZeroIsTruthyInCondition() {
			// 0 is not null and not empty string, so it's truthy
			check("$if(num)yes$end", Map.of("num", 0), "yes");
			check("$if(num)yes$end", Map.of("num", 1), "yes");
		}
	}

	@Nested
	class LoopMetadata {

		@Test
		void indexInLoop() {
			check("""
					$each(items)
					${_index}: ${it}
					$end""", Map.of("items", List.of("a", "b", "c")), """
					0: a
					1: b
					2: c
					""");
		}

		@Test
		void firstInLoop() {
			check("""
					$each(items)
					$if_eq(_first, "true")FIRST: $end${it}
					$end""", Map.of("items", List.of("a", "b", "c")), """
					FIRST: a
					b
					c
					""");
		}

		@Test
		void lastInLoop() {
			check("""
					$each(items)
					${it}$unless_eq(_last, "true"), $end
					$end""", Map.of("items", List.of("a", "b", "c")), "a, b, c");
		}

		@Test
		void allMetadataInLoop() {
			check("""
					$each(items)
					[${_index}] ${it} (first=${_first}, last=${_last})
					$end""", Map.of("items", List.of("x", "y")), """
					[0] x (first=true, last=false)
					[1] y (first=false, last=true)
					""");
		}

		@Test
		void metadataInMapLoop() {
			var ctx = Map.<String, Object>of("map", new TreeMap<>(Map.of("a", 1, "b", 2)));
			check("""
					$each(map)
					${_index}: ${key}=${it}
					$end""", ctx, """
					0: a=1
					1: b=2
					""");
		}

		@Test
		void singleElementLoop() {
			check("""
					$each(items)
					${it} first=${_first} last=${_last}
					$end""", Map.of("items", List.of("only")), """
					only first=true last=true
					""");
		}
	}

	@Nested
	class LastDirective {

		@Test
		void lastOnList() {
			check("""
					$last(persons)
					- ${name}
					$end
					""", Map.of("persons", List.of(Map.of("name", "John"), Map.of("name", "Jane"))), """
					- Jane
					""");
		}

		@Test
		void lastOnEmptyList() {
			check("""
					$last(emptyItems)
					- ${name}
					$end
					""", Map.of("emptyItems", List.of()), """
					""");
		}

		@Test
		void lastOnSingleItem() {
			check("""
					$last(items)
					${it}
					$end
					""", Map.of("items", List.of("only")), """
					only
					""");
		}

		@Test
		void lastOnUnknownVariable() {
			check("""
					$last(unknown)
					- ${name}
					$end
					""", Map.of(), """
					""");
		}

		@Test
		void lastWithParentAccess() {
			var ctx = Map.of("title", "Winners", "people", List.of(
					Map.of("name", "Alice"),
					Map.of("name", "Bob"),
					Map.of("name", "Charlie")
			));
			check("""
					$last(people)
					${../title}: ${name}
					$end
					""", ctx, """
					Winners: Charlie
					""");
		}
	}

	@Nested
	class ComparisonOrEqualConditionals {

		@Test
		void greaterThanOrEqTrue() {
			check("""
					$greater_than_or_eq(count, 5)
					pass
					$end""", Map.of("count", 5), """
					pass
					""");
			check("""
					$greater_than_or_eq(count, 5)
					pass
					$end""", Map.of("count", 6), """
					pass
					""");
		}

		@Test
		void greaterThanOrEqFalse() {
			check("""
					$greater_than_or_eq(count, 5)
					pass
					$end""", Map.of("count", 4), """
					""");
		}

		@Test
		void greaterThanOrEqMissingVariable() {
			check("""
					$greater_than_or_eq(num, 3)
					pass
					$end""", Map.of(), """
					""");
		}

		@Test
		void lessThanOrEqTrue() {
			check("""
					$less_than_or_eq(count, 5)
					pass
					$end""", Map.of("count", 5), """
					pass
					""");
			check("""
					$less_than_or_eq(count, 5)
					pass
					$end""", Map.of("count", 4), """
					pass
					""");
		}

		@Test
		void lessThanOrEqFalse() {
			check("""
					$less_than_or_eq(count, 5)
					pass
					$end""", Map.of("count", 6), """
					""");
		}

		@Test
		void lessThanOrEqMissingVariable() {
			check("""
					$less_than_or_eq(num, 3)
					pass
					$end""", Map.of(), """
					""");
		}

		@Test
		void combinedComparisonRange() {
			// Check if value is in range [3, 7]
			var template = """
					$greater_than_or_eq(n, 3)
					$less_than_or_eq(n, 7)
					in range
					$end
					$end""";
			check(template, Map.of("n", 3), "in range\n");
			check(template, Map.of("n", 5), "in range\n");
			check(template, Map.of("n", 7), "in range\n");
			check(template, Map.of("n", 2), "");
			check(template, Map.of("n", 8), "");
		}
	}

	@Nested
	class TemplateComments {

		@Test
		void simpleComment() {
			check("Hello $-- this is a comment --$ World", Map.of(), "Hello World");
		}

		@Test
		void commentAtStart() {
			check("$-- comment --$Hello", Map.of(), "Hello");
		}

		@Test
		void commentAtEnd() {
			check("Hello$-- comment --$", Map.of(), "Hello");
		}

		@Test
		void multiLineComment() {
			check("""
					Before
					$-- this is a
					multi-line
					comment --$
					After
					""", Map.of(), """
					Before
					After
					""");
		}

		@Test
		void commentDoesNotRenderVariables() {
			check("$-- ${name} is hidden --$visible", Map.of("name", "secret"), "visible");
		}

		@Test
		void multipleComments() {
			check("a$-- 1 --$b$-- 2 --$c", Map.of(), "abc");
		}

		@Test
		void commentInsideConditional() {
			check("""
					$if(show)
					$-- comment inside if --$
					content
					$end""", Map.of("show", "yes"), """
					content
					""");
		}

		@Test
		void unclosedCommentThrows() {
			assertThatThrownBy(() -> TextTemplate.parse("$-- unclosed comment"))
					.hasMessageContaining("Expected '--$' to close comment");
		}
	}

	@Nested
	class DeepParentAccess {

		@Test
		void twoLevelsUp() {
			var ctx = Map.<String, Object>of(
					"root", "ROOT",
					"level1", List.of(Map.of(
							"name", "L1",
							"level2", List.of(Map.of("name", "L2"))
					))
			);
			var result = TextTemplate.render("""
					$each(level1)
					$each(level2)
					L2: ${name}, Root: ${../../root}
					$end
					$end""", ctx);
			assertThat(result).isEqualTo("""
					L2: L2, Root: ROOT
					""");
		}

		@Test
		void mixedParentAccess() {
			var ctx = Map.<String, Object>of(
					"title", "Report",
					"sections", List.of(Map.of(
							"name", "Section A",
							"items", List.of("Item 1", "Item 2")
					))
			);
			var result = TextTemplate.render("""
					Title: ${title}
					$each(sections)
					== ${name} ==
					$each(items)
					- ${it} (from ${../name} in ${../../title})
					$end
					$end""", ctx);
			assertThat(result).isEqualTo("""
					Title: Report
					== Section A ==
					- Item 1 (from Section A in Report)
					- Item 2 (from Section A in Report)
					""");
		}
	}

	// Helper method
	private void check(String template, Map<String, Object> context, String expected) {
		final var actual = TextTemplate.render(template, context);
		assertThat(actual)
				.describedAs("Template: %s%nContext: %s%nExpected: %s%nActual: %s",
						template, context, expected, actual)
				.isEqualTo(expected);
	}
}
