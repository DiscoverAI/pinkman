package com.github.discoverai.pinkman

import org.apache.spark.sql.types.{StringType, StructField, StructType}
import org.apache.spark.sql.{DataFrame, SparkSession}

object Datasets {
  def load(spark: SparkSession, datasetFilePath: String): (DataFrame, DataFrame) = {
    val customSchema = StructType(Array(
      StructField("SMILES", StringType, true),
      StructField("SPLIT", StringType, true),
    ))
    val dataset = spark
      .read
      .options(Map("inferSchema" -> "false", "header" -> "true"))
      .schema(customSchema)
      .csv(datasetFilePath)
    val trainDataset = dataset
      .select(dataset.col("SMILES"))
      .where(dataset.col("SPLIT").===("train"))
    val testDataset = dataset
      .select(dataset.col("SMILES"))
      .where(dataset.col("SPLIT").===("test"))
    trainDataset.show()
    (trainDataset, testDataset)
  }
}
