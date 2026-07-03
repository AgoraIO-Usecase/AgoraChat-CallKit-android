# Maven Publish 迁移 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将 `maven-push-release.gradle` 从旧 `maven` 插件迁移到 `maven-publish`，并新增本地 `manual` Maven 仓库。

**Architecture:** 保持发布脚本作为外部按需 apply 的单文件配置，不改 `chat-callkit/build.gradle` 的注释开关。脚本继续负责读取现有 Maven 凭据、配置 Sonatype staging、生成 source/javadoc jar、签名并发布 release publication。

**Tech Stack:** Gradle 6.7.1、Android Gradle Plugin 4.2.2、`maven-publish`、`signing`、`io.codearte.nexus-staging`。

## Global Constraints

- 不启用 `chat-callkit/build.gradle` 中的 `//apply from: "../maven-push-release.gradle"`。
- Maven 坐标保持 `io.agora.rtc:chat-callkit:${project.sdkVersion}`。
- 远端 Sonatype 仓库继续使用当前 `ossrhUsername` 和 `ossrhPassword`。
- 本地仓库必须命名为 `manual`，地址为 `${rootProject.buildDir}/manual-maven`。
- 保留现有 POM 元数据、签名属性读取方式和 `nexusStaging` 配置。

---

### Task 1: 迁移发布脚本到 maven-publish

**Files:**
- Modify: `maven-push-release.gradle`
- Do not modify: `chat-callkit/build.gradle`

**Interfaces:**
- Consumes:
  - `project.sdkVersion`
  - `project.stagingRepositoryId`
  - `local.properties` 中的 `maven.dir`
  - `${maven.dir}/project.properties` 中的 `ossrhUsername`、`ossrhPassword`、`signing.keyId`、`signing.secretKeyRingFile`、`signing.password`
- Produces:
  - Gradle task `:chat-callkit:publishReleasePublicationToManualRepository`
  - Gradle task `:chat-callkit:publishReleasePublicationToSonatypeRepository`
  - Gradle task `:chat-callkit:publishReleasePublicationToSnapshotRepository`
  - 本地发布目录 `build/manual-maven/io/agora/rtc/chat-callkit/${project.sdkVersion}`

- [ ] **Step 1: 确认当前脚本仍是旧发布 API**

Run:

```bash
rg -n "apply plugin: 'maven'|uploadArchives|mavenDeployer|sign configurations.archives|artifacts \\{" maven-push-release.gradle
```

Expected:

```text
maven-push-release.gradle:4:apply plugin: 'maven'
maven-push-release.gradle:60:artifacts {
maven-push-release.gradle:66:    sign configurations.archives
maven-push-release.gradle:78:uploadArchives {
maven-push-release.gradle:80:        mavenDeployer {
```

- [ ] **Step 2: 修改插件和 jar task 写法**

In `maven-push-release.gradle`, replace:

```gradle
apply plugin: 'maven'
apply plugin: 'signing'
```

With:

```gradle
apply plugin: 'maven-publish'
apply plugin: 'signing'
```

Update jar tasks from deprecated `classifier` assignment:

```gradle
task javadocJar(type: Jar, dependsOn: javadoc) {
    classifier = 'javadoc'
    from javadoc.destinationDir
}

task sourcesJar(type: Jar) {
    from android.sourceSets.main.java.srcDirs
    classifier = 'sources'
}
```

To Gradle 6 compatible archive classifiers:

```gradle
task javadocJar(type: Jar, dependsOn: javadoc) {
    archiveClassifier.set('javadoc')
    from javadoc.destinationDir
}

task sourcesJar(type: Jar) {
    archiveClassifier.set('sources')
    from android.sourceSets.main.java.srcDirs
}
```

- [ ] **Step 3: 移除旧 archives 和 uploadArchives 块**

Delete these blocks from `maven-push-release.gradle`:

```gradle
artifacts {
    archives javadocJar, sourcesJar
}

signing {
    sign configurations.archives
}
```

Delete the entire `uploadArchives { ... }` block.

- [ ] **Step 4: 添加 maven-publish publication、仓库和 POM**

Add this block after `nexusStaging { ... }`:

```gradle
afterEvaluate {
    publishing {
        publications {
            release(MavenPublication) {
                from components.release

                groupId = project.group
                artifactId = project.archivesBaseName
                version = project.version

                artifact sourcesJar
                artifact javadocJar

                pom {
                    name = project.archivesBaseName
                    packaging = 'aar'
                    description = 'Agora SDK CallKit'
                    url = 'http://maven.apache.org'

                    scm {
                        connection = 'scm:git:https://github.com/AgoraIO-Usecase/AgoraChat-CallKit-android.git'
                        developerConnection = 'scm:git:https://github.com/AgoraIO-Usecase/AgoraChat-CallKit-android.git'
                        url = 'https://github.com/AgoraIO-Usecase/AgoraChat-CallKit-android'
                    }

                    licenses {
                        license {
                            name = 'AGORA SDK License'
                            url = 'https://github.com/AgoraIO/full-sdk/blob/master/LICENSE'
                        }
                    }

                    developers {
                        developer {
                            name = 'agorabuilder'
                            email = 'zhaoliang@agora.io'
                            organization = 'https://github.com/AgoraIO'
                            url = 'https://www.agora.io/cn'
                        }
                    }
                }
            }
        }

        repositories {
            maven {
                name = 'sonatype'
                url = uri('https://oss.sonatype.org/service/local/staging/deploy/maven2/')
                credentials {
                    username = ossrhUsername
                    password = ossrhPassword
                }
            }
            maven {
                name = 'snapshot'
                url = uri('https://oss.sonatype.org/content/repositories/snapshots/')
                credentials {
                    username = ossrhUsername
                    password = ossrhPassword
                }
            }
            maven {
                name = 'manual'
                url = uri("${rootProject.buildDir}/manual-maven")
            }
        }
    }

    signing {
        sign publishing.publications.release
    }
}
```

- [ ] **Step 5: 确认模块 apply 开关没有被启用**

Run:

```bash
sed -n '50,58p' chat-callkit/build.gradle
```

Expected:

```text
//apply from: "../maven-push-release.gradle"
```

- [ ] **Step 6: 临时启用脚本验证 task wiring**

Because the module apply switch must remain commented, run a one-command verification that applies the script only for the command:

```bash
./gradlew :chat-callkit:tasks --all -I /tmp/apply-maven-push-release.gradle
```

Before running it, create `/tmp/apply-maven-push-release.gradle` with:

```gradle
allprojects {
    if (path == ':chat-callkit') {
        apply from: rootProject.file('maven-push-release.gradle')
    }
}
```

Expected task names in output:

```text
publishReleasePublicationToManualRepository
publishReleasePublicationToSonatypeRepository
publishReleasePublicationToSnapshotRepository
```

- [ ] **Step 7: 验证本地 manual 仓库发布**

Run:

```bash
./gradlew :chat-callkit:publishReleasePublicationToManualRepository -I /tmp/apply-maven-push-release.gradle
```

Expected:

```text
BUILD SUCCESSFUL
```

Then verify generated files:

```bash
find build/manual-maven/io/agora/rtc/chat-callkit -maxdepth 3 -type f | sort
```

Expected file types include:

```text
.aar
.pom
-sources.jar
-javadoc.jar
.module
.asc
```

- [ ] **Step 8: Final diff review**

Run:

```bash
git diff -- maven-push-release.gradle chat-callkit/build.gradle
```

Expected:

- `maven-push-release.gradle` uses `maven-publish`.
- `maven-push-release.gradle` no longer contains `uploadArchives` or `mavenDeployer`.
- `maven-push-release.gradle` contains `manual` repository with `${rootProject.buildDir}/manual-maven`.
- `chat-callkit/build.gradle` has no diff.

- [ ] **Step 9: Commit implementation**

Run:

```bash
git add maven-push-release.gradle
git commit -m "Migrate release publishing to maven-publish"
```

Expected:

```text
[dev <hash>] Migrate release publishing to maven-publish
```
