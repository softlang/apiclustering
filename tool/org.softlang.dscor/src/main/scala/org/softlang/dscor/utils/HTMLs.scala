package org.softlang.dscor.utils

object HTMLs {
  def templateJSArray(content: Seq[String]) = "[" + content.reduce(_ + "," + _) + "]"
  def templateJSMap(content: Map[String, String]) = "{" + content.map { case (k, v) => k + ":" + v }.reduce(_ + "," + _) + "}"

  def templateJsValue(x: Any) = x match {
    case x: String => "'" + x + "'"
    case x: Double => x.toString()
    case _ => x.toString()
  }

  def template(templateName: String, target: String, replacements: Map[String, String]) =
    Utils.save(target, replacements.seq.foldLeft(Utils.read(templateName))((l, r) => l.replaceAll("%" + r._1 + "%", r._2)))

}