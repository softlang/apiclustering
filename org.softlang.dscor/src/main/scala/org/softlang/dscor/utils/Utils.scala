package org.softlang.dscor.utils

import java.io._
import java.net.{HttpURLConnection, URL}
import java.util.Properties

import com.google.common.io.Files
import org.apache.spark.SparkContext
import org.apache.spark.SparkConf
import org.apache.spark.rdd.{PairRDDFunctions, RDD}

import scala.math._
import breeze.linalg._
import org.apache.log4j.Logger
import org.apache.log4j.Level
import java.util.regex.Pattern

import org.apache.spark.mllib.linalg.Vectors
import com.google.common.base.Charsets

import collection.JavaConverters._
import breeze.linalg.{DenseVector => DenseBreezeVector, SparseVector => SparseBreezeVector, Vector => BreezeVector}
import org.apache.commons.io.IOUtils
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.spark.broadcast.Broadcast
import org.apache.pdfbox.text.PDFTextStripper
import org.apache.spark.mllib.linalg.{DenseVector => DenseSparkVector}
import org.apache.spark.mllib.linalg.{SparseVector => SparseSparkVector}
import org.apache.spark.mllib.linalg.{Vector => SparkVector}

import scala.collection.mutable
import org.tartarus.snowball.ext.PorterStemmer

import scala.reflect.ClassTag

object Utils {

  type MVector = Map[String, Double]

  def charset = Charsets.ISO_8859_1

  //val stopwords = Set("a", "about", "above", "after", "again", "against", "all", "am", "an", "and", "any", "are", "aren't", "as", "at", "be", "because", "been", "before", "being", "below", "between", "both", "but", "by", "can't", "cannot", "could", "couldn't", "did", "didn't", "do", "does", "doesn't", "doing", "don't", "down", "during", "each", "few", "for", "from", "further", "had", "hadn't", "has", "hasn't", "have", "haven't", "having", "he", "he'd", "he'll", "he's", "her", "here", "here's", "hers", "herself", "him", "himself", "his", "how", "how's", "i", "i'd", "i'll", "i'm", "i've", "if", "in", "into", "is", "isn't", "it", "it's", "its", "itself", "let's", "me", "more", "most", "mustn't", "my", "myself", "no", "nor", "not", "of", "off", "on", "once", "only", "or", "other", "ought", "our", "ours 	", "ourselves", "out", "over", "own", "same", "shan't", "she", "she'd", "she'll", "she's", "should", "shouldn't", "so", "some", "such", "than", "that", "that's", "the", "their", "theirs", "them", "themselves", "then", "there", "there's", "these", "they", "they'd", "they'll", "they're", "they've", "this", "those", "through", "to", "too", "under", "until", "up", "very", "was", "wasn't", "we", "we'd", "we'll", "we're", "we've", "were", "weren't", "what", "what's", "when", "when's", "where", "where's", "which", "while", "who", "who's", "whom", "why", "why's", "with", "won't", "would", "wouldn't", "you", "you'd", "you'll", "you're", "you've", "your", "yours", "yourself", "yourselves")

  //var SC: SparkContext = null

  // TODO: This is currently not working with double.
  def saveDocuments(location: String, data: Iterable[(String, Map[String, Double])]) = {
    val documents = data.map { case (id, vector) => id + "," + vector.toSeq.flatMap { case (term, number) => List.fill(number.toInt)(term) }.reduce(_ + " " + _) }.reduce(_ + "\n" + _)

    Utils.write(location + ".txt", documents)
  }

  def isEmpty(v: MVector) = v.size == 0 || !v.exists(_._2 != 0d)

  //
  //  def configuration(key: String): Option[String] = {
  //    val properties = new Properties()
  //    properties.load(new FileInputStream("config.properties"))
  //    Option(properties.getProperty(key))
  //  }

  def readUrl(s: String, properties: Map[String, String] = Map()) = {
    val huc = new URL(s).openConnection().asInstanceOf[HttpURLConnection];
    for ((k, v) <- properties) huc.setRequestProperty(k, v)
    huc.setReadTimeout(0)
    huc.setConnectTimeout(0)
    huc.connect()
    val out = IOUtils.toString(huc.getInputStream())
    huc.getResponseMessage
    huc.disconnect()
    out
  }


  var cores = 16

  lazy val sc = {
    val conf = new SparkConf()
      .setAppName("Main")
      .setMaster("local[" + cores + "]")
      .set("spark.driver.memory", "8g")

    val spark = new SparkContext(conf)
    Logger.getLogger("org.apache").setLevel(Level.OFF)
    Logger.getLogger("akka").setLevel(Level.OFF)
    spark
  }

  def readPDF(inputStream: InputStream): String = {
    val pdf = PDDocument.load(inputStream)
    val stripper = new PDFTextStripper
    stripper.getText(pdf)
  }

  def read(path: String): String = read(new File(path))

  def stem(s: Iterable[String]) = {
    val stemmer = new PorterStemmer()
    s.map { x =>
      stemmer.setCurrent(x)
      stemmer.stem()
      stemmer.getCurrent
    }

  }

  def tree(path: String): Set[File] = tree(new File(path))

  def tree(file: File): Set[File] = {
    if (file.isDirectory())
      file.listFiles().flatMap(tree(_)).toSet
    else Set(file)
  }

  def fulltree(file: File): Set[File] = {
    if (file == null || file.listFiles() == null)
      throw new RuntimeException()

    file.listFiles().flatMap(tree(_)).toSet ++ Set(file)
  }

  def countTerms(seq: Iterable[String]): Map[String, Double] = {
    seq.foldLeft(new mutable.HashMap[String, Double]()) {
      (map, term) =>
        map += term -> (map.getOrElse(term, 0d) + 1d)
        map
    }.toMap

  }

  def preciseCosine(a: SparkVector, b: SparkVector) = {
    var left = 0d
    var right = 0d

    a.foreachActive { case (_, value) => left = left + math.pow(value, 2d) }
    b.foreachActive { case (_, value) => right = right + math.pow(value, 2d) }
    val bottom = math.sqrt(right) * math.sqrt(left)

    var top = 0d
    a.foreachActive { case (index, value) => top = top + value * b.apply(index) }

    var res = Math.abs(top / bottom)

    res
  }

  def length[T](x: Map[T, Double]): Double = math.sqrt(x.values.map(x => math.pow(x, 2d)).sum)

  def read(file: File): String = Files.asCharSource(file, charset).read()

  def lastModified(path: String): Long = lastModified(new File(path))

  def lastModified(file: File): Long = fulltree(file).map {
    _.lastModified()
  }.max

  def readLines(file: File): Seq[String] = Files.asCharSource(file, charset).readLines().asScala.toSeq

  def readLines(path: String): Seq[String] = readLines(new File(path))

  def exists(path: String) = new File(path).exists()

  def save(file: File, content: String): Unit = {
    val sink = Files.asCharSink(file, charset)
    sink.write(content)
  }

  def save(path: String, content: String): Unit = {
    save(new File(path), content)
  }

  def readCsv(file: File, separator: String): Seq[Seq[String]] =
    readLines(file).map { case line => line.split(",").toSeq }.toSeq

  def readCsv(path: String, separator: String): Seq[Seq[String]] = readCsv(new File(path), separator)

  def saveLines(path: String, content: Iterable[String]): Unit = saveLines(new File(path), content)

  def saveLines(file: File, content: Iterable[String]): Unit = {
    val sink = Files.asCharSink(file, charset)
    sink.writeLines(content.asJava)
  }

  def delete(path: String): Unit =
    delete(new File(path))

  def delete(file: File): Unit = {
    if (file.exists()) {
      if (file.isDirectory()) file.listFiles().foreach {
        delete(_)
      }
      file.delete()
    }
  }

  def write(path: String, content: String): Unit = write(new File(path), content)

  def write(file: File, content: String): Unit = {
    Files.createParentDirs(file)
    Files.asCharSink(file, charset).write(content)
  }

  def replace(map: Map[String, String]): String => String = x => map.foldRight(x)((l, r) => r.replaceAll(l._1, l._2))

  def insert(regex: String, content: String, offset: Integer): String => String = x => {
    val matcher = Pattern.compile(regex).matcher(x)
    var current = x
    var number = 0
    while (matcher.find()) {
      def position = matcher.start() + offset + number

      current = current.substring(0, position) + content + current.substring(position)
      // TODO: This has become wrong many times without a reason. Always check this!
      number = number + 1
    }
    current
  }

  def countMatch(regex: String, content: String): Int = {
    val matcher = Pattern.compile(regex).matcher(content)
    var count = 0
    while (matcher.find())
      count = count + 1
    count
  }

  def round(x: Double): Double = round(x, 2)

  def round(x: Double, scale: Integer): Double = if (!x.isNaN) BigDecimal(x).setScale(scale, BigDecimal.RoundingMode.HALF_UP).toDouble else x

  def prettyPrint(x: Double, scale: Integer) =
    if (x.isNaN()) "NaN"
    else if (x.toInt.toDouble == x) x.toInt.toString
    else round(x, scale).toString()

  def asBreeze(x: SparkVector): BreezeVector[Double] = x match {
    case x: DenseSparkVector => new DenseBreezeVector[Double](x.values)
    case x: SparseSparkVector => new SparseBreezeVector[Double](x.indices, x.values, x.size)
  }

  def asSpark(x: BreezeVector[Double]): SparkVector = x match {
    case x: DenseBreezeVector[Double] => new DenseSparkVector(x.data)
    case x: SparseBreezeVector[Double] => if (x.index.length == x.used) {
      new SparseSparkVector(x.length, x.index, x.data)
    } else {
      new SparseSparkVector(x.length, x.index.slice(0, x.used), x.data.slice(0, x.used))
    }
  }

  def normalize(vector: SparkVector) = asSpark(breeze.linalg.normalize(asBreeze(vector)))

  def normalize(x: MVector): MVector = {
    val xLength = length(x)
    x.map { case (k, v) => k -> (v / xLength) }
  }

  def add(a: SparkVector, b: SparkVector) = asSpark(asBreeze(a) + asBreeze(b))

  def mult(a: SparkVector, d: Double) = asSpark(asBreeze(a) :* d)

  def multv(a: MVector, b: MVector) = a.keySet.map { t => (t, a(t) * b.getOrElse(t, 0d)) }.toMap

  def add(a: MVector, b: MVector): MVector = (a.keySet.toSet ++ b.keySet.toSet).map(t => (t, a.getOrElse(t, 0d) + b.getOrElse(t, 0d))).toMap

  def difference(a: MVector, b: MVector): MVector = (a.keySet.toSet ++ b.keySet.toSet).map(t => (t, a.getOrElse(t, 0d) - b.getOrElse(t, 0d))).toMap

  def negate(a: MVector): MVector = a.mapValues(-_)

  def dot(a: SparkVector, b: SparkVector): Double = asBreeze(a).dot(asBreeze(b))

  def cosine(a: SparkVector, b: SparkVector) = breeze.linalg.functions.cosineDistance(asBreeze(a), asBreeze(b))

  def cosine(a: MVector, b: MVector): Double = {
    val left = a.map { case (_, value) => math.pow(value, 2d) }.fold(0d)(_ + _)
    val right = b.map { case (_, value) => math.pow(value, 2d) }.fold(0d)(_ + _)
    // toSeq is fucking important :-(((((( !!!
    val index = (a.keySet.intersect(b.keySet)).toSeq

    val top = index.map { term => a(term) * b(term) }.fold(0d)(_ + _)
    val bottom = math.sqrt(right) * math.sqrt(left)

    if (bottom == 0d) return 0d

    val dareal = top / bottom

    val darealreal = 1d - Math.abs(dareal)

    darealreal
  }

  def jaccardCoefficient(a: MVector, b: MVector) = {
    val index = (a.keySet.intersect(b.keySet)).toSeq

    val bothMult = index.map { term => a(term) * b(term) }.fold(0d)(_ + _)
    val squareA = a.map { case (_, value) => math.pow(value, 2d) }.fold(0d)(_ + _)
    val squareB = b.map { case (_, value) => math.pow(value, 2d) }.fold(0d)(_ + _)

    val out = bothMult / (squareA + squareB - bothMult)
    val darealreal = 1d - Math.abs(out)

    darealreal
  }

  def preprocess(x: String, stopwords: Set[String], stemming: Boolean, camelCaseSplitting: Boolean) = {
    val stemmer = new PorterStemmer()

    def stem(x: String) = {
      stemmer.setCurrent(x)
      stemmer.stem()
      stemmer.getCurrent
    }

    val xi = Utils.replace(Map("[^a-zA-Z]" -> " "))(x)
    // Optional Cammel-case
    val xii = if (camelCaseSplitting) Utils.insert("[a-z][A-Z]", " ", 1)(xi) else xi
    val xiii = if (camelCaseSplitting) Utils.insert("[A-Z][A-Z][a-z]", " ", 1)(xii) else xii
    // Lower everything
    val xiiii = xiii.map(_.toLower)

    // Split.
    val t = xiiii.split(" ")
    //val ti = t.filter { x => x != "" && x != " " }
    val ti = t.filter { x => !("".equals(x) || " ".equals(x)) }
    // Optional  Stemming.
    val tii = if (stemming) ti.map(x => stem(x)) else ti
    // Stopwords
    val tiii = tii.filter { x => !stopwords.contains(x) }
    // Filter empty and blank strings.
    tiii.filter { x => !("".equals(x) || " ".equals(x)) }
  }

  def lpnorm(a: MVector, b: MVector, p: Double) = {
    val index = (a.keySet.union(b.keySet)).toSeq

    math.pow(index.map { case i => math.pow(math.abs(a.getOrElse(i, 0d) - b.getOrElse(i, 0d)), p) }.fold(0d)(_ + _), 1d / p)
  }

  def fromVector[T <: Map[String, Int]](in: MVector, index: Broadcast[T]): SparkVector =
    Vectors.sparse(index.value.size, in.toSeq.map { case (k, v) => (index.value(k), v) })

  def fromVector(x: Map[Int, Double]): SparkVector =
    Vectors.dense(x.toSeq.sortBy(_._1).map(_._2).toArray)

  def fromVector[T <: Map[String, Int]](in: MVector, index: T): SparkVector =
    Vectors.sparse(index.size, in.toSeq.map { case (k, v) => (index(k), v) })

  def asMVector(in: SparkVector, invertedIndex: Broadcast[Map[Int, String]]): MVector = {
    val vector = in.toSparse

    val entries = vector.indices.zip(vector.values)

    entries.map { case (i, v) => (invertedIndex.value(i), v) }.toMap
  }

  def asMVector(in: SparkVector, invertedIndex: Map[Int, String]): MVector = {
    val vector = in.toSparse

    val entries = vector.indices.zip(vector.values)

    entries.map { case (i, v) => (invertedIndex(i), v) }.toMap
  }

  def asMVector(in: SparkVector): Map[Int, Double] = {
    val vector = in.toSparse

    val entries = vector.indices.zip(vector.values)

    entries.map { case (i, v) => (i, v) }.toMap
  }

  def splitAtWhiteLine(input: String) = {
    var fragments: Set[(Int, String)] = Set()
    var current = ""

    var i = 0
    for (line <- input.replace("\r", "").split("\n")) {
      i = i + 1
      if (line == "") {
        fragments = fragments ++ Set((i, current))
        current = ""
      } else current = current + "\n" + line
    }

    fragments = fragments ++ Set((i, current))

    fragments.filter {
      _._2 != ""
    }
  }

  // TODO: Check these measure definitions.
  def avg(iterable: Iterable[Double]) = {

    var current = 0d
    for (x <- iterable)
      current = current + x

    current / iterable.size.toDouble

  }

  def max(iterable: Iterable[Double]) = iterable.max

  def min(iterable: Iterable[Double]) = iterable.min

  def sum(iterable: Iterable[Double]) = iterable.fold(0d)(_ + _)

  def variance(iterable: Iterable[Double]) = {
    val average = avg(iterable)
    iterable.map { x => math.pow(x - average, 2d) }.fold(0d)(_ + _)
  }

  def stdev(iterable: Iterable[Double]) = {
    math.pow(variance(iterable), 0.5d)
  }

  def entropy(iterable: Iterable[Double]) = {
    val _sum = sum(iterable)

    -iterable.map { x =>
      val fx = x / _sum
      fx * math.log(fx)
    }.reduce(_ + _) / iterable.size.toDouble
  }

  def pathParents(x: String) = {
    val fragments = (if (x.indexOf("@") > 0) x.substring(0, x.indexOf("@")) else x).split("\\\\")
    val paths = (1 to fragments.length).map(x => (0 until x).map(y => fragments(y)).reduce(_ + "\\" + _))
    paths.toSet
  }

  def faction(iterable: Iterable[String]): Map[String, Double] = {

    val size = iterable.size.toDouble

    val paths = iterable.flatMap(pathParents).toSet

    val factions = paths.map { path =>
      (path, iterable.count {
        _.startsWith(path)
      }.toDouble / size)
    }.toMap

    factions
  }

  def serialize(value: Any, location: String) = {
    Files.createParentDirs(new File(location))

    val fos = new FileOutputStream(location)
    val oos = new ObjectOutputStream(fos)

    oos.writeObject(value)

    fos.close()
    oos.close()
  }

  def deserialize(location: String): Any = {
    val fis = new FileInputStream(location)
    val ois = new ObjectInputStream(fis)

    val obj = ois.readObject()

    fis.close()
    ois.close()

    obj
  }

  def smoothDistribution[T](distribution: Map[T, Double], symbols: Set[T], e: Double) = {
    val distributionWithE = symbols.map { x => x -> (distribution.getOrElse(x, 0d) + e) }
    val sum = distributionWithE.toSeq.map(_._2).reduce(_ + _)
    distributionWithE.map { case (k, v) => k -> (v / sum) }.toMap
  }

  def kl[T](a: Map[T, Double], b: Map[T, Double]) = {
    assert(!a.exists(_._2 == 0d) && Utils.round(a.map(_._2).reduce(_ + _), 3) == 1d)
    assert(!b.exists(_._2 == 0d) && Utils.round(b.map(_._2).reduce(_ + _), 3) == 1d)

    assert(a.keySet == b.keySet)

    val ls = a.keySet.toSeq.map { x => a(x) * math.log(a(x) / b(x)) }
    val rs = b.keySet.toSeq.map { x => b(x) * math.log(b(x) / a(x)) }
    val l = 0.5 * ls.reduce(_ + _)
    val r = 0.5 * rs.reduce(_ + _)

    l + r
  }

  def smoothKl[T](a: Map[T, Double], b: Map[T, Double]) = {
    val keys = a.keySet ++ b.keySet
    val e = math.pow(10d, -3d)

    val ai = smoothDistribution(a, keys, e)
    val bi = smoothDistribution(b, keys, e)

    kl(ai, bi)
  }

  def smoothJs[T](a: Map[T, Double], b: Map[T, Double]) = {
    val keys = a.keySet ++ b.keySet
    val e = math.pow(10d, -3d)

    val ai = smoothDistribution(a, keys, e)
    val bi = smoothDistribution(b, keys, e)

    js(ai, bi)
  }

  def js[T](a: Map[T, Double], b: Map[T, Double]) = {
    assert(!a.exists(_._2 == 0d) && Utils.round(a.map(_._2).reduce(_ + _), 3) == 1d)
    assert(!b.exists(_._2 == 0d) && Utils.round(b.map(_._2).reduce(_ + _), 3) == 1d)

    val l = 0.5 * (a.keySet ++ b.keySet).toSeq.map { x => a(x) * math.log(a(x) / (0.5d * (a(x) + b(x)))) }.reduce(_ + _)
    val r = 0.5 * (a.keySet ++ b.keySet).toSeq.map { x => b(x) * math.log(b(x) / (0.5d * (a(x) + b(x)))) }.reduce(_ + _)

    l + r
  }

  def vsm(documents: Iterable[(String, Map[String, Double])], ltf: Boolean = true) = {
    val invertedDocumentFrequency = idf(documents.map(_._2))

    def weight(v: Map[String, Double]) = if (ltf) weightLtfIdf(v, invertedDocumentFrequency) else weightIdf(v, invertedDocumentFrequency)

    documents.map { case (k, v) => k -> weight(v) }
  }

  def weightLtfIdf(vector: Map[String, Double], idf: Map[String, Double]) = vector.map { case (k, v) => k -> scala.math.log(1 + v) * scala.math.log(idf(k)) }

  def weightIdf(vector: Map[String, Double], idf: Map[String, Double]) = vector.map { case (k, v) => k -> v * scala.math.log(idf(k)) }

  def idf(documents: Iterable[Map[String, Double]]) = {
    val size = documents.size.toDouble
    df(documents).map { case (k, v) =>
      k -> (size / v.toDouble)
    }
  }

  def df(documents: Iterable[Map[String, Double]]) = {
    documents
      .flatMap(_.keySet)
      .groupBy { x => x }
      .map { x => x._1 -> x._2.size }
  }

  def index(documents: RDD[Map[String, Double]]) = {
    documents.flatMap(_.keySet.toSet).aggregate(Set[String]())(_ ++ Set(_), _ ++ _).toSeq.zipWithIndex.toMap
  }

  def index(documents: Iterable[Map[String, Double]]) = {
    documents.flatMap(_.keySet.toSet).aggregate(Set[String]())(_ ++ Set(_), _ ++ _).toSeq.zipWithIndex.toMap
  }

  def name(vector: MVector, take: Integer, r: Integer) = vector.toSeq.sortBy(-_._2).take(take).map { case (k, v) => if (r == 0) k else (if (r == 1) v.toInt.toString() else round(v, r).toString()) + ":" + k }.reduce(_ + " " + _)

  def cumsum[T](rdd: RDD[T])(value: T => Double): RDD[(T, Double)] = {

    val partials = rdd.map(x => x -> value(x)).mapPartitionsWithIndex((i, iter) => {
      val (keys, values) = iter.toSeq.unzip
      val sums = values.scanLeft(0.0)(_ + _)
      Iterator((keys.zip(sums.tail), sums.last))
    })

    // Collect partials sums

    val partialSums = partials.values.collect

    // Compute cumulative sum over partitions and broadcast it:

    val sumMap = sc.broadcast(
      (0 until rdd.partitions.size)
        .zip(partialSums.scanLeft(0.0)(_ + _))
        .toMap
    )

    // Compute final results:

    partials.keys.mapPartitionsWithIndex((i, iter) => {
      val offset = sumMap.value(i)
      if (iter.isEmpty) Iterator()
      else iter.next.map { case (k, v) => (k, v + offset) }.toIterator
    })
  }

  def cummax[T](rdd: RDD[T])(value: T => Double): RDD[(T, Double)] = {

    val partials = rdd.map(x => x -> value(x)).mapPartitionsWithIndex((i, iter) => {
      val (keys, values) = iter.toSeq.unzip
      val sums = values.scanLeft(0.0)(math.max(_, _))
      Iterator((keys.zip(sums.tail), sums.last))
    })

    // Collect partials sums

    val partialMaxs = partials.values.collect

    // Compute cumulative sum over partitions and broadcast it:

    val maxMap = sc.broadcast(
      (0 until rdd.partitions.size)
        .zip(partialMaxs.scanLeft(0.0)(math.max(_, _)))
        .toMap
    )

    // Compute final results:

    partials.keys.mapPartitionsWithIndex((i, iter) => {
      val baseMax = maxMap.value(i)
      if (iter.isEmpty) Iterator()
      else iter.next.map { case (k, v) => (k, math.max(v, baseMax)) }.toIterator
    })
  }

  /**
    * This is not checked.
    */
  def corr2[K](as: RDD[(K, Double)], bs: RDD[(K, Double)])
              (implicit kt: ClassTag[K], ord: Ordering[K] = null) = {

    val asize = as.count().toDouble
    val bsize = bs.count().toDouble

    val asum = as.map(_._2).sum()
    val bsum = bs.map(_._2).sum()

    val aMean = asum / asize
    val bMean = bsum / bsize

    val t = as.join(bs).map { case (_, (a, b)) => (a - aMean) * (b - bMean) }.sum
    val b1 = as.map { case (_, a) => math.pow(a - aMean, 2d) }.sum()
    val b2 = bs.map { case (_, b) => math.pow(b - bMean, 2d) }.sum()

    val result = t / math.pow((b1 * b2), 0.5d)
    result
  }


  def equalCells[K](as: RDD[(K, Double)], bs: RDD[(K, Double)])
                   (implicit kt: ClassTag[K], ord: Ordering[K] = null) = {

    as.join(bs).map { case (_, (a, b)) => if (a == b) 1d else 0d }.sum
  }

  def incidenceMatrix[T](groups: Set[Set[T]]) =
    groups.flatten.flatMap(x => groups.flatten.map(y => (x, y) -> groups.exists(c => c.contains(x) && c.contains(y)))).toMap


  //    val partials = rdd.mapPartitionsWithIndex((i, iter) => {
  //      val sums  = iter.map(value(_)).scanLeft(0.0)(_ + _)
  //      Iterator(iter.zip(sums))
  //    })
  //
  //    // Collect partials sums
  //
  //    val partialSums = partials.values.collect
  //
  //    // Compute cumulative sum over partitions and broadcast it:
  //
  //    val sumMap = sc.broadcast(
  //      (0 until rdd.partitions.size)
  //        .zip(partialSums.scanLeft(0)(_ + _))
  //        .toMap
  //    )
  //
  //    // Compute final results:
  //
  //    val result = partials.keys.mapPartitionsWithIndex((i, iter) => {
  //      val offset = sumMap.value(i)
  //      if (iter.isEmpty) Iterator()
  //      else iter.next.map{case (k, v) => (k, v + offset)}.toIterator
  //    })


  //
  //  def cohesion[T](clusters: Set[Set[T]], distance: (T, T) => Double) = 
  //    Utils.avg(clusters.map { cluster => cluster.flatMap { a => cluster.collect { case b if (a != b) => distance(a, b) } }.max })
  //  
  //  def separation[T](clusters: Set[Set[T]], distance: (T, T) => Double) = 
  //    Utils.avg(clusters.map { as => clusters.collect{case bs if bs != as => } })
}