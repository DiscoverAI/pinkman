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
val hadoopVersion = "2.9.0"
val sparkVersion = "2.4.5"

dependencies {
    implementation("org.scala-lang:scala-library:$scalaVersion")
    implementation("org.apache.spark:spark-mllib_$scalaCompatVersion:$sparkVersion") {
        exclude(group = "org.apache.hadoop")
    }
    implementation("org.apache.spark:spark-sql_$scalaCompatVersion:$sparkVersion") {
        exclude(group = "org.apache.hadoop")
    }
    implementation("org.apache.hadoop:hadoop-client:$hadoopVersion")
    implementation("org.apache.hadoop:hadoop-aws:$hadoopVersion")
    implementation("com.amazonaws:aws-java-sdk-bundle:1.11.774")
    implementation("org.mlflow:mlflow-client:1.8.0")
    implementation("com.typesafe.scala-logging:scala-logging_$scalaCompatVersion:3.9.2")
    implementation("ch.qos.logback:logback-classic:1.2.3")
    implementation("org.slf4j:log4j-over-slf4j:1.7.30")

    testImplementation("org.scalatest:scalatest_$scalaCompatVersion:3.1.1")
    testImplementation("com.vladsch.flexmark:flexmark-all:0.35.10")
}

configurations {
    implementation {
        exclude(group = "org.slf4j", module = "slf4j-log4j12")
    }
}

application {
    mainClassName = "com.github.discoverai.pinkman.Pinkman"
}
