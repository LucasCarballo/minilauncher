# MiniLauncher — Product Specification

## Vision

A distraction-free Android launcher with a terminal aesthetic. Black void, monospace type, warm white text. No icons, no widgets, no noise. Just your name, the time, and the apps you actually use.

---

## Home Screen

```
┌─────────────────────────────────────┐
│                                     │
│  Good evening, Lucas                │
│  Thursday, Jun 4                    │
│  23:47                              │
│                                     │
│  Telegram                           │
│  Spotify                            │
│  Chrome                             │
│  Camera                             │
│  Settings                           │
│                                     │
│                                     │
│                                     │
│                                     │
│                                     │
│                                     │
│  ← swipe right for all apps         │
└─────────────────────────────────────┘
```

### Layout rules

| Element | Position | Style |
|---------|----------|-------|
| Greeting | Top-left, `paddingTop = 80dp` | `Space Mono Bold`, 20sp, `#F5F5F7` |
| Date | Below greeting, +4dp | `Space Mono Regular`, 14sp, `#A1A1A6` |
| Time | Below date, +4dp | `Space Mono Bold`, 48sp, `#F5F5F7` |
| Pinned apps | Below time, +32dp | `Space Mono Regular`, 18sp, `#F5F5F7`, line spacing 44dp |
| App hint | Bottom, 16dp from bottom | `Space Mono Regular`, 12sp, `#6E6E73` |

### Greeting logic

| Time range | Greeting |
|-----------|-----------|
| 05:00–11:59 | Good morning |
| 12:00–17:59 | Good afternoon |
| 18:00–04:59 | Good evening |

User name comes from `Settings.System` or falls back to "there".

### Pinned apps

- Default: 5 apps, user-configurable (0–8 max)
- Long-press any pinned app → remove from pinned / replace
- Tap → launch app
- No icons. Text only.
- Selected/focused app: `#F5F5F7` (full brightness), unselected: `#A1A1A6` (secondary)

---

## App Drawer (Swipe Right)

```
┌─────────────────────────────────────┐
│                                     │
│  Search...                          │
│                                     │
│  ─────────────────────────          │
│                                     │
│  Calculator                         │
│  Calendar                           │
│  Camera                             │
│  Chrome                             │
│  Clock                              │
│  Files                              │
│  Gmail                              │
│  Google Maps                        │
│  ...                                │
│                                     │
└─────────────────────────────────────┘
```

### Behavior

- **Trigger**: Swipe right on home screen (or swipe up — configurable later)
- **Search**: Auto-focus keyboard on open. Real-time filter as you type.
- **List**: Alphabetical, all launchable apps, text-only (no icons)
- **Launch**: Tap app name → launch, close drawer
- **Close**: Swipe left, back gesture, or tap outside search
- **Search input style**: `Space Mono Regular`, 16sp, cursor color `#F5F5F7`
- **Filtered text**: Matched substring highlighted in `#F5F5F7`, rest in `#6E6E73`

### Search rules

- Case-insensitive substring match on app label
- Results update on every keystroke (debounced 150ms in reducer)
- Empty query = show all apps
- Single remaining match → auto-launch after 300ms (configurable, default off)

---

## Design Tokens

### Colors

```kotlin
// Background
val Black = Color(0xFF000000)           // Pure black — OLED-friendly

// Text hierarchy
val TextPrimary = Color(0xFFF5F5F7)    // Headings, app names, time — 17:1 contrast
val TextSecondary = Color(0xFFA1A1A6)  // Date, unselected apps — 8:1 contrast
val TextTertiary = Color(0xFF6E6E73)  // Hints, disabled — 4.5:1 contrast

// Interactive
val CursorColor = TextPrimary           // Search cursor, focus indicators

// Surfaces (for drawer background, dialogs)
val Surface1 = Color(0xFF1A1A1A)       // Drawer background
val Surface2 = Color(0xFF222222)       // Dialogs, modals
```

### Typography

```kotlin
val Typography = Typography(
    displayLarge = TextStyle(       // Time (48sp)
        fontFamily = SpaceMono,
        fontWeight = FontWeight.Bold,
        fontSize = 48.sp,
        lineHeight = 56.sp,
        letterSpacing = (-0.02).sp
    ),
    headlineMedium = TextStyle(    // Greeting (20sp)
        fontFamily = SpaceMono,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 28.sp
    ),
    bodyLarge = TextStyle(         // App names (18sp)
        fontFamily = SpaceMono,
        fontWeight = FontWeight.Medium,
        fontSize = 18.sp,
        lineHeight = 44.sp,        // Generous line height for tap targets
        letterSpacing = 0.01.sp
    ),
    bodyMedium = TextStyle(        // Search input, list items (16sp)
        fontFamily = SpaceMono,
        fontWeight = FontWeight.Regular,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    labelSmall = TextStyle(        // Date, hints (14sp / 12sp)
        fontFamily = SpaceMono,
        fontWeight = FontWeight.Regular,
        fontSize = 14.sp,
        lineHeight = 20.sp
    )
)
```

### Spacing

| Token | Value | Usage |
|-------|-------|-------|
| `spacing_xs` | 4dp | Tight gaps (date below greeting) |
| `spacing_sm` | 8dp | Compact spacing |
| `spacing_md` | 16dp | Standard padding |
| `spacing_lg` | 32dp | Section breaks (time → apps) |
| `spacing_xl` | 80dp | Top padding before greeting |
| `app_line_height` | 44dp | Vertical spacing between app names (tap target) |

### Animation

| Transition | Duration | Easing |
|-----------|----------|--------|
| Drawer slide-in | 250ms | `EaseOut` |
| Drawer slide-out | 200ms | `EaseIn` |
| App name focus | 150ms | `EaseInOut` |
| Search filter | 0ms (instant) | — (debounce in reducer) |

---

## Interactions

### Gestures

| Gesture | Action |
|---------|--------|
| Tap app name | Launch app |
| Long-press app name | Context menu (remove from pinned / replace / app info) |
| Swipe right | Open app drawer |
| Swipe left (in drawer) | Close drawer, return to home |
| Back gesture | Close drawer if open, else no-op (consume back) |

### Long-press context menu

```
┌──────────────────┐
│  Remove from home │
│  Replace with...  │
│  App info         │
└──────────────────┘
```

- Minimal menu, same monospace font
- `Surface2` background, `TextPrimary` text
- Dismiss on tap outside or back

---

## Settings (Future — Not in MVP)

- App pin configuration (add/remove/reorder)
- Greeting name override
- 12h / 24h time format
- Swipe direction (right vs up for drawer)
- Auto-launch on single search match toggle

---

## Technical Requirements

### Manifest

```xml
<activity
    android:name=".LauncherActivity"
    android:exported="true"
    android:launchMode="singleTask"
    android:stateNotNeeded="true"
    android:clearTaskOnLaunch="true"
    android:resumeWhilePausing="true"
    android:taskAffinity=""
    android:theme="@style/LauncherTheme">

    <intent-filter>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.HOME" />
        <category android:name="android.intent.category.DEFAULT" />
    </intent-filter>
</activity>
```

### Theme

- Full-screen immersive (`enableEdgeToEdge()`)
- Status bar + nav bar transparent
- `windowBackground = Color.Black` (instant first frame, no white flash)
- No action bar, no title bar

### App discovery

- `LauncherApps.getActivityList()` for launcher-optimized queries
- Fallback to `queryIntentActivities(ACTION_MAIN, CATEGORY_LAUNCHER)`
- Defensive: wrap all PM calls in try/catch for `DeadSystemException`, `SecurityException`
- Debounce package change broadcasts (300ms)

### App launching

- Use resolved `ComponentName` from `LauncherActivityInfo`
- `FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_RESET_TASK_IF_NEEDED`
- Handle `ActivityNotFoundException` → refresh app list

### Performance

| Metric | Target |
|--------|--------|
| Cold start TTID | < 300ms |
| Cold start TTFD | < 500ms |
| Scroll FPS | 60fps, < 2% janky |
| APK size | < 3MB |
| Icon cache | < 30MB RSS (not needed for MVP — text only) |

### Font loading

- Bundle `SpaceMono-Bold.ttf` and `SpaceMono-Regular.ttf` in `res/font/`
- Download from Google Fonts (SIL Open Font License)
- Load via `FontFamily(Font(R.font.space_mono_bold), Font(R.font.space_mono_regular))`

---

## MVP Scope

### In

- [ ] Home screen: greeting + date + time + 5 pinned apps
- [ ] App drawer: swipe right → search + alphabetical list
- [ ] App launching (tap to launch)
- [ ] Long-press context menu (remove from pinned, app info)
- [ ] Launcher intent filter (set as default home)
- [ ] Space Mono font, black background, warm white text
- [ ] DataStore: persist pinned app list + user name

### Not in MVP

- [ ] Settings screen
- [ ] App icons (text-only for now)
- [ ] Widget support
- [ ] Notification badges
- [ ] Auto-launch on single search match
- [ ] Gesture customization
- [ ] Wallpaper support (solid black for MVP)

---

## File Structure (MVP)

```
app/src/main/java/com/minilauncher/
├── LauncherApplication.kt
├── LauncherActivity.kt
├── di/
│   └── AppModule.kt
├── data/
│   ├── repository/
│   │   └── AppRepository.kt
│   ├── datastore/
│   │   └── LauncherPrefs.kt
│   └── model/
│       └── AppModel.kt
├── feature/
│   └── home/
│       ├── HomeStore.kt
│       ├── HomeRoute.kt
│       ├── HomeScreen.kt
│       ├── HomeUiState.kt
│       └── HomeReducer.kt
├── feature/
│   └── drawer/
│       ├── DrawerStore.kt
│       ├── DrawerRoute.kt
│       ├── DrawerScreen.kt
│       ├── DrawerUiState.kt
│       └── DrawerReducer.kt
└── ui/
    ├── theme/
    │   ├── Theme.kt
    │   ├── Color.kt
    │   ├── Type.kt
    │   └── Spacing.kt
    └── component/
        ├── AppNameItem.kt
        └── SearchInput.kt
```