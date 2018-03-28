package org.softlang.dscor.process

import java.io.File

import com.google.common.base
import com.google.common.base.{Charsets, Strings}
import org.apache.commons.lang.StringEscapeUtils
import org.apache.spark.rdd.RDD
import org.softlang.dscor.Paths
import org.softlang.dscor.featuremodel.{APIClustering, FeatureModel}
import org.softlang.dscor.utils.{SQLites, Utils, ViewUtils}
import org.softlang.dscor.utils.JUtils
import org.softlang.dscor.utils.CSVSink

import collection.JavaConverters._
import scala.math.Ordering
import scala.reflect.ClassTag

/**
  * Created by Johannes on 18.10.2017.
  */
object APIClustering {

  def main(args: Array[String]): Unit = {
    val length = FeatureModel.length[APIClustering].toInt

    val results = JUtils.asCSVSink(new File(Paths.results), Charsets.UTF_8, CSVSink.SinkType.DYNAMIC)

    for (i <- new scala.util.Random(1234567l).shuffle(0 to length)) {
      // Enumeration of the feature model.
      val target = FeatureModel.enumerate[APIClustering](i)
      val input = FeatureModel.flat(target)
      println(i + " of " + length + " " + target)

      // Computation of API clustering.
      val similarity = target.dataSimilarity
      val x = target.dataClustering
      val linkage = x._2
      val tree = x._1

      // Creating config output with feature-model details and evaluation.
      val thresholds = tree.thresholds().toSeq.sorted.zipWithIndex

      // Crazy computation of correlations:-/ (maybe to crazy)
      val referenceData = target.source.apis()

      def reference(l: String, r: String, property: String): Boolean =
        !referenceData(l).getOrElse(property, "").split(";").toSet.intersect(referenceData(r).getOrElse(property, "").split(";").toSet).isEmpty

      val clusterings = thresholds.map { case (threshold, step) => (step, tree.flatten(threshold).map(_.toSet).toSet) }

      val apis = tree.flatten()

      println(apis.size + " apis :" + apis)

      val structure = Utils.sc.parallelize(clusterings, 16)
        .flatMap { case (step, clustering) =>
          apis.flatMap(left => apis.map(right => (step, left, right, clustering.exists(c => c.contains(left) && c.contains(right)))))
        }

//      val correlationProperties = (if (target.source.isInstanceOf[RoverLP13]) Set("domains")
//      else Set("category", "tags", "api"))

      val correlationProperties = Set("category", "tags", "api")

      val properties = structure.flatMap { case (step, left, right, expected) =>
        correlationProperties.map(x => (x, step, left, right, expected, reference(left, right, x)))
      }.map { case (prop, step, _, _, expected, reference) =>
        (prop, step) -> (if (expected) 1.0 else 0.0, if (reference) 1.0 else 0.0)
      }

      val size = (apis.size * apis.size).toDouble

      val leftSum = properties.mapValues(_._1).reduceByKey(_ + _).collectAsMap()
      val rightSum = properties.mapValues(_._2).reduceByKey(_ + _).collectAsMap()

      val ts = properties.map { case ((prop, step), (left, right)) => (prop, step) -> (left - (leftSum(prop, step) / size)) * (right - (rightSum(prop, step) / size)) }
        .reduceByKey(_ + _).collectAsMap()


      val lefts = properties.map { case ((prop, step), (left, _)) =>
        (prop, step) -> math.pow(left - (leftSum(prop, step) / size), 2d)
      }
        .reduceByKey(_ + _).collectAsMap()

      val rights = properties.map { case ((prop, step), (_, right)) =>
        (prop, step) -> math.pow(right - (rightSum(prop, step) / size), 2d)
      }
        .reduceByKey(_ + _).collectAsMap()

      //      val difference = properties.map { case ((prop, step), (left, right)) => (prop, step) -> math.abs(left - right) }
      //        .reduceByKey(_ + _).collectAsMap()
      //        .map { case ((prop, step), value) => "#diff_" + prop + "_" + Strings.padStart(step.toString, (Math.log10(thresholds.size) + 1).toInt, '0') -> value.toString }

      val output = ts.map { case ((prop, step), t) => (prop, step) -> (t / math.pow(lefts(prop, step) * rights(prop, step), 0.5d)) }
        .map { case ((prop, step), value) => "#corr_" + prop + "_" + Strings.padStart(step.toString, (Math.log10(thresholds.size) + 1).toInt, '0') -> value.toString }

      val metadata = Map(
        "configuration_index" -> i.toString,
        "linkage" ->  linkage.map(x => x._1 + ";" + x._2 + ";" + x._3 + ";" + x._4).reduceOption(_ + " " + _).getOrElse(""),
        "apis" -> apis.sorted.reduce(_ + ";" + _)
      )

      val result = input ++ output ++ metadata

      //      if (apis.size * apis.size * correlationProperties.size * clusterings.size != properties.count())
      //        throw new RuntimeException("error")

      // TODO: linkage matrix needs to be submitted. This can also be used to populate the web interface.
      println(output)
      results.write(result.asJava)
      results.flush();
    }
  }
}

//  def temp_correlation(clusters: Set[Set[String]], property: String): Double = {
//
//
//    val apis = apis()
//    val temp = Utils.incidenceMatrix(clusters)
//    // TODO: Filter bot on input set.
//    val current = Utils.sc.parallelize(temp.toSeq).mapValues(x => if (x) 1d else 0d)
//    val expected = current.map { case ((r, l), _) => ((r, l) -> correlation(apis(r).getOrElse(property, ""), apis(l).getOrElse(property, ""))) }
//
//    Utils.corr2(current, expected)
//  }
//      val output = thresholds.flatMap { case (threshold, step) =>
//        // Flattened clusters.
//        val clusters = tree.flatten(threshold).map(_.toSet).toSet
//        println(step)
//        // Measures.
//        val maximalClusterSize = clusters.toSeq.map(_.size).max.toDouble
//        val nonSingeltonClusters = clusters.filter(_.size > 1).size.toDouble
//        val tags = target.source.correlation(clusters, "tags")
//        val categories = target.source.correlation(clusters, "categories")
//        val domains = target.source.correlation(clusters, "domains")
//
//        Map(
//          // Some merge step meta information.
//          "threshold_merge_" + step -> threshold.toString,
//          "max_cluster_size_merge_" + step -> maximalClusterSize.toString,
//          "number_non_singleton_clusters_merge_" + step -> nonSingeltonClusters.toString,
//          // Different correlation types.
//          "correlation_tags_merge" + step -> tags.toString,
//          "correlation_categories_merge" + step -> categories.toString,
//          "correlation_domains_merge" + step -> domains.toString)
//      }.toMap

// TODO: Create linkage matrix to result.
//val linkage = tree.
//
//  def temp_corr2[K](as: RDD[(K, Double)], bs: RDD[(K, Double)])
//                   (implicit kt: ClassTag[K], ord: Ordering[K] = null) = {
//
//    val asize = as.count().toDouble
//    val bsize = bs.count().toDouble
//
//    val asum = as.map(_._2).sum()
//    val bsum = bs.map(_._2).sum()
//
//    val aMean = asum / asize
//    val bMean = bsum / bsize
//
//    val t = as.join(bs).map { case (_, (a, b)) => (a - aMean) * (b - bMean) }.sum
//    val b1 = as.map { case (_, a) => math.pow(a - aMean, 2d) }.sum()
//    val b2 = bs.map { case (_, b) => math.pow(b - bMean, 2d) }.sum()
//
//    val result = t / math.pow((b1 * b2), 0.5d)
//    result
//  }


//    val domains = Seq(
//      Set("JAMA") -> "Math",
//      Set("Velocity") -> "Output",
//      Set("Spring") -> "NoSQL",
//      Set("OSCache") -> "Caching",
//      Set("BouncyCastle") -> "Encriyption",
//      Set("BSF") -> "Javaassist",
//      Set("Spring") -> "Security",
//      Set("Lucene") -> "Search",
//      Set("Hamcrest", "JUnit") -> "Testing",
//      Set("Ant", "Maven") -> "Build",
//      Set("JGoodies", "org.syntax.jedit") -> "GUI",
//      Set("Guava", "com.apple.eawt") -> "Basics",
//      Set("Guava", "Avalon") -> "Data",
//      Set("Spring", "JGroups", "Smack") -> "Middleware",
//      Set("Spring", "Hibernate", "JPA") -> "SQL",
//      Set("CUP", "ANTLR", "org.htmlparser") -> "Parsing",
//      Set("log4j", "AvalonLogKit", "SLF4J") -> "Logging",
//      Set("Spring", "Avalon", "EJB", "JAF") -> "Component",
//      Set("Guice", "Spring", "Avalon", "JTA") -> "Control",
//      Set("JFreeChart", "JGraph", "POI", "JavaMail") -> "Productivity",
//      Set("Jetty", "Struts", "javax.jnlp", "javax.servlet") -> "Web",
//      Set("iText", "ORO", "JMF", "JAF") -> "Format",
//      Set("javax.net", "Spring", "JGroups", "ApacheXML-RPC", "JNDI") -> "Distribution",
//      Set("javax.annotation", "Jython", "Rhino", "ASM", "BCEL", "BSF", "JDT", "OGNL") -> "Meta",
//      Set("XStream", "Jaxen", "JDOM", "SAX", "PlexusXML", "dom4j", "ApacheXML", "ECS", "Tidy") -> "XML")
//    for(file <- new File("C:/Data/Corpus/APIBaseline/RoverLP14Jars").listFiles()){
//      println(StringEscapeUtils.escapeCsv(file.getName) + ",unknown")
//    }
//    val outs = JUtils.readCsv(new File("C:/Data/Corpus/APIBaseline/RoverLP14.csv")).asScala.map(x =>
//      x.asScala ++ Map("tags" -> domains.filter(_._1.contains(x.get("name"))).map(_._2).reduce(_ + ";" + _)))
//
//    val sink = JUtils.asCSVSink(new File("C:/Data/Corpus/APIBaseline/RoverLP14New.csv"), Charsets.UTF_8, CSVSink.SinkType.FIRST_LINE)
//
//    for (out <- outs)
//      sink.write(out.asJava)
//
//    sink.close()