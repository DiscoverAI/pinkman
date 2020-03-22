package com.github.discoverai.pinkman

import org.apache.spark.sql.{DataFrame, SparkSession}

object Datasets {
  def load(spark: SparkSession, datasetFilePath: String): (DataFrame, DataFrame) = {
    val dataset = spark
      .read
      .options(Map("header" -> "true"))
      .csv(datasetFilePath)
    val trainDataset = dataset
      .select(dataset.col("SMILES"))
      .where(dataset.col("SPLIT").===("train"))
    val testDataset = dataset
      .select(dataset.col("SMILES"))
      .where(dataset.col("SPLIT").===("test"))
    (trainDataset, testDataset)
  }
}
