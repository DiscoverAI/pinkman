package com.github.discoverai.pinkman

import org.apache.spark.ml.Pipeline
import org.apache.spark.sql.{DataFrame, SparkSession}

object Datasets {
  def normalize(spark: SparkSession, dataset: DataFrame): DataFrame = {
    val tokenizer = new WordSplitter()
      .setInputCol("SMILES")
      .setOutputCol("SMILESTokenized")
    val pipeline = new Pipeline()
      .setStages(
        Array(
          tokenizer,
        )
      )
    pipeline.fit(dataset).transform(dataset)
  }

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
