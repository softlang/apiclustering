package org.softlang.dscor.featuremodel

import org.softlang.dscor.modules._

import scala.reflect.ClassTag

/**
  * Created by Johannes on 17.10.2017.
  */
sealed trait Analytical {
  def execute(documents: Documents[(String, String, String)]): Documents[(String, String, String)]
}

final case class AnalyticalLDA() extends Analytical {
  def execute(documents: Documents[(String, String, String)]) = new CleanLDA(documents, k = 25)
}

final case class AnalyticalIdf() extends Analytical {
  def execute(documents: Documents[(String, String, String)]) = new NormalizeUnit(new VSM(documents))
}

final case class AnalyticalLSI() extends Analytical {
  def execute(documents: Documents[(String, String, String)]) = new LSI(new VSM(documents))
}

final case class AnalyticalNone() extends Analytical {
  def execute(documents: Documents[(String, String, String)]) = new NormalizeUnit(documents)
}