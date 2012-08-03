package com.griddynamics.genesis.template.dsl.groovy

import com.griddynamics.genesis.template.{VarDataSource, DataSourceFactory}
import groovy.lang.{Closure, GroovyObjectSupport}
import collection.mutable.ListBuffer
import reflect.BeanProperty

class VariableDeclaration(val dsObjSupport: Option[DSObjectSupport], dataSourceFactories : Seq[DataSourceFactory], projectId: Int) extends GroovyObjectSupport {
    val builders = new ListBuffer[VariableBuilder]

//  val declaration = new DataSourceDeclaration(projectId, dataSourceFactories)

  override def invokeMethod(name: String, args: AnyRef) = {
    if (dataSourceFactories.exists (_.mode == name)) {
      val declaration = new DataSourceDeclaration(projectId, dataSourceFactories)
      declaration.invokeMethod(name , args)
      declaration.builders.headOption.map { _.newDS._2 }.getOrElse(null)
    } else {
      super.invokeMethod(name, args)
    }
  }

  override def setProperty(property: String, newValue: Any) {
    newValue match {
      case cl: Closure[_] => {
        val builder = new DSAwareVariableBuilder(builders, dataSourceFactories, projectId, property, dsObjSupport)
        cl.setDelegate(builder)
        cl.setResolveStrategy(Closure.DELEGATE_FIRST)
        cl.call()
        builders +=builder
      }
      case _ => super.setProperty(property, newValue)
    }
  }

  def variable(name : String) = {
        val builder = new VariableBuilder(name, dsObjSupport)
        builders += builder
        builder
    }
}


class VariableDetails(val name : String, val clazz : Class[_ <: AnyRef], val description : String,
                      val validators : Seq[Closure[Boolean]], val isOptional: Boolean = false, val defaultValue: Option[Any],
                      val valuesList: Option[(Map[String,Any] => Map[String,String])] = None, val dependsOn: Seq[String])

class VariableBuilder(val name : String, dsObjSupport: Option[DSObjectSupport]) extends GroovyObjectSupport {
    @BeanProperty var description : String = _
    @BeanProperty var clazz : Class[_ <: AnyRef] = classOf[String]
    @BeanProperty var defaultValue: Any = _
    @BeanProperty var isOptional: Boolean = false

    var validators = new ListBuffer[Closure[Boolean]]
    var parents = new ListBuffer[String]
    var dataSourceRef: Option[String] = None
    var useOneOf: Boolean = false
    var oneOf: Closure[java.util.Map[String,String]] = _

    var inlineDataSource: Option[InlineDataSource] = None

    var dataSourceFactories : Seq[DataSourceFactory] = Seq()

    def as(value : Class[_ <: AnyRef]) = {
        this.clazz = value
        this
    }

    def description(description : String): VariableBuilder = {
        this.description = description
        this
    }

    def validator(validator : Closure[Boolean]) = {
        validators += validator
        this
    }

    def optional(v: Any) = {
      isOptional = true
      defaultValue = v
      this
    }

    def dependsOn(varName: String) = {
        if (useOneOf) {
            throw new IllegalArgumentException("dependsOn cannot be used with oneOf")
        }
        parents += varName
        this
    }

    def dependsOn(names: Array[String]) = {
        if (useOneOf) {
            throw new IllegalArgumentException("dependsOn cannot be used with oneOf")
        }
        parents ++= names
        this
    }

    def dataSource(dsName: String): VariableBuilder = {
        if (useOneOf) {
            throw new IllegalArgumentException("oneOf cannot be used with dataSource")
        }
        this.dataSourceRef = Option(dsName)
        this
    }

    def oneOf(values: Closure[java.util.Map[String,String]]): VariableBuilder = {
        this.useOneOf = true
        this.oneOf = values
        this
    }

    def inlineDataSource(ds: InlineDataSource) {
      import collection.JavaConversions._
      val vars = ds.dependancyVars
      if(!vars.isEmpty) {
        this.dependsOn(vars.toArray)
        this.inlineDataSource = Some(ds)
      } else {
        this.oneOf( new Closure[java.util.Map[String, String]](this) {
          override def call() = {
            ds.getData
          }
        })
      }
    }

    def valuesList: Option[(Map[String, Any] => Map[String,String])] = {
        if (useOneOf) {
            import collection.JavaConversions._
            dsObjSupport.foreach(oneOf.setDelegate(_))
            val values = Option({ _: Any => oneOf.call().map(kv => (kv._1, kv._2)).toMap})
            validators += new Closure[Boolean]() {
                def doCall(args: Array[Any]): Boolean = {
                    values.get.apply().asInstanceOf[Map[String,String]].exists(_._2.toString == args(0).toString)
                }
            }
            values
        } else if (inlineDataSource.isDefined) {
          val inlineDS = inlineDataSource.get
          val func = { params: Map[String, Any] =>
              if (params.isEmpty) {
                Map[String, String]()
              } else {
                inlineDS.config(parents.map(variable => (variable, params(variable))).toMap)
                inlineDS.getData
              }
          }
          Option(func)
        } else {
           dataSourceRef.flatMap(ds => Option({params : Map[String, Any] => {
               val p = parents.toList.map(params.get(_)).flatten
               dsObjSupport.get.getData(ds, p)
           }}))
        }
    }

    def newVariable = new VariableDetails(name, clazz, description, validators, isOptional, Option(defaultValue), valuesList, parents.toList)
}


class DSAwareVariableBuilder(knownVars: ListBuffer[VariableBuilder],
                             dSourceFactories : Seq[DataSourceFactory],
                             projectId: Int,
                             varName: String,
                             dsObjSupport: Option[DSObjectSupport]) extends VariableBuilder(varName, dsObjSupport)  {

  def setValidator(validator : Closure[Boolean]) {
    this.validator(validator)
  }

  def setDataSource(ds: AnyRef) {
    ds match {
      case name: String => this.dataSource(name)
      case inline: InlineDataSource => this.inlineDataSource(inline)
    }
  }

  override def invokeMethod(name: String, args: AnyRef) = {
    val factory = dSourceFactories.find(_.mode == name)

    if (factory.isDefined) {
      val builder = new DataSourceBuilder(projectId, factory.get, "")
      val config = args.asInstanceOf[Array[AnyRef]].collectFirst { case c: Closure[_] => c }

      new InlineDataSource(builder, config, knownVars)
    } else {
      super.invokeMethod(name, args)
    }
  }
}


