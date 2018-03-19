package org.softlang.dscor.modules

import org.apache.spark.rdd.RDD
import org.softlang.dscor.SingleModule
import org.softlang.dscor.RDDModule
import org.softlang.dscor.utils.{Utils, ViewUtils}

trait Documents [T] {
  def data(): RDD[(T, Map[String, Double])]

  def generateHtmlTermlists(n: Int = 20) = ViewUtils.generateHtmlTermlists(data.collect(), n)

}