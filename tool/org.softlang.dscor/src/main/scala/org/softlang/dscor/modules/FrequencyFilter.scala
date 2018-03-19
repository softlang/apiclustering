package org.softlang.dscor.modules

import org.softlang.dscor.Module
import org.softlang.dscor._
import org.softlang.dscor.utils.Utils
import org.softlang.dscor.macros.Value

/**
  * Created by Johannes on 5/10/2017.
  */
class FrequencyFilter[T](
                          @Dependency val source: Documents[T] = null,
                          @Property val lowerTermFrequencyBound: Double = 0.02d,
                          @Property val upperTermFrequencyBound: Double = 0.80d
                        ) extends RDDModule[(T, Map[String, Double])] with Documents[T] {

  lazy val size = source.data().count()

  def terms() = Utils.df(source.data().collect()
    .map(_._2))
    .map { case (t, v) => t -> (v.toDouble / size.toDouble) }
    .filter { case (t, v) => lowerTermFrequencyBound < v && v < upperTermFrequencyBound }

  override def compute() = {
    val localTerms = terms()
    source.data().map { case (k, x) => k -> x.filter { case (t, v) => localTerms.contains(t) } }
  }
}
