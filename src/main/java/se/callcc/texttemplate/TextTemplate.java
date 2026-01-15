package se.callcc.texttemplate;

import static java.util.TimeZone.getTimeZone;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Objects;
import java.util.function.Function;

/**
 * TextTemplate is a powerful and flexible template language designed to
 * facilitate dynamic content generation.
 *
 * Key Features: Variable Replacement:
 *
 * Use ${variableName} to insert the value of a variable into the template.
 *
 * $if(condition) ... $end: Includes content if the condition is true.
 *
 * $unless(condition) ... $end: Includes content if the condition is false.
 *
 * $if_has_many(iterable) ... $end: Includes content if the condition is true.
 *
 * $unless_has_many(iterable) ... $end: Includes content if the condition is false.

 * Loops:
 *
 * $each(iterable) ... $end: Iterates over a collection and includes content for
 * each item.
 *
 * $each(map)
 * - ${key}: the current map entry key
 * - ${it}: the current map entry value, the current value object (map entry value) is set as the context object
 * $end: Iterates over a map
 *
 * $index(collection, idx): get the collection element at idx
 *
 * $index(map, key): get the value from map by key, the key here could be a variable as well,
 * $(index(map, ${key_var})
 *
 * $length(iterable): compute the length of given iterable data
 *
 * $include(file): include the template file from resource folder
 *
 * Formatting:
 *
 * Supports formatting for dates, numbers, and other data types. Custom
 * formatters can be implemented to handle specific formatting needs. Escaping:
 *
 * Use $$ to include a literal $ character in the output. Supports escaping of
 * special characters to avoid unintended replacements.
 */
public class TextTemplate {

	public static interface Node {
	}

	public static interface ConditionalNode extends Node {
	}

	public static record TextNode(String text) implements Node {
	}

	public static record VariableNode(String variableName, String format) implements Node {
	}

	public static record IfTrueNode(String condition, List<Node> children) implements ConditionalNode {
	}

	public static record IfEqNode(String variable, String literal, List<Node> children) implements ConditionalNode {
	}

	public static record UnlessEqNode(String variable, String literal, List<Node> children) implements ConditionalNode {
	}

	public static record GreaterThanNode(String variable, int literal, List<Node> children) implements ConditionalNode {
	}

	public static record LessThanNode(String variable, int literal, List<Node> children) implements ConditionalNode {
	}

	public static record GreaterThanOrEqNode(String variable, int literal, List<Node> children) implements ConditionalNode {
	}

	public static record LessThanOrEqNode(String variable, int literal, List<Node> children) implements ConditionalNode {
	}

	public static record CommentNode() implements Node {
	}

	public static record IfFalseNode(String condition, List<Node> children) implements ConditionalNode {
	}

	public static record IfHasManyNode(String iterableName, List<Node> children) implements ConditionalNode {
	}

	public static record UnlessHasManyNode(String iterableName, List<Node> children) implements ConditionalNode {
	}

	public static record IncludeNode(String file) implements Node {
	}

	public static record LengthNode(String iterableName) implements Node {
	}

    public static record IndexNode(String variable, String index) implements Node {
    }

	public static record LoopNode(String iterableName, List<Node> children) implements Node {
	}

	public static record FirstNode(String iterableName, List<Node> children) implements Node {
	}

	public static record LastNode(String iterableName, List<Node> children) implements Node {
	}

	private record MacroArgument (String name, List<Node> children) {}

	public static record MacroNode(String macroName, List<MacroArgument> fragments) implements Node {
	}

	public static record Template(List<Node> children) implements Node {
	}

	public static Template parse(String template) {
		return new Parser(template).parse();
	}

	public static String render(Template template, Function<String, Object> lookup) {
		return Renderer.render(template, lookup, DefaultRenderOptions.defaults());
	}

	public static String render(String template, Function<String, Object> lookup) {
		return Renderer.render(TextTemplate.parse(template), lookup, DefaultRenderOptions.defaults());
	}


	public static String render(Template template, Map<String, Object> context) {
		return Renderer.render(template, context::get, DefaultRenderOptions.defaults());
	}

	public static String render(String template, Map<String, Object> context) {
		return render(parse(template), context);
	}

	public static String render(Template template, Function<String, Object> lookup, RenderOptions opts) {
		return Renderer.render(template, lookup, opts);
	}

	public static String render(Template template, Map<String, Object> context, RenderOptions opts) {
		return Renderer.render(template, context::get, opts);
	}

	public static String render(String template, Map<String, Object> context, RenderOptions opts) {
		return render(parse(template), context, opts);
	}

	public static interface RenderOptions {
		String onVariableNotFound(String variable, Function<String, Object> lookup);

		String format(Object value, String format);

		default String getIncludeContent(String file) {
			throw new UnsupportedOperationException();
		}

		Function<String, String> RESOURCE_FILE_INCLUDER = (file) -> {
			try (var is = Thread.currentThread().getContextClassLoader().getResourceAsStream(file)) {
				if (is == null) {
					throw new IOException("Resource not found: " + file);
				}
				return new String(is.readAllBytes(), StandardCharsets.UTF_8);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		};

		String callMacro(String macroName, HashMap<String, String> arguments);
	}

	public interface TextTemplateMacro {
		String name();
		String apply(Map<String,String> arguments);
	}

	public static class TextTemplateStringMacro implements TextTemplateMacro {

		private final String name;
		private final Template template;

		public TextTemplateStringMacro(String name, Template template) {
			this.name = name;
			this.template = template;
		}

		public static TextTemplateStringMacro fromText(String name, String spec) {

			return new TextTemplateStringMacro(name, TextTemplate.parse(spec));
		}

		@Override
		public String name() {
			return name;
		}

		@Override
		public String apply(Map<String, String> arguments) {
			return TextTemplate.render(template, arguments::get);
		}

	}

	public static final class DefaultRenderOptions implements RenderOptions {

		private final Formatters formatters;
		private final Map<String, TextTemplateMacro> macros;
		private final Function<String, String> contentIncluder;
		private final Function<String, String> variableNotFoundHandler;

		private DefaultRenderOptions(Builder builder) {
			this.formatters = new Formatters(List.copyOf(builder.formatters));
			this.macros = Map.copyOf(builder.macros);
			this.contentIncluder = builder.contentIncluder;
			this.variableNotFoundHandler = builder.variableNotFoundHandler;
		}

		@Override
		public String onVariableNotFound(String variable, Function<String, Object> lookup) {
			return variableNotFoundHandler.apply(variable);
		}

		@Override
		public String format(Object value, String format) {
			return formatters.find(format).format(value, format);
		}

		@Override
		public String getIncludeContent(String file) {
			if (contentIncluder == null) {
				throw new UnsupportedOperationException("No content includer configured");
			}
			return contentIncluder.apply(file);
		}

		@Override
		public String callMacro(String macroName, HashMap<String, String> arguments) {
			final var macro = macros.get(macroName);
			if (macro == null) {
				throw new IllegalArgumentException("No such macro %s".formatted(macroName));
			}
			return macro.apply(arguments);
		}

		public static Builder builder() {
			return new Builder();
		}

		public static DefaultRenderOptions defaults() {
			return new Builder().build();
		}

		public static DefaultRenderOptions withContentIncluder(Function<String, String> includer) {
			return new Builder().contentIncluder(includer).build();
		}

		public static final class Builder {
			private final List<ValueFormatter> formatters = new ArrayList<>(
					List.of(new DateFormatter(getTimeZone("CET")), new NumberFormatter()));
			private final Map<String, TextTemplateMacro> macros = new HashMap<>();
			private Function<String, String> contentIncluder;
			private Function<String, String> variableNotFoundHandler = variable -> "";

			private Builder() {}

			public Builder timeZone(java.util.TimeZone timeZone) {
				// Replace the default DateFormatter with one using the specified timezone
				formatters.removeIf(f -> f instanceof DateFormatter);
				formatters.add(0, new DateFormatter(timeZone));
				return this;
			}

			public Builder addFormatter(ValueFormatter formatter) {
				formatters.add(formatter);
				return this;
			}

			public Builder macro(TextTemplateMacro macro) {
				macros.put(macro.name(), macro);
				return this;
			}

			public Builder macros(List<TextTemplateMacro> macroList) {
				macroList.forEach(m -> macros.put(m.name(), m));
				return this;
			}

			public Builder contentIncluder(Function<String, String> includer) {
				this.contentIncluder = includer;
				return this;
			}

			public Builder onVariableNotFound(Function<String, String> handler) {
				this.variableNotFoundHandler = handler;
				return this;
			}

			public DefaultRenderOptions build() {
				return new DefaultRenderOptions(this);
			}
		}
	}

	private static class Renderer {

		public static String render(Node node, Function<String, Object> lookup, RenderOptions opts) {

			final var b = new StringBuilder();
			render(b, node, lookup, opts);
			return b.toString();
		}

		private static void render(StringBuilder b, Node node, Function<String, Object> lookup, RenderOptions opts) {

			switch (node) {
			case TextNode textNode -> b.append(textNode.text);
			case VariableNode varNode -> renderVariable(b, varNode, lookup, opts);
			case Template root -> renderChildren(b, root.children, lookup, opts);
			case LoopNode loop -> renderLoop(b, loop, lookup, opts);
			case FirstNode first -> renderFirst(b, first, lookup, opts);
			case LastNode last -> renderLast(b, last, lookup, opts);
			case CommentNode comment -> { /* Comments render nothing */ }
			case IfTrueNode ifTrueNode -> renderIfTrue(b, ifTrueNode, lookup, opts);
			case IfEqNode ifEqNode -> renderIfEq(b, ifEqNode, lookup, opts);
			case UnlessEqNode unlessEqNode -> renderUnlessEq(b, unlessEqNode, lookup, opts);
			case GreaterThanNode greaterThanNode -> renderGreaterThan(b, greaterThanNode, lookup, opts);
			case LessThanNode lessThanNode -> renderLessThan(b, lessThanNode, lookup, opts);
			case GreaterThanOrEqNode gtoeNode -> renderGreaterThanOrEq(b, gtoeNode, lookup, opts);
			case LessThanOrEqNode ltoeNode -> renderLessThanOrEq(b, ltoeNode, lookup, opts);
			case IfFalseNode ifFalseNode -> renderIfFalse(b, ifFalseNode, lookup, opts);
			case IfHasManyNode ifHasManyNode -> renderIfHasMany(b, ifHasManyNode, lookup, opts);
			case UnlessHasManyNode unlessHasManyNode -> renderUnlessHasMany(b, unlessHasManyNode, lookup, opts);
			case IncludeNode includeNode -> renderInclude(b, includeNode, lookup, opts);
			case LengthNode lengthNode -> renderLength(b, lengthNode, lookup, opts);
            case IndexNode indexNode -> renderIndex(b, indexNode, lookup, opts);
			case MacroNode macroNode -> renderMacro(b,macroNode, lookup, opts);
			default -> throw new IllegalArgumentException("Unknown node type");
			}
			;
		}

		private static void renderLength(StringBuilder b, LengthNode varNode, Function<String, Object> lookup,
				RenderOptions opts) {
			final var value = lookup.apply(varNode.iterableName);
			final var length = switch (value) {
			case null -> 0;
			case String s -> s.length();
			default -> size(value);
			};
			b.append(length);
		}

        private static void renderIndex(StringBuilder b, IndexNode indexNode, Function<String, Object> lookup,
            RenderOptions opts) {
            final var value = lookup.apply(indexNode.variable);
            final var indexLength = indexNode.index == null ? 0 : indexNode.index.length();
            if (indexLength == 0) {
                return;
            }
            final var indexStr = (indexNode.index.startsWith("${") && indexNode.index.endsWith("}")) ?
                String.valueOf(lookup.apply(indexNode.index.substring(2, indexLength - 1))) : indexNode.index;

            switch (value) {
                case Collection<?> c -> {
                    final var index = parseIntOrDefault(indexStr, -1);
                    if (index < 0 || index >= c.size()) {
                        return;
                    }
                    b.append(c.toArray()[index]);
                }
                case Map<?, ?> m -> Optional.ofNullable(m.get(indexStr)).ifPresent(b::append);
                default -> {
                }
            }
        }

		private static void renderInclude(StringBuilder b, IncludeNode varNode, Function<String, Object> lookup,
				RenderOptions opts) {

			final var content = opts.getIncludeContent(varNode.file);
			final var includedTemplate = parse(content);
			renderChildren(b, includedTemplate.children, lookup, opts);
		}

		private static void renderVariable(StringBuilder b, VariableNode varNode, Function<String, Object> lookup,
				RenderOptions opts) {
			final var value = lookup.apply(varNode.variableName);
			if (value != null) {
				if (varNode.format != null) {
					b.append(opts.format(value, varNode.format));
				} else {
					b.append(value);
				}
				return;
			}
			b.append(opts.onVariableNotFound(varNode.variableName, lookup));
		}

		private static void renderIfTrue(StringBuilder b, IfTrueNode cond, Function<String, Object> lookup,
				RenderOptions opts) {
			// It's true, if the value is not null and not the blank string
			final var value = lookup.apply(cond.condition);
			if (value != null && !"".equals(value)) {
				renderChildren(b, cond.children, lookup, opts);
			}
		}


		private static void renderIfEq(StringBuilder b, IfEqNode cond, Function<String, Object> lookup,
				RenderOptions opts) {
			// It's true, if the value is not null and not the blank string
			final var value = lookup.apply(cond.variable);
			if (Objects.equals(String.valueOf(value), cond.literal)) {
				renderChildren(b, cond.children, lookup, opts);
			}
		}



		private static void renderUnlessEq(StringBuilder b, UnlessEqNode cond, Function<String, Object> lookup,
				RenderOptions opts) {
			// It's true, if the value is not null and not the blank string
			final var value = lookup.apply(cond.variable);
			if (!Objects.equals(String.valueOf(value), cond.literal)) {
				renderChildren(b, cond.children, lookup, opts);
			}
		}

		private static void renderGreaterThan(StringBuilder b, GreaterThanNode cond, Function<String, Object> lookup,
				RenderOptions opts) {
			// It's true, if the value is an integer and it's greater than the specified literal
			final var value = lookup.apply(cond.variable);
			final var maybeInt = tryParseInt(value);
			if(maybeInt.isPresent() && maybeInt.get() > cond.literal) {
				renderChildren(b, cond.children, lookup, opts);
			}
		}

		private static void renderLessThan(StringBuilder b, LessThanNode cond, Function<String, Object> lookup,
				RenderOptions opts) {
			// It's true, if the value is an integer and it's less than the specified literal
			final var value = lookup.apply(cond.variable);
			final var maybeInt = tryParseInt(value);
			if(maybeInt.isPresent() && maybeInt.get() < cond.literal) {
				renderChildren(b, cond.children, lookup, opts);
			}
		}

		private static void renderGreaterThanOrEq(StringBuilder b, GreaterThanOrEqNode cond, Function<String, Object> lookup,
				RenderOptions opts) {
			final var value = lookup.apply(cond.variable);
			final var maybeInt = tryParseInt(value);
			if(maybeInt.isPresent() && maybeInt.get() >= cond.literal) {
				renderChildren(b, cond.children, lookup, opts);
			}
		}

		private static void renderLessThanOrEq(StringBuilder b, LessThanOrEqNode cond, Function<String, Object> lookup,
				RenderOptions opts) {
			final var value = lookup.apply(cond.variable);
			final var maybeInt = tryParseInt(value);
			if(maybeInt.isPresent() && maybeInt.get() <= cond.literal) {
				renderChildren(b, cond.children, lookup, opts);
			}
		}

		private static Optional<Integer> tryParseInt(Object value) {
			if(value == null) {
				return Optional.empty();
			}

			try {
				return Optional.of(Integer.parseInt(String.valueOf(value)));
			} catch (NumberFormatException e) {
				return Optional.empty();
			}
		}

		private static void renderIfFalse(StringBuilder b, IfFalseNode cond, Function<String, Object> lookup,
				RenderOptions opts) {
			// It's false, if the value is null or the blank string
			final var value = lookup.apply(cond.condition);
			if (value == null || "".equals(value)) {
				renderChildren(b, cond.children, lookup, opts);
			}
		}

		private static void renderIfHasMany(StringBuilder b, IfHasManyNode node, Function<String, Object> lookup,
				RenderOptions opts) {

			final var list = lookup.apply(node.iterableName);
			// Has many, means at least 2
			if (hasMoreThan1Elements(list)) {
				renderChildren(b, node.children, lookup, opts);

			}

		}

		private static boolean hasMoreThan1Elements(Object list) {
			if (list == null) {
				return false;
			}

			if (list instanceof Iterable iterable) {
				final var iterator = iterable.iterator();
				if (!iterator.hasNext()) {
					return false;
				}
				iterator.next();
				if (!iterator.hasNext()) {
					return false;
				}
				return true;
			}
			return false;
		}

		private static boolean hasAtMost1Elements(Object list) {
			if (list == null) {
				return true;
			}

			if (list instanceof Iterable iterable) {
				final var iterator = iterable.iterator();
				if (!iterator.hasNext()) {
					return true;
				}
				iterator.next();
				if (!iterator.hasNext()) {
					return true;
				}
				return false;
			}
			return false;
		}

		private static void renderUnlessHasMany(StringBuilder b, UnlessHasManyNode node,
				Function<String, Object> lookup, RenderOptions opts) {
			// It's false, if the value is null or the blank string
			final var list = lookup.apply(node.iterableName);
			if (hasAtMost1Elements(list)) {
				renderChildren(b, node.children, lookup, opts);
			}
		}

		private static void renderLoop(StringBuilder b, LoopNode node, Function<String, Object> lookup,
				RenderOptions opts) {
			final var loopOver = lookup.apply(node.iterableName);
			if (loopOver == null) {
				return;
			}

			if (loopOver instanceof Iterable iterable) {
				final int total = size(loopOver);
				int index = 0;
				for (Object item : iterable) {
					// parent is ../<whatever>
					// current object is it
					// add loop metadata: _index, _first, _last
					renderChildren(b, node.children, extendContextWithLoopMetadata(item, lookup, index, total), opts);
					index++;
				}
                return;
			}
            if (loopOver instanceof Map<?,?> m) {
				final int total = m.size();
				int[] index = {0};
                m.forEach((k,v) -> {
                    // parent is ../<whatever>
                    // add the field `key` to access the key of each map entry
                    // the current object (it) is set to the value of each map entry
                    renderChildren(b, node.children, extendMapEntryContextWithLoopMetadata(k, v, lookup, index[0], total), opts);
					index[0]++;
                });
                return;
            }

		}

		private static void renderFirst(StringBuilder b, FirstNode node, Function<String, Object> lookup,
				RenderOptions opts) {
			final var loopOver = lookup.apply(node.iterableName);
			if (loopOver == null) {
				return;
			}

			if (loopOver instanceof Iterable iterable) {
				final var iterator = iterable.iterator();
				if(iterator.hasNext()) {
					final var item = iterator.next();
					renderChildren(b, node.children, extendContext(item, lookup), opts);
				}
			}

		}

		private static void renderLast(StringBuilder b, LastNode node, Function<String, Object> lookup,
				RenderOptions opts) {
			final var loopOver = lookup.apply(node.iterableName);
			if (loopOver == null) {
				return;
			}

			if (loopOver instanceof Iterable iterable) {
				Object lastItem = null;
				for (Object item : iterable) {
					lastItem = item;
				}
				if (lastItem != null) {
					renderChildren(b, node.children, extendContext(lastItem, lookup), opts);
				}
			}
		}


		private static void renderMacro(StringBuilder b, MacroNode node, Function<String, Object> lookup,
				RenderOptions opts) {

			final var arguments = new HashMap<String,String>();

			for(final var fragment : node.fragments()) {
				final var fragmentText = new StringBuilder();
				for(final var fragmentNode : fragment.children()) {
					render(fragmentText,fragmentNode,lookup, opts);
				}
				arguments.put(fragment.name(),fragmentText.toString());
			}

			final var macroText = opts.callMacro(node.macroName,arguments);

			b.append(macroText);
		}

		private static Function<String, Object> extendContext(Object item, Function<String, Object> lookup) {
			return name -> {
				if (name != null && name.startsWith("../")) {
					return lookup.apply(name.substring(3));
				}
				if ("it".equals(name)) {
					return item;
				}

				if (item instanceof Map map) {
					return map.get(name);
				}

				throw new IllegalArgumentException("No such variable " + name);
			};
		}

        private static Function<String, Object> extendMapEntryContext(Object k, Object v, Function<String, Object> lookup) {
            return name -> {
                if (name != null && name.startsWith("../")) {
                    return lookup.apply(name.substring(3));
                }
                if ("it".equals(name)) {
                    return v;
                }
                if ("key".equals(name)) {
                    return k;
                }
                if (v instanceof Map map) {
                    return map.get(name);
                }
                throw new IllegalArgumentException("No such variable " + name);
            };
        }

		private static Function<String, Object> extendContextWithLoopMetadata(Object item, Function<String, Object> lookup, int index, int total) {
			return name -> {
				if (name != null && name.startsWith("../")) {
					return lookup.apply(name.substring(3));
				}
				if ("it".equals(name)) {
					return item;
				}
				if ("_index".equals(name)) {
					return index;
				}
				if ("_first".equals(name)) {
					return index == 0;
				}
				if ("_last".equals(name)) {
					return index == total - 1;
				}

				if (item instanceof Map map) {
					return map.get(name);
				}

				throw new IllegalArgumentException("No such variable " + name);
			};
		}

		private static Function<String, Object> extendMapEntryContextWithLoopMetadata(Object k, Object v, Function<String, Object> lookup, int index, int total) {
			return name -> {
				if (name != null && name.startsWith("../")) {
					return lookup.apply(name.substring(3));
				}
				if ("it".equals(name)) {
					return v;
				}
				if ("key".equals(name)) {
					return k;
				}
				if ("_index".equals(name)) {
					return index;
				}
				if ("_first".equals(name)) {
					return index == 0;
				}
				if ("_last".equals(name)) {
					return index == total - 1;
				}
				if (v instanceof Map map) {
					return map.get(name);
				}
				throw new IllegalArgumentException("No such variable " + name);
			};
		}

		private static void renderChildren(StringBuilder b, List<Node> children, Function<String, Object> lookup,
				RenderOptions opts) {
			if (children == null) {
				return;
			}
			for (final var child : children) {
				render(b, child, lookup, opts);
			}
		}

	}

	// Helper methods to replace external dependencies

	private static String trimToNull(String str) {
		if (str == null) {
			return null;
		}
		String trimmed = str.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}

	private static int size(Object obj) {
		if (obj == null) {
			return 0;
		}
		if (obj instanceof Collection<?> c) {
			return c.size();
		}
		if (obj instanceof Map<?, ?> m) {
			return m.size();
		}
		if (obj instanceof CharSequence cs) {
			return cs.length();
		}
		if (obj.getClass().isArray()) {
			return java.lang.reflect.Array.getLength(obj);
		}
		if (obj instanceof Iterable<?> it) {
			int count = 0;
			for (Object ignored : it) {
				count++;
			}
			return count;
		}
		return 0;
	}

	private static int parseIntOrDefault(String str, int defaultValue) {
		if (str == null || str.isEmpty()) {
			return defaultValue;
		}
		try {
			return Integer.parseInt(str);
		} catch (NumberFormatException e) {
			return defaultValue;
		}
	}

	static class Parser {
		private final String template;
		private int position;

		public Parser(String template) {
			this.template = template;
			this.position = 0;
		}

		public Template parse() {
			List<Node> nodes = new ArrayList<>();
			while (!isAtEnd() && !template.startsWith("$end", position)) {
				if (peek() == '$') {
					if (match("$$")) {
						nodes.add(new TextNode("$"));
					} else if (match("${")) {
						nodes.add(parseVariable());
					} else if (match("$if(")) {
						nodes.add(parseIfTrue());
					} else if (match("$if_eq(")) {
						nodes.add(parseIfEq());
					} else if (match("$unless_eq(")) {
						nodes.add(parseUnlessEq());
					} else if (match("$unless(")) {
						nodes.add(parseIfFalse());
					} else if (match("$if_has_many(")) {
						nodes.add(parseIfHasMany());
					} else if (match("$unless_has_many(")) {
						nodes.add(parseUnlessHasMany());
					} else if (match("$greater_than(")) {
						nodes.add(parseGreaterThan());
					} else if (match("$less_than(")) {
						nodes.add(parseLessThan());
					} else if (match("$each(")) {
						nodes.add(parseLoop());
					} else if (match("$first(")) {
						nodes.add(parseFirst());
					} else if (match("$last(")) {
						nodes.add(parseLast());
					} else if (match("$greater_than_or_eq(")) {
						nodes.add(parseGreaterThanOrEq());
					} else if (match("$less_than_or_eq(")) {
						nodes.add(parseLessThanOrEq());
					} else if (match("$--")) {
						nodes.add(parseComment());
					} else if (match("$call(")) {
						nodes.add(parseMacro());
					} else if (match("$include(")) {
						nodes.add(parseInclude());
					} else if (match("$length(")) {
						nodes.add(parseLength());
                    } else if (match("$index(")) {
                        nodes.add(parseIndex());
                    } else {
						throw new IllegalArgumentException("Unknown directive at position: " + position);
					}
				} else {
					nodes.add(parseText());
				}
			}
			return new Template(nodes);
		}

		private Node parseText() {
			StringBuilder builder = new StringBuilder();
			while (!isAtEnd() && !(peek() == '$')) {
				builder.append(advance());
			}
			return new TextNode(builder.toString());
		}

		private Node parseVariable() {
			StringBuilder variableNameBuf = new StringBuilder();
			while (!isAtEnd() && !(peek() == '}') && !(peek() == '|')) {
				variableNameBuf.append(advance());
			}
			StringBuilder formatBuf = new StringBuilder();
			if (!isAtEnd() && peek() == '|') {
				match("|");
				while (!isAtEnd() && !(peek() == '}')) {
					formatBuf.append(advance());
				}
			}

			if (!match("}")) {
				throw new IllegalArgumentException("Expected '}' at position: " + position);
			}
			return new VariableNode(variableNameBuf.toString(), trimToNull(formatBuf.toString()));
		}

		private Node parseIfTrue() {
			String condition = parseUntil(')');
			if (!match(")")) {
				throw new IllegalArgumentException("Expected ')' at position: " + position);
			}
			onTerminatorMatch();
			List<Node> children = parseBlock();
			return new IfTrueNode(condition, children);
		}

		private Node parseIfEq() {
			String variable = parseUntil(',');
			if (!match(",")) {
				throw new IllegalArgumentException("Expected ',' at position: " + position);
			}
			String literal = parseStringLiteral();
			if (!match(")")) {
				throw new IllegalArgumentException("Expected ')' at position: " + position);
			}

			onTerminatorMatch();
			List<Node> children = parseBlock();
			return new IfEqNode(variable, literal, children);
		}

		private Node parseUnlessEq() {
			String variable = parseUntil(',');
			if (!match(",")) {
				throw new IllegalArgumentException("Expected ',' at position: " + position);
			}
			String literal = parseStringLiteral();
			if (!match(")")) {
				throw new IllegalArgumentException("Expected ')' at position: " + position);
			}

			onTerminatorMatch();
			List<Node> children = parseBlock();
			return new UnlessEqNode(variable, literal, children);
		}

		private Node parseGreaterThan() {
			String variable = parseUntil(',');
			if (!match(",")) {
				throw new IllegalArgumentException("Expected ',' at position: " + position);
			}
			int literal = parseIntegerLiteral();
			if (!match(")")) {
				throw new IllegalArgumentException("Expected ')' at position: " + position);
			}

			onTerminatorMatch();
			List<Node> children = parseBlock();
			return new GreaterThanNode(variable, literal, children);
		}

		private Node parseLessThan() {
			String variable = parseUntil(',');
			if (!match(",")) {
				throw new IllegalArgumentException("Expected ',' at position: " + position);
			}
			int literal = parseIntegerLiteral();
			if (!match(")")) {
				throw new IllegalArgumentException("Expected ')' at position: " + position);
			}

			onTerminatorMatch();
			List<Node> children = parseBlock();
			return new LessThanNode(variable, literal, children);
		}


		private String parseStringLiteral() {
			trimWhitespaces();
			if (!match("\"")) {
				throw new IllegalArgumentException("Expected '\"' at position: " + position);
			}

			StringBuilder builder = new StringBuilder();
			while (!isAtEnd() && peek() != '"') {
				builder.append(advance());
			}

			if (!match("\"")) {
				throw new IllegalArgumentException("Expected '\"' at position: " + position);
			}

			return builder.toString();
		}

		private int parseIntegerLiteral() {
			trimWhitespaces();
			StringBuilder builder = new StringBuilder();
			while (!isAtEnd() && isIntegerCharacter(peek())) {
				builder.append(advance());
			}

			if (builder.isEmpty()) {
				throw new IllegalArgumentException("Expected integer literal at position: " + position);
			}

			return Integer.parseInt(builder.toString());
		}

     	private boolean isIntegerCharacter(char c) {
     		switch(c) {
     		case '0':
     		case '1':
     		case '2':
     		case '3':
     		case '4':
     		case '5':
     		case '6':
     		case '7':
     		case '8':
     		case '9':
     			return true;
     			default: return false;
     		}
     	}

		private Node parseIfFalse() {
			String condition = parseUntil(')');
			if (!match(")")) {
				throw new IllegalArgumentException("Expected ')' at position: " + position);
			}
			onTerminatorMatch();
			List<Node> children = parseBlock();
			return new IfFalseNode(condition, children);
		}

		private Node parseIfHasMany() {
			String condition = parseUntil(')');
			if (!match(")")) {
				throw new IllegalArgumentException("Expected ')' at position: " + position);
			}
			onTerminatorMatch();
			List<Node> children = parseBlock();
			return new IfHasManyNode(condition, children);
		}

		private Node parseUnlessHasMany() {
			String condition = parseUntil(')');
			if (!match(")")) {
				throw new IllegalArgumentException("Expected ')' at position: " + position);
			}
			onTerminatorMatch();
			List<Node> children = parseBlock();
			return new UnlessHasManyNode(condition, children);
		}

		private Node parseInclude() {
			String file = parseUntil(')');
			if (!match(")")) {
				throw new IllegalArgumentException("Expected ')' at position: " + position);
			}
			onTerminatorMatch();
			return new IncludeNode(file);
		}

		private Node parseLength() {
			String iterableName = parseUntil(')');
			if (!match(")")) {
				throw new IllegalArgumentException("Expected ')' at position: " + position);
			}
			return new LengthNode(iterableName);
		}

        private Node parseIndex() {
            var variable = parseUntil(',');
            if (!match(",")) {
                throw new IllegalArgumentException("Expected ',' at position: " + position);
            }
            String index = parseUntil(')');
            if (!match(")")) {
                throw new IllegalArgumentException("Expected ')' at position: " + position);
            }
            return new IndexNode(variable, trimToNull(index));
        }

		private Node parseLoop() {
			String iterableName = parseUntil(')');
			if (!match(")")) {
				throw new IllegalArgumentException("Expected ')' at position: " + position);
			}
			onTerminatorMatch();
			List<Node> children = parseBlock();
			return new LoopNode(iterableName, children);
		}

		private Node parseFirst() {
			String iterableName = parseUntil(')');
			if (!match(")")) {
				throw new IllegalArgumentException("Expected ')' at position: " + position);
			}
			onTerminatorMatch();
			List<Node> children = parseBlock();
			return new FirstNode(iterableName, children);
		}

		private Node parseLast() {
			String iterableName = parseUntil(')');
			if (!match(")")) {
				throw new IllegalArgumentException("Expected ')' at position: " + position);
			}
			onTerminatorMatch();
			List<Node> children = parseBlock();
			return new LastNode(iterableName, children);
		}

		private Node parseGreaterThanOrEq() {
			String variable = parseUntil(',');
			if (!match(",")) {
				throw new IllegalArgumentException("Expected ',' at position: " + position);
			}
			int literal = parseIntegerLiteral();
			if (!match(")")) {
				throw new IllegalArgumentException("Expected ')' at position: " + position);
			}

			onTerminatorMatch();
			List<Node> children = parseBlock();
			return new GreaterThanOrEqNode(variable, literal, children);
		}

		private Node parseLessThanOrEq() {
			String variable = parseUntil(',');
			if (!match(",")) {
				throw new IllegalArgumentException("Expected ',' at position: " + position);
			}
			int literal = parseIntegerLiteral();
			if (!match(")")) {
				throw new IllegalArgumentException("Expected ')' at position: " + position);
			}

			onTerminatorMatch();
			List<Node> children = parseBlock();
			return new LessThanOrEqNode(variable, literal, children);
		}

		private Node parseComment() {
			// Parse until we find --$
			while (!isAtEnd() && !template.startsWith("--$", position)) {
				position++;
			}
			if (!match("--$")) {
				throw new IllegalArgumentException("Expected '--$' to close comment at position: " + position);
			}
			onTerminatorMatch();
			return new CommentNode();
		}

		private Node parseMacro() {
			String macroName = parseUntil(')');

			if (!match(")")) {
				throw new IllegalArgumentException("Expected ')' at position: " + position);
			}

			onTerminatorMatch();
			final var fragments = parseFragments();
			return new MacroNode(macroName, fragments);
		}

		private List<MacroArgument> parseFragments() {
			final var arguments = new ArrayList<MacroArgument>();
			skipWhitespaces();
			while (!isAtEnd() && !template.startsWith("$end", position)) {
				arguments.add(parseMacroArg());
				skipWhitespaces();
			}
			if (!match("$end")) {
				throw new IllegalArgumentException("Expected '$end' at position: " + position);
			}
			onTerminatorMatch();
			return arguments;
		}


		private MacroArgument parseMacroArg() {
			match("$arg(");
			final var fragmentName = parseUntil(')');

			if (!match(")")) {
				throw new IllegalArgumentException("Expected ')' at position: " + position);
			}

			onTerminatorMatch();
			List<Node> children = parseBlock();
			return new MacroArgument(fragmentName, children);
		}


		/**
		 * this could be used to remove any white spaces after control token otherwise,
		 * unexpected whitespaces, such as `\n` could break the markdown layout, E.g:
		 * Consider following Markdown table with $each:
		 *
		 * <pre>
		 * | column 1 | column 2 |
		 * | --- | --- |
		 * $each(items)
		 * | $$name | $name |
		 * | $$age | $age |
		 * $end
		 * </pre>
		 *
		 * In above example, the `$each(items)` after render would yield to a text node
		 * with a single `new line`, and that unexpected new line breaks the Markdown
		 * table definition
		 */
		private void trimWhitespaces() {
			if (isAtEnd()) {
				return;
			}

			var nextChar = peek();
			while (Character.isWhitespace(nextChar)) {
				position++;
				if (isAtEnd() || nextChar == '\n') {
					break;
				}
				nextChar = peek();
			}
		}

		private void skipWhitespaces() {
			if (isAtEnd()) {
				return;
			}

			var nextChar = peek();
			while (Character.isWhitespace(nextChar)) {
				position++;
				if (isAtEnd()) {
					break;
				}
				nextChar = peek();
			}
		}

		private List<Node> parseBlock() {
			List<Node> children = new ArrayList<>();
			while (!isAtEnd() && !template.startsWith("$end", position)) {
				children.add(parse());
			}
			if (!match("$end")) {
				throw new IllegalArgumentException("Expected '$end' at position: " + position);
			}
			onTerminatorMatch();
			return children;
		}

		private String parseUntil(char terminator) {
			StringBuilder builder = new StringBuilder();
			while (!isAtEnd() && peek() != terminator) {
				builder.append(advance());
			}
			return builder.toString();
		}

		private boolean match(String expected) {
			if (template.startsWith(expected, position)) {
				position += expected.length();
				return true;
			}
			return false;
		}

		private char advance() {
			return template.charAt(position++);
		}

		private char peek() {
			return template.charAt(position);
		}

		private boolean isAtEnd() {
			return position >= template.length();
		}

		// invoked when terminator found, such as
		// - `)` found for $each(), $if() and $unless()
		// - or `$end` found for a block
		private void onTerminatorMatch() {
			trimWhitespaces();
		}
	}

}
