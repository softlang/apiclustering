package org.softlang.dscor

import java.util

import scala.reflect.ClassTag

case class Definition(
    val module: String,
    val properties: Map[String, Any],
    val dependencies: Map[String, Definition]) {
}