package com.griddynamics.genesis.template.dsl.groovy.transformations

import org.codehaus.groovy.transform.{GroovyASTTransformation, ASTTransformation}
import org.codehaus.groovy.ast._
import org.codehaus.groovy.control.{CompilePhase, SourceUnit}
import com.griddynamics.genesis.util.Logging
import expr._
import stmt.ExpressionStatement

@GroovyASTTransformation(phase = CompilePhase.SEMANTIC_ANALYSIS)
class ContextASTTransformation extends ASTTransformation with Logging {
  def visit(nodes: Array[ASTNode], source: SourceUnit) {
    val methods = source.getAST.getStatementBlock.getStatements
    val it = methods.iterator().next()
    it.visit(new VariableToClosureReplace("$context", source.getAST))
  }
}

class VariableToClosureReplace(val name: String, val node: ModuleNode) extends CodeVisitorSupport with Logging {

  override def visitBinaryExpression(expression: BinaryExpression) {
    val rightExpression = expression.getRightExpression
    if (rightExpression.isInstanceOf[PropertyExpression]) {
      val propertyExpression = rightExpression.asInstanceOf[PropertyExpression]
      if (isAppropriateExpression(propertyExpression)){
        log.debug(s"Found appropriate assignment to ${name} variable in expression ${expression.getText}")
        expression.setRightExpression(buildClosure(propertyExpression))
      } else {
        log.debug(s"Not transforming ${propertyExpression.getText}")
      }
    }
  }


  private def buildClosure(top: PropertyExpression) : ClosureExpression = {
    val block = new ExpressionStatement(stripLeader(top))
    val expression: ClosureExpression = new ClosureExpression(null, block)
    expression.setVariableScope(new VariableScope())
    expression
  }


  private def stripLeader(top: PropertyExpression): Expression = {
    val (_, expressions) = getLatestPropertyExpression(top)
    expressions match {
      case x :: xs => {
        val newVariable = new VariableExpression(x.asInstanceOf[ConstantExpression].getValue.toString)
        val newTop = xs.fold(newVariable)((acc, e) => new PropertyExpression(acc, e))
        log.debug(s"Shortened expression ${top.getText} to form ${newTop.getText}")
        newTop
      }
      case _ => {
        log.debug("Expression list was empty. No transformation applied")
        top
      }
    }
  }

  private def isAppropriateExpression(top: PropertyExpression): Boolean = {
    val (left, list) = getLatestPropertyExpression(top)
    left.getObjectExpression.isInstanceOf[VariableExpression] &&
      left.getObjectExpression.asInstanceOf[VariableExpression].getName == name &&
      ! list.isEmpty
  }

  def getLatestPropertyExpression(top: PropertyExpression): (PropertyExpression, List[Expression]) = {
    var originalExpression = top
    var expressions: List[Expression] = List()
    while (originalExpression.getObjectExpression.isInstanceOf[PropertyExpression]) {
      expressions = originalExpression.getProperty :: expressions
      originalExpression = originalExpression.getObjectExpression.asInstanceOf[PropertyExpression]
    }
    (originalExpression, originalExpression.getProperty :: expressions)
  }
}
