package org.softlang.dscor.featuremodel

import org.softlang.dscor.modules.{Blanks, Documents}

/**
  * Created by Johannes on 17.10.2017.
  */
sealed trait Sampling {
  def execute(documents: Documents[(String, String, String)]): Documents[(String, String, String)]
}

case class SamplingClass() extends Sampling {
  def execute(documents: Documents[(String, String, String)]) = Blanks.source(documents, this).documents {
    case (documents, _) =>
      val data = documents.data()
      val apis = data.map(_._1._1).collect()

      data
        .map { case ((api, pkg, cls), content) => (api, ((api, pkg, cls), content)) }
        .sampleByKey(false, apis.map(x => x -> 0.5d).toMap, 123456l)
        .map {
          _._2
        }
  }
}

case class SamplingAPI() extends Sampling {
  def execute(documents: Documents[(String, String, String)]) = Blanks.source(documents, this).documents {
    case (documents, _) =>
      documents
        .data()
        .groupBy { case ((api, _, _), _) => api }
        .sample(false, 0.5d, 123456l).flatMap(_._2)
  }
}

case class SamplingNone() extends Sampling {
  def execute(documents: Documents[(String, String, String)]) = Blanks.source(documents, this).documents {
    case (documents, _) =>
      documents
        .data()
  }
}