# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.6] - 2026-10-16

### Added
- Aquecimento opcional de 7 minutos antes do treino principal

## [1.0.5] - 2026-10-14

### Added
- Option to select Light, Dark, or System Automatic theme in settings

### Fixed
- Screen headers no longer collide or overlap with the system notification/status bar

## [1.0.4] - 2026-10-14

### Fixed
- Partial workout sessions now log actual elapsed training duration instead of defaulting to 20 minutes

## [1.0.3] - 2026-10-14

### Added
- Workout session duration is now visible and editable in History
- Weekly active minutes now tally the real duration of all completed sessions

## [1.0.2] - 2026-10-13

### Added
- Ending workout session now stops the timer and automatically prefills target repetitions
- Increased size and visual emphasis of the next-rep interval countdown timer

## [1.0.1] - 2026-10-12

### Fixed
- UI layout adjustments and responsive fixes:
  - **General**: Even distribution of action buttons and filter chips using `Modifier.weight(1f)`, `maxLines = 1`, and `TextOverflow.Ellipsis`, preventing text wrapping or clipping on 360dp devices and enlarged system font scales (1.3x and 1.5x).
  - **Home**: Vertical centering of day labels (Mon, Wed, Fri, Sun) aligned with heatmap rows, and 3-letter month labels (Jan, Feb, Mar, etc.) displayed without truncation.
  - **Workout Configuration**: "Add Minute" button placed full-width under the section title, and second interval adjustment with uniform button dimensions (-5s, -1s, value, +1s, +5s). Corrected top header insets.
  - **History**: Uniform height for category filters in a single row, chart legend placed on its own row below the title, removed redundant "+" header button in favor of the bottom list FAB, hidden empty observation blocks, and displayed clean dates for imported sessions.
  - **Log Past Session Dialog**: Date chips arranged with `FlowRow` and native `DatePicker`, clickable date display with "Choose another date", and identical styling across Six-Count and Navy Seal chips.
  - **Previews**: Added `@Preview` configurations with `fontScale = 1.5f` across key screens.

## [1.0.0] - 2026-10-12

### Initial Release

First official release of **Burpee Log**, a private, offline Android workout tracker designed for practitioners of the Busy Dad Program.

#### Features
- **20-Minute Timer with Dynamic Cadence**:
  - Structured 20-minute sessions with interval countdown and rep counters.
  - Flexible repetition cadence patterns (e.g., `[10]` for 1 rep every 10s, or `[30, 20]` for minute-by-minute variation).
  - Automatic calculation of target reps based on configured intervals.
  - Multi-tone audio cues and haptic vibration feedback on every repetition.
  - Background execution support with screen locked/off via Android Foreground Service with a persistent notification.
- **Session Summary & Logging**:
  - Fast adjustment of completed repetitions at the end of each session.
  - Optional notes field to track perceived exertion, pace, or reflections.
- **Progress Dashboard & Analytics**:
  - GitHub-style 12-month activity heatmap shaded proportionally to personal daily records.
  - Activity filter for Six-Count, Navy Seal, or combined workouts.
  - Weekly training minutes tracking (80-minute goal / 4 sessions per week).
  - Streak counter tracking consecutive completed training weeks.
  - Total reps counter and all-time personal records (PR) for Six-Count and Navy Seal.
- **History & Workout Management**:
  - Complete list of past sessions with filter by exercise type.
  - Interactive progression line charts over time.
  - Inline editing of past repetitions, duration, and notes, as well as record deletion.
- **Data Import & Export**:
  - Import historical logs via CSV files (`dd/MM/yyyy` and `yyyy-MM-dd` formats).
  - Interactive preview with automatic deduplication before committing to the database.
  - Full CSV backup export with built-in formula injection sanitization.
- **Security & Privacy**:
  - 100% offline: no `INTERNET` permission, zero third-party analytics, telemetry, or remote dependencies.
  - No user accounts, registration, or cloud sign-in required.
  - Secure local database using Room (SQLite) stored in private app sandbox.
  - Strict input boundary validation and sanitization in CSV parser.
  - Release build secured with R8/ProGuard code minification, obfuscation, and resource shrinking.
