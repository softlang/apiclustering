package org.softlang.dscor.utils

import java.io.{File, FileReader}
import java.util.regex.Pattern

import au.com.bytecode.opencsv.{CSVParser, CSVReader}
import com.google.common.base.Charsets
import com.google.common.io.Files

import collection.JavaConverters._

/**
  * Created by Johannes on 16.11.2017.
  */
object SUtils {

  def writeLatexTable(file: File, rows: Iterable[Iterable[Any]]) = Files.asCharSink(file, Charsets.UTF_8).write(
    rows.map(column => column.map(_.toString).reduce(_ + " & " + _) + "\\\\ \\hline").reduce(_ + "\r\n" + _))

  // TODO: Replace that with csv api.
  def writeCsv(file: File, rows: Iterable[Iterable[Any]]) = Files.asCharSink(file, Charsets.UTF_8).write(
    rows.map(row => row.map(_.toString).reduce(_ + "," + _)).reduce(_ + "\r\n" + _))

  def readCsv(file: File): Seq[Map[String, String]] = JUtils.readCsv(file).asScala.map(_.asScala.toMap)

  def readCsvRow(header: String, row: String): Map[String, String] =
    header.split(",").zip(row.split(",")).toMap


  def replace(map: Map[String, String]): String => String = x => map.foldRight(x)((l, r) => r.replaceAll(l._1, l._2))

  def insert(regex: String, content: String, offset: Integer): String => String = x => {
    val matcher = Pattern.compile(regex).matcher(x)
    var current = x
    var number = 0
    while (matcher.find()) {
      def position = matcher.start() + offset + number

      current = current.substring(0, position) + content + current.substring(position)
      // TODO: This has become wrong many times without a reason. Always check this!
      number = number + 1
    }
    current
  }

}
