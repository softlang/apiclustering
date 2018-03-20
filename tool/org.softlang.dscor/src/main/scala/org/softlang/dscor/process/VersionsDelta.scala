package org.softlang.dscor.process

import java.io.File

import com.google.common.base.Charsets
import org.softlang.dscor.Paths
import org.softlang.dscor.utils._

import scala.collection.JavaConverters._

/**
  * Created by Johannes on 15.01.2018.
  */
object VersionsDelta {

  def unqualify(x: String): String = if (x.lastIndexOf(".") == -1) x else x.substring(x.lastIndexOf(".") + 1)

  def toPackage(x: String): String = if (x.lastIndexOf(".") == -1) "default_package" else x.substring(0, x.lastIndexOf("."))

  def unqualify(set: Set[String]): Set[String] = set.map(unqualify)

  def toPackage(set: Set[String]): Set[String] = set.map(toPackage)

  def approx(x: String): Set[String] = {
    // Only keep letters.
    val xi = SUtils.replace(Map("[^a-zA-Z]" -> " "))(x)
    // Cammel-case
    val xii = SUtils.insert("[a-z][A-Z]", " ", 1)(xi)
    val xiii = SUtils.insert("[A-Z][A-Z][a-z]", " ", 1)(xii)
    // Lower everything
    val xiiii = xiii.map(_.toLower)

    // Split to set
    xiiii.split(" ").toSet
  }

  def buhc(clusters: Seq[(Int, Int, Set[String])]): Seq[(Int, Int, Double, Int)] = {
    if (clusters.size == 1) return Seq()

    val similarities = clusters.zip(clusters.drop(1)).map { case ((l, lsize, lset), (r, rsize, rset)) =>
      (l, r, lsize + rsize, jc(lset, rset))
    }.zipWithIndex

    val ((l, r, size, sim), position) = similarities.sortBy(_._1._4).last

    val nextIndex = clusters.maxBy(_._1)._1 + 1

    val nextElements = clusters(position)._3.union(clusters(position + 1)._3)
    val nextClusters = clusters.take(position) ++ Seq((nextIndex, size, nextElements)) ++ clusters.drop(position + 2)

    Seq((l, r, 1.0 - sim, size)) ++ buhc(nextClusters)
  }

  def jc(a: Set[String], b: Set[String]) = a.intersect(b).size.toDouble / a.union(b).size.toDouble

  def main(args: Array[String]): Unit = {

    Utils.cores = 16

    val sources = Utils.sc.textFile(Paths.metadata, 10)
    val header = sources.first()

    val deltas = sources.filter(_ != header).map(SUtils.readCsvRow(header, _)).groupBy(row => (row("groupId"), row("artifactId"))).flatMap {
      case ((groupId, artifactId), iterator) =>

        val metadata = iterator.toSeq.sortBy(x => x("version_index").toInt).zipWithIndex.map(_.swap).toMap

        val elements = (0 to metadata.size - 1).map(x => (x,unqualify(SMavens.jarElements(groupId + ":" + artifactId + ":" + metadata(x)("version")))))

        val linkage = buhc(elements.map { case (i, e) => (i, 1, e) })

        elements.zip(Seq(Set[String]()) ++ elements.map(_._2)).map { case ((version, current), previous) =>
          val jcsimilarity = jc(current, previous)
          val add = current.diff(previous).size.toDouble / current.union(previous).size.toDouble
          val remove = previous.diff(current).size.toDouble / current.union(previous).size.toDouble
          val size = current.size

          metadata(version) ++ Map(
            "add" -> add.toString,
            "remove" -> remove.toString,
            "jcsimilarity" -> jcsimilarity.toString,
            "linkage" -> linkage.map(x => x._1 + ";" + x._2 + ";" + x._3 + ";" + x._4).reduceOption(_ + " " + _).getOrElse(""))
        }

    }


    val sinkDelta = JUtils.asCSVSink(new File(Paths.versionDelta), Charsets.UTF_8, CSVSink.SinkType.FIRST_LINE)

    for (row <- deltas.collect()) {
      println(row)
      sinkDelta.write(row.asJava)
    }
    sinkDelta.close()

  }

}
