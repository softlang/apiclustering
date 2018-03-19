package org.softlang.dscor.modules

import org.apache.spark.rdd.RDD
import org.softlang.dscor.utils.Utils
import org.softlang.dscor.{Dependency, Module, Property, RDDModule}

/** // A Sample how merge uri can be used to remove class names and map to api:
  * val m1 = "\\.[Dach.]*$".r.findAllIn("org.apache.lucene.Part22_yhard")
  * val m2 = "org\\.apache\\.lucene\\..*".r.findAllIn("org.apache.lucene.a3f.Part22_yhard")
  * for(x <- m1)
  * println(x)
  * *
  * for(x <- m2)
  * println(x)
  */
class URIMap(@Dependency val source: Documents[String],
             @Property val mapping: Map[String, String] = Map()) extends RDDModule[(String, Map[String, Double])] with Documents[String] {

  override def backup(): Boolean = false

  override def compute(): RDD[(String, Map[String, Double])] = {
    // TODO: Solve this serializable spark shit using macros maybe.
    val _mapping = mapping

    source
      .data()
      .map { case (uri, content) => Utils.replace(_mapping).apply(uri) -> content }
      .groupBy(_._1)
      .map { case (uri, iterable) => uri -> iterable.map(_._2).reduce(Utils.add(_, _)) }

    //    source.data().map { case (uri, content) =>
    //      uri.split(if (_separator == ".") "\\." else _separator) -> content
    //
    //    }.collect { case (uri, content) if uri.size > math.max(_position, -_position) =>
    //      val groupFragments = if (_position > 0) uri.take(_position) else uri.take(uri.size + _position)
    //      groupFragments.reduce(_ + _separator + _) -> content
    //    }
    //      .groupBy(_._1)
    //      .map { case (uri, iterable) => uri -> iterable.map(_._2).reduce(Utils.add(_, _)) }
  }
}
