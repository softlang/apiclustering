package org.softlang.dscor.featuremodel

import org.softlang.dscor.utils.ReflectionUtils

import scala.reflect.runtime.universe._

/**
  * Created by Johannes on 16.10.2017.
  */
object FeatureModel {

  //  def cantor(k1: Long, k2: Long) =
  //    ((k1+k2)*(k1+k2+1))/2 + k2
  //
  //  def icantor(z: Long): (Long,Long) = {
  //    val w = math.floor((math.sqrt(8*z + 1) - 1d)/2d)
  //    val t = (math.pow(w,2) + w) / 2d
  //
  //    null
  //  }

  def cantorX(z: Long): Long = {
    val j = Math.floor(Math.sqrt(0.25 + 2 * z) - 0.5).toLong
    j - (z - j * (j + 1) / 2)
  }

  def cantorY(z: Long): Long = {
    val j = Math.floor(Math.sqrt(0.25 + 2 * z) - 0.5).toLong
    z - j * (j + 1) / 2
  }

  //  def fix1(z: Long, length: Long): Long = z % length
  //
  //  def fix2(z: Long, length: Long): Long = z / length

  //  def fixRightX(z: Long, rightLength: Long): Long = z / rightLength
  //
  //  def fixRightY(z: Long, rightLength: Long): Long = z % rightLength

  def length[Root: TypeTag]: Long = {
    length(typeOf[Root])
  }

  def length(tpe: Type): Long = {
    if (ReflectionUtils.isSealed(tpe)) ReflectionUtils.sealedDescendants(tpe).map { case c => length(c.asType.toType) }.reduce(_ + _)
    else ReflectionUtils.classAccessors(tpe).map { case c => length(c.typeSignature) }.fold(1l)(_ * _)
  }

  def enumerate[Root: TypeTag](z: Long): Root = {
    enumerate(typeOf[Root], z).asInstanceOf[Root]
  }

  def enumerate(tpe: Type, z: Long): AnyRef = {
    if (ReflectionUtils.isSealed(tpe)) {
      // Alternative. This needs to be sorted since order depends on some internal svm constraints.
      val clss = ReflectionUtils.sealedDescendants(tpe).sortBy(_.fullName)
      val l = clss.length
      val cls = clss((z % l).toInt).typeSignature
      enumerate(cls, z / l)
    } else {
      // Product.
      val clss = ReflectionUtils.classAccessors(tpe)
      val lengths = clss.map(x => length(x.typeSignature))
      val rights = lengths.scanLeft(z) { case (z, l) => z / l }
      val lefts = lengths.zip(rights).map { case (l, r) => r % l }
      val args = lefts.zip(clss).map { case (z, cls) => enumerate(cls.typeSignature, z) }
      ReflectionUtils.createInstance(tpe, args.toArray: _*)
    }
  }

  def flat(instance: Product): Map[String, String] = instance match {
    case product: Product =>
      ReflectionUtils.members(product).map {
        case (key, value: Product) => key -> value.getClass.getSimpleName
        case (key, value: String) => key -> value
        case (key, value: Int) => key -> value.toString
      } ++
        ReflectionUtils.members(product).flatMap {
          case (key, value: Product) => flat(value).map { case (nkey, nvalue) => key + "." + nkey -> nvalue }
          case _ => Map[String,String]()
        }
  }

  def main(args: Array[String]): Unit = {
    //val members = ReflectionUtils.sealedDescendants[APIClustering].get

    //println(enumerate()[APIClustering])

    println(length[APIClustering])
    println(flat(enumerate[APIClustering](0)))
    println(flat(enumerate[APIClustering](1296)))

  }
}
