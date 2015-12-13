You can check your MANIFEST.MF in your JAR:

```
$ ./gradlew build
$ unzip -p build/libs/example.jar META-INF/MANIFEST.MF
Manifest-Version: 1.0
Git-Commit: c57cfad
Git-Committer-Date: 2015-12-13 15:12:35 +0900
Build-Date: 2015-12-13 15:12:47 +0900
```
