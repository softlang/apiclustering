package org.softlang.dscor.process

import java.io.File
import java.net.{ConnectException, URL}

import com.google.common.base.Charsets
import org.softlang.dscor.Paths
import org.softlang.dscor.utils._

import scala.collection.JavaConverters._

/**
  * Created by Johannes on 07.01.2018.
  * Uses the Focus or later FrequentAPIs to create the available API metadata.
  */
object Metadata {

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

    def source = Utils.sc.parallelize(SUtils.readCsv(new File(Paths.extendedCounts)), 20)

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
}
