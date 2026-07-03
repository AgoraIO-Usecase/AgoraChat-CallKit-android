# Task 1 Report: 迁移发布脚本到 maven-publish

Status: DONE_WITH_CONCERNS
Time: 2026-07-03 16:36:36 CST

## Changes

- Migrated `maven-push-release.gradle` from legacy `maven` plugin to `maven-publish`.
- Replaced deprecated `classifier` assignment with `archiveClassifier.set(...)` for `javadocJar` and `sourcesJar`.
- Removed legacy `artifacts { archives ... }`, `sign configurations.archives`, and `uploadArchives { mavenDeployer ... }`.
- Added `release(MavenPublication)` with `components.release`, sources/javadoc artifacts, Sonatype/snapshot/manual repositories, POM metadata, and publication signing.
- Kept `chat-callkit/build.gradle` unchanged; the `apply from: "../maven-push-release.gradle"` line remains commented.
- Added compatibility in the publishing script for init-script application order by deferring Android-specific publication setup until `com.android.library` is applied.
- Resolved root/project property lookup so `sdkVersion` and `stagingRepositoryId` work when the script is applied to `:chat-callkit` by the verification init script.

## Verification

Commands run:

```bash
rg -n "apply plugin: 'maven'|uploadArchives|mavenDeployer|sign configurations.archives|artifacts \\{" maven-push-release.gradle
```

Result: no matches after migration.

```bash
sed -n '50,58p' chat-callkit/build.gradle
```

Result: confirmed `//apply from: "../maven-push-release.gradle"` remains commented.

```bash
JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-11.0.2.jdk/Contents/Home sh ./gradlew :chat-callkit:tasks --all -I /tmp/apply-maven-push-release.gradle
```

Result: `BUILD SUCCESSFUL`; output included:

- `publishReleasePublicationToManualRepository`
- `publishReleasePublicationToSonatypeRepository`
- `publishReleasePublicationToSnapshotRepository`

```bash
JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-11.0.2.jdk/Contents/Home sh ./gradlew :chat-callkit:publishReleasePublicationToManualRepository -I /tmp/apply-maven-push-release.gradle
```

Result: failed before publication during `:chat-callkit:extractReleaseAnnotations`; Gradle could not resolve `org.codehaus.groovy:groovy-all:2.4.15` from Maven Central because HTTPS returned `peer not authenticated`.

```bash
JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk1.8.0_181.jdk/Contents/Home sh ./gradlew :chat-callkit:publishReleasePublicationToManualRepository -I /tmp/apply-maven-push-release.gradle
```

Result: failed before publication during `:chat-callkit:compileReleaseJavaWithJavac`; JDK 8 javac raised `AssertionError: annotationType(): unrecognized Attribute name MODULE`.

```bash
find build/manual-maven/io/agora/rtc/chat-callkit -maxdepth 3 -type f
```

Result: no files; manual publication did not run because both publish attempts failed earlier in build tasks.

```bash
git diff --check -- maven-push-release.gradle
```

Result: passed with no output.

## Self-review

- Scope check: implementation only modified `maven-push-release.gradle`; report file added under requested `.superpowers/sdd` path. `chat-callkit/build.gradle` was not modified.
- Legacy API check: old `maven` plugin, `uploadArchives`, `mavenDeployer`, `artifacts { archives ... }`, and `sign configurations.archives` are no longer present.
- Task wiring check: target maven-publish tasks are registered and visible when applying the script via the requested init script.
- Remaining concern: full local manual publish could not be verified in this environment because build prerequisites failed before the publish task executed.

---

Status: DONE_WITH_CONCERNS
Time: 2026-07-03 17:00:06 CST

## Post-review Compatibility Fix

- Added AGP 8 publishing variant declaration in `maven-push-release.gradle`:
  `android { publishing { singleVariant("release") } }`.
- Preserved the existing `release(MavenPublication)` block, Maven coordinates, repositories, POM metadata, signing behavior, and `manual` repository.
- Kept `chat-callkit/build.gradle` unchanged; `//apply from: "../maven-push-release.gradle"` remains commented.

## Verification

Commands run:

```bash
printf "%s\n" "allprojects {" "    if (path == ':chat-callkit') {" "        apply from: rootProject.file('maven-push-release.gradle')" "    }" "}" > /tmp/apply-maven-push-release.gradle
```

Result: init script created with the requested contents.

```bash
sh ./gradlew :chat-callkit:tasks --all -I /tmp/apply-maven-push-release.gradle
```

Result: `BUILD SUCCESSFUL`; output included `publishReleasePublicationToManualRepository`.

```bash
sh ./gradlew :chat-callkit:publishReleasePublicationToManualRepository -I /tmp/apply-maven-push-release.gradle
```

Result: failed at `:chat-callkit:signReleasePublication`:

```text
Cannot perform signing task ':chat-callkit:signReleasePublication' because it has no configured signatory
```

```bash
git diff --check -- maven-push-release.gradle
```

Result: passed with no output.
