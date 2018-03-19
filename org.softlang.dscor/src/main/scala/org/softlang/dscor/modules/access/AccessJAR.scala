package org.softlang.dscor.modules.access

import java.io.{ByteArrayInputStream, InputStream}
import java.util.zip.ZipFile

import com.google.common.io.Files
import org.apache.commons.io.IOUtils
import org.apache.spark.rdd.RDD
import org.softlang.dscor.Property
import org.softlang.dscor.utils.{InputStreamDelegate, Utils}

import scala.collection.JavaConverters._

/**
  * Created by Johannes on 20.06.2017.
  */
class AccessJAR(@Property location: String) extends Access() {
  @Property
  def lastModified(): Long = Utils.lastModified(location)

  override def compute(): RDD[(String, InputStreamDelegate)] =
    sc.parallelize(Utils.tree(location)
      .filter(x => Files.getFileExtension(x.getName) == "jar")
      .flatMap { file =>
        val zip = new ZipFile(file)
        val entries = zip.entries.asScala
        val zipContent = entries.map { entry =>
          val uri = file.getName + "/" + entry.getName
          val content = new ByteInputStreamDelegate(IOUtils.toByteArray(zip.getInputStream(entry)))
          (uri, content)
        }
        zipContent
      }.toSeq)

  override def backup() = false
}
