# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

# Changelog

## [1.3] - 2026-02-10

### Added
- **New Game Mode: Attack Mode** - Complete sudokus against the clock
    - Easy: 20 minutes time limit
    - Medium: 30 minutes time limit
    - Hard: 45 minutes time limit
- Improved navigation system with difficulty submenu per game mode
- Resume saved game dialog on game entry with paused timer
- Multiple independent saved games (one per mode + difficulty combination)
- Timeout dialog for Attack Mode when time runs out
- Statistics now show best remaining time for Attack Mode

### Fixed
- Timer continuing after game completion
- Timer jumping backwards on orientation changes
- Timer not pausing during error dialog
- Same sudoku appearing in Normal and Attack modes
- Double arrow (←) in back button

### Changed
- Game state management now differentiates between game modes
- Statistics display adapted for Attack Mode (remaining time vs. elapsed time)
- Improved timer synchronization across all game states

### Technical
- Version code: 3 → 4
- Version name: 1.2.0 → 1.3.0
- Enhanced `GameStateManager` with mode parameter
- Improved `rememberTimer` function with better pause handling
- New `hasSavedGame()` function for checking saved games
- Added `showResumeDialog` state for saved game prompts

---

## [1.2.0] - 2026-02-10

### Added
- Completely random Sudoku generation algorithm from scratch
- Infinite puzzle variety with access to 10^16+ different puzzles
- Random backtracking solver for solution generation

### Changed
- Replaced fixed-base generation with fully randomized approach
- Each puzzle now completely unique and unrelated to previous ones
- Improved puzzle diversity across all difficulty levels

### Technical
- Version code: 2 → 3
- Version name: 1.1.0 → 1.2.0
- New functions: `generateRandomSolution()`, `fillBoardRandomly()`, `isValidPlacement()`
- Removed fixed solution base dependency

---

## [1.1.0] - 2026-02-09
### Fixed
- Puzzles with multiple solutions issue
- Puzzle quality improvements

### Added
• All Sudoku puzzles now have exactly one unique solution

### Improved
- Enhanced puzzle generation algorithm
- Better difficulty consistency

---

## [1.1] - 2026-02-08
### Fixed
- Reset button now works correctly after loading a saved game
- Original initial board is now properly stored separately from saved game state

### Added
- Timer controls: pause, play, and restart buttons
- Global pencil mode for quick handwriting recognition
- Independent saved games for each difficulty level
- Auto-save functionality when exiting games
- Automatic game recovery when returning to a difficulty

### Improved
- Adaptive scaled drawing canvas for all screen sizes
- Better visual differentiation between fixed numbers (bold + black) and user numbers (light + gray)
- Enhanced UI scaling system for phones and tablets
- Adaptive layouts: vertical for phones, horizontal for tablets
- Cell background colors for better contrast

### Changed
- Darker background for fixed number cells (improved visibility on e-ink displays)
- Updated translations for all languages (CA/ES/EN)
- Updated dependencies (Material Icons Extended)
- Improved save system architecture

### Technical
- New `AdaptiveSizes.kt` for intelligent screen scaling
- Enhanced `GameState.kt` with per-difficulty save management
- Improved timer implementation with pause/resume functionality

---

## [1.0.0] - 2026-02-07
### Added
- Initial release
- 3 difficulty levels (Easy, Medium, Hard)
- Smart Sudoku generator with optimized algorithms
- Handwriting digit recognition with TensorFlow Lite
- Built-in timer
- Detailed statistics tracking (completed games, best times)
- Notes mode for marking possible numbers
- Hint system (5/3/1 hints per difficulty)
- Undo functionality with unlimited history
- E-ink optimized interface with high contrast
- Multi-language support (Catalan, Spanish, English)
- Persistent game state saving
- Reset game functionality
