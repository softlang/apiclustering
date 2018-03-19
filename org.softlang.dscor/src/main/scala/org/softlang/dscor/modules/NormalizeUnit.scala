package org.softlang.dscor.modules

import org.apache.spark.rdd.RDD
import org.softlang.dscor.{Dependency, Module}
import org.softlang.dscor.utils.Utils

/**
  * Created by Johannes on 04.09.2017.
  */
class NormalizeUnit[T](@Dependency val source: Documents[T]) extends Module with Documents[T] {

  def data(): RDD[(T, Map[String, Double])] = {
    source.data().map { case (k, v) =>
      k -> Utils.normalize(v)
    }
  }
}