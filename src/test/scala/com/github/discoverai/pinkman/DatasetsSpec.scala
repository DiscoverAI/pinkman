package com.github.discoverai.pinkman

import org.apache.spark.sql.{DataFrame, SparkSession}
import org.scalatest.featurespec.AnyFeatureSpec
import org.scalatest.matchers.should.Matchers

class DatasetsSpec extends AnyFeatureSpec with Matchers {
  val spark: SparkSession = SparkSession.builder
    .appName("Pinkman Test")
    .master("local[2]")
    .getOrCreate()

  import spark.implicits._

  Feature("load dataset") {
    Scenario("should 3 test and 3 train datasets from file") {
      val (actualTrain, actualTest) = Datasets.load(spark, "src/test/resources/dataset.csv")
      val expectedTrain = Seq(
        "CCCS(=O)c1ccc2[nH]c(=NC(=O)OC)[nH]c2c1",
        "CC(C)(C)C(=O)C(Oc1ccc(Cl)cc1)n1ccnc1",
        "Cc1c(Cl)cccc1Nc1ncccc1C(=O)OCC(O)CO",
      ).toDS().collect()
      val expectedTest = Seq(
        "CC1C2CCC(C2)C1CN(CCO)C(=O)c1ccc(Cl)cc1",
        "Cn1cnc2c1c(=O)n(CC(O)CO)c(=O)n2C",
        "CC1Oc2ccc(Cl)cc2N(CC(O)CO)C1=O",
      ).toDS().collect()

      actualTrain.collect().map(_.get(0)) should contain theSameElementsAs expectedTrain
      actualTest.collect().map(_.get(0)) should contain theSameElementsAs expectedTest
    }
  }

  Feature("normalize features") {
    Scenario("should tokenize 3 strings containing each one string") {
      val givenDataset = Seq("c", "1", "=").toDF("SMILES")

      val actualNormalized: DataFrame = Datasets.normalize(spark, givenDataset)
      val actual = actualNormalized.select(actualNormalized.col("SMILESTokenized"))
      val expected: DataFrame = Seq(
        Seq("c"),
        Seq("1"),
        Seq("="),
      ).toDF("SMILESTokenized")

      actual.collect() should contain theSameElementsAs expected.collect()
    }

    Scenario("should tokenize 3 strings containing each multiple strings") {
      val givenDataset = Seq("c1=", "C1=", "C=CC").toDF("SMILES")

      val actualNormalized: DataFrame = Datasets.normalize(spark, givenDataset)
      val actual = actualNormalized.select(actualNormalized.col("SMILESTokenized"))
      val expected: DataFrame = Seq(
        Seq("c", "1", "="),
        Seq("C", "1", "="),
        Seq("C", "=", "C", "C"),
      ).toDF("SMILESTokenized")

      actual.collect() should contain theSameElementsAs expected.collect()
    }
  }
}
