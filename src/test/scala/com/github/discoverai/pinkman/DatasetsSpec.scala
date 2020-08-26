package com.github.discoverai.pinkman

import org.apache.spark.sql.DataFrame

class DatasetsSpec extends UnitTest with LocalSparkContext {

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

  Feature("tokenize") {
    Scenario("1 character each") {
      val givenDataset = Seq("c", "1", "=").toDF("SMILES")

      val actual = Datasets.tokenize(givenDataset)
      val expected = Seq(
        TokenizedSMILES(Seq("c")),
        TokenizedSMILES(Seq("1")),
        TokenizedSMILES(Seq("=")),
      ).toDS()

      actual.collect() should contain theSameElementsAs expected.collect()
    }

    Scenario("3 characters each") {
      val givenDataset = Seq("caa", "11f", "=b3").toDF("SMILES")

      val actual = Datasets.tokenize(givenDataset)
      val expected = Seq(
        TokenizedSMILES(Seq("c", "a", "a")),
        TokenizedSMILES(Seq("1", "1", "f")),
        TokenizedSMILES(Seq("=", "b", "3")),
      ).toDS()

      actual.collect() should contain theSameElementsAs expected.collect()
    }

    Scenario("multiple characters each") {
      val givenDataset = Seq("c=(a)", "11f", "=").toDF("SMILES")

      val actual = Datasets.tokenize(givenDataset)
      val expected = Seq(
        TokenizedSMILES(Seq("c", "=", "(", "a", ")")),
        TokenizedSMILES(Seq("1", "1", "f")),
        TokenizedSMILES(Seq("=")),
      ).toDS()

      actual.collect() should contain theSameElementsAs expected.collect()
    }
  }

  Feature("normalize features") {
    Scenario("should normalize 3 strings containing each one string") {
      val givenDataset = Seq(
        TokenizedSMILES(Seq("c")),
        TokenizedSMILES(Seq("1")),
        TokenizedSMILES(Seq("=")),
      ).toDS()
      val givenDictionary = spark.sparkContext.broadcast(Map(
        "1" -> 2.0,
        "=" -> 5.0,
        "c" -> 3.0,
      ))
      val smilesLength = spark.sparkContext.broadcast(3)

      val actual = Datasets.normalize(givenDataset, givenDictionary, smilesLength)
      val expected = Seq(
        Seq(3.0, 0.0, 0.0),
        Seq(2.0, 0.0, 0.0),
        Seq(5.0, 0.0, 0.0),
      ).toDF("features")

      actual.collect() should contain theSameElementsAs expected.collect()
    }

    Scenario("should normalize 3 strings containing each 3 strings") {
      val givenDataset = Seq(
        TokenizedSMILES(Seq("c", "1", "=")),
        TokenizedSMILES(Seq("C", "1", "=")),
        TokenizedSMILES(Seq("C", "=", "C")),
      ).toDS()
      val givenDictionary = spark.sparkContext.broadcast(Map(
        "C" -> 1.0,
        "1" -> 2.0,
        "c" -> 3.0,
        "=" -> 4.0,
      ))
      val smilesLength = spark.sparkContext.broadcast(5)

      val actual = Datasets.normalize(givenDataset, givenDictionary, smilesLength)
      val expected = Seq(
        Seq(3.0, 2.0, 4.0, 0.0, 0.0),
        Seq(1.0, 2.0, 4.0, 0.0, 0.0),
        Seq(1.0, 4.0, 1.0, 0.0, 0.0),
      ).toDF("features")

      actual.collect() should contain theSameElementsAs expected.collect()
    }

    Scenario("should normalize 3 strings containing each 3 strings and index missing token with 0.0") {
      val givenDataset = Seq(
        TokenizedSMILES(Seq("c", "1", "=")),
        TokenizedSMILES(Seq("C", "1", "=")),
        TokenizedSMILES(Seq("C", "=", "C")),
      ).toDS()
      val givenDictionary = spark.sparkContext.broadcast(Map(
        "1" -> 2.0,
        "c" -> 3.0,
        "=" -> 4.0,
      ))
      val smilesLength = spark.sparkContext.broadcast(5)

      val actual = Datasets.normalize(givenDataset, givenDictionary, smilesLength)
      val expected = Seq(
        Seq(3.0, 2.0, 4.0, 0.0, 0.0),
        Seq(0.0, 2.0, 4.0, 0.0, 0.0),
        Seq(0.0, 4.0, 0.0, 0.0, 0.0),
      ).toDF("features")

      actual.collect() should contain theSameElementsAs expected.collect()
    }
  }

  Feature("stringify features") {
    Scenario("one feature available") {
      val givenDataset = Seq(
        Seq(3.0),
        Seq(1.0),
        Seq(1.0),
      ).toDF("features")

      val actual: DataFrame = Datasets.stringifyVectors(givenDataset)
      val expected = Seq(
        "3.0",
        "1.0",
        "1.0",
      ).toDF("features")

      actual.collect() should contain theSameElementsAs expected.collect()
    }

    Scenario("multiple features available") {
      val givenDataset = Seq(
        Seq(3.0, 2.0, 4.0),
        Seq(1.0, 2.0, 4.0),
        Seq(1.0, 4.0, 1.0),
      ).toDF("features")

      val actual: DataFrame = Datasets.stringifyVectors(givenDataset)
      val expected = Seq(
        "3.0,2.0,4.0",
        "1.0,2.0,4.0",
        "1.0,4.0,1.0",
      ).toDF("features")

      actual.collect() should contain theSameElementsAs expected.collect()
    }
  }

  Feature("get maximal sequence length") {
    Scenario("maximal length of 4") {
      val givenDataset = Seq(
        TokenizedSMILES(Seq("3.0", "2.0", "4.0")),
        TokenizedSMILES(Seq("1.0", "2.0", "4.0")),
        TokenizedSMILES(Seq("1.0", "4.0", "1.0", "5.0")),
      ).toDS()

      val actual = Datasets.maxSmilesLength(givenDataset)
      val expected = 4

      actual shouldBe expected
    }

    Scenario("maximal length of 3 when all of them are 3") {
      val givenDataset = Seq(
        TokenizedSMILES(Seq("3.0", "2.0", "4.0")),
        TokenizedSMILES(Seq("1.0", "2.0", "4.0")),
        TokenizedSMILES(Seq("1.0", "4.0", "1.0")),
      ).toDS()

      val actual = Datasets.maxSmilesLength(givenDataset)
      val expected = 3

      actual shouldBe expected
    }
  }

  Feature("normalise features to configured length") {
    val smilesLength = spark.sparkContext.broadcast(10)

    Scenario("feature with length less than configured length") {
      val givenFeature = Seq(3.0, 2.0, 4.0)

      val actual = Datasets.padSequence(smilesLength)(givenFeature)
      val expected = Seq(3.0, 2.0, 4.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0)

      actual shouldBe expected
    }

    Scenario("feature with length equal to configured length") {
      val givenFeature = Seq(3.0, 2.0, 4.0, 5.0, 6.0, 8.0, 1.0, 2.0, 4.0, 5.0)

      val actual = Datasets.padSequence(smilesLength)(givenFeature)
      val expected = Seq(3.0, 2.0, 4.0, 5.0, 6.0, 8.0, 1.0, 2.0, 4.0, 5.0)

      actual shouldBe expected
    }
  }
}
