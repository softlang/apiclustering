package org.softlang.dscor.modules

import org.softlang.dscor._
import org.softlang.dscor.utils.Utils
import org.apache.spark.rdd.RDD
import org.softlang.dscor.macros.Value
import org.softlang.dscor.utils.Utils.{weightIdf, weightLtfIdf}

class VSM[T](
              @Dependency val source: Documents[T],
              @Property val ltf: Boolean = true) extends Module with Documents[T] {

  def data(): RDD[(T, Map[String, Double])] = {
    val _ltf = ltf

    val invertedDocumentFrequency = Utils.idf(source.data().map(_._2).collect())

    source.data().map { case (k, v) =>
      k -> (if (_ltf) weightLtfIdf(v, invertedDocumentFrequency) else weightIdf(v, invertedDocumentFrequency))
    }
  }
}