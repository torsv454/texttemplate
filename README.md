# TextTemplate

A simple, lightweight text templating library for Java with **zero runtime dependencies**.

## Features

- Variable substitution with optional formatting
- Conditionals (`$if`, `$unless`, `$if_eq`, `$greater_than`, etc.)
- Loops over collections and maps (`$each`, `$first`)
- Template includes
- Macros
- Built-in date and number formatting
- Extensible formatter system

## Requirements

- Java 21+

## Installation

### Gradle

```kotlin
dependencies {
    implementation("se.callcc:texttemplate:1.0.0")
}
```

### Maven

```xml
<dependency>
    <groupId>se.callcc</groupId>
    <artifactId>texttemplate</artifactId>
    <version>1.0.0</version>
</dependency>
```

## Quick Start

```java
import se.callcc.texttemplate.TextTemplate;
import java.util.Map;

var template = "Hello ${name}! You have ${count} messages.";
var context = Map.of("name", "Alice", "count", 5);
var result = TextTemplate.render(template, context);
// "Hello Alice! You have 5 messages."
```

## Template Syntax

### Variables

```
${variableName}           Insert variable value
${variable|format}        Insert with formatting
$$                        Literal $ character
```

### Conditionals

```
$if(condition) ... $end              Include if truthy (not null/empty)
$unless(condition) ... $end          Include if falsy (null/empty)
$if_eq(var, "literal") ... $end      Include if var equals literal
$unless_eq(var, "literal") ... $end  Include if var does not equal literal
$greater_than(var, N) ... $end       Include if var > N
$less_than(var, N) ... $end          Include if var < N
$greater_than_or_eq(var, N) ... $end Include if var >= N
$less_than_or_eq(var, N) ... $end    Include if var <= N
$if_has_many(list) ... $end          Include if list has 2+ items
$unless_has_many(list) ... $end      Include if list has 0-1 items
```

**Note on truthiness:** `$if` checks for "not null AND not empty string". This means:
- `null` and `""` are falsy
- `false`, `0`, and all other values are **truthy**!
- Use `$if_eq(flag, "true")` for boolean checks

### Loops

```
$each(items) ... $end    Loop over collection or map
$first(items) ... $end   Process only first item
$last(items) ... $end    Process only last item
```

Inside loops:
- `${it}` - Current item value
- `${key}` - Current map key (when iterating maps)
- `${../varName}` - Access parent context
- `${_index}` - Current loop index (0-based)
- `${_first}` - Boolean, true if first iteration
- `${_last}` - Boolean, true if last iteration

### Utilities

```
$length(items)           Output collection/string/array length
$index(collection, N)    Get item at index N
$index(map, key)         Get map value by key
$include(path)           Include another template file
```

### Macros

```
$call(macroName)
  $arg(argName) ... $end
$end
```

### Comments

```
$-- comment text --$     Template comment (not rendered in output)
```

Comments can span multiple lines and are completely removed from the output.
Use them to document your templates without affecting the rendered result.

## Formatting

### Date Formatting

Use standard Java date format patterns:

```java
var ctx = Map.of("date", LocalDate.of(2024, 1, 15));
TextTemplate.render("${date|yyyy-MM-dd}", ctx);    // "2024-01-15"
TextTemplate.render("${date|MMMM dd, yyyy}", ctx); // "January 15, 2024"
```

Supported types: `Date`, `LocalDate`, `LocalDateTime`, `ZonedDateTime`, `Instant`

### Number Formatting

Use `DecimalFormat` patterns:

```java
var ctx = Map.of("num", 1234.5);
TextTemplate.render("${num|#,##0.00}", ctx);  // "1,234.50"
TextTemplate.render("${num|00000}", ctx);     // "01235"
```

## Advanced Usage

### Template Caching

Parse once, render many times:

```java
var template = TextTemplate.parse("Hello ${name}!");
var result1 = TextTemplate.render(template, Map.of("name", "Alice"));
var result2 = TextTemplate.render(template, Map.of("name", "Bob"));
```

### Custom Variable Not Found Handler

```java
var opts = DefaultRenderOptions.builder()
    .onVariableNotFound(variable -> "[MISSING: " + variable + "]")
    .build();
TextTemplate.render("Hello ${unknown}!", Map.of(), opts);
// "Hello [MISSING: unknown]!"
```

### File Includes

```java
var opts = DefaultRenderOptions.withContentIncluder(RenderOptions.RESOURCE_FILE_INCLUDER);
TextTemplate.render("$include(templates/header.md)", context, opts);
```

### Macros

```java
var opts = DefaultRenderOptions.builder()
    .macro(TextTemplateStringMacro.fromText("greeting", "Hello ${name}!"))
    .build();

TextTemplate.render("$call(greeting) $arg(name)World$end $end", Map.of(), opts);
// "Hello World!"
```

Or implement `TextTemplateMacro` directly:

```java
var opts = DefaultRenderOptions.builder()
    .macro(new TextTemplateMacro() {
        @Override
        public String name() { return "wrapper"; }

        @Override
        public String apply(Map<String, String> arguments) {
            return "<div>" + arguments.get("body") + "</div>";
        }
    })
    .build();
```

## Examples

### Conditional Content

```
$if(user)
Welcome back, ${user}!
$end
$unless(user)
Please log in.
$end
```

### Iterating a List

```
Items:
$each(items)
- ${it}
$end
```

### Iterating a Map

```
$each(config)
${key} = ${it}
$end
```

### Nested Loops with Parent Access

```
$each(categories)
Category: ${name}
$each(../items)
  - ${it}
$end
$end
```

### Markdown Table

```
| Name | Age |
|------|-----|
$each(people)
| ${name} | ${age} |
$end
```

## API Reference

### TextTemplate

| Method | Description |
|--------|-------------|
| `parse(String)` | Parse template string to `Template` |
| `render(Template, Map)` | Render with context map |
| `render(String, Map)` | Parse and render in one step |
| `render(Template, Function)` | Render with lookup function |
| `render(*, *, RenderOptions)` | Render with custom options |

### DefaultRenderOptions

| Method | Description |
|--------|-------------|
| `defaults()` | Create with default settings |
| `withContentIncluder(Function)` | Create with file includer |
| `builder()` | Create a new Builder |

### DefaultRenderOptions.Builder

| Method | Description |
|--------|-------------|
| `timeZone(TimeZone)` | Set timezone for date formatting |
| `addFormatter(ValueFormatter)` | Add a custom formatter |
| `macro(TextTemplateMacro)` | Add a macro |
| `macros(List<TextTemplateMacro>)` | Add multiple macros |
| `contentIncluder(Function)` | Set file include handler |
| `onVariableNotFound(Function)` | Set missing variable handler |
| `build()` | Build immutable options |

### RenderOptions (interface)

| Method | Description |
|--------|-------------|
| `onVariableNotFound(String, Function)` | Handle missing variables |
| `format(Object, String)` | Format a value |
| `getIncludeContent(String)` | Load included file |
| `callMacro(String, HashMap)` | Execute a macro |

## Thread Safety

All classes are immutable and thread-safe:

- **`Template`** - Parsed AST, safe to cache and share across threads
- **`DefaultRenderOptions`** - Immutable, built via `Builder`
- **`render()` methods** - Stateless, safe to call concurrently

Recommended pattern for multi-threaded applications:

```java
// Parse once at startup
private static final Template TEMPLATE = TextTemplate.parse("Hello ${name}!");
private static final RenderOptions OPTIONS = DefaultRenderOptions.defaults();

// Render from any thread
public String greet(String name) {
    return TextTemplate.render(TEMPLATE, Map.of("name", name), OPTIONS);
}
```
