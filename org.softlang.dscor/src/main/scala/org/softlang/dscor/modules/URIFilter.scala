package org.softlang.dscor.modules

import org.apache.spark.rdd.RDD
import org.softlang.dscor.utils.Utils
import org.softlang.dscor.{Dependency, Property, RDDModule}

/** // A Sample how merge uri can be used to remove class names and map to api:
  * val m1 = "\\.[Dach.]*$".r.findAllIn("org.apache.lucene.Part22_yhard")
  * val m2 = "org\\.apache\\.lucene\\..*".r.findAllIn("org.apache.lucene.a3f.Part22_yhard")
  * for(x <- m1)
  * println(x)
  * *
  * for(x <- m2)
  * println(x)
  */
class URIFilter(@Dependency val source: Documents[String],
                @Property val filter: Set[String] = Set(".*"),
                @Property val filterInvert: Set[String] = Set()
               ) extends RDDModule[(String, Map[String, Double])] with Documents[String] {

  override def backup(): Boolean = false

  override def compute(): RDD[(String, Map[String, Double])] = {
    // TODO: Solve this serializable spark shit using macros maybe.
    val _filter = filter
    val _filterInvert = filterInvert

    source
      .data()
      .filter { case (uri, _) => _filter.exists(x => uri.matches(x)) }
      .filter { case (uri, _) => !_filterInvert.exists(x => uri.matches(x)) }
  }
}
