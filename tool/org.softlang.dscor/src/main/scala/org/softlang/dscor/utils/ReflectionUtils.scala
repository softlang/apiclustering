package org.softlang.dscor.utils

import scala.reflect.runtime.universe._
import scala.reflect.runtime._
import java.lang.reflect.Method
import java.lang.reflect.Field
//import scala.reflect.TypeTags.TypeTag

object ReflectionUtils {
  def getAllMethods(cls: Class[_]): Set[Method] = {
    if (cls == null) Set()
    else cls.getDeclaredMethods.toSet ++ getAllMethods(cls.getSuperclass)
  }

  def getAllFieldes(cls: Class[_]): Set[Field] = {
    if (cls == null) Set()
    else cls.getDeclaredFields.toSet ++ getAllFieldes(cls.getSuperclass)
  }

  def getValue(any: Any, m: Field) = {
    val access = m.isAccessible()
    if (!access) m.setAccessible(true)
    val value = m.get(any)
    if (!access) m.setAccessible(false)

    value
  }

  def setValue(any: Any, m: Field, value: Any) = {
    val access = m.isAccessible()
    if (!access) m.setAccessible(true)
    m.set(any, value)
    if (!access) m.setAccessible(false)
  }

  def members(product: Product) = (Map[String, Any]() /: product.getClass.getDeclaredFields) { (a, f) =>
    f.setAccessible(true)
    val value = f.get(product)
    f.setAccessible(false)
    a + (f.getName -> value)
  }

  def getValue(any: Any, m: Method) = {
    val access = m.isAccessible()
    if (!access) m.setAccessible(true)
    val value = m.invoke(any)
    if (!access) m.setAccessible(false)

    value
  }

  def createInstanceByName(name: String, args: AnyRef*) =
    instantiate(Class.forName(name))(args: _*)

  def instantiate[T](clazz: java.lang.Class[T])(args: AnyRef*): T = {
    val constructor = clazz.getConstructors()(0)
    return constructor.newInstance(args: _*).asInstanceOf[T]
  }

  def createInstance[T: TypeTag](args: AnyRef*): T = {
    val tt = typeTag[T]

    currentMirror.reflectClass(tt.tpe.typeSymbol.asClass).reflectConstructor(
      tt.tpe.members.filter(m =>
        m.isMethod && m.asMethod.isConstructor).iterator.toSeq(0).asMethod)(args: _*).asInstanceOf[T]
  }

  def createInstance(typ: Type, args: AnyRef*): AnyRef = {
    currentMirror.reflectClass(typ.typeSymbol.asClass).reflectConstructor(
      typ.members.filter(m =>
        m.isMethod && m.asMethod.isConstructor).iterator.toSeq(0).asMethod)(args: _*).asInstanceOf[AnyRef]
  }

  def sealedDescendants[Root: TypeTag]: Option[Set[Symbol]] = {
    val symbol = typeOf[Root].typeSymbol
    val internal = symbol.asInstanceOf[scala.reflect.internal.Symbols#Symbol]
    if (internal.isSealed)
      Some(internal.sealedDescendants.map(_.asInstanceOf[Symbol]) - symbol)
    else None
  }

  def isSealed(typ: Type): Boolean = {
    val symbol = typ.typeSymbol
    val internal = symbol.asInstanceOf[scala.reflect.internal.Symbols#Symbol]
    internal.isSealed
  }

  def isTrait(typ: Type): Boolean = {
    val symbol = typ.typeSymbol
    val internal = symbol.asInstanceOf[scala.reflect.internal.Symbols#Symbol]
    internal.isTrait
  }

  def sealedDescendants(typ: Type): Seq[Symbol] = {
    val symbol = typ.typeSymbol
    val internal = symbol.asInstanceOf[scala.reflect.internal.Symbols#Symbol]
    internal.sealedDescendants.map(_.asInstanceOf[Symbol]).filter(x => !isTrait(x.asType.toType)).toSeq
  }

  def classAccessors[T: TypeTag]: List[MethodSymbol] = typeOf[T].decls.sorted.collect {
    case m: MethodSymbol if m.isCaseAccessor => m
  }.toList

  def classAccessors(typ: Type): List[MethodSymbol] = typ.decls.sorted.collect {
    case m: MethodSymbol if m.isCaseAccessor => m
  }.toList
}