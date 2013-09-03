package com.griddynamics.genesis.template.dsl.groovy.transformations

import org.codehaus.groovy.transform.{ASTTransformation, GroovyASTTransformation}
import org.codehaus.groovy.control.{SourceUnit, CompilePhase}
import com.griddynamics.genesis.util.Logging
import org.codehaus.groovy.ast.{Parameter, CodeVisitorSupport, ASTNode}
import org.codehaus.groovy.ast.expr._
import scala.collection.mutable
import org.codehaus.groovy.ast.stmt.{ExpressionStatement, Statement, BlockStatement}
import java.util
import scala.collection.JavaConversions._
import com.griddynamics.genesis.template.dsl.groovy.transformations.exceptions.MacroParametersException
import org.codehaus.groovy.control.messages.SimpleMessage

@GroovyASTTransformation(phase = CompilePhase.CONVERSION)
class MacroASTTransformation extends ASTTransformation with Logging {
  def visit(nodes: Array[ASTNode], source: SourceUnit) {
    log.trace(s"Inside a macros transformer: ${nodes.length}")
    val methods = source.getAST.getStatementBlock.getStatements
    val it = methods.iterator().next()
    val collector = new MacroCollector(source)
    it.visit(new NameMatchArgumentTransformer("template", collector))
    if (collector.macros.size > 0) {
      val replacementMap = new mutable.HashMap[Statement, Expression]()
      log.trace(s"Found ${collector.macros.size} macros")
      methods.iterator().next().visit(new MacroExpandVisitor(collector.macros.toMap, replacementMap, source))
    }
    log.trace("Done")
  }
}

trait SourceReporting {
  def source: SourceUnit
  def error[B](message: String)(value: B) = {
    source.getErrorCollector.addError(new SimpleMessage(message, "", source))
    value
  }
}

class ApplyVariablesVisitor(values: Map[String, Expression], replacements: mutable.Map[Statement, Expression]) extends CodeVisitorSupport with ExpressionTransformer
  with Logging {
  override def visitExpressionStatement(statement: ExpressionStatement) {
    val expression: Expression = statement.getExpression
    //expression.visit(this)
    val toChange: Expression = expression
    val newExpression: Expression = toChange.transformExpression(this)
    statement.setExpression(newExpression)
    super.visitExpressionStatement(statement)
  }


  override def visitMapEntryExpression(expression: MapEntryExpression) {
    val key: Expression = expression.getKeyExpression
    val value: Expression = expression.getValueExpression
    expression.setKeyExpression(transform(key))
    expression.setValueExpression(transform(value))
    super.visitMapEntryExpression(expression)
  }

  override def visitBinaryExpression(expression: BinaryExpression) {
    expression.setLeftExpression(transform(expression.getLeftExpression))
    expression.setRightExpression(transform(expression.getRightExpression))
    super.visitBinaryExpression(expression)
  }

  def transform(expression: Expression): Expression = {
    expression match {
      case variable: VariableExpression => {
        values.get(variable.getName).getOrElse(variable)
      }
      case constant: ConstantExpression if constant.getText.startsWith("$") =>
        values.get(constant.getText).getOrElse(constant)
      case cast: CastExpression => {
        new CastExpression(cast.getType, transform(cast.getExpression), cast.isIgnoringAutoboxing)
      }
      case other => other
    }
  }
}

class MacroExpandVisitor(val macrodefs: Map[String, Macro], replacements: mutable.Map[Statement,Expression], val source: SourceUnit) extends CodeVisitorSupport with Logging with SourceReporting {
  val nameExtract: PartialFunction[Expression, String] = {
    case call if call.isInstanceOf[MethodCallExpression] => call.asInstanceOf[MethodCallExpression].getMethodAsString
  }
  private def substitute(code: Macro, call: MethodCallExpression): Statement = {
    val arguments: Expression = call.getArguments
    //possible variants: ArgumentListExpression, Map
    val passed: Seq[PassedParameter] = arguments match {
      case list: ArgumentListExpression => list.getExpressions.zipWithIndex.map({case (expr, index) => new PositionedParameter(index, expr)}).toSeq
      case tuple: TupleExpression => tuple.getExpressions.flatMap(e => e match {
        case named: NamedArgumentListExpression => {
          named.getMapEntryExpressions.map(expr => expr.getKeyExpression match {
            case constant: ConstantExpression => Some(NamedParameter(constant.getText, expr.getValueExpression))
            case other => error("Only constants allowed in macro named arguments")(None)
          })
        }
      }).flatten
      case other => error("Macro parameters can be passed as comma-separated list or as named pairs")(Seq())
    }
    //apply
    val values: Map[String, Option[Expression]] = code.parameters.zipWithIndex.map({case ((name, value), idx) => {
      val found = passed.find({
        case named: NamedParameter => named.name == name
        case pos: PositionedParameter => pos.position == idx
      }).map(_.value).orElse(value)
      (name, found)
    }}).toMap
    val unsassigned = values.collect({case (name, value) if value.isEmpty || value.get == null => name})
    if (unsassigned.size > 0) {
      error(s"Some arguments for macro `${code.name}` were not assigned and they don't have default value: " + unsassigned.mkString(", "))(code.code)
    } else {
      applyArguments(values.map({case (name, expr) => (name, expr.get)}), code.code)
    }
  }

  private def applyArguments(values: Map[String, Expression], code: BlockStatement) : BlockStatement = {
    val visitor = new ApplyVariablesVisitor(values, replacements)
    log.debug(s"Applying variables $values")
    val copy = copyBlock(code)
    log.trace(s"Initial code is: $code")
    copy.visit(visitor)
    log.debug(s"Copy with expanded variables: $copy")
    copy
  }

  private def copyBlock(original: BlockStatement): BlockStatement = {
    val bs = new BlockStatement()
    addBlockToBlock(original, bs)
    bs
  }

  private def addBlockToBlock(source: BlockStatement, target: BlockStatement) {
    source.getStatements.foreach(addStatement(target))
  }

  private val copy: PartialFunction[Expression, Expression] = {
    case variable: VariableExpression => new VariableExpression(variable.getName)
    case constant: ConstantExpression => new ConstantExpression(constant.getText)
    case binary: BinaryExpression => new BinaryExpression(copy(binary.getLeftExpression), binary.getOperation,
      copy(binary.getRightExpression))
    case closure: ClosureExpression => copyClosureExpression(closure)
    case arguments: ArgumentListExpression => new ArgumentListExpression(arguments.getExpressions.map(copy))
    case mapEntry: MapEntryExpression => new MapEntryExpression(copy(mapEntry.getKeyExpression), copy(mapEntry.getValueExpression))
    case prop: PropertyExpression => new PropertyExpression(copy(prop.getObjectExpression), copy(prop.getProperty))
    case not: NotExpression => new NotExpression(copy(not.getExpression))
    case cast: CastExpression => new CastExpression(cast.getType, copy(cast.getExpression), cast.isIgnoringAutoboxing)
    case method: MethodCallExpression => new MethodCallExpression(copy(method.getObjectExpression),
      copy(method.getMethod), copy(method.getArguments))
    case c: ConstructorCallExpression => new ConstructorCallExpression(c.getType, copy(c.getArguments))
    case elvis: ElvisOperatorExpression => {
      val trueExpression = copy(elvis.getTrueExpression)
      val falseExpression = copy(elvis.getFalseExpression)
      val base = new BooleanExpression(trueExpression)
      base.setSourcePosition(trueExpression)
      new ElvisOperatorExpression(base, falseExpression)
    }
  }

  private def copyClosureExpression(closure: ClosureExpression): ClosureExpression = {
    def copyParameters (closure: ClosureExpression) = {
        closure.getParameters.map(p => new Parameter(p.getType, p.getName, copy(p.getInitialExpression)))
    }
    closure.getCode match {
      case bs: BlockStatement => new ClosureExpression(copyParameters(closure), copyBlock(bs))
    }
  }

  private def addBinaryExpression(be: BinaryExpression, bs: BlockStatement) = {
    bs.addStatement(new ExpressionStatement(copy(be)))
  }

  private def addMethodCall(objectExpr: Expression, method: String, arguments: Expression, statement: BlockStatement) {
     val newExpr = new MethodCallExpression(objectExpr, method, copy(arguments))
     statement.addStatement(new ExpressionStatement(newExpr))
  }

  private def addMapExpression(map: MapExpression, statement: BlockStatement) {
    val copies = map.getMapEntryExpressions.map(entry => {
       copy(entry).asInstanceOf[MapEntryExpression]
    })
    val newExpr = new MapExpression(copies.toList)
    statement.addStatement(new ExpressionStatement(newExpr))
  }

  private def addStatement(blockStatement: BlockStatement)(statement: Statement) {
     statement match {
       case bs: BlockStatement => addBlockToBlock(bs, blockStatement)
       case es: ExpressionStatement => {
         val expr: Expression = es.getExpression
         expr match {
           case constant: ConstantExpression => blockStatement.addStatement(es)
           case de: DeclarationExpression => blockStatement.addStatement(es)
           case be: BinaryExpression => addBinaryExpression(be, blockStatement)
           case method: MethodCallExpression => addMethodCall(method.getObjectExpression, method.getMethodAsString, method.getArguments, blockStatement)
           case map: MapExpression => addMapExpression(map, blockStatement)
         }
       }
     }
  }

  private def replaceStatement(statement: BlockStatement, expression: ExpressionStatement): Statement = {
    val initialStatements: util.List[Statement] = statement.getStatements
    val position = initialStatements.indexOf(expression)
    expression.getExpression match {
      case mce: MethodCallExpression =>
        val key: String = nameExtract(expression.getExpression)
        log.trace(s"Expanding call of $key at position $position")
        macrodefs.get(key).map(e => {
          val code: Statement = if (e.parameters.isEmpty) e.code else substitute(e, mce)
          initialStatements.set(position, code)
          code
        }) orElse error(s"Macro $key not found anywhere in template")(None)
      case other =>
        error("Macro call must have a form of function call: " + expression.getExpression)_
    }
    val cleared = initialStatements.filterNot(_.getStatementLabel == "macro")
    new BlockStatement(cleared, statement.getVariableScope)
  }

  override def visitClosureExpression(call: ClosureExpression) {
    call.getCode match {
      case bs: BlockStatement => {
        bs.getStatements.foreach( statement =>
          statement match {
            case es: ExpressionStatement => {
              if (es.getStatementLabel == "macro") {
                call.setCode(replaceStatement(bs, es))
              } else {
                es.getExpression match {
                  case call: MethodCallExpression => {
                    this.visitMethodCallExpression(call)
                  }
                  case other =>
                }
              }
            }
            case other =>
          }
        )
      }
      case other =>
    }
  }



  override def visitMethodCallExpression(call: MethodCallExpression) {
    call.getArguments match {
      case ale: ArgumentListExpression => {
         ale.visit(this)
      }
      case _ =>
    }
  }
}

case class Macro(name: String, parameters: List[Macro.ParameterDefinition], code: BlockStatement) {
  val parametersMap = parameters.toMap
  val zippedMap = parameters.zipWithIndex.map({case (definition,index) => (index,definition)}).toMap
}

sealed trait PassedParameter {
  def value: Expression
}
case class NamedParameter(name: String, value: Expression) extends PassedParameter
case class PositionedParameter(position: Int, value: Expression) extends PassedParameter

object Macro {
  type ParameterDefinition = (String, Option[Expression])
}

class MacroCollector(val source: SourceUnit) extends ExpressionTransformer with Logging with SourceReporting {
  var macros: mutable.Map[String, Macro] = new mutable.HashMap[String, Macro]()
  def transform(expression: Expression) : Expression = {
    expression match {
      case a: ClosureExpression => {
        a.getCode match {
          case block: BlockStatement => {
            val statements: util.List[Statement] = block.getStatements
            val seq: Seq[Statement] = statements.toSeq
            val macroDefs: Seq[Statement] = seq.filter(evalStatement)
            if (macroDefs.size > 0) {
              val rest: Seq[Statement] = seq.diff(macroDefs)
              val content = new BlockStatement(rest.toArray, block.getVariableScope)
              a.setCode(content)
            }
            macroDefs.foreach(m => {
               m match {
                 case statement: ExpressionStatement => {
                   statement.getExpression match {
                     case mce: MethodCallExpression => saveExpr(mce)
                     case other =>
                   }
                 }
                 case other =>
               }
            })
            a
          }
          case x => a
        }

      }
      case x => x
    }
  }

  def saveExpr(expression: MethodCallExpression) {
    def parseArgumentList(mapExpr: NamedArgumentListExpression): Option[(String, ClosureExpression)] = {
      val top = mapExpr.getMapEntryExpressions.iterator().next()
      val key: String = top.getKeyExpression.asInstanceOf[ConstantExpression].getValue.toString
      top.getValueExpression match {
        case closure: ClosureExpression => Some(key, closure)
        case other => error(s"Macro body must be a closure: $key; actual $other")(None)
      }
    }
    def parseMacro(name: String, closure: ClosureExpression): Option[Macro] = {
      val parameters = closure.getParameters.map(parameter => {
        new Macro.ParameterDefinition(parameter.getName, Some(parameter.getInitialExpression))
      })
      closure.getCode match {
        case bs: BlockStatement => Some(Macro(name, parameters.toList, closure.getCode.asInstanceOf[BlockStatement]))
        case other => error(s"Macro body must be a block statement: $name; actual: $other")(None)
      }

    }
    val nameAndCode: Option[(String, ClosureExpression)] = expression.getArguments match {
      case mapExpr: NamedArgumentListExpression =>
        parseArgumentList(mapExpr)
      case tuple: TupleExpression =>
        tuple.getExpression(0) match {
          case arguments: NamedArgumentListExpression => parseArgumentList(arguments)
        }
    }
    nameAndCode.flatMap ({case (name,body) => parseMacro(name,body) }) map (result => macros(result.name) = result)
  }

  def evalStatement(statement: Statement): Boolean = {
     statement match {
       case es: ExpressionStatement => {
         es.getExpression match {
           case mce: MethodCallExpression => {
             mce.getMethodAsString == "defmacro"
           }
           case other => false
         }
       }
       case other => {
         false
       }
     }
  }
}
