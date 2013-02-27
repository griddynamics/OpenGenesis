package com.griddynamics.genesis.template.dsl.groovy.transformations

import org.codehaus.groovy.transform.{GroovyASTTransformation, ASTTransformation}
import org.codehaus.groovy.ast._
import org.codehaus.groovy.control.{CompilePhase, SourceUnit}
import com.griddynamics.genesis.util.Logging
import expr._
import stmt.ExpressionStatement
import org.codehaus.groovy.syntax.Types
import com.griddynamics.genesis.template.dsl.groovy.Reserved

@GroovyASTTransformation(phase = CompilePhase.SEMANTIC_ANALYSIS)
class ContextASTTransformation extends ASTTransformation with Logging {
  def visit(nodes: Array[ASTNode], source: SourceUnit) {
    val methods = source.getAST.getStatementBlock.getStatements
    val it = methods.iterator().next()
    it.visit(new VariableToClosureReplace(Reserved.contextRef, source.getAST))
    it.visit(new VariableToClosureReplace(Reserved.instanceRef, source.getAST))
  }
}

class PropertyFinder(val name: String) extends CodeVisitorSupport with Logging {
  var findings: Int = 0

  /**
   * This method searches for property expression involving required variable.
   * Case when variable is considered found:
   *
   * - Code that use at least one property of variable, for example
   *
   * <b>$context</b>.property
   *
   * @param expression expression to visit
   */
  override def visitPropertyExpression(expression: PropertyExpression) {
    log.trace(s"Examining property expression ${expression.getText}")
    if (isAppropriateExpression(expression)) {
      log.trace(s"Assuming property expression ${expression.getText} as appropriate for variable ${name}")
      findings += 1
    }
  }

  def isFound() = {
    findings > 0
  }

  /**
   * This method searches for binary expression involving required variable.
   * Case when variable is considered found:
   *
   * - Getting anything by index or key from required variable
   *
   * <b>$context</b>[something]
   *
   * @param expression expression to visit
   */
  override def visitBinaryExpression(expression: BinaryExpression) {
    log.trace(s"Examining binary expression ${expression.getText}")
    val right = expression.getRightExpression
    var left = expression.getLeftExpression
    if (expression.getOperation.getMeaning == Types.LEFT_SQUARE_BRACKET) {
      if (left.isInstanceOf[VariableExpression] && left.asInstanceOf[VariableExpression].getName == name) {
        if (right.isInstanceOf[ConstantExpression] || right.isInstanceOf[VariableExpression]) {
          log.trace(s"Assuming binary expression ${expression.getText} as appropriate for variable ${name}")
          findings += 1
        }
      }
    } else if (left.isInstanceOf[PropertyExpression]) {
      left.visit(this)
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

class VariableToClosureReplace(val name: String, val node: ModuleNode) extends CodeVisitorSupport with Logging {

  override def visitBinaryExpression(expression: BinaryExpression) {
    val rightExpression = expression.getRightExpression
    if (expression.getOperation.getMeaning == Types.EQUAL) {
      if (! rightExpression.isInstanceOf[ClosureExpression] && rightExpression.getText.contains(name)) {
        log.trace(s"Right expression contains has a textual inclusion of token ${name}")
        val visitor = new PropertyFinder(name)
        rightExpression.visit(visitor)
        if (visitor.isFound()) {
          log.trace(s"Found at least one appropriate usage of variable ${name}. Wrapping expression ${rightExpression.getText} with closure")
          expression.setRightExpression(wrapWithClosure(rightExpression))
        }
      }
    }
  }


  private def wrapWithClosure(top: Expression) : ClosureExpression = {
    val block = new ExpressionStatement(top)
    val expression: ClosureExpression = new ClosureExpression(null, block)
    expression.setVariableScope(new VariableScope())
    expression
  }
}
