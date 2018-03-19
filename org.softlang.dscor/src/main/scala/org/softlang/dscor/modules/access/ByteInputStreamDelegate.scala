package org.softlang.dscor.modules.access

import java.io.{ByteArrayInputStream, InputStream}

import org.softlang.dscor.utils.InputStreamDelegate

/**
  * Created by Johannes on 27.06.2017.
  */
class ByteInputStreamDelegate(bytes: Array[Byte]) extends InputStreamDelegate {
  override def delegate: InputStream = new ByteArrayInputStream(bytes);
}
