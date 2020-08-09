package com.github.discoverai.pinkman

import org.apache.spark.ml.UnaryTransformer
import org.apache.spark.ml.param.ParamMap
import org.apache.spark.ml.util.{DefaultParamsReadable, DefaultParamsWritable, Identifiable}
import org.apache.spark.sql.types.{ArrayType, DataType, StringType}

class WordSplitter(override val uid: String)
  extends UnaryTransformer[String, Seq[String], WordSplitter]
    with DefaultParamsWritable
    with DefaultParamsReadable[WordSplitter] {
  def this() = this(Identifiable.randomUID("wordSplitter"))

  override protected def createTransformFunc: String => Seq[String] = { originStr =>
    originStr.toSeq.map(_.toString)
  }

  override protected def validateInputType(inputType: DataType): Unit = {
    require(inputType == StringType, s"Input type must be string type but got $inputType.")
  }

  override protected def outputDataType: DataType = new ArrayType(StringType, true)

  override def copy(extra: ParamMap): WordSplitter = defaultCopy(extra)
}
