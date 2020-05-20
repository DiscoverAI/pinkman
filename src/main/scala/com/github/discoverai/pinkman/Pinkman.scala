package com.github.discoverai.pinkman

import com.typesafe.scalalogging.LazyLogging
import org.apache.spark.SparkConf
import org.apache.spark.broadcast.Broadcast
import org.apache.spark.sql.functions.rand
import org.apache.spark.sql.{Dataset, SparkSession}
import org.mlflow.api.proto.Service.RunStatus
import org.mlflow.tracking.{ActiveRun, MlflowContext}

object Pinkman extends LazyLogging {
  def broadcastDictionary(spark: SparkSession, dictionary: Dataset[DictionaryEntry]): Broadcast[Map[String, Double]] = {
    val entries: Array[DictionaryEntry] = dictionary.collect()
    val map: Map[String, Double] = entries.map(de => de.molecularInput -> de.index).toMap
    spark.sparkContext.broadcast(map)
  }

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
    val mlflowContext = new MlflowContext(System.getenv("MLFLOW_TRACKING_URI"))
    val client = mlflowContext.getClient
    val experimentOpt = client.getExperimentByName("pinkman");
    if (!experimentOpt.isPresent) {
      client.createExperiment("pinkman")
    }
    mlflowContext.setExperimentName("pinkman")
    val run: ActiveRun = mlflowContext.startRun("run")
    logger.info("Done loading spark and mlflow")

    try {
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

      logger.info("Start normalizing dataset")
      val tokenizedSMILESTrain = Datasets.tokenize(train)
      val tokenizedSMILESTest = Datasets.tokenize(test)
      val tokenizedSMILES = tokenizedSMILESTest.union(tokenizedSMILESTrain)
      val dictionary = Datasets.dictionary(spark, tokenizedSMILES)
      val broadcastedDictionary = broadcastDictionary(spark, dictionary)
      val normalizedTrain = Datasets.normalize(tokenizedSMILESTrain, broadcastedDictionary)
      val normalizedTest = Datasets.normalize(tokenizedSMILESTest, broadcastedDictionary)
      logger.info("Done normalizing dataset")

      logger.info("Start persisting datasets and dictionary")
      val outputPath = s"$baseUri/pinkman"
      run.logParam("output", outputPath)
      normalizedTrain.show()
      normalizedTrain
        .orderBy(rand())
        .coalesce(1)
        .write
        .mode("overwrite")
        .csv(s"$outputPath/train.csv")
      normalizedTest
        .orderBy(rand())
        .coalesce(1)
        .write
        .mode("overwrite")
        .csv(s"$outputPath/test.csv")

      logger.info("Done persisting datasets")
      run.endRun()
      spark.stop()
    } catch {
      case exception: Exception =>
        run.endRun(RunStatus.FAILED)
        spark.stop()
        logger.error("Could not tokenize, normalize and persist data", exception)
        sys.exit(1)
    }
  }
}
