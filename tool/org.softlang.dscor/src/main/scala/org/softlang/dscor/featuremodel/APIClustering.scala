package org.softlang.dscor.featuremodel

import java.io.File

import org.softlang.dscor.modules.Blanks
import org.softlang.dscor.modules.access.{AccessJAR, AccessMavenJAR}
import org.softlang.dscor.utils.Utils

object APIClustering {
  //def apiClusteringCorpus = Utils.configuration("copusFolder").get + "/apiclustering"

  val stopwords = Utils.read("src/main/resources/stopwordsmallet.txt").split("\r\n").toSet ++
    Utils.read("src/main/resources/java_stopwords.txt").split("\r\n").toSet

  //val csv = SUtils.readCsv(new File("C:/Data/Corpus/APIBaseline/Baseline.csv"))

  //val categories = csv.map { case row => row("groupId") + ":" + row("artifactId") + ":" + row("version") -> row("category") }.toMap

  //val coordinates = csv.map { case row => row("groupId") + ":" + row("artifactId") + ":" + row("version") }

  // val sameCategory = categories.toSeq.groupBy(_._2).map { case (category, group) => group.map(_._1).toSet }.toSet

}

/**
  * Created by Johannes on 16.10.2017.
  */
case class APIClustering(source: Source,
                         featureSelection: FeatureSelection,
                         normalization: Normalization,
                         sampling: Sampling,
                         granularity: Granularity,
                         analytical: Analytical,
                         similarity: Similarity,
                         clustering: Clustering) {

  def mouduleSource = source.compute()

  def moduleFeatureSelection = featureSelection.execute(mouduleSource)

  def moduleNormalization = normalization.execute(moduleFeatureSelection)

  def moduleIdentified = Blanks.source(moduleNormalization).documents {
    case (documents) => documents.data().map {
      case (uri, content) => {
        val api = uri.split("#")(0)
        val packageAndClass = uri.split("#")(1)
        val pkg = if (packageAndClass.contains(".")) packageAndClass.substring(0, packageAndClass.lastIndexOf(".")) else "default"
        val cls = if (packageAndClass.contains(".")) packageAndClass.substring(packageAndClass.lastIndexOf(".") + 1, packageAndClass.length) else packageAndClass

        //        var api = apiMapping.find(x => uri.matches(x._1)).map(_._2).getOrElse("default")
        //        var pkg = if (uri.contains(".")) uri.substring(0, uri.lastIndexOf(".")) else "default"
        //        var cls = if (uri.contains(".")) uri.substring(uri.lastIndexOf(".") + 1, uri.length) else uri

        (api, pkg, cls) -> content
      }
    }.filter { case ((api, pkg, cls), content) => (api != "default" && pkg != "default") }
  }

  def moduleSampling = sampling.execute(moduleIdentified)

  def moduleGranularity = granularity.execute(moduleSampling)

  def moduleAnalytical = analytical.execute(moduleGranularity)

  def dataSimilarity = similarity.execute(moduleAnalytical)

  def dataClustering = clustering.execute(dataSimilarity)

}
