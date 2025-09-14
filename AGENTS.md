# Agent Development Guidelines

## NixOS Environment
- Must be in nix devShell: check `$IN_NIX_SHELL` or use `nix develop -c -- <command>`
- Use `.envrc` with `use flake` for automatic environment loading

## Build Commands
- `nix build` - Build the application package
- `nix flake check` - Run all checks including tests
- `gradle build` - Build desktop application (in devShell)
- `gradle run` - Run desktop application
- `gradle test` - Run unit tests only
- `gradle integrationTest` - Run integration tests only
- `gradle check` - Run all tests and checks

## Code Style & Conventions
- Kotlin Multiplatform with Compose Desktop
- Package structure: `com.isaakhanimann.journal.<module>`
- Material 3 theming with dark/light mode support
- Dependency injection with Koin
- Testing with Kotest (StringSpec style) and Cucumber for BDD
- Database: SQLDelight with coroutines extensions
- Serialization: kotlinx.serialization

## File Organization
- Common code: `src/commonMain/kotlin/`
- Desktop-specific: `src/desktopMain/kotlin/`
- Tests: `src/commonTest/kotlin/` and `src/desktopTest/kotlin/`
- Use proper package structure with clear module separation

## Dependencies
- Use version catalog (`gradle/libs.versions.toml`) for dependency management
- Follow existing patterns for adding new dependencies