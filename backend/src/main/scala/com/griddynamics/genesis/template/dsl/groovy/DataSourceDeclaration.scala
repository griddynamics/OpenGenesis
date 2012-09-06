package com.griddynamics.genesis.template.dsl.groovy

import com.griddynamics.genesis.template.{DependentDataSource, VarDataSource, DataSourceFactory}
import groovy.lang.{Closure, GroovyObjectSupport}
import collection.mutable.ListBuffer
import com.griddynamics.genesis.util.ScalaUtils
import java.lang.reflect.Method

class DataSourceDeclaration(val projectId: Int, dsFactories: Seq[DataSourceFactory]) extends GroovyObjectSupport {
    val builders = new ListBuffer[DataSourceBuilder]

    override def invokeMethod(name: String, args: AnyRef) = {
        val factory = dsFactories.find(ds => ds.mode == name).getOrElse(
          throw new IllegalArgumentException("Datasource for mode %s is not found".format(name))
        )

        val argsIterator: Iterator[_] = args.asInstanceOf[Array[_]].iterator
        if (argsIterator.isEmpty) {
            throw new IllegalArgumentException(
                """At least name must be provided for datasource %s. Example:
                  | %s("name") { ... }
                """.stripMargin.format(name, name))
        }
        val dsName = argsIterator.next().asInstanceOf[String]
        val builder = new DataSourceBuilder(projectId, factory, dsName)
        if (argsIterator.hasNext) {
            val closure = argsIterator.next().asInstanceOf[Closure[_]]
            closure.setDelegate(builder)
            closure.setResolveStrategy(Closure.DELEGATE_FIRST)
            closure.call()
        }
        builders += builder
    }
}

class DataSourceBuilder(val projectId: Int, val factory : DataSourceFactory, val name: String) extends GroovyObjectSupport {
    var conf = new scala.collection.mutable.HashMap[String, Any]()

    override def setProperty(name: String, args: AnyRef) {
        conf.put(name, args)
        super.setProperty(name, args)
    }

    def newDS = (name, {val ds = factory.newDataSource; ds.config(conf.toMap + ("projectId" -> projectId)); ds})
}


class DSObjectSupport(val dsMap: Map[String, DataSourceBuilder]) extends GroovyObjectSupport {
     override def getProperty(name: String)  = {
         dsMap.get(name) match {
             case Some(src) => {
                 val s: (String, VarDataSource) = src.newDS
                 collection.JavaConversions.mapAsJavaMap(s._2.getData)
             }
             case _ => super.getProperty(name)
         }
     }

     def getData(name: String, args: List[Any]): Map[String,String] = {
         dsMap.get(name) match {
             case Some(src) => args match {
                 case Nil => src.newDS._2.getData
                 case x :: Nil => {
                     src.newDS._2.asInstanceOf[DependentDataSource].getData(x)
                 }
                 case head :: tail => {
                     val ds = src.newDS._2
                     val params: Array[AnyRef] = args.map(v => ScalaUtils.toAnyRef(v)).toArray
                     val find: Option[Method] = ds.getClass.getDeclaredMethods.find(m => m.getName == "getData"
                       && m.getParameterTypes.length == params.length
                     )
                     find match  {
                         case Some(m) => {
                             m.invoke(ds, params:_*).asInstanceOf[Map[String,String]]
                         }
                         case _ => throw new IllegalStateException("Cannot find method getData for args %s".format(args))
                     }
                 }
                 case _ => throw new IllegalStateException("Cannot find any suitable method at datasource %s".format(src.newDS._2))
             }
             case _ => throw new IllegalStateException("Can't get datasource for argument %s".format(name))
         }
     }
 }


class InlineDataSource( builder: DataSourceBuilder,
                        configDeclaration: Option[Closure[_]],
                        knownVariables: Seq[VariableBuilder] ) extends VarDataSource {

  def getData = {
    val cbuilder = configuredBuilder
    val isLazy = cbuilder.conf.get("lazy").collect { case b: Boolean => b }.getOrElse(false)
    if (!isLazy) {
      val (_, datasource) = cbuilder.newDS
      datasource.getData
    } else {
      Map()
    }
  }

  override def hasValue(value: Any) = {
    val (_, datasource) = configuredBuilder.newDS
    datasource.hasValue(value)
  }

  private[this] def configuredBuilder = {
    for (config <- configDeclaration) {
      config.setDelegate(builder)
      config.setResolveStrategy(Closure.DELEGATE_FIRST)
      config.call()
    }
    builder
  }

  def config(map: Map[String, Any]) {
    for (config <- configDeclaration) {
      map.foreach { case (key, value) => config.setProperty(key, value) }
    }
  }

  lazy val dependancyVars: Seq[String] = {
    if(configDeclaration.isEmpty) {
      Seq()
    } else {
      val config = configDeclaration.get

      val collector = new DependencyRefCollector(knownVariables)

      val oldDelegateValue = config.getDelegate
      val oldStrategyValue = config.getResolveStrategy

      config.setDelegate(collector)
      config.setResolveStrategy(Closure.DELEGATE_FIRST)
      config.call

      config.setDelegate(oldDelegateValue)
      config.setResolveStrategy(oldStrategyValue)

      collector.links
    }
  }

  private[this] class DependencyRefCollector(knownVars: Seq[VariableBuilder]) extends GroovyObjectSupport {
      val links = new ListBuffer[String]

      private[this] def fakeValue(clazz: Class[_]): AnyRef = {
        if( clazz == classOf[String]) {
          ""
        } else if(clazz.isAssignableFrom(classOf[Number])) {
          Int.box(0)
        } else {
          null
        }
      }

      override def getProperty(property: String) = {
        knownVars.find(_.name == property) match {
          case Some(variable) => {
            links += property
            fakeValue(variable.getClazz())
          }
          case None => super.getProperty(property)
        }
      }
    }

}