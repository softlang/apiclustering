package org.softlang.dscor.utils

import java.io.InputStream

/**
  * Created by Johannes on 26.06.2017.
  */
trait InputStreamDelegate extends java.io.Serializable{
  def delegate: InputStream
}
