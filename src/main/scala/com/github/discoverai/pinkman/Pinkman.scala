package com.github.discoverai.pinkman

import com.typesafe.scalalogging.LazyLogging
import org.apache.spark.SparkConf
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions.rand

object Pinkman extends LazyLogging {
  def main(args: Array[String]): Unit = {
    logger.info("Start loading spark")
    val sparkConf = new SparkConf()
      .set("fs.azure", "org.apache.hadoop.fs.azure.NativeAzureFileSystem")
      .set(
        s"fs.azure.account.key.${System.getenv("AZURE_STORAGE_ACCOUNT_NAME")}.blob.core.windows.net",
        System.getenv("AZURE_STORAGE_KEY")
      )
      .set("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
    val spark: SparkSession = SparkSession.builder
      .appName("pinkman")
      .master("local[*]")
      .config(sparkConf)
      .getOrCreate()
    logger.info("Done loading spark")

    logger.info("Start loading datasets")
    val baseUri = s"wasbs://${System.getenv("AZURE_STORAGE_CONTAINER_NAME")}@${System.getenv("AZURE_STORAGE_ACCOUNT_NAME")}.blob.core.windows.net"
    val (train, test) = Datasets.load(spark, s"$baseUri/moses.csv")
    logger.info(s"Train dataset size: ${train.count()}")
    logger.info(s"Test dataset size: ${test.count()}")
    logger.info("Done loading datasets")

    logger.info("Start persisting datasets")
    train
      .orderBy(rand())
      .coalesce(1)
      .write
      .mode("overwrite")
      .csv("./src/main/resources/train.csv")
    test
      .orderBy(rand())
      .coalesce(1)
      .write
      .mode("overwrite")
      .csv("./src/main/resources/test.csv")
    logger.info("Done persisting datasets")
    spark.stop()
  }
}
