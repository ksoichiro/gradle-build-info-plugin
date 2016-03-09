# gradle-build-info-plugin

[![Build Status](https://img.shields.io/travis/ksoichiro/gradle-build-info-plugin/master.svg?style=flat-square)](https://travis-ci.org/ksoichiro/gradle-build-info-plugin)
[![Build status](https://img.shields.io/appveyor/ci/ksoichiro/gradle-build-info-plugin/master.svg?style=flat-square)](https://ci.appveyor.com/project/ksoichiro/gradle-build-info-plugin)
[![Coverage Stagus](https://img.shields.io/coveralls/ksoichiro/gradle-build-info-plugin/master.svg?style=flat-square)](https://coveralls.io/github/ksoichiro/gradle-build-info-plugin?branch=master)
[![Bintray](https://img.shields.io/bintray/v/ksoichiro/maven/gradle-build-info-plugin.svg?style=flat-square)](https://bintray.com/ksoichiro/maven/gradle-build-info-plugin/_latestVersion)
[![Maven Central](http://img.shields.io/maven-central/v/com.github.ksoichiro/gradle-build-info-plugin.svg?style=flat-square)](https://github.com/ksoichiro/gradle-build-info-plugin/releases/latest)

> Gradle plugin to include build information such as Git commit ID to your JAR.

## Usage

Apply plugin:

```gradle
plugins {
    id 'com.github.ksoichiro.build.info' version '0.1.5'
}
```

Note that you can use this plugin only when you use java plugin because you build JAR:

```gradle
apply plugin: 'java'
```

When used with Spring Boot Actuator dependency, this plugin will also generate
`git.properties`, which will be recognized by Spring Boot Actuator's
info endpoint.

Then just build your project:

```console
$ ./gradlew build
```

Now your Manifest in JAR includes special attributes:

```console
$ unzip -p build/libs/yourJar.jar META-INF/Manifest.MF
Manifest-Version: 1.0
Git-Branch: master
Git-Commit: 154f026
Git-Committer-Date: 2015-12-17 20:05:55 +0900
Build-Date: 2015-12-17 20:15:15 +0900
Build-Java-Version: 1.8.0_45
Build-Java-Vendor: Oracle Corporation
Build-Os-Name: Mac OS X
Build-Os-Version: 10.10.1
```

And when Spring Boot Actuator is used with it, git.properties
will be generated:

```console
$ cat build/resources/main/git.properties
git.branch=master
git.commit.id=38a0c0c
git.commit.time=2015-12-16 23\:40\:13 +0900
```

## Configuration

You can configure this plugin with `buildInfo` extension in build.gradle:

```gradle
buildInfo {
    // Date format string used to Git committer date.
    committerDateFormat 'yyyy-MM-dd HH:mm:ss Z'

    // Date format string used to build date.
    buildDateFormat 'yyyy-MM-dd HH:mm:ss Z'

    // Set to true if you want to generate/merge Manifest.MF.
    manifestEnabled true

    // Set to true if you want to generate git.properties.
    // Default is false, but when you use Spring Boot Actuator
    // and you don't set this property explicitly to false,
    // git.properties will be generated.
    gitPropertiesEnabled false

    // Behavior when the plugin cannot read .git directory.
    // Set to MODE_IGNORE if you want to ignore it and proceed task.
    // Set to MODE_ERROR if you want to throw an exception to stop build.
    // Default is MODE_DEFAULT.
    // MODE_DEFAULT will set the values of branch, commit,
    // and committer date to "unknown", then proceed task.
    gitInfoMode com.github.ksoichiro.build.info.BuildInfoExtension.MODE_DEFAULT

    // Set to false if you want to suppress log when .git directory
    // cannot be read.
    // Default is true.
    warnIfGitDirectoryIsMissing false

    // Set to false if you want to exclude some attributes
    // from the manifest file.
    // All properties are true by default.
    attributeGitBranchEnabled false
    attributeGitCommitEnabled false
    attributeGitCommitterDateEnabled false
    attributeBuildDateEnabled false
    attributeBuildJavaVersionEnabled false
    attributeBuildJavaVendorEnabled false
    attributeBuildOsNameEnabled false
    attributeBuildOsVersionEnabled false
}
```

## License

    Copyright 2015 Soichiro Kashima

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
