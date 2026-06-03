# MiniLauncher — Architecture Research & Recommendations

## 1. Language: Kotlin

**Verdict: 100% Kotlin. No debate.**

| Factor | Kotlin | Java | Rust |
|--------|--------|------|------|
| Compose support | Native (only option) | None | None |
| Coroutines/Flow | First-class | Verbose CompletableFuture | N/A (JNI bridge) |
| Modern Android libs | Kotlin-first | Legacy support | N/A |
| Build time | ~15-25% slower than Java cold | Faster cold builds | ~10x slower |
| Runtime perf | ~identical to Java on JVM | Baseline | 5-15x faster for CPU-bound native |
| Ecosystem maturity | Full Android ecosystem | Full but aging | Niche, JNI pain |
| Cold start | ~914ms (Ktor/Native) | ~2.7s (JVM) | ~153ms (native) |

**Why not Rust?** Rust is 5-15x faster than Kotlin for CPU-bound work, but a launcher's bottlenecks are:
- Cold start time (JVM warmup, not CPU)
- I/O (PackageManager queries, DataStore reads)
- UI rendering (Compose framework overhead)

None of these are CPU-bound computation. The JNI bridge overhead (Kotlin → C → Rust) adds complexity for zero measurable gain in a launcher context. Rust makes sense for image processing, crypto, or media codecs — not for a home screen app.

**Why not Java?** Compose is Kotlin-only. Period. Every new Jetpack library is Kotlin-first. Java has no path forward for new Android UI work in 2026.

---

## 2. Architecture Pattern: MVI (Model-View-Intent)

**Verdict: MVI with unidirectional data flow, single immutable state per screen.**

### MVVM vs MVI for a Launcher

| Aspect | MVVM | MVI |
|--------|------|-----|
| State model | Multiple `StateFlow`/`LiveData` | Single immutable `UiState` |
| Debugging | Hard to trace state inconsistencies | Replayable intent→state transitions |
| Compose fit | Good | **Excellent** (Compose is state-driven) |
| Boilerplate | Low | Moderate |
| Race conditions | Possible with multiple flows | **Impossible** (single state reduction) |
| Predictability | Medium | **High** |

**Why MVI wins for a launcher:**

1. **Launcher state is inherently complex** — home screen layout, app list, search query, gesture state, notification badges, theme settings. Multiple `StateFlow` streams in MVVM lead to desynchronized UI states. MVI's single `UiState` eliminates this class of bugs entirely.

2. **Compose recomposition** — MVI's immutable state model maps 1:1 to Compose's `@Stable`/`@Immutable` contract. One state object = one recomposition trigger = predictable rendering.

3. **Debugging** — When a user reports "the app drawer showed stale icons," you can replay the exact intent sequence that produced that state. MVVM gives you scattered state mutations.

4. **Launcher-specific interactions** — Swipe gestures, long-press menus, drag-and-drop, search filtering. These produce rapid, interleaved events. MVI's reducer processes them sequentially, preventing race conditions.

### Recommended MVI Structure

```
Intent (sealed class) → Reducer (pure function) → New UiState → Compose render
                                    ↓
                              Side Effects (Channel, bounded)
```

**Key rules:**
- Reducer is **pure** — no coroutines, no side effects, no `Job` handles
- Async work lives in **middleware** (or ViewModel methods that emit internal intents)
- Effects use `Channel<BUFFERED>` — never `SharedFlow` for one-shot events
- `@Immutable` on every `UiState` data class, `@Stable` on Intent/Effect sealed roots
- Use `kotlinx.collections.immutable` for lists in state (never `List<T>`)

---

## 3. App Architecture: Lean Clean Architecture

**Verdict: Two-layer (UI + Data) with optional Domain layer only when needed.**

```
┌─────────────────────────────────────────────┐
│                  UI Layer                     │
│  Compose Screens ← Route (lifecycle)        │
│  Route collects state, resolves UiText       │
│  Screen = pure function of (state, actions)  │
│  Store/ViewModel: MVI reducer + middleware    │
├─────────────────────────────────────────────┤
│              Data Layer                      │
│  Repository pattern (interfaces)             │
│  DataStore (preferences)                      │
│  Room (structured data, if needed)           │
│  PackageManager integration                  │
│  LauncherApps integration                    │
└─────────────────────────────────────────────┘
```

**Why NOT three layers (with Domain)?**

A launcher is not a banking app. The business logic is:
- Query installed apps → sort/filter → display
- Load/save home screen layout
- Handle gesture → route to action

This doesn't warrant UseCase classes. A `Repository` with suspend functions is sufficient. Add a Domain layer **only** when you have complex, reusable business logic that multiple ViewModels share (e.g., app usage tracking algorithms, smart sorting heuristics).

**Module structure (start simple, split later):**

```
:app                    ← Application, MainActivity, DI setup
:feature:home           ← Home screen, app drawer, search
:feature:settings       ← Settings screens
:core:data              ← Repositories, DataStore, Room
:core:launcher          ← LauncherActivity, wallpaper, widget host
:core:model             ← Shared data classes
:core:designsystem      ← Theme, Compose components
```

Start with `:app` + `:core:data` + `:core:model`. Split into feature modules when the team grows or build times become painful.

---

## 4. Launcher-Specific Architecture Decisions

### 4.1 Single Activity Architecture

A launcher has essentially one screen (the home screen) with overlays (drawer, settings). Use a **single `LauncherActivity`** with Compose Navigation for screen transitions.

```kotlin
// AndroidManifest.xml
<activity
    android:name=".LauncherActivity"
    android:launchMode="singleTask"
    android:stateNotNeeded="true"
    android:clearTaskOnLaunch="true"
    android:resumeWhilePausing="true"
    android:taskAffinity=""
    android:exported="true">
    <intent-filter>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.HOME" />
        <category android:name="android.intent.category.DEFAULT" />
    </intent-filter>
</activity>
```

Key flags:
- `singleTask` — prevents multiple launcher instances
- `stateNotNeeded` — allows system to kill process freely
- `clearTaskOnLaunch` — clean state on relaunch
- `resumeWhilePausing` — faster return to launcher

### 4.2 Cold Start Optimization (Critical for Launchers)

A launcher must be **instantly** available. Target: TTID < 300ms, TTFD < 500ms.

**Strategy:**

1. **App Startup library** — Consolidate all `ContentProvider` initializers into one. Only crash reporting runs before `Application.onCreate()`. Everything else on `Dispatchers.IO`.

2. **Lazy initialization** — Never load app list, icons, or preferences on main thread. Show a minimal skeleton UI immediately, populate asynchronously.

3. **Baseline Profiles** — Generate custom profiles covering the launcher startup journey. Default profiles only cover ~50% of hot methods. Custom profiles cover ~85% and reduce cold start by 30-40%.

4. **Minimal Application.onCreate()** — Return in < 5ms. All SDK init on background threads.

```kotlin
class LauncherApplication : Application(), Configuration.Provider {
    override fun onCreate() {
        super.onCreate()
        // < 5ms on main thread
        if (BuildConfig.DEBUG) {
            StrictMode.enableDefaults()
        }
        // Everything else on background threads
        processScope.launch(Dispatchers.IO) {
            initializeNonCriticalComponents()
        }
    }
}
```

5. **SplashScreen API** — Use Android 12+ `SplashScreen` to show branded content while app initializes. Keep it minimal — just icon + brand color.

### 4.3 Icon Loading & Caching

Icons are the heaviest part of a launcher. Use `LruCache` with a size limit:

```kotlin
class IconCache(
    maxSize: Int = (Runtime.getRuntime().maxMemory() / 8).toInt() // ~3MB typical
) : LruCache<String, Drawable>(maxSize) {

    override fun sizeOf(key: String, value: Drawable): Int {
        return (value as? BitmapDrawable)?.bitmap?.byteCount ?: 0
    }
}
```

**Rules:**
- Load icons on `Dispatchers.IO`
- Convert `Drawable` → `Bitmap` once, cache the bitmap
- Use `remember(key)` in Compose to avoid recomputation
- Debounce package change events (300ms) — bulk install/uninstall shouldn't trigger N reloads

### 4.4 Compose Performance Patterns

These are **non-negotiable** for a launcher:

```kotlin
// 1. Stable keys in LazyColumn/LazyVerticalGrid
LazyVerticalGrid(columns = AdaptiveGridSize(72.dp)) {
    items(
        count = apps.size,
        key = { apps[it].packageName }  // ← ALWAYS
    ) { index ->
        AppItem(apps[index])
    }
}

// 2. Draw-phase animations (NOT composition-phase)
// BAD: triggers recomposition every frame
val alpha by animateFloatAsState(targetValue = 1f)
Box(Modifier.alpha(alpha)) { ... }

// GOOD: only draw phase invalidated
val alphaState = infiniteTransition.animateFloat(...)
Box(Modifier.graphicsLayer { alpha = alphaState.value }) { ... }

// 3. derivedStateOf for filtering
val filteredApps by remember {
    derivedStateOf { allApps.filter { it.matches(query) } }
}

// 4. Immutable state classes
@Immutable
data class LauncherUiState(
    val apps: ImmutableList<AppModel>,  // kotlinx.collections.immutable
    val query: String = "",
    val isLoading: Boolean = false
)
```

### 4.5 Defensive PackageManager

Launchers must handle every OEM quirk and edge case:

```kotlin
class SafePackageManager(private val pm: PackageManager) {
    fun getInstalledApps(): List<AppModel> {
        return try {
            pm.queryIntentActivities(...)
                .mapNotNull { resolveInfo ->
                    try {
                        resolveInfo.toAppModel()
                    } catch (e: Exception) {
                        null  // Skip bad packages, don't crash
                    }
                }
        } catch (e: DeadSystemException) {
            emptyList()  // System is dying, return empty
        } catch (e: SecurityException) {
            emptyList()
        }
    }
}
```

### 4.6 Data Persistence

| Data | Storage | Why |
|------|---------|-----|
| User preferences | DataStore (Preferences) | Async, type-safe, no blocking I/O |
| Home screen layout | DataStore (Proto) or Room | Structured data with migrations |
| App list | In-memory cache + PackageManager | Never persist what the system provides |
| Widget state | System WidgetHost | Android manages this |

**DataStore corruption handling:**

```kotlin
val Context.launcherDataStore by dataStore(
    file = "launcher_prefs.pb",
    corruptionHandler = ReplaceFileCorruptionHandler {
        // Reset to defaults instead of crash-looping
        LauncherPrefs.getDefault()
    }
)
```

---

## 5. Dependency Injection: Hilt

Use Hilt. It's the standard, it's compile-time verified, and it integrates with ViewModels, WorkManager, and everything else.

**Rules:**
- `@ApplicationContext` only — never pass `Activity`/`Context` into singletons
- Repository interfaces in `:core:data`, implementations in `:core:data` too (YAGNI until you need to swap)
- Don't over-abstract. A `AppRepository` interface with one implementation is fine. Don't create `AppRepositoryImpl` + `AppRepositoryFake` until you need the fake.

---

## 6. Tech Stack Summary

| Component | Choice | Rationale |
|-----------|--------|-----------|
| Language | **Kotlin 2.1+** | Compose, coroutines, modern Android |
| UI | **Jetpack Compose + Material 3** | Declarative, state-driven, modern |
| Architecture | **MVI (unidirectional data flow)** | Predictable state, Compose-native |
| DI | **Hilt** | Standard, compile-time, ViewModel integration |
| Async | **Kotlin Coroutines + Flow** | Structured concurrency, lifecycle-aware |
| State | **StateFlow + Channel** | Single source of truth, bounded effects |
| Persistence | **DataStore (Proto + Prefs)** | Async, safe, corruption recovery |
| Navigation | **Navigation Compose** | Type-safe, single-activity |
| Icons | **LruCache + Coil** | Memory-bounded, async loading |
| Startup | **App Startup library** | Single ContentProvider, lazy init |
| Performance | **Baseline Profiles + Macrobenchmark** | 30-40% cold start improvement |
| Build | **AGP 9.x, KSP, R8 full mode** | Modern toolchain, code shrinking |

---

## 7. What NOT to Use

| Avoid | Why |
|-------|-----|
| Room (initially) | Overkill for launcher data. Use DataStore. Add Room only if you need complex queries. |
| Multi-module (initially) | Start with `:app` + `:core:data`. Split when build times hurt. |
| Clean Architecture Domain layer | YAGNI. Repositories with suspend functions are enough. |
| XML Views | Compose is the future. No reason to start a new project with Views. |
| LiveData | StateFlow is superior. LiveData is legacy. |
| RxJava | Coroutines + Flow replace it entirely. |
| Rust/JNI | No CPU-bound bottleneck in a launcher. JNI complexity isn't worth it. |
| Kotlin Multiplatform | Launcher is Android-only. No sharing benefit. |

---

## 8. Reference Implementations

These open-source launchers demonstrate the patterns recommended above:

| Project | Architecture | Key Takeaway |
|---------|-------------|--------------|
| [Kolibri-Launcher](https://github.com/reygnn/Kolibri-Launcher) | MVVM + Clean Arch + Hilt | Best reference for minimal launcher with modern arch |
| [Lawnchair-Lite](https://github.com/SysAdminDoc/Lawnchair-Lite) | Compose + StateFlow | Best reference for stability patterns (crash handling, defensive PM) |
| [CanvasLauncher](https://github.com/khnychenkoav/CanvasLauncher) | Multi-module + Compose | Best reference for modular architecture and viewport culling |
| [FokusLauncher](https://github.com/luantak/FokusLauncher) | MVVM + Hilt + Compose | Best reference for gesture-based minimal launcher |
| [AndroidPerfLab](https://github.com/mohdaquib/androidperflab) | Benchmark patterns | Best reference for startup optimization and Compose perf |

---

## 9. Recommended Project Structure

```
minilauncher/
├── app/
│   ├── src/main/
│   │   ├── java/com/minilauncher/
│   │   │   ├── LauncherApplication.kt        ← App Startup, Hilt entry
│   │   │   ├── LauncherActivity.kt            ← Single activity, Compose host
│   │   │   ├── di/                            ← Hilt modules
│   │   │   └── startup/                       ← App Startup initializers
│   │   └── AndroidManifest.xml
│   └── build.gradle.kts
├── core/
│   ├── data/
│   │   ├── repository/                         ← Repository interfaces + impls
│   │   ├── datastore/                          ← DataStore definitions
│   │   └── model/                              ← Domain models
│   ├── designsystem/                           ← Theme, Compose components
│   └── launcher/                               ← Launcher-specific: PM, widget host
├── feature/
│   ├── home/                                   ← Home screen MVI store + UI
│   │   ├── HomeStore.kt                        ← MVI reducer + middleware
│   │   ├── HomeRoute.kt                        ← Lifecycle, collection
│   │   ├── HomeScreen.kt                       ← Pure composable
│   │   └── HomeUiState.kt                      ← @Immutable state + intents
│   ├── drawer/                                 ← App drawer MVI store + UI
│   └── settings/                               ← Settings MVI store + UI
├── baseline-profile/                            ← Macrobenchmark + profile generator
└── build.gradle.kts
```

**Start with `:app` only.** Extract `:core:data` when you have 2+ features. Extract `:feature:*` when build times exceed 30s. The structure above is the **target**, not the starting point.

---

## 10. Performance Budget Targets

| Metric | Target | How to Measure |
|--------|--------|----------------|
| Cold start TTID | < 300ms | Macrobenchmark `StartupTimingMetric` |
| Cold start TTFD | < 500ms | `reportFullyDrawn()` + Macrobenchmark |
| App list render | < 100ms | `LazyVerticalGrid` first frame |
| Scroll FPS | 60fps (no jank) | Macrobenchmark `FrameTimingMetric` |
| APK size | < 3MB | R8 full mode, split APKs |
| Memory (icons) | < 30MB | LruCache with eviction |
| Package update debounce | 300ms | `Flow.debounce(300)` |

---

## Sources

- Google's official app architecture guide (developer.android.com/topic/architecture)
- AndroidPerfLab startup benchmarks (5.5x TTID improvement via async init)
- ProAndroidDev "0 Recompositions in Complex Custom UI" (LazyLayout + draw-phase state)
- Lawnchair-Lite stability patterns (defensive PM, DataStore corruption recovery)
- CanvasLauncher viewport culling and modular architecture
- Kolibri-Launcher MVI + Clean Architecture reference
- Strict-MVI Playbook (Staff Android Engineers, Medium)
- Android Baseline Profiles CI pipeline (35% cold start reduction)