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

@GroovyASTTransformation(phase = CompilePhase.SEMANTIC_ANALYSIS)
class PhaseContainerASTTransformation extends ASTTransformation with Logging{
  def visit(nodes: Array[ASTNode], source: SourceUnit) {
    val methods = source.getAST.getStatementBlock.getStatements
    val it = methods.iterator().next()
    it.visit(new PhaseEraser)
  }
}

class PhaseEraser extends CodeVisitorSupport with Logging {
   override def visitMethodCallExpression(call: MethodCallExpression) {
     if (call.getMethodAsString == "steps") {
       log.trace("Found steps call")
       val expr = call.getArguments.transformExpression(new PhaseTransformer)
       call.setArguments(expr)
     } else {
       call.getArguments.visit(this)
     }
   }
}

class PhaseTransformer extends ExpressionTransformer with Logging{
  def transform(expression: Expression): Expression = {
    if (expression.isInstanceOf[ClosureExpression]) {
      val closure: ClosureExpression = expression.asInstanceOf[ClosureExpression]
      val phaseCall = closure.getCode
       val phasesCalls = new PhasesSearch()
       phaseCall.visit(phasesCalls)
       if (phasesCalls.phases.isEmpty) {
         expression
       } else {
         val statements = phasesCalls.phases.toList.collect({case p => p.toStatements}).flatten
         closure.setCode(new BlockStatement((phasesCalls.other ++ statements).toArray, closure.getVariableScope))
         closure
       }
    } else {
      expression
    }
  }
}

class PhasesSearch extends CodeVisitorSupport with Logging {
  val phases: ListBuffer[PhaseDefinition] = ListBuffer[PhaseDefinition]()
  val other: ListBuffer[Statement] = ListBuffer[Statement]()
  override def visitMethodCallExpression(call: MethodCallExpression) {
    if (call.getMethodAsString == "phase") {
      val arguments: Expression = call.getArguments
      val phaseDef = arguments match {
        case a: ArgumentListExpression => extractPhaseDefinition(a)
        case _ => None
      }
      phaseDef.map(phases.append(_))
    } else {
      other.append(new ExpressionStatement(call))
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

  def transform(expression: Expression): Expression = {
    expression match {
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
            case _ => println("No block found")
          }
        }
        arguments
      }
      case _ => log.trace(s"Will not transform ${expression}") ; expression
  }
  }

  def toStatements: List[Statement] = steps.map(step => {
    val statement: ExpressionStatement = step.asInstanceOf[ExpressionStatement]
    val transformExpression: Expression = statement.getExpression.transformExpression(this)
    log.trace(s"Transformed expression: ${transformExpression}")
    new ExpressionStatement(transformExpression)
  })
}

object PhaseDefinition {
  val ASSIGN = Token.newSymbol("=", -1, -1)
}
