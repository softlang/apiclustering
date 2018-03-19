package org.softlang.dscor.modules.access

import java.io.{ByteArrayInputStream, InputStream}
import java.net.URL

import org.apache.spark.rdd.RDD
import org.softlang.dscor.{Property, RDDModule}
import org.softlang.dscor.utils.InputStreamDelegate
import org.kohsuke.github.GitHub
import org.kohsuke.github.GHRepository
import org.kohsuke.github.GHTreeEntry

import scala.collection.JavaConversions._

/**
  * Created by Johannes on 27.06.2017.
  */
class AccessGit(@Property val repositories: Set[String]) extends Access {

  // TODO: Check if Github api can be called on cluster node.
  override def compute(): RDD[(String, InputStreamDelegate)] = {
    val github = GitHub.connect

    sc.parallelize(repositories.toSeq.flatMap { case repo =>
      github.getRepository(repo)
        .getTreeRecursive("master", 1)
        .getTree.toIterable
        .filter(x => x.getType != "tree")
        .map { x => repo + "/master/" + x.getPath }
        .map { x => x -> new URLInputStreamDelegate("https://raw.githubusercontent.com/" + x) }
    })
  }

  override def backup() = false
}
