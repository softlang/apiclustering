package org.softlang.dscor.modules

import org.apache.spark.rdd.RDD
import org.softlang.dscor.{RDDModule, Module, SingleModule}

case class Union[T](sources: Documents[T]*) extends RDDModule[(T, Map[String, Double])] with Documents[T] {

  override def getDependencies: Map[String, Module] = sources.zipWithIndex.map { case (k, v) => v.toString() -> k.asInstanceOf[Module] }.toMap

  override def compute(): RDD[(T, Map[String, Double])] = sources.map(_.data()).reduce(_ ++ _)

}