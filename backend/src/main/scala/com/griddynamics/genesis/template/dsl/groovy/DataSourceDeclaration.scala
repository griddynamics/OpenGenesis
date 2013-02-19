package com.griddynamics.genesis.template.dsl.groovy

import com.griddynamics.genesis.template.{DependentDataSource, VarDataSource, DataSourceFactory}
import groovy.lang.{MissingPropertyException, Closure, GroovyObjectSupport}
import collection.mutable.ListBuffer
import com.griddynamics.genesis.util.ScalaUtils
import java.lang.reflect.Method

class DataSourceDeclaration(val projectId: Int, dsFactories: Seq[DataSourceFactory]) extends GroovyObjectSupport with Delegate {
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
            Delegate(argsIterator.next().asInstanceOf[Closure[_]]).to(builder)
        }
        builders += builder
    }
}

class DataSourceBuilder(val projectId: Int, val factory : DataSourceFactory, val name: String) extends GroovyObjectSupport with Delegate {
    var conf = new scala.collection.mutable.HashMap[String, Any]()

  override def delegationStrategy = Closure.DELEGATE_FIRST

  override def setProperty(name: String, args: AnyRef) {
        conf.put(name, args)
    }

    def newDS:(String, VarDataSource) = (name, {val ds = factory.newDataSource; ds.config(conf.toMap + ("projectId" -> projectId)); ds})
}


class DSObjectSupport(val dsMap: Map[String, DataSourceBuilder]) extends GroovyObjectSupport {

     override def getProperty(name: String)  = {
         dataSource(name) match {
             case Some(s) => {
                 collection.JavaConversions.mapAsJavaMap(s._2.getData)
             }
             case _ => super.getProperty(name)
         }
     }

     def default(name: String): Option[Any] = dataSource(name).flatMap(_._2.default)

     def getData(name: String, args: List[Any]): Map[String, String] = {
         dataSource(name) match {
             case Some(src) => args match {
                 case Nil => src._2.getData
                 case x :: Nil => {
                     src._2.asInstanceOf[DependentDataSource].getData(x)
                 }
                 case head :: tail => {
                     val ds = src._2
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
                 case _ => throw new IllegalStateException("Cannot find any suitable method at datasource %s".format(src._2))
             }
             case _ => throw new IllegalStateException("Can't get datasource for argument %s".format(name))
         }
     }

     def dataSource(name: String) : Option[(String, VarDataSource)] = dsMap.get(name).map(_.newDS)
 }


class InlineDataSource( builder: DataSourceBuilder,
                        configDeclaration: Option[Closure[_]],
                        knownVariables: Seq[VariableBuilder] ) extends VarDataSource {

  private lazy val datasource = configuredBuilder.newDS._2

  def getData = configuredBuilder.conf.get("lazy") match {
    case Some(true) => Map()
    case _ => datasource.getData
  }

  override def default = {
      try {
         datasource.default
      } catch {
          case mpe: MissingPropertyException => None
      }
  }

  override def hasValue(value: Any) = datasource.hasValue(value)

  private[this] def configuredBuilder =
    configDeclaration.map { it => Delegate(it).to(builder) }.getOrElse(builder)

  def config(map: Map[String, Any]) {
    for (config <- configDeclaration) {
      map.foreach {
        case (key, value) => config.setProperty(key, value)    //todo: wtf  . why is it propagated
      }
    }
  }

  lazy val dependencyVars: Seq[String] =
    configDeclaration.map { closure => Delegate(closure).to(new DependencyRefCollector(knownVariables)).links }.getOrElse(Seq())

  private[this] class DependencyRefCollector(knownVars: Seq[VariableBuilder]) extends GroovyObjectSupport with Delegate {

    override def delegationStrategy = Closure.DELEGATE_FIRST

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
          case Some(variable) =>
            links += property
            fakeValue(variable.getClazz())
          case None => super.getProperty(property)
        }
    }
  }

}