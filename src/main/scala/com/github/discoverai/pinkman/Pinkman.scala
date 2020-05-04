package com.github.discoverai.pinkman

import com.typesafe.scalalogging.LazyLogging
import org.apache.spark.SparkConf
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions.rand

object Pinkman extends LazyLogging {
  def main(args: Array[String]): Unit = {
    logger.info("Start loading spark")
    val sparkConf = new SparkConf()
      .set("fs.s3a.access.key", System.getenv("AWS_ACCESS_KEY_ID"))
      .set("fs.s3a.secret.key", System.getenv("AWS_SECRET_ACCESS_KEY"))
      .set("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
    val spark: SparkSession = SparkSession.builder
      .appName("pinkman")
      .master("local[*]")
      .config(sparkConf)
      .getOrCreate()
    logger.info("Done loading spark")

    logger.info("Start loading datasets")
    val baseUri = s"s3a://${System.getenv("S3_BUCKET_NAME")}"
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
      .csv(s"$baseUri/pinkman/train.csv")
    test
      .orderBy(rand())
      .coalesce(1)
      .write
      .mode("overwrite")
      .csv(s"$baseUri/pinkman/test.csv")
    logger.info("Done persisting datasets")
    spark.stop()
  }
}
