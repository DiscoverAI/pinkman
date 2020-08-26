package com.github.discoverai.pinkman

import com.typesafe.scalalogging.LazyLogging
import org.apache.spark.SparkConf
import org.apache.spark.broadcast.Broadcast
import org.apache.spark.sql.functions.rand
import org.apache.spark.sql.{DataFrame, Dataset, SparkSession}
import org.mlflow.api.proto.Service.RunStatus
import org.mlflow.tracking.{ActiveRun, MlflowContext}

object Pinkman extends LazyLogging {
  val mlFlowExperimentName = "sars-cov-2"
  val mlFlowRunName = "pinkman"
  val maxSmilesLength = 57

  type BroadcastedDictionary = Broadcast[Map[String, Double]]
  type DictionaryDataset = Dataset[DictionaryEntry]

  def loadDatasets(spark: SparkSession, run: ActiveRun, baseUri: String): (DataFrame, DataFrame) = {
    val inputFile = s"$baseUri/moses.csv"
    run.logParam("input", inputFile)
    val (train, test) = Datasets.load(spark, inputFile)
    val trainSize = train.count()
    val testSize = test.count()
    logger.info(s"Train dataset size: ${trainSize}")
    logger.info(s"Test dataset size: ${testSize}")
    run.logParam("trainSize", trainSize.toString)
    run.logParam("testSize", testSize.toString)
    (train, test)
  }

  def createDictionary(spark: SparkSession, run: ActiveRun, tokenizedSMILES: Dataset[TokenizedSMILES]): (BroadcastedDictionary, DictionaryDataset) = {
    val dictionary = Datasets.dictionary(spark, tokenizedSMILES)
    val dictionarySize = dictionary.count()
    logger.info(s"Dictionary size: $dictionarySize unique characters")
    run.logParam("dictionarySize", dictionarySize.toString)
    (broadcastDictionary(spark, dictionary), dictionary)
  }

  def broadcastDictionary(spark: SparkSession, dictionary: DictionaryDataset): BroadcastedDictionary = {
    val entries: Array[DictionaryEntry] = dictionary.collect()
    val map: Map[String, Double] = entries.map(de => de.molecularInput -> de.index).toMap
    spark.sparkContext.broadcast(map)
  }

  def persistDataset(dataset: DataFrame, outputPath: String): Unit = {
    dataset
      .orderBy(rand())
      .write
      .mode("overwrite")
      .option("quote", "\u0020")
      .csv(outputPath)
  }

  def persistDatasetsAndDictionary(
                                    run: ActiveRun,
                                    baseUri: String,
                                    normalizedTrainDataset: DataFrame,
                                    normalizedTestDataset: DataFrame,
                                    dictionary: DictionaryDataset
                                  ): Unit = {
    val outputPath = s"$baseUri/pinkman"
    run.logParam("output", outputPath)
    dictionary.coalesce(1).write.mode("overwrite").csv(s"$outputPath/dictionary.csv")
    val trainDatasetStringified = Datasets.stringifyVectors(normalizedTrainDataset)
    val testDatasetStringified = Datasets.stringifyVectors(normalizedTestDataset)
    persistDataset(trainDatasetStringified, s"$outputPath/train.csv")
    persistDataset(testDatasetStringified, s"$outputPath/test.csv")
  }

  def main(args: Array[String]): Unit = {
    val baseUri = s"s3a://${args(0)}"
    logger.info("Start loading spark")
    val sparkConf = new SparkConf()
//      .set("fs.s3a.access.key", System.getenv("AWS_ACCESS_KEY_ID"))
//      .set("fs.s3a.secret.key", System.getenv("AWS_SECRET_ACCESS_KEY"))
      .set("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
    val spark: SparkSession = SparkSession.builder
      .appName("pinkman")
      .config(sparkConf)
      .getOrCreate()
    val mlflowContext = new MlflowContext(args(1))
    val client = mlflowContext.getClient
    val experimentOpt = client.getExperimentByName(mlFlowExperimentName);
    if (!experimentOpt.isPresent) {
      client.createExperiment(mlFlowExperimentName)
    }
    mlflowContext.setExperimentName(mlFlowExperimentName)
    val run: ActiveRun = mlflowContext.startRun(mlFlowRunName)
    logger.info("Done loading spark and mlflow")

    try {
      logger.info("Start loading datasets")
      val (train, test) = loadDatasets(spark, run, baseUri)
      logger.info("Done loading datasets")

      logger.info("Start tokenizing dataset")
      val tokenizedSMILESTrain = Datasets.tokenize(train)
      val tokenizedSMILESTest = Datasets.tokenize(test)
      val tokenizedSMILES = tokenizedSMILESTest.union(tokenizedSMILESTrain)
      logger.info("Done tokenizing dataset")

      logger.info("Start creating dictionary")
      val (broadcastedDictionary, dictionary) = createDictionary(spark, run, tokenizedSMILES)
      logger.info("Done creating dictionary")

      logger.info("Start normalizing dataset")
      val normalizedTrain = Datasets.normalize(tokenizedSMILESTrain, broadcastedDictionary, maxSmilesLength)
      val normalizedTest = Datasets.normalize(tokenizedSMILESTest, broadcastedDictionary, maxSmilesLength)
      logger.info("Done normalizing dataset")

      logger.info("Start persisting datasets and dictionary")
      persistDatasetsAndDictionary(run, baseUri, normalizedTrain, normalizedTest, dictionary)
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
