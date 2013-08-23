package com.griddynamics.genesis.template.dsl.groovy.transformations

import org.codehaus.groovy.transform.{GroovyASTTransformation, ASTTransformation}
import com.griddynamics.genesis.util.Logging
import org.codehaus.groovy.ast.{CodeVisitorSupport, ASTNode}
import org.codehaus.groovy.control.{CompilePhase, SourceUnit}
import org.codehaus.groovy.ast.stmt.{ExpressionStatement, Statement, BlockStatement}
import org.codehaus.groovy.ast.expr._
import java.util
import scala.collection.mutable.ListBuffer
import scala.collection.JavaConversions._
import org.codehaus.groovy.syntax.Token
import PartialFunction._


@GroovyASTTransformation(phase = CompilePhase.SEMANTIC_ANALYSIS)
class PhaseContainerASTTransformation extends ASTTransformation with Logging{
  def visit(nodes: Array[ASTNode], source: SourceUnit) {
    val methods = source.getAST.getStatementBlock.getStatements
    val it = methods.iterator().next()
    it.visit(new NameMatchArgumentTransformer("steps", new PhaseTransformer))
  }
}

class NameMatchArgumentTransformer(name: String, transformer: ExpressionTransformer) extends CodeVisitorSupport with Logging {
   override def visitMethodCallExpression(call: MethodCallExpression) {
     if (call.getMethodAsString == name) {
       val expr = call.getArguments.transformExpression(transformer)
       call.setArguments(expr)
     } else {
       call.getArguments.visit(this)
     }
   }
}

class PhaseTransformer extends ExpressionTransformer with Logging{
  def transform(expression: Expression): Expression = expression match {
    case closure: ClosureExpression => {
      val collector = new PhasesCollector()
      closure.getCode.visit(collector)
      val unwrapped = collector.phases.toList.collect({case p => p.toStatements}).flatten
      closure.setCode(new BlockStatement((collector.plainSteps ++ unwrapped).toArray, closure.getVariableScope))
      closure
    }
    case x => x
  }
}

class PhasesCollector extends CodeVisitorSupport with Logging {
  val phases: ListBuffer[PhaseDefinition] = ListBuffer[PhaseDefinition]()
  val plainSteps: ListBuffer[Statement] = ListBuffer[Statement]()
  override def visitMethodCallExpression(call: MethodCallExpression) {
    if (call.getMethodAsString == "phase") {
      val arguments: Expression = call.getArguments
      val phaseDef = arguments match {
        case a: ArgumentListExpression => extractPhaseDefinition(a)
        case _ => None
      }
      phaseDef.map(phases.append(_))
    } else {
      plainSteps.append(new ExpressionStatement(call))
    }
  }

  def extractPhaseDefinition(list: ArgumentListExpression): Option[PhaseDefinition] = {
    def hasName(name: String, expr: Expression) = {
      expr.isInstanceOf[ConstantExpression] && expr.asInstanceOf[ConstantExpression].getValue == name
    }

    def hasType(klass: Class[_], expr: Expression) = {
      expr.getClass == klass
    }

    def readPhaseArguments(expr: MapExpression) : Option[PhaseDefinition] = {
       if (expr.getMapEntryExpressions.size() > 0) {
         import scala.collection.JavaConversions._
         val nameExpr = expr.getMapEntryExpressions.find(p => hasName("name", p.getKeyExpression))
         val afterExpr = expr.getMapEntryExpressions.find(p => hasName("after", p.getKeyExpression) && hasType(classOf[ListExpression], p.getValueExpression))
         (nameExpr, afterExpr) match {
           case (None, _) => None
           case (Some(name), None) => Some(new PhaseDefinition(name.getValueExpression))
           case (Some(name), Some(lst)) => Some(new PhaseDefinition(name.getValueExpression,
             lst.getValueExpression.asInstanceOf[ListExpression].getExpressions.toList))
         }
       } else {
         None
       }
    }
    val expressions: util.List[Expression] = list.getExpressions
    if (expressions.size() < 2) {
      None
    } else {
      val firstExpression: Expression = expressions.get(0)
      val definition = (firstExpression match {
        case map: MapExpression => readPhaseArguments(map)
        case _ => None
      }).flatMap(phase => {
         expressions.get(1).asInstanceOf[ClosureExpression].getCode match {
           case block: BlockStatement => Some(phase.copy(steps = block.getStatements.toList))
           case _ => None
         }
      })
      definition
    }
  }
}


case class PhaseDefinition(name: Expression, preceding: List[Expression] = List(), steps: List[Statement] = List()) extends ExpressionTransformer with Logging {

  val expressionTransformer: PartialFunction[Expression, Expression] = {
    case arguments: ArgumentListExpression => {
      val expressions: util.List[Expression] = arguments.getExpressions
      if (! expressions.isEmpty) {
        val closure: ClosureExpression = expressions.get(0).asInstanceOf[ClosureExpression]
        val stepCode = closure.getCode
        stepCode match {
          case block: BlockStatement => {
            val nameStatement: BinaryExpression = new BinaryExpression(new VariableExpression("phase"),
              PhaseDefinition.ASSIGN, name)
            block.addStatement(new ExpressionStatement(nameStatement))
            if (! preceding.isEmpty) {
              val precedingStatement = new BinaryExpression(new VariableExpression("precedingPhases"),
                PhaseDefinition.ASSIGN, new ListExpression(preceding))
              block.addStatement(new ExpressionStatement(precedingStatement))
            }
            closure.setCode(block)
          }
          case _ => log.trace("No block found in closure, so don't append phases-related assignments")
        }
      }
      arguments
    }
  }

  def transform(expression: Expression): Expression =
    condOpt(expression)(expressionTransformer).getOrElse(expression)

  def toStatements: List[Statement] = steps.map({case step: ExpressionStatement => {
    val transformExpression: Expression = step.getExpression.transformExpression(this)
    log.trace(s"Transformed expression: $transformExpression")
    new ExpressionStatement(transformExpression)
  }})
}

object PhaseDefinition {
  val ASSIGN = Token.newSymbol("=", -1, -1)
}
