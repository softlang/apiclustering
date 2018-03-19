package org.softlang.dscor.featuremodel

import org.softlang.dscor.utils.{SHierarchicalClustering, Tree}


/**
  * Created by Johannes on 17.10.2017.
  */
sealed trait Clustering {
  def execute(vectorSimilarity: Map[(String, String), Double]): (Tree[String], Seq[(Int, Int, Double, Int)])


  def executeWithSchema(vectorSimilarity: Map[(String, String), Double], schema: String): (Tree[String], Seq[(Int, Int, Double, Int)]) = {
    val elements = vectorSimilarity.flatMap { case ((k1, k2), _) => Seq(k1, k2) }.toSet.toList.sorted

    val hc = new SHierarchicalClustering[String](elements, { case x => vectorSimilarity(x) }, schema)
    hc.compute()
    (hc.tree(), hc.linkageMatrix)
  }
}

final case class ClusteringAverage() extends Clustering {
  def execute(vectorSimilarity: Map[(String, String), Double]) = executeWithSchema(vectorSimilarity, "average")
}

final case class ClusteringSingle() extends Clustering {
  def execute(vectorSimilarity: Map[(String, String), Double]) = executeWithSchema(vectorSimilarity, "single")
}

final case class ClusteringComplete() extends Clustering {
  def execute(vectorSimilarity: Map[(String, String), Double]) = executeWithSchema(vectorSimilarity, "complete")
}
