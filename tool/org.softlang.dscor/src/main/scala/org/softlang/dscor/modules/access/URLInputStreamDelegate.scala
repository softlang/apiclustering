package org.softlang.dscor.modules.access

import java.io.InputStream
import java.net.URL

import org.softlang.dscor.utils.InputStreamDelegate

/**
  * Created by Johannes on 27.06.2017.
  */

class URLInputStreamDelegate(url: String) extends InputStreamDelegate {
  override def delegate: InputStream = new URL(url).openStream()

  override def toString: String = "URLAccess on: '" + url + "'"
}