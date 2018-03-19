package org.softlang.dscor.process

import java.io.File
import java.nio.file.Files

import com.google.common.base.Charsets
import org.apache.hadoop.hdfs.server.datanode.DataStorage
import org.apache.maven.model.Model
import org.apache.spark.sql.SQLContext
import org.apache.spark.storage.StorageLevel
import org.softlang.dscor.utils.{CSVSink, JUtils, Mavens, Utils}

import collection.JavaConverters._


/**
  * Created by Johannes on 15.12.2017.
  */
object ParseGitPomsProgramToRepos {
  def blacklist = Set("https://raw.githubusercontent.com/NKaladhar/helloworldmaven/1de7b79af9a1168cd42c459b9130528c69512b1e/pom.xml",
    "https://raw.githubusercontent.com/masthan01/helloworldmaven/396b73493a324b5e5df79e519bf0f231673ec48c/pom.xml")

  def parseNoExceptions(url: String) = {
    try {
      parse(url)
    } catch {
      case _ => Seq()
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
    // import sqlContext.implicits._

    val sources = Utils.sc.textFile(JUtils.configuration("api_baseline") + "/PomSourceList.csv")
    val header = sources.first()
    val applicationPoms = sources
      .filter { case line => line != header }
      .map(_.split(","))
      .map { case line => ApplicationPom(line(0), line(1)) }
      .filter(x => !blacklist.contains(x.pomUrl))
      //.sample(false, 0.0001)

    val applicationGroups = applicationPoms
      .groupBy(x => x.applicationUrl)
      .repartition(1000)
      .sample(false,0.1)

    val dependencies = applicationGroups.flatMap { case (application, iterable) =>
      // Parse this and return normalized set of this applications dependencies.
      val result = iterable.flatMap(x => parseNoExceptions(x.pomUrl)).toSet
      val filteredResult = result.filter { case (groupId, artifactId, version) => (groupId != "undefined" & artifactId != "undefined" & version != "undefined") }
      // Accumulate result.
      dependencyAccumulator.add(filteredResult.size)
      // Return result.
      filteredResult.map { case (groupId, artifactId, verions) => (application, groupId, artifactId, verions) }
    }
    val csvsink = JUtils.asCSVSink(new File(Paths.clients), Charsets.UTF_8, CSVSink.SinkType.STATIC, "client", "groupId", "artifactId", "version")

    val persisted = dependencies.persist(StorageLevel.MEMORY_AND_DISK)
    persisted.foreach(x => null)

    for (row <- persisted.toLocalIterator.map(x => Seq(x._1, x._2, x._3, x._4)))
      csvsink.write(row.toArray: _*)

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
