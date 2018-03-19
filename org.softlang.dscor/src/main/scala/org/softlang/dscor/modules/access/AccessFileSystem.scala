package org.softlang.dscor.modules.access

import java.io.{File, FileInputStream, InputStream}

import org.apache.spark.rdd.RDD
import org.softlang.dscor.Property
import org.softlang.dscor.utils.{InputStreamDelegate, Utils}

/**
  * Created by Johannes on 27.06.2017.
  */
class AccessFileSystem(@Property location: String) extends Access() {
  @Property
  def lastModified(): Long = Utils.lastModified(location)

  override def compute(): RDD[(String, InputStreamDelegate)] =
    sc.parallelize(Utils.tree(location).map(file => file.getPath -> new FileSystemInputStreamDelegate(file.getPath)).toSeq)

  override def backup() = false

}