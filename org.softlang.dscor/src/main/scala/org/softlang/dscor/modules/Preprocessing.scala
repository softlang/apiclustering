package org.softlang.dscor.modules

import org.softlang.dscor._
import org.softlang.dscor.utils.Utils
import org.apache.spark.rdd.RDD
import org.tartarus.snowball.ext.PorterStemmer
import java.io.File

import scala.io.Source

class Preprocessing(@Dependency val input: RDDModule[(String, String)],
                    @Property val stopwords: Set[String] = Utils.read("src/main/resources/stopwordsmallet.txt").split("\r\n").toSet,
                    @Property val stemming: Boolean = true,
                    @Property val camelCaseSplitting: Boolean = true
                   ) extends RDDModule[(String, Map[String, Double])] with Documents[String] {

  override def compute(): RDD[(String, Map[String, Double])] = {
    val _camelCaseSplitting = camelCaseSplitting
    val _stemming = stemming

    val preprocessedStopwords = stopwords.flatMap(x => Utils.preprocess(x, Set(), _stemming, _camelCaseSplitting))

    input.data().map { case (uri, x) => uri -> Utils.countTerms(Utils.preprocess(x, preprocessedStopwords, _stemming, _camelCaseSplitting)) }.filter(_._2.size > 0)
      // TODO: Insert computation modes (distributed/local).
      .cache() //.sortBy(_._1)
  }

  override def backup() = false

  // Override load and save to get readable format that can be used by other programs. TODO: This can be done for Document trait considering Double.
  override def save(location: String, data: Any): Unit = {
    val documents = data.asInstanceOf[RDD[(String, Map[String, Double])]].map { case (id, vector) => id + "," + vector.toSeq.flatMap { case (term, number) => List.fill(number.toInt)(term) }.reduce(_ + " " + _) }.reduce(_ + "\n" + _)

    Utils.write(location + ".txt", documents)
  }

  override def load(location: String): Any = {
    Utils.sc.parallelize(Source.fromFile(location + ".txt").getLines().toSeq).map(x => x.split(",")).map(arr => arr(0) -> Utils.countTerms(arr(1).split(" ")))
  }
}