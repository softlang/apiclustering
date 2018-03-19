package org.softlang.dscor.modules

import org.softlang.dscor.Module
import org.softlang.dscor._
import org.softlang.dscor.utils.{Utils, ViewUtils}
import org.apache.spark.mllib.clustering.DistributedLDAModel
import org.apache.spark.mllib.clustering.LocalLDAModel
import org.apache.spark.rdd.RDD
import org.softlang.dscor.macros.Value


class CleanLDA[T](
                   @Dependency val source: Documents[T] = null,
                   @Property val runid: Int = 0,
                   @Property val k: Int = 30,
                   @Property val beta: Double = 0.1,
                   @Property val alpha: Double = 0.1,
                   @Property val optimizer: String = "em",
                   @Property val iterations: Int = 200) extends Module with Documents[T] {

  @Value
  def index(): Map[String, Int] = Utils.index(source.data().map(_._2))

  // Derived Value.
  lazy val invertedIndex = index().map(_.swap).toMap

  lazy val ids = source.data().zipWithIndex().collect().map { case ((id, v), n) => n -> id }.toMap

  def input() = {
    val _index = Utils.sc.broadcast(index())

    source.data().zipWithIndex().map { case ((_, v), n) => (n, Utils.fromVector(v, _index)) }
  }

  override def data(): RDD[(T, Map[String, Double])] =
    Utils.sc.parallelize(topicDistribution().map { case (k1, v1) => k1 -> v1.map { case (k, v) => k.toString -> v } }.toSeq)

  @Value
  def model(): LocalLDAModel = {
    if (optimizer == "em") new org.apache.spark.mllib.clustering.LDA().setOptimizer(optimizer).setK(k).setBeta(beta + 1.0d).setAlpha(alpha + 1.0d).setMaxIterations(iterations).run(input()).asInstanceOf[DistributedLDAModel].toLocal
    else if (optimizer == "online") new org.apache.spark.mllib.clustering.LDA().setOptimizer(optimizer).setK(k).setBeta(beta).setAlpha(alpha).setMaxIterations(iterations).run(input()).asInstanceOf[LocalLDAModel]
    else throw new RuntimeException("optimizer not found")
  }

  def generateTopicHtmlTermlists(n: Int = 20) = ViewUtils.generateHtmlTermlists(topics.map { case (k, v) => k.toString -> v }, n)

  /**
    * Returns the each topics term distribution. TODO: Make this value.
    */
  lazy val topics: Map[Int, Map[String, Double]] = model().describeTopics().map { case (i, v) => i.map { x => invertedIndex(x) }.zip(v).toMap }.zipWithIndex.map { case (terms, id) => id -> terms }.toMap

  /**
    * Topic distribution for the documents.
    */
  @Value
  def topicDistribution(): Map[T, Map[Int, Double]] = model().topicDistributions(input).collect().map {
    case (id, distribution) => ids(id) -> Range(0, k).map { x => x -> distribution(x) }.toMap
  }.toMap
}