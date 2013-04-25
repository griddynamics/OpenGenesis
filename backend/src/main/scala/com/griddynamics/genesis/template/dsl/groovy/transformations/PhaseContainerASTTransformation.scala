package com.griddynamics.genesis.template.dsl.groovy.transformations

import org.codehaus.groovy.transform.{GroovyASTTransformation, ASTTransformation}
import com.griddynamics.genesis.util.Logging
import org.codehaus.groovy.ast.{CodeVisitorSupport, ASTNode}
import org.codehaus.groovy.control.{CompilePhase, SourceUnit}
import org.codehaus.groovy.ast.stmt.{Statement, BlockStatement}
import org.codehaus.groovy.ast.expr._
import java.util
import scala.collection.mutable.ListBuffer
import scala.collection.JavaConversions._

@GroovyASTTransformation(phase = CompilePhase.SEMANTIC_ANALYSIS)
class PhaseContainerASTTransformation extends ASTTransformation with Logging{
  def visit(nodes: Array[ASTNode], source: SourceUnit) {
    log.debug("In phase transformation")
    val methods = source.getAST.getStatementBlock.getStatements
    val it = methods.iterator().next()
    it.visit(new PhaseEraser)
  }
}

class PhaseEraser extends CodeVisitorSupport with Logging {
   override def visitMethodCallExpression(call: MethodCallExpression) {
     if (call.getMethodAsString == "steps") {
       log.debug("Found steps call")
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
       log.debug(s"Found ${phasesCalls.found} of phases container at examined expression")
       if (phasesCalls.found == 0) {
         expression
       } else {
         val statements = phasesCalls.phases.toList.collect({case p => p.toStatements}).flatten
         closure.setCode(new BlockStatement(statements.toArray, closure.getVariableScope))
         closure
       }
    } else {
      expression
    }
  }
}

class PhasesSearch extends CodeVisitorSupport with Logging {
  var found: Int = 0
  val phases: ListBuffer[PhaseDefinition] = ListBuffer[PhaseDefinition]()
  override def visitMethodCallExpression(call: MethodCallExpression) {
    if (call.getMethodAsString == "phase") {
      found += 1
      val arguments: Expression = call.getArguments
      val phaseDef = arguments match {
        case a: ArgumentListExpression => extractPhaseDefinition(a)
        case _ => None
      }
      phaseDef.map(phases.append(_))
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


case class PhaseDefinition(name: Expression, preceding: List[Expression] = List(), steps: List[Statement] = List()) {
  def toStatements: List[Statement] = steps
}
