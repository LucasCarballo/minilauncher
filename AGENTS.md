# MiniLauncher — Agent Guidelines

## Project Identity

Minimal, fast Android launcher built with Kotlin + Jetpack Compose. Functional MVI architecture. Zero bloat, zero tracking, zero network calls. Home screen that gets out of your way.

## Core Principles

### YAGNI — You Aren't Gonna Need It

- Build what the home screen needs **today**. No abstract interfaces with one implementation. No Domain layer until two ViewModels share the same business logic. No feature modules until build times hurt.
- Start with `:app`. Extract `:core:data` when you have 2+ features. Extract `:feature:*` when incremental builds exceed 30s.
- Don't create `FooRepository` + `FooRepositoryImpl` + `FooRepositoryFake` until you need the fake. One concrete class is enough.
- Don't add Room until you need complex queries. DataStore covers launcher data.
- Don't add a library until the standard library or a 5-line function won't do the job.

### DRY — Don't Repeat Yourself

- Shared Compose components go in `:core:designsystem`. Extract after the second use, not the first.
- Shared MVI patterns (Store base class, reducer signature, effect handling) go in a single utility file. Don't copy-paste across features.
- Repository logic for PackageManager queries is written once in `AppRepository`. Every screen goes through it.
- String resources, dimensions, and theme tokens are centralized. No magic numbers in composables.

### KISS — Keep It Simple, Stupid

- MVI reducer is a **pure function**: `fun reduce(state: UiState, intent: Intent): UiState`. No coroutines, no side effects, no `Job` handles inside.
- One `UiState` data class per screen. One `Intent` sealed class per screen. One `Effect` sealed class per screen. That's the entire contract.
- If a composable has more than 3 parameters, split it. If a reducer `when` block exceeds 15 branches, split the screen's responsibility.
- Prefer Kotlin stdlib over libraries. `Result<T>` over Arrow's `Either`. `data class copy()` over optics. Extension functions over inheritance hierarchies.
- Comments explain **why**, never **what**. Code explains what.

## Architecture

### MVI — Unidirectional Data Flow

```
User Action → Intent → Reducer (pure) → New UiState → Compose Render
                                    ↓
                              Side Effects (Channel)
```

**Rules:**

1. **Reducer is pure.** No `suspend`, no `viewModelScope.launch`, no `try/catch`, no `Job` references. It takes `(state, intent)` and returns a new `state`. Period.
2. **Single source of truth.** One `StateFlow<UiState>` per screen. Never expose multiple mutable state variables.
3. **Effects are one-shot.** Use `Channel<Effect>(BUFFERED)` for navigation, toasts, and launcher intents. Never `SharedFlow`. Never put one-shot events in state.
4. **Intents are sealed classes.** Every user action and every async result maps to exactly one intent. No generic `OnEvent` catch-all.
5. **State is immutable.** `@Immutable` on every `UiState` data class. `kotlinx.collections.immutable` for all lists in state. Never `List<T>` (it's a mutable interface).

### Screen Structure

Every screen follows this pattern:

```
feature/home/
├── HomeStore.kt          ← MVI store: reducer + middleware + effect channel
├── HomeRoute.kt          ← Lifecycle-aware collection, UiText resolution, navigation
├── HomeScreen.kt         ← Pure composable: @Composable fun HomeScreen(state, actions)
├── HomeUiState.kt         ← @Immutable state + sealed Intent + sealed Effect
└── HomeReducer.kt         ← Pure function, extracted for testability
```

**Route vs Screen:**
- `Route` owns all Android dependencies: Hilt injection, lifecycle collection (`collectAsStateWithLifecycle`), `rememberUpdatedState` on callbacks, navigation calls.
- `Screen` is a pure function of `(state, actions)`. Zero Android imports. Zero lifecycle awareness. Testable with Compose UI tests.

### Functional Patterns

- **Algebraic data types** — `sealed class` for Intents, Effects, and state variants. Exhaustive `when` blocks enforced by the compiler.
- **No nulls in state** — Use `sealed interface Loading/Loaded/Error` instead of `nullable + boolean flags`.
- **Extension functions for composition** — `List<AppModel>.sortedByUserPreference(order)`, `List<AppModel>.filteredBy(query)`. Pure, testable, composable.
- **Typed errors** — `sealed interface AppListError` instead of throwing exceptions from repositories. Let the reducer decide what state to produce.

## Language & Stack

| What | Choice | Version |
|------|--------|---------|
| Language | Kotlin | 2.1+ |
| UI | Jetpack Compose + Material 3 | BOM latest stable |
| Architecture | MVI | — |
| DI | Hilt | — |
| Async | Coroutines + Flow | 1.9+ |
| State | StateFlow + Channel | — |
| Persistence | DataStore (Proto + Preferences) | — |
| Navigation | Navigation Compose | — |
| Build | AGP 9.x, KSP, R8 full mode | — |
| Min SDK | 26 (Android 8.0) | — |
| Target SDK | 36 (Android 16) | — |

**Never use:** LiveData, RxJava, XML Views, Room (until proven necessary), Arrow/FP libraries, Kotlin Multiplatform, Rust/JNI.

## Testing

### Coverage Target

- **Reducers: 95%+** — Pure functions, trivial to test. Every intent branch gets at least 3 tests: happy path, error, edge case.
- **Repositories: 80%+** — Test async behavior, error handling, DataStore reads/writes.
- **Compose UI: 60%+** — Test state rendering, user interactions, navigation. Don't test framework behavior.
- **Store/ViewModel: 70%+** — Test middleware, effect emission, cancellation.

### Testing Rules

1. **Reducer tests are plain JUnit.** No Robolectric, no Android framework, no dispatcher setup. They're pure functions. Run in < 5ms each.
2. **Test the contract, not the implementation.** Given this intent + this state → assert that state. Don't assert on internal method calls.
3. **Parameterized tests for error mapping.** Every `Throwable → UiText` mapping should be a `@ParameterizedTest` row.
4. **Compose UI tests use `createComposeRule()`.** Test that `HomeScreen(LoadedState)` renders app names. Test that clicking the search icon sends `Intent.SearchClicked`. Don't test animations.
5. **Integration tests for critical paths.** App launch → home screen visible. App install → list updates. Gesture → drawer opens. Use Macrobenchmark for performance regression gates.
6. **No mocking Android framework classes.** If you need to mock `PackageManager`, wrap it behind `AppRepository` and mock the repository. Test the real `AppRepository` with Robolectric or instrumented tests.

### Test Naming

```kotlin
// Pattern: `functionName_stateUnderTest_expectedBehavior`
@Test
fun reduce_searchQueryEntered_filtersAppsByQuery() { ... }

@Test
fun reduce_appListLoadFails_showsErrorState() { ... }

@Test
fun reduce_packageAdded_debouncesAndReloads() { ... }
```

## Performance

### Budgets (Non-Negotiable)

| Metric | Target | Gate |
|--------|--------|------|
| Cold start TTID | < 300ms | Macrobenchmark CI gate |
| Cold start TTFD | < 500ms | Macrobenchmark CI gate |
| Scroll FPS | 60fps, < 2% janky | Macrobenchmark CI gate |
| APK size | < 3MB | Build check |
| Icon cache | < 30MB RSS | Debug overlay |
| Package event debounce | 300ms | Unit test |

### Compose Performance Rules

1. **`key = { it.id }`** on every `items()` call. No exceptions.
2. **`graphicsLayer`** for animations. Never read animated state in composition scope. `Modifier.alpha(animatedValue)` → `Modifier.graphicsLayer { alpha = animatedValue }`.
3. **`derivedStateOf`** for derived/computed state. Never recompute filtered lists on every recomposition.
4. **`remember`** for bitmap conversions and expensive computations. Key by the input that changes.
5. **`@Immutable`** on every state class. `kotlinx.collections.immutable` for all collections in state.
6. **`collectAsStateWithLifecycle()`** in Routes. Never `collectAsState()`.
7. **LazyVerticalGrid** for app lists. Never `Column { items.forEach { ... } }`.

### Startup Rules

1. **`Application.onCreate()` returns in < 5ms.** All SDK init on `Dispatchers.IO`.
2. **App Startup library** consolidates all ContentProvider initializers. Only crash reporting runs before `onCreate`.
3. **Baseline Profiles** generated from custom Macrobenchmark journeys covering real startup paths. Regenerated on every release.
4. **`launchMode="singleTask"`** on LauncherActivity. Never multiple instances.
5. **Debounced package events.** `Flow.debounce(300)` on `PACKAGE_ADDED/REMOVED` broadcasts.

## Launcher-Specific Patterns

### Defensive PackageManager

```kotlin
// ALWAYS wrap PM calls. Never let one bad package crash the launcher.
fun getInstalledApps(): List<AppModel> {
    return try {
        pm.queryIntentActivities(...)
            .mapNotNull { it.toAppModelSafely() }
    } catch (e: DeadSystemException) { emptyList() }
      catch (e: SecurityException) { emptyList() }
}
```

### DataStore Corruption Recovery

```kotlin
// ALWAYS provide a corruption handler. Never crash-loop on corrupted prefs.
val Context.launcherDataStore by dataStore(
    file = "launcher_prefs.pb",
    corruptionHandler = ReplaceFileCorruptionHandler { LauncherPrefs.getDefault() }
)
```

### Icon Loading

- Load on `Dispatchers.IO`. Convert `Drawable → Bitmap` once. Cache in `LruCache<String, Bitmap>`.
- Use `remember(key)` in Compose. Never re-decode on recomposition.
- Evict on `onLowMemory` callback.

## Anti-Patterns — Do Not Do These

| Anti-Pattern | Why | Instead |
|---|---|---|
| Multiple `MutableStateFlow` in one ViewModel | State desynchronization bugs | Single `StateFlow<UiState>` |
| `SharedFlow` for one-shot events | Events can be lost or duplicated | `Channel<Effect>(BUFFERED)` |
| `Modifier.alpha(animatedValue)` | Recomposition every frame | `Modifier.graphicsLayer { alpha = ... }` |
| `items(list)` without `key` | Position-based recycling, breaks on insert/delete | `items(count, key = { list[it].id })` |
| `List<T>` in UiState | Mutable interface, breaks Compose skipping | `ImmutableList<T>` from kotlinx.collections.immutable |
| Business logic in Composables | Untestable, unpredictable | Reducer only |
| `catch(e) { }` empty block | Silent failures | Typed error state or crash in debug |
| `Dispatchers.IO` in reducer | Reducer must be pure | Move to middleware |
| `Job` references in state | Not serializable, not pure | Cancellation keys in middleware |
| Room for simple key-value data | Overkill, migration overhead | DataStore |
| Arrow/FP type-level libraries | Complexity without measurable benefit | Kotlin stdlib + sealed classes |
| God ViewModel (1000+ lines) | Unmaintainable | Split screen responsibility or extract middleware |

## Git & Code Style

- **Branch naming:** `feat/home-screen`, `fix/icon-cache-eviction`, `refactor/reducer-extraction`
- **Commit messages:** Conventional. `feat: add app drawer search`, `fix: debounce package events`, `perf: baseline profile for cold start`.
- **Code style:** ktlint with default rules. No semicolons. Trailing commas. 4-space indent.
- **Imports:** No wildcard imports. Organize by package depth.

## File Templates

### UiState + Intent + Effect

```kotlin
@Immutable
data class HomeUiState(
    val apps: ImmutableList<AppModel> = persistentListOf(),
    val query: String = "",
    val isLoading: Boolean = true,
    val error: HomeError? = null
)

sealed interface HomeIntent {
    data class QueryChanged(val query: String) : HomeIntent
    data class AppListLoaded(val apps: List<AppModel>) : HomeIntent
    data class AppListLoadFailed(val error: HomeError) : HomeIntent
    data object RetryClicked : HomeIntent
}

sealed interface HomeEffect {
    data class LaunchApp(val packageName: String) : HomeEffect
    data class ShowToast(val message: UiText) : HomeEffect
}
```

### Reducer

```kotlin
object HomeReducer {
    operator fun invoke(state: HomeUiState, intent: HomeIntent): HomeUiState =
        when (intent) {
            is HomeIntent.QueryChanged -> state.copy(query = intent.query)
            is HomeIntent.AppListLoaded -> state.copy(
                apps = intent.apps.toImmutableList(),
                isLoading = false,
                error = null
            )
            is HomeIntent.AppListLoadFailed -> state.copy(
                isLoading = false,
                error = intent.error
            )
            is HomeIntent.RetryClicked -> state.copy(isLoading = true, error = null)
        }
}
```

### Store (ViewModel)

```kotlin
@HiltViewModel
class HomeStore @Inject constructor(
    private val appRepository: AppRepository
) : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    private val _effects = Channel<HomeEffect>(Channel.BUFFERED)
    val effects: ReceiveChannel<HomeEffect> = _effects

    fun send(intent: HomeIntent) {
        val newState = HomeReducer(_state.value, intent)
        _state.value = newState
        handleSideEffects(intent)
    }

    private fun handleSideEffects(intent: HomeIntent) {
        when (intent) {
            is HomeIntent.RetryClicked -> loadApps()
            // Other intents with side effects
            else -> { /* Pure state transitions, no side effects */ }
        }
    }

    private fun loadApps() {
        viewModelScope.launch {
            appRepository.getInstalledApps()
                .onSuccess { send(HomeIntent.AppListLoaded(it)) }
                .onFailure { send(HomeIntent.AppListLoadFailed(it.toHomeError())) }
        }
    }
}
```

### Route + Screen

```kotlin
// HomeRoute.kt — owns lifecycle, DI, navigation
@Composable
fun HomeRoute(
    onNavigateToSettings: () -> Unit,
    viewModel: HomeStore = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(Unit) {
        viewModel.effects.receiveAsFlow()
            .flowWithLifecycle(lifecycleOwner.lifecycle, Lifecycle.State.STARTED)
            .collect { effect ->
                when (effect) {
                    is HomeEffect.LaunchApp -> { /* startActivity */ }
                    is HomeEffect.ShowToast -> { /* toast */ }
                }
            }
    }

    HomeScreen(
        state = state,
        onIntent = viewModel::send
    )
}

// HomeScreen.kt — pure composable, zero Android imports
@Composable
fun HomeScreen(
    state: HomeUiState,
    onIntent: (HomeIntent) -> Unit
) {
    when {
        state.isLoading -> LoadingIndicator()
        state.error != null -> ErrorState(state.error, onIntent)
        else -> AppGrid(state.apps, state.query, onIntent)
    }
}
```

### Reducer Test

```kotlin
class HomeReducerTest {

    @Test
    fun `query changed updates query and filters apps`() {
        val state = HomeUiState(apps = defaultApps.toImmutableList(), query = "")
        val intent = HomeIntent.QueryChanged("Set")

        val result = HomeReducer(state, intent)

        assertEquals("Set", result.query)
        assertEquals(listOf("Settings"), result.filteredApps.map { it.label })
    }

    @Test
    fun `app list loaded replaces apps and clears loading`() {
        val state = HomeUiState(isLoading = true)
        val apps = listOf(appModel("com.example.app", "Example"))

        val result = HomeReducer(state, HomeIntent.AppListLoaded(apps))

        assertFalse(result.isLoading)
        assertNull(result.error)
        assertEquals(1, result.apps.size)
    }

    @Test
    fun `app list load failed sets error and clears loading`() {
        val state = HomeUiState(isLoading = true)

        val result = HomeReducer(state, HomeIntent.AppListLoadFailed(HomeError.PackageManagerDead))

        assertFalse(result.isLoading)
        assertNotNull(result.error)
    }
}
```