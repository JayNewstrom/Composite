Local Development
=================

Test Local Changes
------------------
1. `./gradlew test run`

Before submitting a PR
----------------------
1. `./gradlew test run ktlintCheck checkstyle`

Publish to mavenLocal
---------------------

1. Update `ARTIFACT_VERSION` in `gradle.properties`
4. `./gradlew install`

Releasing to bintray
--------------------

1. `git checkout master && git pull`
2. Remove `SNAPSHOT` from `ARTIFACT_VERSION` in `gradle.properties`
3. Update versions in `README.md`
4. `./gradlew clean bintrayUpload`
5. Publish the artifacts from the bintray website
6. `git add . && git commit -m "Release version x.y.z"`
7. `git tag -a release-x.y.z -m "Release version x.y.z"`
8. Increment the version, and add `SNAPSHOT` to `ARTIFACT_VERSION` in `gradle.properties`
9. `git add . && git commit -m "Prepare for next development iteration"`
10. `git push && git push --tags`
