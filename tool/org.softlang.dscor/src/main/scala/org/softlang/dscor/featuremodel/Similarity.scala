package org.softlang.dscor.featuremodel

import org.softlang.dscor.modules.Documents
import org.softlang.dscor.utils.Utils

/**
  * Created by Johannes on 17.10.2017.
  */
sealed trait Similarity {

  def execute(documents: Documents[(String, String, String)]): Map[(String, String), Double]

}

final case class SimilarityCosine() extends Similarity {
  def execute(documents: Documents[(String, String, String)]) = {
    val apivectors = documents
      .data()
      .groupBy { case ((api, pkg, cls), content) => api }
      .map { case (api, iterable) => api -> iterable.map(_._2).reduce(Utils.add(_, _)) }.cache()

    apivectors.cartesian(apivectors)
      .map { case ((id1, vector1), (id2, vector2)) => (id1, id2) -> Utils.cosine(vector1, vector2) }.collect().toMap
  }
}

final case class SimilarityJaccardCoefficient() extends Similarity {
  def execute(documents: Documents[(String, String, String)]) = {
    val apivectors = documents
      .data()
      .groupBy { case ((api, pkg, cls), content) => api }
      .map { case (api, iterable) => api -> iterable.map(_._2).reduce(Utils.add(_, _)) }.cache()

    apivectors.cartesian(apivectors)
      .map { case ((id1, vector1), (id2, vector2)) => (id1, id2) -> Utils.jaccardCoefficient(vector1, vector2) }.collect().toMap
  }
}
