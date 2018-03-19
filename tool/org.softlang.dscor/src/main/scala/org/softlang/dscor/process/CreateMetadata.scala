package org.softlang.dscor.process

import java.io.File
import java.net.{ConnectException, URL}

import com.google.common.base.Charsets
import org.softlang.dscor.utils._

import scala.collection.JavaConverters._

/**
  * Created by Johannes on 07.01.2018.
  * Uses the Focus or later FrequentAPIs to create the available API metadata.
  */
object CreateMetadata {

  def exists(url: URL): String = {
    try {
      String.valueOf(JUtils.exists(url))
    } catch {
      case _: ConnectException => "EXCEPTION"
    }
  }


  def main(args: Array[String]): Unit = {
    // Do (not) use (too) many cores since maven may block access.

    Utils.cores = 1

    // Replace that by FrequentApi file.
    def source = Utils.sc.parallelize(SUtils.readCsv(new File(JUtils.configuration("dataset") + "/ExtendedCount.csv")),20)

    def threshold_usage_rank_api = 100

    val metadata = source
      .map { case row => row.filter(_._1 != "") }
      .filter(x => x("api_usage_rank").toInt < threshold_usage_rank_api)
      .groupBy { case row => (row("groupId"), row("artifactId")) }
      .flatMap { case ((groupId, artifactId), rows) =>
        // TODO: Use guava RateLimiter if something is not working.
        val optionalCategory = Mavens.category("https://mvnrepository.com/artifact/" + groupId + "/" + artifactId)
        val category =
          if (optionalCategory.isPresent) optionalCategory.get().asScala.reduceOption(_ + ";" + _).getOrElse("")
          else "EXCEPTION"

        val optionalTags = Mavens.tags("https://mvnrepository.com/artifact/" + groupId + "/" + artifactId)
        val tags =
          if (optionalTags.isPresent) optionalTags.get().asScala.reduceOption(_ + ";" + _).getOrElse("")
          else "EXCEPTION"

        // TODO: Coorect thaat vesion 1.0 of commons-lang is repeated.
        rows.filter { case row => !(
          row("version").endsWith("android") ||
            row("version").contains("[") ||
            row("version").contains("]") ||
            row("version").contains("(") ||
            row("version").contains(")") ||
            row("version").contains("\\"))
        }
          .map { case row => row ++ Map("size" -> SMavens.jarElements(groupId + ":" + artifactId + ":" + row("version")).size.toString) }
          .filter { case row => row("size").toInt > 0 }
          .toSeq
          .sortBy { case row => new Version(row("version")) }
          .zipWithIndex
          .map { case (row, version_index) =>
            val source_available = exists(new URL(Mavens.MAVEN + Mavens.pathOfSource(groupId, artifactId, new Version(row("version")))))
            row ++ Map("source_available" -> source_available.toString, "tags" -> tags, "category" -> category, "version_index" -> version_index.toString)
          }
      }

    // Save metadata.
    val sinkDelta = JUtils.asCSVSink(new File(Paths.metadata), Charsets.UTF_8, CSVSink.SinkType.FIRST_LINE)

    for (row <- metadata.collect()) {
      println(row)
      sinkDelta.write(row.asJava)
    }
    sinkDelta.close()

  }

  //
  //    val versions = Mavens.findAvailableVersions(groupId + ":" + artifactId + ":[0,)", Mavens.newRepositorySystem())
  //      .asScala
  //      .filter { case x => SMavens.jarElements(groupId + ":" + artifactId + ":" + Mavens.version(x)).size > 0 }
  //      // Filter all android specific versions.
  //      .filter { case x => !x.endsWith("android") }
  //      .zipWithIndex
  //
  //    val versionMap = versions.map { case (version, index) => index -> version }.toMap
  //
  //    versions.map { case (version, versionIndex) =>
  //      val source = exists(new URL(Mavens.MAVEN + Mavens.pathOfSource(groupId, artifactId, Mavens.version(version))))
  //      val previous = versionMap.get(versionIndex - 1).map(Mavens.version(_)).getOrElse("")
  //      Seq(versionIndex, Mavens.groupId(version), Mavens.artifactId(version), Mavens.version(version), previous, source, category, tags).map(_.toString)
  //    }
  //  }.flatMap(x => x)

}
