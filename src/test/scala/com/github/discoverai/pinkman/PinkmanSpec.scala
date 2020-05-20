package com.github.discoverai.pinkman

import org.apache.spark.broadcast.Broadcast
import org.apache.spark.sql.Dataset

class PinkmanSpec extends UnitTest with LocalSparkContext {

  import spark.implicits._

  Feature("broadcasted dictionary") {
    Scenario("dictionary with one entry") {
      val givenDataset: Dataset[DictionaryEntry] = Seq(
        DictionaryEntry("f", 42, 1.0),
      ).toDS()

      val actual: Broadcast[Map[String, Double]] = Pinkman.broadcastDictionary(spark, givenDataset)
      val expected = spark.sparkContext.broadcast(Map("f" -> 1.0))

      actual.value shouldBe expected.value
    }

    Scenario("any dictionary dataset") {
      val givenDataset: Dataset[DictionaryEntry] = Seq(
        DictionaryEntry("f", 42, 1.0),
        DictionaryEntry("o", 1337, 2.0),
      ).toDS()

      val actual: Broadcast[Map[String, Double]] = Pinkman.broadcastDictionary(spark, givenDataset)
      val expected = spark.sparkContext.broadcast(Map("f" -> 1.0, "o" -> 2.0))

      actual.value shouldBe expected.value
    }
  }
}
