package com.griddynamics.genesis.template.dsl.groovy

import com.griddynamics.genesis.template.DataSourceFactory
import groovy.lang.{MissingPropertyException, Closure, GroovyObjectSupport}
import collection.mutable.ListBuffer
import reflect.BeanProperty
import groovy.util.Expando

class VariableDeclaration(val dsObjSupport: Option[Closure[Unit]], dataSourceFactories : Seq[DataSourceFactory], projectId: Int) extends GroovyObjectSupport {
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
        val builder = new VariableBuilder(name, dsObjSupport, dataSourceFactories, projectId)
        builders += builder
        builder
    }
}


class VariableDetails(val name : String, val clazz : Class[_ <: AnyRef], val description : String,
                      val validators : Seq[(String, Closure[Boolean])], val isOptional: Boolean = false, val defaultValue: Option[Any],
                      val valuesList: Option[(Map[String,Any] => Map[String,String])] = None, val dependsOn: Seq[String])

class VariableBuilder(val name : String, dsClosure: Option[Closure[Unit]],
                      val dataSourceFactories: Seq[DataSourceFactory], val projectId: Int) extends GroovyObjectSupport {
    @BeanProperty var description : String = _
    @BeanProperty var clazz : Class[_ <: AnyRef] = classOf[String]
    @BeanProperty var defaultValue: Any = _
    @BeanProperty var isOptional: Boolean = false

    var validators = new collection.mutable.LinkedHashMap[String, Closure[Boolean]]
    var props = new collection.mutable.LinkedHashMap[String, AnyRef]
    var parents = new ListBuffer[String]
    var dataSourceRef: Option[String] = None
    var useOneOf: Boolean = false
    var oneOf: Closure[java.util.Map[String,String]] = _

    var inlineDataSource: Option[InlineDataSource] = None

    lazy val dsObj = {
        dsClosure.map(closure => {
            val dsDelegate = new DataSourceDeclaration(projectId, dataSourceFactories)
            closure.setDelegate(dsDelegate)
            closure.call()
            val dsBuilders = dsDelegate.builders
            val map = (for (builder <- dsBuilders) yield (builder.name, builder)).toMap
            new DSObjectSupport(map)
        })
    }

    def as(value : Class[_ <: AnyRef]) = {
        this.clazz = value
        this
    }

    def description(description : String): VariableBuilder = {
        this.description = description
        this
    }

    def validator(validator : Closure[Boolean]) = {
        validators.put("Validation failed", validator)
        this
    }

    def validator(arg : java.util.Map[String, Closure[Boolean]]) = {
        import collection.JavaConversions._
        validators ++= arg
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
      val vars = ds.dependancyVars
      if (!vars.isEmpty) {
        this.dependsOn(vars.toArray)
      }
      this.inlineDataSource = Some(ds)
    }

    def valuesList: Option[(Map[String, Any] => Map[String,String])] = {
        if (useOneOf) {
            import collection.JavaConversions._
            oneOf.setDelegate(dsObj)

            val getValues = { _: Any => oneOf.call().toMap }

            validator(new Closure[Boolean](this.oneOf) {
                def doCall(args: Array[Any]): Boolean = {
                    getValues().exists(_._2.toString == args(0).toString)
                }
            })

          Option(getValues)
        } else if (inlineDataSource.isDefined) {
          val inlineDS = inlineDataSource.get

          validators.put("Invalid value", new Closure[Boolean](new Expando()) {
            def doCall(args: Array[Any]): Boolean = {
              val deps = inlineDS.dependancyVars
              inlineDS.config(deps.map { it => (it, this.getProperty(it)) }.toMap)
              inlineDS.hasValue( args(0) )
            }
          })

          val func = { params: Map[String, Any] =>
            if (params.nonEmpty || inlineDS.dependancyVars.isEmpty) {
              inlineDS.config(parents.map(variable => (variable, params(variable))).toMap)
              inlineDS.getData
            } else {
              Map[String, String]()
            }
          }

          Option(func)
        } else {
           dataSourceRef.flatMap(ds => Option({params : Map[String, Any] => {
               val p = parents.toList.map(params.get(_)).flatten
               dsObj.map(_.getData(ds, p)).getOrElse(throw new IllegalStateException("No datasource configuration found, though variable %s tries to read from datasource".format(name)))
           }}))
        }
    }

    def newVariable = {
      val values = valuesList
      new VariableDetails(name, clazz, description, validators.toSeq, isOptional, Option(defaultValue), values, parents.toList)
    }

    override def setProperty(property: String, arg: AnyRef) {
       if (super.getMetaClass.hasProperty(this, property) != null) {
           super.setProperty(property, arg)
       } else {
           props.put(property, arg)
       }
    }

    override def getProperty(property: String): AnyRef = {
        if (super.getMetaClass.hasProperty(this, property) != null) {
            super.getProperty(property)
        } else{
            props.get(property).getOrElse(throw new MissingPropertyException(property, this.getClass))
       }
    }
}


class DSAwareVariableBuilder(knownVars: ListBuffer[VariableBuilder],
                             dSourceFactories : Seq[DataSourceFactory],
                             projectId: Int,
                             varName: String,
                             dsObjSupport: Option[Closure[Unit]]) extends VariableBuilder(varName, dsObjSupport,
    dSourceFactories, projectId)  {

  def setValidator(validator : Closure[Boolean]) {
    this.validator(validator)
  }

  def setValidator(validator: java.util.Map[String, Closure[Boolean]]) {
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


