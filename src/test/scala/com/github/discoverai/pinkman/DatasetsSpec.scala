package com.github.discoverai.pinkman

import org.apache.spark.sql.SparkSession
import org.scalatest.featurespec.AnyFeatureSpec
import org.scalatest.matchers.should.Matchers

class DatasetsSpec extends AnyFeatureSpec with Matchers {
  val spark: SparkSession = SparkSession.builder
    .appName("Pinkman Test")
    .master("local[2]")
    .getOrCreate()

  import spark.implicits._

  Feature("load dataset") {
    Scenario("should return 3 test and 3 train datasets from file") {
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

  Feature("create dictionary") {
    Scenario("3 characters that each appear once") {
      val givenDataset = Seq(
        TokenizedSMILES(Seq("c")),
        TokenizedSMILES(Seq("1")),
        TokenizedSMILES(Seq("=")),
      ).toDS()

      val actual = Datasets.dictionary(spark, givenDataset)
      val expected = Seq(
        DictionaryEntry("c", 1, 2.0),
        DictionaryEntry("1", 1, 3.0),
        DictionaryEntry("=", 1, 1.0),
      ).toDS()

      actual.collect() should contain theSameElementsAs expected.collect()
    }

    Scenario("3 characters witch increasing frequency") {
      val givenDataset = Seq(
        TokenizedSMILES(Seq("c", "=")),
        TokenizedSMILES(Seq("1", "c", "=")),
        TokenizedSMILES(Seq("=", "=", "=")),
      ).toDS()

      val actual = Datasets.dictionary(spark, givenDataset)
      val expected = Seq(
        DictionaryEntry("1", 1, 1.0),
        DictionaryEntry("c", 2, 2.0),
        DictionaryEntry("=", 5, 3.0),
      ).toDS()

      actual.collect() should contain theSameElementsAs expected.collect()
    }
  }

  //  Feature("normalize features") {
  //    Scenario("should normalize 3 strings containing each one string") {
  //      val givenDataset = Seq("c", "1", "=").toDF("SMILES")
  //      val givenDictionary = Seq(
  //        DictionaryEntry("1", 3, 2.0),
  //        DictionaryEntry("=", 5, 5.0),
  //        DictionaryEntry("c", 4, 3.0),
  //      ).toDS()
  //
  //      val actual: DataFrame = Datasets.normalize(spark, givenDataset, givenDictionary)
  //      val expected: DataFrame = Seq(
  //        Seq(3.0),
  //        Seq(2.0),
  //        Seq(5.0),
  //      ).toDF("features")
  //
  //      actual.collect() should contain theSameElementsAs expected.collect()
  //    }
  //
  //    //    Scenario("should tokenize 3 strings containing each multiple strings") {
  //    //      val givenDataset = Seq("c1=", "C1=", "C=CC").toDF("SMILES")
  //    //
  //    //      val actualNormalized: DataFrame = Datasets.normalize(spark, givenDataset)
  //    //      val actual = actualNormalized.select(actualNormalized.col("SMILESTokenized"))
  //    //      val expected: DataFrame = Seq(
  //    //        Seq("c", "1", "="),
  //    //        Seq("C", "1", "="),
  //    //        Seq("C", "=", "C", "C"),
  //    //      ).toDF("SMILESTokenized")
  //    //
  //    //      actual.collect() should contain theSameElementsAs expected.collect()
  //    //    }
  //  }
}
