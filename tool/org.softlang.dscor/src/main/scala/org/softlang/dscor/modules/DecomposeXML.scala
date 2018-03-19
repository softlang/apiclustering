package org.softlang.dscor.modules

import org.apache.spark.rdd.RDD
import org.softlang.dscor.utils.XMLLocationLoader
import org.apache.commons.io.IOUtils
import org.softlang.dscor._
import org.softlang.dscor.modules.access.Access

class DecomposeXML(@Dependency access: Access, @Property val encoding: String = null) extends Decompose(access) {

  override def compute(): RDD[(String, String)] = {
    val _encoding = encoding
    access
      .data()
      .filter(_._1.endsWith(".xml"))
      .flatMap {
        case (uri, content) =>
          XMLLocationLoader
            .loadString(IOUtils.toString(content.delegate, _encoding))
            .flatMap(xml => (xml \ "p").map { x => (uri + "@" + x.attribute("line").get.toString, x.child.collect { case scala.xml.Text(t) => t }.mkString) })
      }
  }

  override def backup() = false
}