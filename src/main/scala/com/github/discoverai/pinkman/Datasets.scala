package com.github.discoverai.pinkman

import org.apache.spark.sql._
import org.apache.spark.sql.functions.{col, explode, lit, udf}

object Datasets {
  private val smilesColumnName = "SMILES"
  private val tokenColumnName = "tokenizedSMILES"
  private val molecularInputColumnName = "molecularInput"

  def dictionary(spark: SparkSession, tokenizedDataset: Dataset[TokenizedSMILES]): Dataset[DictionaryEntry] = {
    val explodedDataset = tokenizedDataset
      .withColumn(molecularInputColumnName, explode(tokenizedDataset(tokenColumnName)))
      .withColumn("count", lit(1))
    val groupedByCount: Dataset[Row] = explodedDataset
      .groupBy(explodedDataset.col(molecularInputColumnName))
      .count()
      .orderBy("count")

    implicit val dictionaryEntryEncoder: Encoder[DictionaryEntry] = Encoders.product
    spark.sqlContext.createDataset(
      groupedByCount.rdd.zipWithIndex.map {
        case (row, index) => DictionaryEntry(row.getString(0), row.getLong(1), index + 1)
      })
  }

  def tokenize(dataset: DataFrame): Dataset[TokenizedSMILES] = {
    val tokenizer = new WordSplitter()
      .setInputCol(smilesColumnName)
      .setOutputCol(tokenColumnName)
    implicit val tokenizedSMILESEncoder: Encoder[TokenizedSMILES] = Encoders.product
    tokenizer.transform(dataset).select(tokenColumnName).as[TokenizedSMILES]
  }

  def index(dictionary: Dataset[DictionaryEntry])(tokenizedSmiles: Seq[String]): Seq[Double] = {
    val frames: Seq[Double] = tokenizedSmiles.map {
      tokenizedSMILE =>
        dictionary
          .where(col("molecularInput") === tokenizedSMILE)
          .select("index").collect().head.getDouble(0)
    }
    frames
  }

  def normalize(tokenizedDataset: Dataset[TokenizedSMILES], dictionary: Dataset[DictionaryEntry]): DataFrame = {
    val indexed = udf(index(dictionary)(_))
    val indexedDataset = tokenizedDataset
      .withColumn("features", indexed(col(tokenColumnName)))
      .select("features")

    indexedDataset
  }

  def load(spark: SparkSession, datasetFilePath: String): (DataFrame, DataFrame) = {
    val dataset = spark
      .read
      .options(Map("header" -> "true"))
      .csv(datasetFilePath)
    val trainDataset = dataset
      .select(dataset.col(smilesColumnName))
      .where(dataset.col("SPLIT").===("train"))
    val testDataset = dataset
      .select(dataset.col(smilesColumnName))
      .where(dataset.col("SPLIT").===("test"))
    (trainDataset, testDataset)
  }
}
