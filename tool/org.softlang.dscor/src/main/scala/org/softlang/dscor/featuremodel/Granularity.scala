package org.softlang.dscor.featuremodel

import org.softlang.dscor.modules.{Blanks, Documents}
import org.softlang.dscor.utils.Utils

/**
  * Created by Johannes on 17.10.2017.
  */
sealed trait Granularity {
  def execute(documents: Documents[(String, String, String)]): Documents[(String, String, String)]
}

final case class GranularityAPI() extends Granularity {
  def execute(documents: Documents[(String, String, String)]) =
    Blanks.source(this, documents).documents[(String, String, String)] {
      case (_, documents) => documents.data()
        .groupBy { case ((api, pkg, cls), content) => api }
        .map { case (api, iterable) => (api, "merged", "merged") -> iterable.map(_._2).reduce(Utils.add(_, _)) }
    }
}

final case class GranularityPackage() extends Granularity {
  def execute(documents: Documents[(String, String, String)]) =
    Blanks.source(this, documents).documents[(String, String, String)] {
      case (_, documents) => documents.data()
        .groupBy { case ((api, pkg, cls), content) => (api, pkg) }
        .map { case ((api, pkg), iterable) => (api, pkg, "merged") -> iterable.map(_._2).reduce(Utils.add(_, _)) }
    }
}

final case class GranularityClass() extends Granularity {
  def execute(documents: Documents[(String, String, String)]) =
    Blanks.source(this, documents).documents[(String, String, String)] {
      case (_, documents) => documents.data().map { case ((api, pkg, cls), content) => ((api, pkg, cls), content) }
    }
}