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
    val configurationCount = FeatureModel.length[APIClustering].toInt

    val results = JUtils.asCSVSink(new File(Paths.results), Charsets.UTF_8, CSVSink.SinkType.DYNAMIC)
    println("Stating computation of " + configurationCount  + " configurations")

    for ((i, index) <- new scala.util.Random(1234567l).shuffle(0 to configurationCount).toSeq.zipWithIndex) {
      // Enumeration of the feature model.
      val target = FeatureModel.enumerate[APIClustering](i)
      val input = FeatureModel.flat(target)

      println()
      println("configuration: " + i)
      println("configurations: " + configurationCount)
      println("index: " + index)
      println("implementation" + target)

      // Computation of API clustering.
      val similarity = target.dataSimilarity
      val (tree, linkage) = target.dataClustering

      // Computation of correlation.
      print("Computing correlation")
      var correlations = Map[String, String]()

      // Initial lists.
      val thresholds = tree.thresholds().toSeq.sorted.zipWithIndex
      val steps = thresholds.map(_._2)

      def externals = Set("category", "tags", "api")

      val apis = tree.flatten().toSet
      val numberApis = apis.size
      val matrixSize = math.pow(numberApis, 2)
      val metadata = Utils.sc.parallelize(target.source.apis().toSeq.filter { case (api, _) => apis(api) }, 6)

      // Step and element to cluster id submitted to nodes.
      val clusters = Utils.sc.broadcast(thresholds.flatMap { case (threshold, step) =>
        tree.flatten(threshold).map(_.toSet).zipWithIndex.flatMap { case (elements, clusterid) =>
          elements.map { case element =>
            (step, element) -> clusterid
          }
        }
      }.toMap)

      // Ones in the matrix for external criteria.
      val externalOnes = externals.map { case external =>
        val x = metadata.map { case (api, meta) => (meta.get(external).map(_.split(";").toSet).getOrElse(Set())) }
        external -> x.cartesian(x).filter { case (l, r) => !l.intersect(r).isEmpty }.count().toDouble
      }

      // Ones in the matrix for clusters.
      val cluserOnes = thresholds.map { case (threshold, step) =>
        step -> tree.flatten(threshold).map(x => math.pow(x.toSet.size, 2)).reduce(_ + _)
      }

      for ((external, externalOne) <- externalOnes) {
        val x = metadata.map { case (api, meta) => (api, meta.get(external).map(_.split(";").toSet).getOrElse(Set())) }
        // Numeric computation of correlation parts for external matrix.
        val externalZeros = matrixSize - externalOne
        val externalMean = externalOne / matrixSize
        val externalDenominator = math.pow(1d - externalMean, 2) * externalOne + math.pow(0d - externalMean, 2) * externalZeros

        for ((step, clusteringOne) <- cluserOnes) {
          // Numeric computation of correlation parts for clustering matrix.
          val clusterZeros = matrixSize - clusteringOne
          val clusterMean = clusteringOne / matrixSize
          val clusterDenominator = math.pow(1d - clusterMean, 2) * clusteringOne + math.pow(0d - clusterMean, 2) * clusterZeros

          if(step%100 ==0) print(".")
          val xi = x.map { case (api, external) => (external, clusters.value((step, api))) }

          val xii = xi.cartesian(xi).map { case ((external1, cls1), (external2, cls2)) =>
            (!external1.intersect(external2).isEmpty, cls1 == cls2)
          }

          // Alternative Computation of numerator for correlation.
          //          val numOneOne = xii.filter { case (x, c) => x && c }.count.toDouble
          //          val numZeroZero = xii.filter { case (x, c) => !x && !c }.count.toDouble
          //          val numOneZero = xii.filter { case (x, c) => x && !c }.count.toDouble
          //          val numZeroOne = xii.filter { case (x, c) => !x && c }.count.toDouble
          //
          //          val numerator = numOneOne * (1d - externalMean) * (1d - clusteringMean) +
          //            numZeroZero * (0d - externalMean) * (0d - clusteringMean) +
          //            numOneZero * (1d - externalMean) * (0d - clusteringMean) +
          //            numOneZero * (0d - externalMean) * (1d - clusteringMean)

          val numerator = xii.map {
            case (true, true) => (1d - externalMean) * (1d - clusterMean)
            case (false, false) => (0d - externalMean) * (0d - clusterMean)
            case (true, false) => (1d - externalMean) * (0d - clusterMean)
            case (false, true) => (0d - externalMean) * (1d - clusterMean)
          }.reduce(_ + _)

          val correlation = numerator / math.sqrt(externalDenominator * clusterDenominator)

          correlations = correlations + ("#corr_" + external + "_" + Strings.padStart(step.toString, (Math.log10(thresholds.size) + 1).toInt, '0') -> correlation.toString)
        }
      }

      val result = input ++ correlations ++ Map(
        "configuration_index" -> i.toString,
        "linkage" -> linkage.map(x => x._1 + ";" + x._2 + ";" + x._3 + ";" + x._4).reduceOption(_ + " " + _).getOrElse(""),
        "apis" -> apis.toList.sorted.reduce(_ + ";" + _)
      )

      // TODO: linkage matrix needs to be submitted. This can also be used to populate the web interface.
      results.write(result.asJava)
      results.flush()
    }
  }
}