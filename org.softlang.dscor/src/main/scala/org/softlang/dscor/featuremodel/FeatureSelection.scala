package org.softlang.dscor.featuremodel

import org.softlang.dscor.RDDModule
import org.softlang.dscor.modules.DecomposeClass
import org.softlang.dscor.utils.InputStreamDelegate

/**
  * Created by Johannes on 16.10.2017.
  */
sealed trait FeatureSelection {
  def execute(input: RDDModule[(String, InputStreamDelegate)]): RDDModule[(String, String)]
}

final case class FeatureSelectionVisible() extends FeatureSelection {
  override def execute(input: RDDModule[(String, InputStreamDelegate)]): RDDModule[(String, String)] =
    new DecomposeClass(input, "UTF-8", "visible")
}

final case class FeatureSelectionAll() extends FeatureSelection {
  override def execute(input: RDDModule[(String, InputStreamDelegate)]): RDDModule[(String, String)] =
    new DecomposeClass(input, "UTF-8", "all")
}
