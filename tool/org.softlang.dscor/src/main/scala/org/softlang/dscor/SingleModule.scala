package org.softlang.dscor

import org.softlang.dscor.utils.Utils
import scala.reflect.runtime.universe._
import scala.reflect.ClassTag
import org.apache.log4j.Logger
import scala.collection.mutable.WeakHashMap
import scala.reflect.runtime.universe._
import scala.reflect.runtime._
import org.softlang.dscor.utils.ReflectionUtils

case class Dependency() extends annotation.StaticAnnotation

case class Property() extends annotation.StaticAnnotation

object Module {

  var global: WeakHashMap[Any, (String, Definition)] = new WeakHashMap()

}

abstract class Module {

  // TODO: Think about acyclic graphs and corresponding definition.

  val local: WeakHashMap[Any, String] = new WeakHashMap()

  lazy val definition: Definition = Definition(getClass.getName, getProperties, getDependencies.map { case (k, v) => (k, v.definition) })

  def save(location: String, data: Any) = Utils.serialize(data, location + ".tmp")

  def load(location: String): Any = Utils.deserialize(location + ".tmp")

  def getProperties: Map[String, Any] = annotated[Property]

  def getDependencies: Map[String, Module] = annotated[Dependency].mapValues(_.asInstanceOf[Module])

  def value[T](name: String, computation: () => T): T = data(name, true, computation).asInstanceOf[T]

  def backupLocation(name: String) = "backup/" + this.getClass.getSimpleName() + "/" + name + "/" + definition.toString.hashCode + "/data"

  def data(name: String, dump: Boolean, computation: () => Any): Any = {
    // Check if in local store
    var value: Option[Any] = None

    value = local.map(_.swap).toMap.get(name)

    if (value.isDefined)
      return value.get

    val identification = this.getClass.getSimpleName() + "@" + name
    // Check if in global store
    value = Module.global.map(_.swap).toMap.get((name, definition))
    if (value.isDefined) {
      println("global memory:     " + identification + "[]")
      local.put(value.get, name)
      return value.get
    }

    // Check if on filesystem
    val path = "backup/" + this.getClass.getSimpleName() + "/" + name + "/" + definition.toString.hashCode
    val loadStart = System.currentTimeMillis()
    value = if (Utils.exists(path + "/definition.tmp") && Utils.deserialize(path + "/definition.tmp") == definition) Some(load(path + "/data"))
    else None
    val loadEnd = System.currentTimeMillis()

    if (value.isDefined) {
      Module.global.put(value.get, (name, definition))
      local.put(value.get, name)
      println("filesystem memory: " + identification + "[" + String.valueOf(loadEnd - loadStart) + "ms]")
      return value.get
    }

    // Compute 
    val computationStart = System.currentTimeMillis()
    value = Some(computation.apply())
    val computationEnd = System.currentTimeMillis()
    println("computed:          " + identification + "[" + String.valueOf(computationEnd - computationStart) + "ms]")

    // Store.
    if (dump) {
      Utils.delete(path)
      val dumpStart = System.currentTimeMillis()
      save(path + "/data", value.get)
      Utils.serialize(definition, path + "/definition.tmp")
      val dumpEnd = System.currentTimeMillis()
      println("dumped:            " + identification + "[" + String.valueOf(dumpEnd - dumpStart) + "ms]")
    }

    Module.global.put(value.get, (name, definition))
    local.put(value.get, name)

    value.get
  }

  private def annotated[T: TypeTag]() = {
    val m = runtimeMirror(getClass.getClassLoader)
    val classSymbol = m.classSymbol(getClass)
    val classMirror = m.reflectClass(classSymbol)
    val instanceMirror = m.reflect(this)
    val cls = m.classSymbol(getClass)
    val members = cls.toType.members

    val constructor = cls.primaryConstructor.typeSignature.paramLists.head.map { param => param.asTerm.fullName -> param.annotations }.toMap

    members
      .collect { case m: TermSymbol if (m.isVal || m.isVal) => (m, m.annotations ++ constructor.getOrElse(m.fullName, Seq())) }
      .filter { case (ts, annotations) => annotations.exists {
        _.tree.tpe =:= m.typeOf[T]
      }
      }
      .map { case (ts, ann) => ts.name.decodedName.toString() -> instanceMirror.reflectField(ts).get }
      .toMap
  }
}

/**
  * TODO: Move up.
  * A modules describes a fixed computation unit where data purely depends on dependencies and properties.
  */
abstract class SingleModule[T](implicit tag: ClassTag[T]) extends Module {

  protected def compute(): T

  def backup() = true

  def data(): T = data("data", backup(), () => compute()).asInstanceOf[T]

}