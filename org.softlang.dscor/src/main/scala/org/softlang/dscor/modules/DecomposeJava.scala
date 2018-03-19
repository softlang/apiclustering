package org.softlang.dscor.modules

import java.io.{IOException, InputStream}

import com.google.common.io.Files
import org.apache.spark.rdd.RDD

import scala.collection.JavaConverters._
import org.softlang.dscor.utils.XMLLocationLoader
import org.apache.commons.io.IOUtils
import com.google.inject.name.Named
import japa.parser.{ASTParser, JavaParser, ParseException, SourcesHelper}
import japa.parser.ast.CompilationUnit
import japa.parser.ast.visitor.{GenericVisitorAdapter, VoidVisitor}
import org.softlang.dscor._
import org.softlang.dscor.modules.access.Access
import org.softlang.dscor.utils.JavaParsers

class DecomposeJava(
                     @Dependency access: Access,
                     @Property val encoding: String,
                     @Property val granularity: String) extends Decompose(access) {

  override def compute(): RDD[(String, String)] = {
    val _encoding = encoding
    val _granularity = granularity
    access.data()
      .map { case (uri, isd) => (uri, Files.getFileExtension(uri), _granularity, isd) }
      .collect {
        case (uri, "java", "id", isd) => uri + "@" + "impl" -> IOUtils.toString(isd.delegate)
        // TODO: Add counting of ParseExceptions
        case (uri, "java", "implementation", isd) => uri + "@" + "impl" -> JavaParsers.implementation(isd.delegate, _encoding).asScala.filter(!_.isEmpty).fold("")(_ + " " + _)
        case (uri, "java", "documentation", isd) => uri + "@" + "doc" -> JavaParsers.documentation(isd.delegate, _encoding).asScala.filter(!_.isEmpty).fold("")(_ + " " + _)
      }
  }

  override def backup() = false
}
