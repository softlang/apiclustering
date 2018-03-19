package org.softlang.dscor

import scala.reflect.runtime.universe._
import org.apache.spark.rdd.RDD

import scala.reflect.ClassTag
import org.softlang.dscor.utils.Utils

import scala.collection.mutable

/**
  * Special Module that overrides the default persistence and replaces it by RDD.
  */
abstract class RDDModule[T](implicit tag: ClassTag[T]) extends SingleModule[RDD[T]] {

  def sc = Utils.sc

  override def load(location: String): Any = sc.objectFile[Object](location, 2).map { x => x.asInstanceOf[T] }

  override def save(location: String, data: Any) = data.asInstanceOf[RDD[T]].saveAsObjectFile(location)

  val localNotDrop: mutable.HashMap[Any, String] = new mutable.HashMap[Any, String]()

  // RDDs never need to be dropped (at least this is what i think). TODO: This need to be reengineered properly.
  override def data(name: String, dump: Boolean, computation: () => Any): Any = {
    val value = localNotDrop.map(_.swap).toMap.get(name)

    if (value.isDefined) return value.get

    val computedVal = super.data(name, dump, computation)
    localNotDrop.put(computedVal, name)
    return computedVal
  }

}