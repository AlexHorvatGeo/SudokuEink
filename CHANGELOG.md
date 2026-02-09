# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.1.0] - 2026-02-09
### Fixed
- Puzzles with multiple solutions issue
- Puzzle quality improvements

### Added
• All Sudoku puzzles now have exactly one unique solution

### Improved
- Enhanced puzzle generation algorithm
- Better difficulty consistency

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
