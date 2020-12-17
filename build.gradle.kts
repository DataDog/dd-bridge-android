/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

buildscript {
    repositories {
        google()
        mavenCentral()
        maven { setUrl(com.datadog.gradle.Dependencies.Repositories.Gradle) }
        jcenter()
    }

    dependencies {
        classpath(com.datadog.gradle.Dependencies.ClassPaths.AndroidTools)
        classpath(com.datadog.gradle.Dependencies.ClassPaths.Kotlin)
        classpath(com.datadog.gradle.Dependencies.ClassPaths.KtLint)
        classpath(com.datadog.gradle.Dependencies.ClassPaths.Dokka)
        classpath(com.datadog.gradle.Dependencies.ClassPaths.Bintray)
        classpath(com.datadog.gradle.Dependencies.ClassPaths.Unmock)
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
        maven { setUrl(com.datadog.gradle.Dependencies.Repositories.Jitpack) }
        jcenter()
        flatDir { dirs("libs") }
    }
}

task<Delete>("clean") {
    delete(rootProject.buildDir)
}

tasks.register("checkAll") {
    dependsOn(
        "ktlintCheckAll",
        "detektAll",
        "lintCheckAll",
        "unitTestAll",
        "jacocoReportAll"
    )
}

tasks.register("assembleAll") {
    dependsOn(
        ":dd-bridge-android:assemble"
    )
}

tasks.register("unitTestRelease") {
    dependsOn(
        ":dd-bridge-android:testReleaseUnitTest"
    )
}

tasks.register("unitTestDebug") {
    dependsOn(
        ":dd-bridge-android:testDebugUnitTest",
        ":dd-bridge-android:jacocoTestDebugUnitTestReport"
    )
}

tasks.register("unitTestAll") {
    dependsOn(
        ":unitTestDebug",
        ":unitTestRelease"
    )
}

tasks.register("ktlintCheckAll") {
    dependsOn(
        ":dd-bridge-android:ktlintCheck"
    )
}

tasks.register("lintCheckAll") {
    dependsOn(
        ":dd-bridge-android:lintRelease"
    )
}

tasks.register("detektAll") {
    dependsOn(
        ":dd-bridge-android:detekt"
    )
}

tasks.register("jacocoReportAll") {
    dependsOn(
        ":dd-bridge-android:jacocoTestDebugUnitTestReport"
    )
}
