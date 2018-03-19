package org.softlang.dscor.modules

import org.apache.spark.rdd.RDD
import org.softlang.dscor.utils.XMLLocationLoader
import org.apache.commons.io.IOUtils
import org.softlang.dscor._
import org.softlang.dscor.modules.access.Access

class DecomposePython(
 access: Access,
    @Property val encoding: String = null) extends Decompose(access) {

  override def compute(): RDD[(String, String)] = {

    val localEncoding = encoding
    access
      .data()
      .filter(_._1.endsWith(".py"))
      .map {
        case (uri, content) =>
          (uri, IOUtils.toString(content.delegate, localEncoding))
      }
  }

  override def backup() = false
}