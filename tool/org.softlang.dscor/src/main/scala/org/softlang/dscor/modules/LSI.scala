package org.softlang.dscor.modules

import org.softlang.dscor._
import org.softlang.dscor.utils.Utils
import org.apache.spark.rdd.RDD
import org.apache.spark.mllib.linalg.distributed.RowMatrix
import org.softlang.dscor.macros.Value

import scala.reflect.ClassTag

class LSI[T: ClassTag](
           @Dependency val source: VSM[T],
           @Property val dimension: Int = 200) extends RDDModule[(T, Map[String, Double])] with Documents[T]{

  override def compute(): RDD[(T, Map[String, Double])] = {
    val index = Utils.sc.broadcast(Utils.index(source.data().map(_._2)))

    val vectors = source.data().map { case (id, v) => (id, Utils.fromVector(v, index)) }
    val mat = new RowMatrix(vectors.map(_._2))

    val svd = mat.computeSVD(dimension, false)
    val s = svd.s
    val V = svd.V

    val projected = mat.multiply(V).rows

    //val invertedPCIndex = sc.broadcast((0 to V.numCols).map(x => (x, "dimension" + x.toString())).toMap)

    vectors.map(_._1).zip(projected.map { x => Utils.asMVector(x).map{case (k,v) => k.toString -> v} })
  }
}