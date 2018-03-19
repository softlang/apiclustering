package org.softlang.dscor.modules.access

import java.util.zip.ZipFile

import com.google.common.io.Files
import org.apache.commons.io.IOUtils
import org.apache.spark.rdd.RDD
import org.softlang.dscor.Property
import org.softlang.dscor.utils.{InputStreamDelegate, Utils}
import org.softlang.dscor.utils.Mavens
import scala.collection.JavaConverters._

/**
  * Created by Johannes on 20.06.2017.
  */
class AccessMavenJAR(@Property coordinates: Seq[String]) extends Access() {

  override def compute(): RDD[(String, InputStreamDelegate)] =
    sc.parallelize(coordinates).map(x => x -> Mavens.downloadJar(x))
      .collect { case (coordinate, optional) if optional.isPresent => coordinate -> optional.get() }
      .flatMap { case (coordinate, zip) => zip.entries.asScala.map(entry =>
        coordinate + "#" + entry.getName -> new ByteInputStreamDelegate(IOUtils.toByteArray(zip.getInputStream(entry)))
      )
      }

  override def backup() = false

}
