package com.github.discoverai.pinkman

import org.apache.spark.sql._
import org.apache.spark.sql.functions.{explode, lit}

object Datasets {
  def dictionary(spark: SparkSession, tokenizedDataset: Dataset[TokenizedSMILES]): Dataset[DictionaryEntry] = {
    val explodedDataset = tokenizedDataset
      .withColumn("molecularInput", explode(tokenizedDataset("tokenizedSMILES")))
      .withColumn("count", lit(1))
    val groupedByCount: Dataset[Row] = explodedDataset
      .groupBy(explodedDataset.col("molecularInput"))
      .count()
      .orderBy("count")

    implicit val dictionaryEntryEncoder: Encoder[DictionaryEntry] = Encoders.product
    spark.sqlContext.createDataset(
      groupedByCount.rdd.zipWithIndex.map {
        case (row, index) => DictionaryEntry(row.getString(0), row.getLong(1), index + 1)
      })
  }

  //  def index(dictionary: Dataset[Dictionary])(tokenizedSmiles: Seq[String]): Seq[Double] = {
  //    val frames: Seq[Double] = tokenizedSmiles.map {
  //      molecularInput =>
  //        dictionary
  //          .where(col("molecularInput") === molecularInput)
  //          .select("index").as[Double]
  //    }
  //    frames
  //  }

  //  def normalize(spark: SparkSession, dataset: DataFrame, dictionary: Dataset[DictionaryEntry]): DataFrame = {
  //    val tokenizer = new WordSplitter()
  //      .setInputCol("SMILES")
  //      .setOutputCol("tokenizedSMILES")
  //    implicit val tokenizedSMILESEncoder: Encoder[TokenizedSMILES] = Encoders.product
  //    val tokenizedDataset = tokenizer.transform(dataset).select("tokenizedSMILES").as[TokenizedSMILES]
  //    tokenizedDataset.show()
  //
  //    //    val indexed = udf(index(dictionary)(_))
  //    //    val indexedDataset = tokenizedDataset
  //    //      .withColumn("features", indexed(col("SMILESTokenized")))
  //    //      .select("features")
  //
  //    tokenizedDataset.toDF()
  //  }

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
