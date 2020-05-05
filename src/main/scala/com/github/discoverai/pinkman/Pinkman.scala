package com.github.discoverai.pinkman

import com.typesafe.scalalogging.LazyLogging
import org.apache.spark.SparkConf
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions.rand
import org.mlflow.tracking.MlflowContext

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
    val mlflowContext = new MlflowContext("file:/./.mlflow")
    val client = mlflowContext.getClient
    val experimentOpt = client.getExperimentByName("pinkman");
    if (!experimentOpt.isPresent) {
      client.createExperiment("pinkman")
    }
    mlflowContext.setExperimentName("pinkman")
    val run = mlflowContext.startRun("run")
    logger.info("Done loading spark")

    logger.info("Start loading datasets")
    val baseUri = s"s3a://${System.getenv("S3_BUCKET_NAME")}"
    val inputFile = s"$baseUri/moses.csv"
    run.logParam("input", inputFile)
    val (train, test) = Datasets.load(spark, inputFile)
    val trainSize = train.count()
    val testSize = test.count()
    logger.info(s"Train dataset size: ${trainSize}")
    logger.info(s"Test dataset size: ${testSize}")
    run.logParam("trainSize", trainSize.toString)
    run.logParam("testSize", testSize.toString)
    logger.info("Done loading datasets")

    logger.info("Start persisting datasets")
    val outputPath = s"$baseUri/pinkman"
    run.logParam("output", outputPath)
    train
      .orderBy(rand())
      .coalesce(1)
      .write
      .mode("overwrite")
      .csv(s"$outputPath/train.csv")
    test
      .orderBy(rand())
      .coalesce(1)
      .write
      .mode("overwrite")
      .csv(s"$outputPath/test.csv")
    logger.info("Done persisting datasets")
    run.endRun()
    spark.stop()
  }
}
