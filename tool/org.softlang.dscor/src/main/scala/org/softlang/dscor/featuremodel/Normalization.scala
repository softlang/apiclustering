package org.softlang.dscor.featuremodel

import org.softlang.dscor.RDDModule
import org.softlang.dscor.modules.{Documents, Preprocessing}
import org.softlang.dscor.utils.Utils

/**
  * Created by Johannes on 16.10.2017.
  */
sealed trait Normalization {
  def execute(input: RDDModule[(String, String)]): Documents[String]
}

final case class NormalizationNone() extends Normalization {
  override def execute(input: RDDModule[(String, String)]): Documents[String] =
    new Preprocessing(input, stopwords = Set(), camelCaseSplitting = false, stemming = false)
}

final case class NormalizationCCSW() extends Normalization {
  override def execute(input: RDDModule[(String, String)]): Documents[String] =
    new Preprocessing(input, stopwords = APIClustering.stopwords, camelCaseSplitting = true, stemming = false)
}

final case class NormalizationAll() extends Normalization {
  override def execute(input: RDDModule[(String, String)]): Documents[String] =
    new Preprocessing(input, stopwords = APIClustering.stopwords, camelCaseSplitting = true, stemming = true)
}

