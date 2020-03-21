plugins {
    scala
    application
    id("com.github.maiflai.scalatest") version "0.26"
}

repositories {
    jcenter()
}

val scalaCompatVersion = "2.12"
val scalaVersion = "$scalaCompatVersion.10"

dependencies {
    implementation("org.scala-lang:scala-library:$scalaVersion")
    implementation("org.apache.spark:spark-sql_$scalaCompatVersion:2.4.5")

    testImplementation("org.scalatest:scalatest_$scalaCompatVersion:3.1.1")
    testImplementation("com.vladsch.flexmark:flexmark-all:0.35.10")
}



application {
    mainClassName = "com.github.discoverai.pinkman.Pinkman"
}
