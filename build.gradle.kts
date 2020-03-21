plugins {
    scala
    application
    id("com.github.maiflai.scalatest") version "0.26"
}

repositories {
    jcenter()
}

val scalaCompatVersion = "2.13"
val scalaVersion = "$scalaCompatVersion.1"

dependencies {
    implementation("org.scala-lang:scala-library:$scalaVersion")

    testImplementation("org.scalatest:scalatest_$scalaCompatVersion:3.1.1")
    testImplementation("com.vladsch.flexmark:flexmark-all:0.35.10")
}



application {
    mainClassName = "com.github.discoverai.pinkman.Pinkman"
}
