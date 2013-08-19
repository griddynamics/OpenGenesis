package com.griddynamics.genesis.template.dsl.groovy.transformations

import org.codehaus.groovy.transform.{ASTTransformation, GroovyASTTransformation}
import org.codehaus.groovy.control.{SourceUnit, CompilePhase}
import com.griddynamics.genesis.util.Logging
import org.codehaus.groovy.ast.{CodeVisitorSupport, ASTNode}
import org.codehaus.groovy.ast.expr._
import scala.collection.mutable
import org.codehaus.groovy.ast.stmt.{ExpressionStatement, Statement, BlockStatement}
import java.util
import scala.collection.JavaConversions._

@GroovyASTTransformation(phase = CompilePhase.CONVERSION)
class MacroASTTransformation extends ASTTransformation with Logging {
  def visit(nodes: Array[ASTNode], source: SourceUnit) {
    log.trace(s"Inside a macros transformer: ${nodes.length}")
    val methods = source.getAST.getStatementBlock.getStatements
    val it = methods.iterator().next()
    val collector = new MacroCollector
    it.visit(new NameMatchArgumentTransformer("template", collector))
    if (collector.macros.size > 0) {
      methods.iterator().next().visit(new MacroExpandVisitor(collector.macros.toMap))
    }
  }
}

class MacroExpandVisitor(val macrodefs: Map[String, ClosureExpression]) extends CodeVisitorSupport with Logging {

  val nameExtract: PartialFunction[Expression, String] = {
    case call if call.isInstanceOf[MethodCallExpression] => call.asInstanceOf[MethodCallExpression].getMethodAsString
  }

  def replaceStatement(statement: BlockStatement, expression: ExpressionStatement): Statement = {
    val initialStatements: util.List[Statement] = statement.getStatements
    val position = initialStatements.indexOf(expression)
    expression.getExpression match {
      case mce: MethodCallExpression =>
        val key: String = nameExtract(expression.getExpression)
        log.trace(s"Expanding call of $key at position $position")
        macrodefs.get(key).map(e => {
          initialStatements.set(position, e.getCode)
        }) orElse(throw new IllegalArgumentException(s"Macro $key not found anywhere in template"))
      case other =>
        throw new IllegalArgumentException("Macro call must have a form of function call: " + expression.getExpression)
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

class MacroCollector extends ExpressionTransformer with Logging {
  var macros: mutable.Map[String, ClosureExpression] = new mutable.HashMap[String, ClosureExpression]()
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
    def parseArgurmentList(mapExpr: NamedArgumentListExpression): (String, ClosureExpression) = {
      val top = mapExpr.getMapEntryExpressions.iterator().next()
      val key: String = top.getKeyExpression.asInstanceOf[ConstantExpression].getValue.toString
      val closure = top.getValueExpression.asInstanceOf[ClosureExpression]
      (key, closure)
    }
    val (name, body) = expression.getArguments match {
      case mapExpr: NamedArgumentListExpression =>
        parseArgurmentList(mapExpr)
      case tuple: TupleExpression =>
        parseArgurmentList(tuple.getExpression(0).asInstanceOf[NamedArgumentListExpression])
    }
    macros(name) = body
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
