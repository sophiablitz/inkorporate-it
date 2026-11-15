# Team review rubric

What a reviewer looks for on inkorporate-it, in order of importance. Use it to
decide what to flag and how serious it is.

## 1. Correctness (usually Must fix)

- The feature does what the PR says, including edge cases: empty lists, no
  network, a player leaving, rotating the phone, pressing Back.
- No crashes: unsafe `!!`, unchecked casts, index out of range, `lateinit` used
  before it's set, parsing that can throw.
- State updates reach the screen. In Compose, a value the UI depends on lives in
  `State` (`mutableStateOf`, `StateFlow` collected with `collectAsState`), not a
  plain `var`.
- User input survives what it should: use `rememberSaveable` or a ViewModel when
  it has to survive rotation.
- Coroutines and network: no blocking calls on the main thread; work is launched in
  a scope that matches its lifetime (`viewModelScope`, `LaunchedEffect`), not
  `GlobalScope`; errors are caught and shown to the user.
- Server (Ktor): every route handles bad or missing input; shared state touched by
  several connections at once is protected; WebSocket sessions are cleaned up when a
  phone disconnects.

## 2. Project rules (Must fix)

- Branch name starts with `front-end/`, `back-end/`, or `shared/`.
- Every commit that changes the Android app has `[emulator-tested]`.
- `scripts/test.sh` passes: unit tests and lint.
- No secrets, local config, or build output in the PR.
- Business logic and data models used by both the app and the server go in `shared/`,
  not copied into both.
- The branch merges into `main` without conflicts. Being behind `main` with no
  conflicts is fine, because GitHub still allows the merge: make that a Suggestion at most.

## 3. Kotlin, Compose, and Ktor practice (Must fix or Suggestion)

- **State hoisting:** composables take state and callbacks as parameters; the
  screen or ViewModel owns the state.
- **Side effects:** no network calls, logging, or state changes directly in a
  composable body; use `LaunchedEffect`, `DisposableEffect`, or event callbacks.
- **Resources and theme:** user-facing text in `strings.xml`; colors, fonts, and
  shapes from the theme (`MaterialTheme.colorScheme`, `Theme.kt`), not hard-coded
  values.
- **Previews:** new composables have a `@Preview` when practical.
- **Kotlin idioms:** `val` over `var`, data classes for plain data, `when` over long
  `if` chains, null-safety (`?.`, `?:`) over `!!`.
- **Routes:** route handlers stay short and call into classes that do the work.

## 4. Readability (usually Suggestion)

- Names say what something is or does (`submitGuess()`, not `doIt()`).
- Functions do one thing; long ones are split.
- No dead code, commented-out blocks, leftover `println`/`Log.d` debugging, or
  `TODO`s without a note on who and when.
- Comments explain *why*, not what.
- Formatting matches the rest of the file (Android Studio: Code ▸ Reformat Code).

## 5. Tests (Suggestion, or Must fix when logic is untested and risky)

- New logic outside the UI (game rules, scoring, parsing, server state) has a unit
  test.
- Suggest a specific first test: its name, the setup, and the expected result.

## How serious is it?

| Label | Use when |
| --- | --- |
| Must fix | It breaks, crashes, fails a check, breaks a project rule, or leaks a secret. |
| Decision | Two or more reasonable designs exist and the choice affects other code or teammates. |
| Suggestion | The code works and follows the rules, but could be clearer, simpler, or safer. |
| Nice work | Something done well that others should copy. |
