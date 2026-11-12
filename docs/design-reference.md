# Design Reference Doc

Material 3 Expressive design, with an "ink" look: thick dark outlines, solid shadows, dotted background.

## Rules

- `InkorporateitTheme(dynamicColor = false)`
- Only `MaterialTheme.colorScheme` and `MaterialTheme.typography`. No hex colors or custom fonts.
- `sp` for text, `dp` for everything else.
- Use the shared components in `ui/component/` (`InkButton`, `InkTextField`, `Modifier.dotGrid`).
- Icons: Vector Asset, no icon libraries.

## InkButton

Outline, solid shadow, and a press animation (slides down onto its shadow, then runs `onClick`).
Disabled = grey, no shadow.

```kotlin
InkButton(
    onClick = { },
    containerColor = MaterialTheme.colorScheme.secondaryContainer, // fill
    contentColor = inkOnYellow(),                                  // text
    shadow = 4.dp,                                                 // 3–6dp, bigger button = bigger shadow
    enabled = true
) { Text("Join game") }
```

## Text

Font: Sniglet (set in the theme, `Type.kt`).

| Use | Style |
|---|---|
| Title | `displayMedium` |
| Big button | `headlineSmall` |
| Heading, button text | `titleMedium` |

## Colors

| Role | For |
|---|---|
| `primary` | Main action |
| `secondaryContainer` + `inkOnYellow()` text | Second actions |
| `surfaceContainerLowest` | Other buttons, text fields |
| `onBackground` | Outlines, text, shadows |
