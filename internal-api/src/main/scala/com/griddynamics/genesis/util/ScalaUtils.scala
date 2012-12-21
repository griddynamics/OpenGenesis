package com.griddynamics.genesis.util

import java.lang.reflect.Method
import net.sf.ehcache.CacheManager
import com.griddynamics.genesis.cache.EhCacheManager


object ScalaUtils extends com.griddynamics.genesis.cache.Cache {

  lazy val cacheManager = new EhCacheManager(CacheManager.getInstance())

  private val CacheName = "PropertyCache"

  private val wrappers: Map[Class[_], Class[_]] = Map(
    java.lang.Byte.TYPE -> classOf[java.lang.Byte],
    java.lang.Short.TYPE -> classOf[java.lang.Short],
    java.lang.Integer.TYPE -> classOf[java.lang.Integer],
    java.lang.Long.TYPE -> classOf[java.lang.Long],
    java.lang.Float.TYPE -> classOf[java.lang.Float],
    java.lang.Double.TYPE -> classOf[java.lang.Double],
    java.lang.Character.TYPE -> classOf[java.lang.Character],
    java.lang.Boolean.TYPE -> classOf[java.lang.Boolean]
  )

  def toAnyRef(value: Any): AnyRef = value match {
    case i: scala.Int => scala.Int.box(i)
    case l: scala.Long => scala.Long.box(l)
    case d: scala.Double => scala.Double.box(d)
    case b: scala.Boolean => scala.Boolean.box(b)
    case f: scala.Float => scala.Float.box(f)
    case c: scala.Char => scala.Char.box(c)
    case s: scala.Short => scala.Short.box(s)
    case b: scala.Byte => scala.Byte.box(b)
    case r: AnyRef => r
    case _ => throw new IllegalArgumentException("Cannot convert %s to object".format(value))
  }

  def getType(value: Any): Class[_] = value match {
    case b: Byte => java.lang.Byte.TYPE
    case s: Short => java.lang.Short.TYPE
    case i: Int => java.lang.Integer.TYPE
    case l: Long => java.lang.Long.TYPE
    case f: Float => java.lang.Float.TYPE
    case d: Double => java.lang.Double.TYPE
    case c: Char => java.lang.Character.TYPE
    case b: Boolean => java.lang.Boolean.TYPE
    case v: Unit => java.lang.Void.TYPE
    case r: AnyRef => r.getClass
    case _ => throw new IllegalArgumentException("Can't get type of %s".format(value))
  }


  def loadClass(path: String, classLoader: ClassLoader) = path match {
    case "scala.Predef.Map" => classOf[Map[_, _]]
    case "scala.Predef.Set" => classOf[Set[_]]
    case "scala.Predef.String" => classOf[String]
    case "scala.package.List" => classOf[List[_]]
    case "scala.package.Seq" => classOf[Seq[_]]
    case "scala.package.Sequence" => classOf[Seq[_]]
    case "scala.package.Collection" => classOf[Seq[_]]
    case "scala.package.IndexedSeq" => classOf[IndexedSeq[_]]
    case "scala.package.RandomAccessSeq" => classOf[IndexedSeq[_]]
    case "scala.package.Iterable" => classOf[Iterable[_]]
    case "scala.package.Iterator" => classOf[Iterator[_]]
    case "scala.package.Vector" => classOf[Vector[_]]
    case "scala.package.BigDecimal" => classOf[BigDecimal]
    case "scala.package.BigInt" => classOf[BigInt]
    case "scala.package.Integer" => classOf[java.lang.Integer]
    case "scala.package.Character" => classOf[java.lang.Character]
    case "scala.Long" => classOf[java.lang.Long]
    case "scala.Int" => classOf[java.lang.Integer]
    case "scala.Boolean" => classOf[java.lang.Boolean]
    case "scala.Short" => classOf[java.lang.Short]
    case "scala.Byte" => classOf[java.lang.Byte]
    case "scala.Float" => classOf[java.lang.Float]
    case "scala.Double" => classOf[java.lang.Double]
    case "scala.Char" => classOf[java.lang.Character]
    case "scala.Any" => classOf[Any]
    case "scala.AnyRef" => classOf[AnyRef]
    case name => classLoader.loadClass(name)
  }


  private[this] def findPropertyMutator(obj: AnyRef, name: String, valueType: Class[_]): Option[Method] = fromCache(CacheName, CacheKey(obj.getClass, name, valueType)) {
    def isAssignable(to: Class[_], from: Class[_]) = {
        val toWrap = if (to.isPrimitive) wrappers(to) else to
        val fromWrap = if (from.isPrimitive) wrappers(from) else from
        toWrap.isAssignableFrom(fromWrap)
    }

    val accessorName = "set" + name.capitalize
    val allMethods = obj.getClass.getMethods.toSet ++ obj.getClass.getDeclaredMethods.toSet
    allMethods.find { m =>
      m.getName == accessorName &&
        m.getParameterTypes.length == 1 &&
        isAssignable(m.getParameterTypes.apply(0), valueType)
    }
  }

  private[this] def findPropertyMutators(obj: AnyRef, name: String): Set[Method] = {
    val accessorName = "set" + name.capitalize
    val allMethods = obj.getClass.getMethods.toSet ++ obj.getClass.getDeclaredMethods.toSet
    allMethods.filter { m => m.getName == accessorName && m.getParameterTypes.length == 1 }
  }

  def hasProperty(obj: AnyRef, name: String, valueType: Class[_]): Boolean = findPropertyMutator(obj, name, valueType).isDefined

  def setProperty(obj: AnyRef, name: String, value: Any) {
    if (value == null) {
      val setters = findPropertyMutators(obj, name)
      if (setters.size != 1) {
        throw new IllegalArgumentException("Failed to set null to property [%s] in class [%s]. Ambigous name resolution".format(name, obj.getClass.getName))
      }
      val setter = setters.head

      setter.setAccessible(true) //for anonymous classes scala transform @BeanProperty into private setter.
      setter.invoke(obj, null)
    } else {
      val valueRef = toAnyRef(value)
      val setter = findPropertyMutator(obj, name, valueRef.getClass).getOrElse(
        throw new IllegalArgumentException("Can't find setter for property [%s] in class [%s]".format(name, obj.getClass.getName))
      )

      setter.setAccessible(true)
      setter.invoke(obj, valueRef)
    }
  }

  private case class CacheKey(hostClass: Class[_], propertyName: String, valueType: Class[_])
}