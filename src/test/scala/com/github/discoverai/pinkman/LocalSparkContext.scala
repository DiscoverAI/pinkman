package com.github.discoverai.pinkman

import org.apache.spark.sql.SparkSession

trait LocalSparkContext {
  val spark: SparkSession = SparkSession.builder
    .appName("Pinkman Test")
    .master("local[2]")
    .getOrCreate()
}
