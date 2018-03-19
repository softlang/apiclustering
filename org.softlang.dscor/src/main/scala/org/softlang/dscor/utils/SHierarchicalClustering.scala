package org.softlang.dscor.utils

import scala.collection.{mutable => mutable}

object SHierarchicalClustering {

  def compute[T](index: Iterable[T])(function: (T, T) => Double, scheme: String = "average"): Tree[T] = {
    val hc = new SHierarchicalClustering(index.toList, function, scheme)
    hc.compute()
    hc.tree
  }

  def linkageMatrix[T](index: Iterable[T])(function: (T, T) => Double, scheme: String = "average"): Seq[(Int, Int, Double, Int)] = {
    val hc = new SHierarchicalClustering(index.toList, function, scheme)
    hc.compute()
    hc.linkageMatrix
  }
}

class SHierarchicalClustering[T](val index: List[T], val function: (T, T) => Double, val scheme: String = "average") {

  val mapping: mutable.HashMap[Int, Int] = mutable.HashMap()
  val unused = mutable.ArrayStack[Int]()
  val size = mutable.HashMap() ++= (0 to index.size - 1).map((_, 1))
  var last = index.size - 1

  private val ordered = new mutable.PriorityQueue[(Double, Int, Int)]()(Ordering[(Double, Int, Int)].reverse)

  val matrix: Array[Array[Double]] = new Array(index.size)

  def update(d: Double, ai: Int, bi: Int) {
    val a = mapping(ai)
    val b = mapping(bi)

    if (a > b) matrix(a).update(b, d)
    if (a < b) matrix(b).update(a, d)
  }

  def free(i: Int) {
    unused.push(mapping(i))
    mapping.remove(i)
  }

  def occupy(i: Int) = {
    mapping.put(i, unused.pop())
  }

  def next(): (Double, Int, Int) = {
    while (true) {
      val (d, a, b) = ordered.dequeue()

      if (mapping.contains(a) && mapping.contains(b))
        return (d, a, b)
    }
    throw new RuntimeException()
  }

  def dissimilarity(ai: Int, bi: Int): Double = {
    val a = mapping(ai)
    val b = mapping(bi)

    if (a > b) matrix(a)(b)
    else if (a < b) matrix(b)(a)
    else throw new RuntimeException();
  }

  val linkageMatrix: mutable.ArrayStack[(Int, Int, Double, Int)] = new mutable.ArrayStack()

  def compute() = {

    // Initialize dissimilarity array.
    for (x <- 0 to index.size - 1) {
      mapping.put(x, x)

      val array = new Array[Double](x)
      for (y <- 0 to x - 1) {
        val dissimilarity = function(index(x), index(y))
        ordered.enqueue((dissimilarity, x, y))
        array.update(y, dissimilarity)
      }

      matrix.update(x, array)
    }

    for (i <- 0 to index.size - 2) {
      val (d, a, b) = next()

      // Append to result
      linkageMatrix.push((a, b, d, size(a) + size(b)))

      val updates = mapping.keySet.filter(x => x != a && x != b).map { x =>
        val dax = dissimilarity(a, x)
        val dbx = dissimilarity(b, x)
        val dab = dissimilarity(a, b)
        val d = computeUpdate(dax, dbx, dab, size(a), size(b), size(x))

        (d, x)
      }

      free(a)
      free(b)
      last = last + 1
      occupy(last)

      for ((d, x) <- updates) {
        update(d, last, x)
        ordered.enqueue((d, last, x))
      }

      size.put(last, size(a) + size(b))
    }
  }

  private def computeUpdate(dax: Double, dbx: Double, dab: Double, sizea: Int, sizeb: Int, sizex: Int) = scheme match {
    // Average update formula.
    case "average" => (sizea.doubleValue() * dax + sizeb.doubleValue() * dbx) / (sizea.doubleValue() + sizeb.doubleValue())
    case "single" => math.min(dax, dbx)
    case "complete" => math.max(dax, dbx)
    case "weighted" => (dax + dbx) / 2d
  }

  // API.
  val root: Int = index.size * 2 - 2

  def parentOf(id: Int): Int = {
    linkageMatrix.indexWhere(x => x._1 == id || x._2 == id) + index.size
  }

  def childrenOf(id: Int): (Int, Int) = {
    if (id < index.size)
      null
    else {
      val x = (index.size - 2) - (id - index.size)
      val children = (linkageMatrix(x)._1, linkageMatrix(x)._2)
      children
    }
  }

  def allChildrenOf(id: Int): Set[Int] = childrenOf(id) match {
    case (x, y) => Set(id) ++ allChildrenOf(x) ++ allChildrenOf(y)
    case null => Set(id)
  }

  def allLeafsOf(id: Int): Set[Int] = childrenOf(id) match {
    case (x, y) => allLeafsOf(x) ++ allLeafsOf(y)
    case null => Set(id)
  }

  def sizeOf(id: Int) = size(id)

  /**
    * Content can not be represented as a Set because of equal document vectors.
    */
  def contentOf(id: Int): Seq[T] = childrenOf(id) match {
    case (x, y) => contentOf(x) ++ contentOf(y)
    case null => Seq(index(id))
  }

  /**
    * Traverses the leafs of the tree (big subtrees first).
    */
  def traverse(id: Int): List[Int] = childrenOf(id) match {
    case (x, y) if size(x) > size(y) => traverse(x) ++ traverse(y)
    case (x, y) if size(x) <= size(y) => traverse(y) ++ traverse(x)
    case null => List(id)
  }

  def dissimilarityOf(id: Int) = {
    if (id < index.size)
      0d
    else {
      val x = (index.size - 2) - (id - index.size)
      val dis = linkageMatrix(x)._3

      dis
    }
  }

  def thresholdChildrenOf(id: Int, threshold: Double): Set[Int] = {
    if (dissimilarityOf(id) <= threshold)
      Set(id)
    else {
      val (l, r) = childrenOf(id)
      thresholdChildrenOf(l, threshold) ++ thresholdChildrenOf(r, threshold)
    }
  }

  def tree(): Tree[T] = tree(root)

  def tree(id: Int): Tree[T] = childrenOf(id) match {
    case null => Leaf(contentOf(id).head)
    case (l, r) if sizeOf(l) > sizeOf(r) => Node(tree(l), tree(r), dissimilarityOf(id))
    case (l, r) if sizeOf(l) <= sizeOf(r) => Node(tree(r), tree(l), dissimilarityOf(id))
  }
}

trait Tree[T] {
  def map[U](f: T => U): Tree[U]

  def flatten(): Seq[T]

  def connected(a: T, b: T): Double

  def thresholds(): Set[Double]

  def isChild(t: T): Boolean

  def flatten(threshold: Double): Seq[Seq[T]]
}

case class Node[T](left: Tree[T], right: Tree[T], similarity: Double) extends Tree[T] {
  override def map[U](f: T => U): Tree[U] = Node(right.map(f), left.map(f), similarity)

  override def connected(a: T, b: T): Double =
    math.min((if (isChild(a) && isChild(b)) similarity else 1d),
      math.min(left.connected(a, b), right.connected(a, b)))

  override def flatten(): Seq[T] = left.flatten() ++ right.flatten()

  override def isChild(t: T): Boolean = left.isChild(t) || right.isChild(t)

  override def flatten(threshold: Double): Seq[Seq[T]] =
    if (similarity <= threshold) Seq(flatten()) else left.flatten(threshold) ++ right.flatten(threshold)

  override def thresholds(): Set[Double] = left.thresholds() ++ Seq(similarity) ++ right.thresholds()
}

case class Leaf[T](t: T) extends Tree[T] {
  override def map[U](f: T => U): Tree[U] = Leaf(f(t))

  override def connected(a: T, b: T): Double = 1d

  override def flatten(): Seq[T] = Seq(t)

  override def isChild(t: T): Boolean = t == this.t

  override def flatten(threshold: Double) = Seq(Seq(t))

  override def thresholds(): Set[Double] = Set()
}
