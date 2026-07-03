# Maven Publish 迁移设计

## 目标

将当前发布脚本从 Gradle 已废弃的 `maven` 插件迁移到受支持的 `maven-publish` 插件。

`chat-callkit/build.gradle` 中的启用开关必须保持不变：

```gradle
//apply from: "../maven-push-release.gradle"
```

发布脚本由外部脚本按需启用，因此本次变更只更新可复用的发布脚本本身。

## 当前上下文

- 库模块：`:chat-callkit`
- 发布脚本：`maven-push-release.gradle`
- Gradle wrapper：`6.7.1`
- Android Gradle 插件：`4.2.2`
- 当前 Maven 坐标：
  - groupId：`io.agora.rtc`
  - artifactId：`chat-callkit`
  - version：`project.sdkVersion`
- 现有 Sonatype staging close/release 由 `io.codearte.nexus-staging` 支持。

## 选定方案

在 `maven-push-release.gradle` 内做窄范围迁移。

替换以下旧配置：

- `apply plugin: 'maven'`
- `uploadArchives`
- `mavenDeployer`
- `artifacts { archives ... }`
- `signing { sign configurations.archives }`

改为：

- `apply plugin: 'maven-publish'`
- `publishing { publications { release(MavenPublication) { ... } } }`
- `signing { sign publishing.publications.release }`

这样可以保持发布行为稳定，同时把 publication 创建和仓库上传迁移到 Gradle 当前支持的 API。

## Publication 设计

创建一个名为 `release` 的 publication。

该 publication 必须满足：

- 发布 `components.release`。
- 保持 groupId 为 `io.agora.rtc`。
- 保持 artifactId 为 `chat-callkit`。
- 保持 version 为 `project.sdkVersion`。
- 附带生成的 sources jar。
- 附带生成的 Javadoc jar。
- 保留现有 POM 元数据：
  - name
  - packaging `aar`
  - description
  - project URL
  - SCM 信息
  - license 信息
  - developer 信息

考虑到 Android Gradle 插件下的组件可能在项目 evaluation 后才可用，publication 配置需要按 Gradle 6.7.1 和 Android Gradle 插件 4.2.2 的兼容方式延后配置。

## 仓库设计

配置以下 `publishing.repositories`：

- `sonatype`：`https://oss.sonatype.org/service/local/staging/deploy/maven2/`
- `snapshot`：`https://oss.sonatype.org/content/repositories/snapshots/`
- `manual`：`${rootProject.buildDir}/manual-maven`

两个远端仓库继续使用现有 `ossrhUsername` 和 `ossrhPassword`，值从当前 Maven properties 文件读取。

本地 `manual` 仓库不需要凭据。

## 签名设计

保留当前签名属性读取方式：

- `signing.keyId`
- `signing.secretKeyRingFile`
- `signing.password`

这些值继续从 `local.properties` 和 `maven.dir` 指向的外部 `project.properties` 文件读取。

签名目标从已移除的 `archives` configuration 改为 `release` Maven publication。

## Nexus Staging 设计

保留现有 `nexusStaging` 配置块。

它继续使用：

- package group 来自 `group`
- staging profile id 来自 `group`
- username 和 password 来自现有 Maven properties
- staging repository id 来自 `project.stagingRepositoryId`

## 验证方式

先验证任务配置是否正确：

```bash
./gradlew :chat-callkit:tasks --all
```

如果本地签名凭据可用，再验证本地发布：

```bash
./gradlew :chat-callkit:publishReleasePublicationToManualRepository
```

成功标准是 `build/manual-maven` 下生成预期的 `io/agora/rtc/chat-callkit/<version>` artifacts 和 metadata。
