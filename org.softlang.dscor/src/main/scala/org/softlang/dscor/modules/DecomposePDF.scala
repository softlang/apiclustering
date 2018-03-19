package org.softlang.dscor.modules

import org.apache.spark.rdd.RDD
import com.google.inject.name.Named
import org.softlang.dscor.{Dependency, Property}
import org.softlang.dscor.utils.Utils
import java.io.File

import org.apache.pdfbox.pdmodel.PDDocument
import java.io.InputStream

import org.apache.pdfbox.text.PDFTextStripper
import org.softlang.dscor.modules.access.Access

import scala.xml.NodeSeq

class DecomposePDF(
@Dependency access: Access,
                    @Property val firstLine: Int = 0,
                    @Property val lastLine: Int = 0,
                    @Property val logging: String) extends Decompose(access) {

  override def compute(): RDD[(String, String)] = {
    val output = access.data().flatMap {
      case (id, inputStream) =>
        val lines = Utils.readPDF(inputStream.delegate).split("\r\n").toSeq.zipWithIndex.map(_.swap).toMap
        val size = lines.size

        val watermarks = lines.map { case (index, line) => (index, Utils.replace(Map("[^a-zA-Z]" -> ""))(line)) }.groupBy {
          _._2
        }.filter(_._2.size > 5).flatMap(x => x._2.map(_._1)).toSet
        val scoreInclude = smoothing(lines.map { case (index, line) => index -> includes.map(x => Utils.countMatch(x, line)).fold(0)(_ + _).toDouble }, 2)
        val scoreExclude = smoothing(lines.map { case (index, line) => index -> excludes.map(x => Utils.countMatch(x, line)).fold(0)(_ + _).toDouble }, 2)

        def isIncluded(index: Int): Boolean = (index, scoreExclude(index) - scoreInclude(index)) match {
          case (_, f) if f < -epsilon => true
          case (_, f) if f > epsilon => false
          case (0, _) => true
          case (index, _) => isIncluded(index - 1)
        }

        def isOutside(index: Int) = index < firstLine || index > lastLine

        def isWatermark(index: Int) = watermarks(index)

        def getClassification(index: Int) = (isWatermark(index), isIncluded(index), isOutside(index)) match {
          case (_, _, true) => "outside"
          case (true, _, false) => "watermark"
          case (false, true, false) => "included"
          case (false, false, false) => "excluded"
        }

        val averageIncludedSize = lines.filter { case (index, line) => isIncluded(index) }.map(_._2.size.toDouble).reduce(_ + _) / size.toDouble

        def isParagraphBorder(index: Int) = getClassification(index) == "included" && lines(index).size < averageIncludedSize && lines(index).replace(" ", "").endsWith(".")

        def nextParagraphBorder(index: Int): Int = index match {
          case x if x == size => size
          case x if isParagraphBorder(index) => x
          case x => nextParagraphBorder(index + 1)
        }

        val result = lines.map {
          case (index, line) =>
            Line(index, line, isIncluded(index), isOutside(index), isWatermark(index), getClassification(index), isParagraphBorder(index), nextParagraphBorder(index), scoreInclude(index), scoreExclude(index))
        }

        Utils.write(logging + "/decomposition.html", svg(result).toString)

        def result2 = result
          .filter {
            _.classificaiton == "included"
          }
          .groupBy {
            _.nextParagraphBorder
          }
          .map { case (index, lines) => index -> lines.toSeq.sortBy {
            _.index
          }.map(_.text).reduce(_ + "\r\n" + _)
          }.toSeq

        Utils.write(logging + "/paragraphs.txt", result2.sortBy(_._1).map(_._2).reduce(_ + "\r\n\r\n" + _))

        result2.map(x => id + "@" + x._1 -> x._2)
    }

    output
  }

  case class Line(index: Int, text: String, included: Boolean, outside: Boolean, watermark: Boolean, classificaiton: String, paragraphBorder: Boolean, nextParagraphBorder: Int, scoreIncluded: Double, scoreExcluded: Double)


  @Property
  def smoothingFrame = 1

  @Property
  def epsilon = 0.3d

  def smoothing(input: Map[Int, Double], iterations: Int): Map[Int, Double] = iterations match {
    case 0 => input
    case x =>
      smoothing(input.map(_._1).map { i =>
        val scope = (-smoothingFrame to smoothingFrame).map(_ + i)
        val values = scope.flatMap { x => input.get(x) }
        val result = Utils.avg(values)

        i -> result
      }.toMap, iterations - 1)
  }

  @Property // TODO: make following propoerties configurable.
  val excludes = Seq(
    // Brackets.
    "\\{",
    "\\}",
    "\\[",
    "\\]",
    "\\<",
    "\\>" // No comments 3*
    , "//\\S", "//\\S", "//\\S" // No WS and opening bracket.
    , "\\S\\(" // Dot and no WS
    , "\\.\\S" // PL domain words.
    , "if[^a-zA-Z]", "def[^a-zA-Z]", "class[^a-zA-Z]", "import[^a-zA-Z]", "return[^a-zA-Z]", "var[^a-zA-Z]", "map[^a-zA-Z]", "else[^a-zA-Z]", "get[^a-zA-Z]", "set[^a-zA-Z]")

  @Property
  val includes = Set("a", "about", "above", "after", "again", "against", "all", "am", "an", "and", "any", "are", "aren't", "as", "at", "be", "because", "been", "before", "being", "below", "between", "both", "but", "by", "can't", "cannot", "could", "couldn't", "did", "didn't", "do", "does", "doesn't", "doing", "don't", "down", "during", "each", "few", "for", "from", "further", "had", "hadn't", "has", "hasn't", "have", "haven't", "having", "he", "he'd", "he'll", "he's", "her", "here", "here's", "hers", "herself", "him", "himself", "his", "how", "how's", "i", "i'd", "i'll", "i'm", "i've", "if", "in", "into", "is", "isn't", "it", "it's", "its", "itself", "let's", "me", "more", "most", "mustn't", "my", "myself", "no", "nor", "not", "of", "off", "on", "once", "only", "or", "other", "ought", "our", "ours 	", "ourselves", "out", "over", "own", "same", "shan't", "she", "she'd", "she'll", "she's", "should", "shouldn't", "so", "some", "such", "than", "that", "that's", "the", "their", "theirs", "them", "themselves", "then", "there", "there's", "these", "they", "they'd", "they'll", "they're", "they've", "this", "those", "through", "to", "too", "under", "until", "up", "very", "was", "wasn't", "we", "we'd", "we'll", "we're", "we've", "were", "weren't", "what", "what's", "when", "when's", "where", "where's", "which", "while", "who", "who's", "whom", "why", "why's", "with", "won't", "would", "wouldn't", "you", "you'd", "you'll", "you're", "you've", "your", "yours", "yourself", "yourselves")
    .map { x => "[^a-zA-Z]" + x.replace("'", "?") + "[^a-zA-Z]" }.toSeq

  def svg(lines: Iterable[Line]) = {
    def linesize = 20

    def length = lines.size

    val scoresInclude = lines.map { line => line.index -> line.scoreIncluded }.toMap
    val scoresExclude = lines.map { line => line.index -> line.scoreExcluded }.toMap

    <svg height={(length * linesize).toString()} width="1000">
      {lines.map { line =>
      def colore = line.classificaiton match {
        case "outside" => "grey"
        case "watermark" => "blue"
        case "included" => "black"
        case "excluded" => "red"
      }

      <text stroke="none" fill={colore} x="0" y={(line.index * linesize).toString()}>
        {line.index.toString() + " " + line.text}
      </text>
          <line x1={(scoresInclude.getOrElse(line.index - 1, 0d) * 10 + 700).toInt.toString()} y1={((line.index - 1) * linesize).toString()} x2={(scoresInclude(line.index) * 10 + 700).toInt.toString()} y2={(line.index * linesize).toString()} style={"stroke:black;stroke-width:2"}/>
          <line x1={(scoresExclude.getOrElse(line.index - 1, 0d) * 10 + 700).toInt.toString()} y1={((line.index - 1) * linesize).toString()} x2={(scoresExclude(line.index) * 10 + 700).toInt.toString()} y2={(line.index * linesize).toString()} style={"stroke:red;stroke-width:2"}/>
    }}{lines.filter {
      _.paragraphBorder
    }.map { line =>
        <line x1="0" y1={(line.index * linesize + 4).toString()} x2="600" y2={(line.index * linesize + 4).toString()} style={"stroke:black;stroke-width:2"}/>
    }}
    </svg>
  }

}