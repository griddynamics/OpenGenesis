package com.griddynamics.genesis.template.dsl.groovy.transformations

import org.codehaus.groovy.transform.{GroovyASTTransformation, ASTTransformation}
import org.codehaus.groovy.ast._
import org.codehaus.groovy.control.{CompilePhase, SourceUnit}
import com.griddynamics.genesis.util.Logging
import expr._
import stmt.ExpressionStatement
import org.codehaus.groovy.syntax.{Types, Token}

@GroovyASTTransformation(phase = CompilePhase.SEMANTIC_ANALYSIS)
class ContextASTTransformation extends ASTTransformation with Logging {
  def visit(nodes: Array[ASTNode], source: SourceUnit) {
    val methods = source.getAST.getStatementBlock.getStatements
    val it = methods.iterator().next()
    it.visit(new VariableToClosureReplace("$context", source.getAST))
  }
}

class PropertyFinder(val name: String) extends CodeVisitorSupport with Logging {
  var findings: Int = 0
  override def visitPropertyExpression(expression: PropertyExpression) {
    if (isAppropriateExpression(expression)) {
      findings += 1
    }
  }

  def isFound() = {
    findings > 0
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
        log.debug(s"Right expression contains at least one inclusion of token ${name}")
        val visitor = new PropertyFinder(name)
        rightExpression.visit(visitor)
        if (visitor.isFound()) {
          log.debug(s"Found at least one appropriate usage of variable ${name}. Wrapping expression ${rightExpression.getText} with closure")
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
