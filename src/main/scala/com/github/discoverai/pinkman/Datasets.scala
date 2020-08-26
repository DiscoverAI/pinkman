package com.github.discoverai.pinkman

import com.github.discoverai.pinkman.Pinkman.DictionaryDataset
import org.apache.spark.broadcast.Broadcast
import org.apache.spark.sql._
import org.apache.spark.sql.functions.{col, explode, lit, udf, size, max}

object Datasets {
  private val smilesColumnName = "SMILES"
  private val tokenColumnName = "tokenizedSMILES"
  private val molecularInputColumnName = "molecularInput"

  def dictionary(spark: SparkSession, tokenizedDataset: Dataset[TokenizedSMILES]): DictionaryDataset = {
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

  def index(dictionary: Broadcast[Map[String, Double]])(tokenizedSmiles: Seq[String]): Seq[Double] = {
    tokenizedSmiles.map {
      tokenizedSMILE =>
        dictionary.value.getOrElse(tokenizedSMILE, 0.0)
    }
  }

  def normalize(tokenizedDataset: Dataset[TokenizedSMILES], dictionary: Broadcast[Map[String, Double]], maxSmilesLength: Broadcast[Int]): DataFrame = {
    val indexed = udf[Seq[Double], Seq[String]](index(dictionary)(_))
    val paddedSequence = udf(padSequence(maxSmilesLength)(_))

    val indexedPaddedDataset = tokenizedDataset
      .withColumn("features", paddedSequence(indexed(col(tokenColumnName))))
      .select("features")

    indexedPaddedDataset
  }

  def stringifyVectors(normalizedDataset: DataFrame): DataFrame = {
    val stringify = udf((row: Seq[Double]) => row.mkString(","))
    normalizedDataset
      .withColumn("stringifiedFeatures", stringify(col("features")))
      .drop("features")
      .withColumnRenamed("stringifiedFeatures", "features")
  }

  def maxSmilesLength(normalizedDataset: Dataset[TokenizedSMILES]): Int = {
    normalizedDataset.agg(max(size(col("tokenizedSMILES")))).head().getInt(0)
  }

  def padSequence(maxSmilesLength: Broadcast[Int])(feature: Seq[Double]): Seq[Double] = {
    Seq.fill(maxSmilesLength.value)(0.0).zipAll(feature, 0.0, 0.0).map { case (x,y) => x + y }
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
