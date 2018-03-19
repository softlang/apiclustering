package org.softlang.dscor.utils

/**
  * Created by Johannes on 21.06.2017.
  */
object ViewUtils {

  def techTableRow(x: Seq[String]) = x.reduce(_ + " & " + _) + "\\\\ \\hline"

  def csvTableRow(x: Seq[String]) = x.reduce(_ + "," + _)

  def techTable(x: Seq[Seq[String]]): String = x.map(techTableRow(_)).reduce(_ + "\r\n" + _)

  def csvTable(x: Seq[Seq[String]]): String = x.map(csvTableRow(_)).reduce(_ + "\r\n"+ _)

  //def csvTable(x: Seq[Product]): String = csvTable(x.map(x => x.productIterator.map(_.toString).toSeq))

  def mapCsv(content: Seq[Seq[String]], function: (String, Int, Int) => String) = {
    content.zipWithIndex.map { case (x, xindex) => x.zipWithIndex.map { case (y, yindex) => function(y, xindex, yindex) } }
  }

  def csvTableMap(x: Seq[Map[String, String]]): String = {
    csvTable(x.zipWithIndex.flatMap { case (row, index) => row.map { case (column, value) => ((column, index.toString) -> value) } }.toMap)
  }

  def csvTable(values: Map[(String, String), String]): String = {
    val xs = values.keys.map(_._1).toSet.toSeq.sorted
    val ys = values.keys.map(_._2).toSet.toSeq.sorted
    val data = Seq(Seq("keys") ++ xs) ++ ys.toSeq.map(y => Seq(y) ++ xs.toSeq.map(x => values.get((x, y)).getOrElse("undefined").toString))

    csvTable(data)
  }


  def generateHtmlTermlists[T](data: Iterable[(T, Map[String, Double])], n: Int = 20) = {
    // TODO: Add this!
    def getMostFrequentStemming(s: String) = s

    val top = data.toSeq.map { case (id, terms) => (id, terms.toList.sortBy(-_._2).take(n).map(x => getMostFrequentStemming(x._1))) }

    <table border="1" cellpadding="5" cellspacing="5">
      <tr>
        {top.map(x => <th>
        {x._1}
      </th>)}
      </tr>{Range(0, n).map(x => <tr>
      {top.map { case (_, terms) => <td>
        {terms.lift(x).getOrElse("")}
      </td>
      }}
    </tr>)}
    </table>
  }

  def coloredGrid(input: Map[(String, String), Double], dx: Double = 30, dy: Double = 30) = {

    val xs = input.map(_._1._1).toSet.toSeq.sorted.zipWithIndex
    val ys = input.map(_._1._2).toSet.toSeq.sorted.zipWithIndex
    val cartesian = xs.flatMap(x => ys.map(y => (x, y)))

    <svg width={(dx * xs.size).toString} hight={(dy * ys.size).toString}>
      <g stroke-width="0">
        {cartesian.map {
        case ((x, xindex), (y, yindex)) =>

          def value = input((x, y))

          def color = math.min((value * 255).toInt, 255).toString

          def intX = xindex * dx

          def intY = yindex * dy

          <g>
            <title>
              {x + ":" + y + "= " + value.toString}
            </title>
            <rect fill={"rgb(" + color + "," + color + ",255)"} x={intX.toString} y={intY.toString} width={dx.toString} height={dy.toString}/>
          </g>
      }}
      </g>
    </svg>
  }

  def main(args: Array[String]): Unit = {
    val grid = coloredGrid(Map(("a", "a") -> 0.5, ("a", "b") -> 0.2))
    Utils.save("aa.html", grid.toString())
  }
}
