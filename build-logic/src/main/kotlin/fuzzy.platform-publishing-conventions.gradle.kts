import com.vanniktech.maven.publish.JavaPlatform

// Maven Central publishing for a `java-platform` module — fuzzy-bom.
//
// CLAUDE.md §23.6: fuzzy.publishing-conventions is hard-wired to vanniktech's
// KotlinJvm (Dokka javadoc jar, sources jar), none of which a platform has —
// a BOM is one POM and no code. Same portal route, same signing, same POM
// (FuzzyPom.kt); only the artifact shape differs.

plugins {
    id("java-platform")
    id("com.vanniktech.maven.publish")
}

mavenPublishing {
    // Registers the Central Portal endpoint. Does not upload — same
    // configured-but-not-published state as every other module (§14.3).
    publishToMavenCentral()
    signAllPublications()

    configure(JavaPlatform())

    fuzzyPom(project)
}
