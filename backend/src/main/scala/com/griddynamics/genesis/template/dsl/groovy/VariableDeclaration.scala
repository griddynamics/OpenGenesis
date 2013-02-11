package com.griddynamics.genesis.template.dsl.groovy

import com.griddynamics.genesis.template.DataSourceFactory
import groovy.lang.{MissingPropertyException, Closure, GroovyObjectSupport}
import collection.mutable.ListBuffer
import reflect.BeanProperty
import groovy.util.Expando

class VariableDeclaration(dsObjSupport: Option[Closure[Unit]],
                          dataSourceFactories : Seq[DataSourceFactory],
                          projectId: Int) extends GroovyObjectSupport with Delegate {
  protected val builders = new ListBuffer[VariableBuilder]

//  val declaration = new DataSourceDeclaration(projectId, dataSourceFactories)
  override def delegationStrategy = Closure.DELEGATE_FIRST

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
      case cl: Closure[_] =>
        builders += Delegate(cl).to(new DSAwareVariableBuilder(builders, dataSourceFactories, projectId, None, property, dsObjSupport))
      case _ =>
        super.setProperty(property, newValue)
    }
  }

  def variable(name : String) = {
        val builder = new VariableBuilder(name, dsObjSupport, dataSourceFactories, projectId)
        builders += builder
        builder
    }

  import collection.JavaConversions.mapAsScalaMap
  def group(params: java.util.Map[String, Any], variables : Closure[Unit]) = {
    val details = groupDetails(params.toMap, variables.hashCode)
    val groupDeclaration = new GroupDeclaration(builders, dsObjSupport, dataSourceFactories, projectId, details)
    builders ++= Delegate(variables).to(groupDeclaration).getBuilders
  }


  private def groupDetails(params: Map[String, Any], id: Int) = (params.get("description"), params.getOrElse("required", false)) match {
    case (Some(desc: String), req: Boolean) => GroupDetails(id, desc, req)
    case _ => throw new IllegalArgumentException("String parameter 'description' is mandatory, boolean parameter 'required' is optional")
  }

  def getBuilders = {
    builders
  }
}

class GroupDeclaration(parentBuilders: ListBuffer[VariableBuilder],
                       dsObjSupport: Option[Closure[Unit]],
                       dataSourceFactories : Seq[DataSourceFactory],
                       projectId: Int,
                       group: GroupDetails) extends VariableDeclaration(dsObjSupport, dataSourceFactories, projectId) {

  override def group(params: java.util.Map[String, Any], variables: Closure[Unit]): ListBuffer[VariableBuilder] = {
      throw new IllegalArgumentException("Nested groups are not supported!")
  }

  override def setProperty(property: String, newValue: Any) {
    newValue match {
      case cl: Closure[_] =>
        val builder = new DSAwareVariableBuilder(parentBuilders ++ builders, dataSourceFactories,  projectId, Option(group), property, dsObjSupport)
        builders += Delegate(cl).to(builder)
      case _ => super.setProperty(property, newValue)
    }
  }

  override def variable(name : String) = {
        val builder = new VariableBuilder(name, dsObjSupport, dataSourceFactories, projectId, Option(group))
        builders += builder
        builder
   }

  override def getBuilders = {
    builders.foreach(_.setIsOptional(true))
    builders
  }

}

case class GroupDetails(id: Int, description: String, required: Boolean = false)

object VariableDetails {
  type ValuesListType = Option[(Map[String,Any] => (Option[Any], Map[String,String]))]
}

class VariableDetails(val name : String, val clazz : Class[_ <: AnyRef], val description : String,
                      val validators : Seq[(String, Closure[Boolean])], val isOptional: Boolean = false, val defaultValue: () => Option[Any],
                      val valuesList: VariableDetails.ValuesListType = None, val dependsOn: Seq[String],
                      val group: Option[GroupDetails] = None)

class VariableBuilder(val name : String, dsClosure: Option[Closure[Unit]],
                      val dataSourceFactories: Seq[DataSourceFactory],
                      val projectId: Int,
                      val group: Option[GroupDetails] = None) extends GroovyObjectSupport {
    @BeanProperty var description : String = _
    @BeanProperty var clazz : Class[_ <: AnyRef] = classOf[String]
    private var defaultVal: Any = _
    @BeanProperty var isOptional: Boolean = false

    var validators = new collection.mutable.LinkedHashMap[String, Closure[Boolean]]
    var props = new collection.mutable.LinkedHashMap[String, AnyRef]
    var parents = new ListBuffer[String]
    var dataSourceRef: Option[String] = None
    var useOneOf: Boolean = false
    var oneOf: Closure[java.util.Map[String,String]] = _

    var inlineDataSource: Option[InlineDataSource] = None

    lazy val dsObj = {
      val dsDecl = new DataSourceDeclaration(projectId, dataSourceFactories)
      dsClosure.foreach(Delegate(_).to(dsDecl))
      val map = (for (builder <- dsDecl.builders) yield (builder.name, builder)).toMap
      Option(new DSObjectSupport(map))
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

    def optional() = {
      isOptional = true
      this
    }

  def defaultValue(v: Any) = {
     this.defaultVal = v
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
      val vars = ds.dependencyVars
      if (!vars.isEmpty) {
        this.dependsOn(vars.toArray)
      }
      this.inlineDataSource = Some(ds)
    }

    def valuesList: VariableDetails.ValuesListType = {
        if (useOneOf) {
            import collection.JavaConversions._
            oneOf.setDelegate(dsObj)

            val getValues = { _: Any => (Option(defaultVal), oneOf.call().toMap) }

            validator(new Closure[Boolean](this.oneOf) {
                def doCall(args: Array[Any]): Boolean = {
                    val (_, oneOfValues) = getValues()
                    oneOfValues.exists { case (key, value)=> key.toString == args(0).toString }
                }
            })

          Option(getValues)
        } else if (inlineDataSource.isDefined) {
          val inlineDS = inlineDataSource.get

          validators.put("Invalid value", new Closure[Boolean](new Expando()) {
            def doCall(args: Array[Any]): Boolean = {
              val deps = inlineDS.dependencyVars
              inlineDS.config(deps.map { it => (it, this.getProperty(it)) }.toMap)
              inlineDS.hasValue( args(0) )
            }
          })

          val func = { params: Map[String, Any] =>
            if (params.nonEmpty || inlineDS.dependencyVars.isEmpty) {
              inlineDS.config(parents.map(variable => (variable, params(variable))).toMap)
              (inlineDS.default, inlineDS.getData)
            } else {
              (Option(defaultVal), Map[String, String]())
            }
          }

          Option(func)
        } else {
           dataSourceRef.flatMap(ds => Option({params : Map[String, Any] => {
               val p = parents.toList.map(params.get(_)).flatten
               dsObj.map(dso => {(dso.default(ds), dso.getData(ds, p))})
                 .getOrElse(throw new IllegalStateException("No datasource configuration found, though variable %s tries to read from datasource".format(name)))
           }}))
        }
    }

    def newVariable = {
      val values = valuesList
      val default = () => {
         if (defaultVal != null) {
             Option(defaultVal)
         } else if (inlineDataSource.isDefined){
             val inlineDS = inlineDataSource.get
             inlineDS.default
         } else {
             dataSourceRef.flatMap(ds => {dsObj.flatMap(_.default(ds))})
         }
      }
      new VariableDetails(name, clazz, description, validators.toSeq, isOptional, default, values, parents.toList, group)
    }

  override def setProperty(property: String, arg: AnyRef) {
    if (super.getMetaClass.hasProperty(this, property) != null || Reserved.configRef == property) {
      super.setProperty(property, arg)
    } else {
      props.put(property, arg)
    }
  }

  override def getProperty(property: String): AnyRef = {
    if (super.getMetaClass.hasProperty(this, property) != null || Reserved.configRef == property) {
      super.getProperty(property)
    } else {
      props.get(property).getOrElse(throw new MissingPropertyException(property, this.getClass))
    }
  }
}


class DSAwareVariableBuilder(knownVars: ListBuffer[VariableBuilder],
                             dSourceFactories : Seq[DataSourceFactory],
                             projectId: Int,
                             group: Option[GroupDetails] = None,
                             varName: String,
                             dsObjSupport: Option[Closure[Unit]]) extends VariableBuilder(varName, dsObjSupport,
    dSourceFactories, projectId, group) with Delegate {

  override def delegationStrategy = Closure.DELEGATE_FIRST

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


