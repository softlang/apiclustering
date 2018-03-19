package org.softlang.dscor.modules.access

import java.io.{File, FileInputStream, InputStream}

import org.softlang.dscor.utils.InputStreamDelegate

/**
  * Created by Johannes on 27.06.2017.
  */
class FileSystemInputStreamDelegate(path: String) extends InputStreamDelegate  {
  override def delegate: InputStream = new FileInputStream(new File(path))
}
