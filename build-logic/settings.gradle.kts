// build-logic — the included build that carries fuzzy-math's convention plugins.

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }

    // An included build does NOT inherit the root build's version catalog.
    // Re-declare it by pointing at the same TOML file, so there is still
    // exactly one source of truth (CLAUDE.md §1).
    //
    // Deliberately `from(files(...))` and NOT a symlink: Git for Windows checks
    // symlinks out as plain text files unless the user has Developer Mode
    // enabled, which would silently break the build for contributors.
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}

rootProject.name = "build-logic"
