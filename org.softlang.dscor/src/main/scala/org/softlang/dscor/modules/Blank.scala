package org.softlang.dscor.modules

import org.apache.spark.rdd.RDD
import org.softlang.dscor.modules.access.AccessJAR
import org.softlang.dscor.{Dependency, Module}


object Blanks {

  def source[T1 <: Any](t1: T1) = new ModuleBuilder[T1](t1)

  def source[T1 <: Any, T2 <: Any](t1: T1, t2: T2) = new ModuleBuilder[(T1, T2)]((t1, t2))

  def source[T1 <: Any, T2 <: Any, T3 <: Any](t1: T1, t2: T2, t3: T3) = new ModuleBuilder[(T1, T2, T3)]((t1, t2, t3))

  def source[T1 <: Any, T2 <: Any, T3 <: Any, T4 <: Any](t1: T1, t2: T2, t3: T3, t4: T4) = new ModuleBuilder[(T1, T2, T3, T4)]((t1, t2, t3, t4))

//  def test = {
//    var lsi = new LSI[String](null)
//
//    val out = source("String", lsi).documents { case (string, lsi) => lsi.data() }
//  }
}

class ModuleBuilder[Ts](ts: Ts) {
  def documents[T](function: Ts => RDD[(T, Map[String, Double])]) = new Module with Documents[T] {

    def args: Product = ts match {
      case x: Product => x
      case x => new Tuple1(x)
    }

    override def getDependencies: Map[String, Module] =
      args.productIterator.filter { case x => x.isInstanceOf[Module] }.zipWithIndex.map { case (m, index) => index.toString -> m.asInstanceOf[Module] }.toMap

    override def getProperties: Map[String, Any] =
      args.productIterator.filter { case x => !x.isInstanceOf[Module] }.zipWithIndex.map { case (m, index) => index.toString -> m }.toMap

    override def data(): RDD[(T, Map[String, Double])] = function(ts)
  }
}

/**
  * Created by Johannes on 18.09.2017.
  */
//class Blank[M <: Module, T](@Dependency val m: M)(function: M => T) extends Module {
//
///**
//  * Only tracks the dependent module m. The function producing it is ignored and has to be manually deleted form backup.
//  */
//def blank(): T = function(m)
//}
//
//class BlankDocument[M <: Module, T](@Dependency val m: M)(function: M => RDD[(T, Map[String, Double])]) extends Module with Documents[T] {
//
//  override def data(): RDD[(T, Map[String, Double])] = function(m)
//}
