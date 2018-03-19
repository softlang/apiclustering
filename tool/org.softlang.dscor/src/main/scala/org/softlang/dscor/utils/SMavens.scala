package org.softlang.dscor.utils

import java.io.{File, InputStream}
import java.net.URL
import java.util.zip.ZipFile

import com.google.common.base.Charsets
import jdk.internal.org.objectweb.asm.ClassReader
import jdk.internal.org.objectweb.asm.tree.ClassNode
import org.apache.commons.io.IOUtils
import org.apache.spark.rdd.RDD

import scala.collection.mutable
import collection.JavaConverters._
import scala.reflect.ClassTag
import scala.io.Source


/**
  * Created by Johannes on 15.11.2017.
  */
object SMavens {


  //
  //  def category(coorginates: String): Set[String] = {
  //    def url = new URL("https://mvnrepository.com/artifact/" + Mavens.groupId(coorginates) + "/" + Mavens.artifactId(coorginates) + "/" + Mavens.version(coorginates))
  //
  //    val inputStream = url.openStream()
  //
  //    def attribute(key: String, value: String)(node: Node) =
  //      node.attributes.exists(x => x.key == key && x.value.text == value)
  //
  //
  //    val xml = scala.xml.XML.loadString(IOUtils.toString(inputStream, Charsets.UTF_8))
  //    val maincontent = (xml \\ "div" ).filter(attribute("id", "maincontent"))
  //
  //    println(maincontent)
  //
  //    inputStream.close()
  //    Set()
  //  }


  def jarElements(coordinates: String): Set[String] = {
    try {
      val zipFile = Mavens.downloadJar(coordinates)

      if (!zipFile.isPresent) return Set()
      val zip = zipFile.get()

      val entries = zip.entries.asScala
      val elements = new mutable.HashSet[String]()

      entries.filter(x => x.getName.endsWith(".class") && x.getName != "module-info.class").foreach { entry =>
        val uri = entry.getName
        val content = IOUtils.toByteArray(zip.getInputStream(entry))

        try {
          val node: ClassNode = new ClassNode()
          val cr: ClassReader = new ClassReader(content)
          cr.accept(node, 0)

          val classname = node.name.replaceAll("/", ".")
          for (element <- node.methods.asScala.map(x => classname + "::" + x.name))
            elements.add(element)

        } catch {
          case _: IllegalArgumentException =>
        }
      }
      zip.close()
      elements.toSet
    } catch {
      case _: IllegalArgumentException => return Set()
    }
  }
}
