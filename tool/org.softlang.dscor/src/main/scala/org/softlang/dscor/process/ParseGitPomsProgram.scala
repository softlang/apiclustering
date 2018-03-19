package org.softlang.dscor.process

import java.io.File
import java.nio.file.Files

import com.google.common.base.Charsets
import org.apache.maven.model.Model
import org.apache.spark.sql.SQLContext
import org.apache.spark.{SparkConf, SparkContext}
import org.softlang.dscor.utils.{CSVSink, JUtils, Mavens, Utils}

import collection.JavaConverters._

case class ApplicationPom(pomUrl: String, applicationUrl: String)

/**
  * Created by Johannes on 15.12.2017.
  */
object ParseGitPomsProgram {
  def blacklist = Set("https://raw.githubusercontent.com/NKaladhar/helloworldmaven/1de7b79af9a1168cd42c459b9130528c69512b1e/pom.xml",
    "https://raw.githubusercontent.com/masthan01/helloworldmaven/396b73493a324b5e5df79e519bf0f231673ec48c/pom.xml")

  def parseNoExceptions(url: String) = {
    try {
      parse(url)
    } catch {
      case x: Exception => Seq()
    }
  }

  def parse(url: String) = {
    val model = Mavens.parseGitPom(url)

    val properties = if (model.getProperties != null) model.getProperties.asScala else Seq()

    val dependencies = model.getDependencies.asScala ++
      (if (model.getDependencyManagement != null) model.getDependencyManagement.getDependencies.asScala else Seq())

    def resolve(string: String, limit: Int): String = string match {
      case x if limit == 0 => "undefined"
      case null => "undefined"
      case x if x.contains("${project.version}") => resolve(x.replace("${project.version}", model.getVersion), limit - 1)
      case x if x.contains("${project.groupId}") => resolve(x.replace("${project.groupId}", model.getGroupId), limit - 1)
      case x if x.contains("${project.artifactId}") => resolve(x.replace("${project.artifactId}", model.getArtifactId), limit - 1)
      case x if x.contains("{") | x.contains("$") => {
        val replaced = properties.scanLeft(x)((a, b) => a.replace("${" + b._1 + "}", b._2)).last
        resolve(replaced, limit - 1)
      }
      // Default case where everything is resolved.
      case x => x
    }

    dependencies.map(dependency => (resolve(dependency.getGroupId, 6), resolve(dependency.getArtifactId, 6), resolve(dependency.getVersion, 6)))
  }

  def main(args: Array[String]): Unit = {

    Utils.cores = 256

    val sqlContext = new SQLContext(Utils.sc)
    val dependencyAccumulator = Utils.sc.longAccumulator("Dependencies")

    val sources = Utils.sc.textFile(JUtils.configuration("dataset") + "/PomSourceList.csv")
    val header = sources.first()
    val applicationPoms = sources
      .filter { case line => line != header }
      .map(_.split(","))
      .map { case line => ApplicationPom(line(0), line(1)) }
      .filter(x => !blacklist.contains(x.pomUrl))


    val applicationGroups = applicationPoms
      .groupBy(x => x.applicationUrl)
      .repartition(1000)

    val dependencies = applicationGroups.flatMap { case (application, iterable) =>
      // Parse this and return normalized set of this applications dependencies.
      val result = iterable.flatMap(x => parseNoExceptions(x.pomUrl)).toSet
      val filteredResult = result.filter { case (groupId, artifactId, version) => (groupId != "undefined" & artifactId != "undefined" & version != "undefined") }
      // Accumulate result.
      dependencyAccumulator.add(filteredResult.size)
      // Return result.
      filteredResult
    }.map(x => (x, 1L))
      .reduceByKey(_ + _)
      .sortBy(x => x._2, ascending = false)

    val csvsink = JUtils.asCSVSink(new File(Paths.counts), Charsets.UTF_8, CSVSink.SinkType.STATIC, "groupId", "artifactId", "version", "count")

    for (row <- dependencies.map(x => Seq(x._1._1, x._1._2, x._1._3, x._2.toString)).collect()) csvsink
      .write(row.toArray: _*)

    csvsink.close()

    // Count directly and persist as count file.

    //
    //    for (x <- applicationPoms.take(200) ) {
    //      println("parsing " + x.pomUrl)
    //      if (x.pomUrl.endsWith("pom.xml")) {
    //        try {
    //          println(parse(x.pomUrl))
    //        } catch {
    //          case e => println(e)
    //        }
    //      }
    //    }

    //println(textRDD.foreach(println)
    //    val empRdd = textRDD.map {
    //      line =>
    //        val col = line.split(",")
    //        Employee(col(0), col(1), col(2), col(3), col(4), col(5), col(6))
    //    }
    //    val empDF = empRdd.toDF()
    //    empDF.show()
    /* Spark 2.0 or up
      val empDF= sqlContext.read.format("csv")
  .option("header", "true")
  .option("inferSchema", "true")
  .load("src\\main\\resources\\emp_data.csv")
     */
  }

}
