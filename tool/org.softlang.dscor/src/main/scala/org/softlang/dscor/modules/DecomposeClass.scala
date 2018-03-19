package org.softlang.dscor.modules

import java.io.{ByteArrayInputStream, FileInputStream, InputStream}
import java.nio.charset.Charset

import collection.JavaConverters._
import jdk.internal.org.objectweb.asm.{ClassReader, Opcodes}
import jdk.internal.org.objectweb.asm.tree.ClassNode
import org.softlang.dscor.RDDModule
import org.softlang.dscor.utils.InputStreamDelegate


//import org.apache.bcel.classfile.ClassParser
import org.apache.commons.io.IOUtils
import org.apache.spark.rdd.RDD
import org.softlang.dscor.modules.access.Access
import org.softlang.dscor.{Dependency, Module, Property}


/**
  * Created by Johannes on 20.06.2017.
  */
class DecomposeClass(@Dependency access: RDDModule[(String, InputStreamDelegate)], @Property val encoding: String, @Property visibility: String = "all") extends Decompose(access) {

  override def compute(): RDD[(String, String)] = {

    val _visibility = visibility

    access
      .data()
      .filter(_._1.endsWith(".class"))
      .flatMap {
        case (uri, content) =>

          val api = uri.split("#")(0)
          val node: ClassNode = new ClassNode()
          val cr: ClassReader = new ClassReader(content.delegate)

          cr.accept(node, 0)

          def isPublic(v: Int) = (v & Opcodes.ACC_PUBLIC) == Opcodes.ACC_PUBLIC

          def isProtected(v: Int) = (v & Opcodes.ACC_PROTECTED) == Opcodes.ACC_PROTECTED

          def isPrivate(v: Int) = (v & Opcodes.ACC_PRIVATE) == Opcodes.ACC_PRIVATE

          val classname = node.name.replaceAll("/", ".")
          val lastDotIndex = classname.lastIndexOf(".")
          val simpleName = if (lastDotIndex == -1) classname else classname.substring(lastDotIndex + 1)
          val publicClass = isPublic(node.access)


          val publicMethods = node.methods.asScala.filter(x => isPublic(x.access)).map(_.name)
          val protectedMethods = node.methods.asScala.filter(x => isProtected(x.access)).map(_.name)
          val privateMethods = node.methods.asScala.filter(x => isPrivate(x.access)).map(_.name)
          val allMethods = node.methods.asScala.map(_.name)

          def prepare(content: Seq[String]) = if (content.isEmpty) Map() else Map(api + "#" + classname -> content.reduce(_ + " " + _))

          val result = _visibility match {
            case "all" => prepare(Seq(simpleName) ++ allMethods)
            case "visible" => prepare(if (publicClass) Seq(simpleName) ++ publicMethods ++ protectedMethods else Seq())
          }

          result
      }
  }

  override def backup() = false
}


//          val cp = new ClassParser(content.delegate, "")
//          val jc = cp.parse
//
//          val className = jc.getClassName
//          val simpleName = if (className.lastIndexOf(".") == -1) className else className.substring(className.lastIndexOf(".") + 1, className.length)


//          visibility match {
//            case "all" => {
////              val entries = List(simpleName) ++
////                jc.getAttributes.toSeq.map(_.getName) ++
////                jc.getMethods.toSeq.map(_.getName)
//              Seq(className -> Seq().fold("")(_ + " " + _))
//            }
//}
//object DecomposeClass {
//  def main(args: Array[String]): Unit = {
//    val node: ClassNode = new ClassNode()
//    val cr: ClassReader = new ClassReader(new FileInputStream("C:\\Data\\Corpus\\APIClustering\\sample\\WizardDialog.class"))
//
//    cr.accept(node, 0)
//
//    val classname = node.name
//    val publicClass = isPublic(node.access)
//
//    val publicMethods = node.methods.asScala.filter(x => isPublic(x.access)).map(_.name)
//    val protectedMethods = node.methods.asScala.filter(x => isProtected(x.access)).map(_.name)
//    val privateMethods = node.methods.asScala.filter(x => isPrivate(x.access)).map(_.name)
//
//    println("")
//
//
//  }
//
//  def isPublic(v: Int) = (v & Opcodes.ACC_PUBLIC) == Opcodes.ACC_PUBLIC
//
//  def isProtected(v: Int) = (v & Opcodes.ACC_PROTECTED) == Opcodes.ACC_PROTECTED
//
//  def isPrivate(v: Int) = (v & Opcodes.ACC_PRIVATE) == Opcodes.ACC_PRIVATE
//}
