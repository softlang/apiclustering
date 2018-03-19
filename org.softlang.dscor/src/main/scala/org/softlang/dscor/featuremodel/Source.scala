package org.softlang.dscor.featuremodel

import java.io.File
import java.util.zip.ZipFile

import org.apache.commons.io.IOUtils
import org.apache.commons.lang.StringEscapeUtils
import org.apache.spark.rdd.RDD
import org.softlang.dscor.{Property, RDDModule}
import org.softlang.dscor.modules.access.{Access, ByteInputStreamDelegate}
import org.softlang.dscor.modules.{Blanks, Documents}
import org.softlang.dscor.process.Paths
import org.softlang.dscor.utils._

import collection.JavaConverters._

/**
  * Created by Johannes on 17.10.2017.
  */
sealed trait Source {
  def compute(): RDDModule[(String, InputStreamDelegate)]

  def apis(): Map[String, Map[String, String]]

}

case class RoverLP13() extends Source {

  def apis() = SUtils.readCsv(new File(Paths.roverLP14)).map(x => x("name") -> x).toMap

  override def compute(): RDDModule[(String, InputStreamDelegate)] = new RoverLP13Module(apis())

}

case class HaertelAL18() extends Source {

  // TODO: Remove cut.
  def cut = 300

  def apis() = SUtils.readCsv(new File(Paths.haertelAL18)).take(cut).map(x => x("coordinates") -> x).toMap

  override def compute(): RDDModule[(String, InputStreamDelegate)] = new HaertelAL18Module(apis())
}


class HaertelAL18Module(@Property apis: Map[String, Map[String, String]]) extends Access {
  override def compute(): RDD[(String, InputStreamDelegate)] =
    Utils.sc.parallelize(apis.keySet.toSeq).map(x => x -> Mavens.downloadJar(x))
      .collect { case (coordinate, optional) if optional.isPresent => coordinate -> optional.get() }
      .flatMap { case (coordinate, zip) => zip.entries.asScala.map(entry => coordinate + "#" + entry.getName -> new ByteInputStreamDelegate(IOUtils.toByteArray(zip.getInputStream(entry)))
      )
      }
}

class RoverLP13Module(@Property apis: Map[String, Map[String, String]]) extends Access {
  override protected def compute(): RDD[(String, InputStreamDelegate)] =
    Utils.sc.parallelize(apis.toSeq)
      .flatMap { case (name, kvs) => kvs("files").split(";").map(file => name -> new ZipFile(new File(JUtils.configuration("api_baseline") + "/RoverLP14Jars/" + file))) }
      .flatMap { case (name, zip) => zip.entries.asScala.map(entry => name + "#" + entry.getName -> new ByteInputStreamDelegate(IOUtils.toByteArray(zip.getInputStream(entry)))
      )
      }
}