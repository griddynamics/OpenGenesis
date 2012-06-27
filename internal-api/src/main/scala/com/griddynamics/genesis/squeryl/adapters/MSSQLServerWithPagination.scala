package com.griddynamics.genesis.squeryl.adapters

import org.squeryl.internals.StatementWriter
import org.squeryl.adapters.MSSQLServer
import org.squeryl.dsl.ast.QueryExpressionElements

class MSSQLServerWithPagination extends MSSQLServer {

    override def writeQuery(qen: QueryExpressionElements, sw: StatementWriter) {
        if(qen.page == None)
            super.writeQuery(qen, sw)
        else {
            sw.write("With ___row___ as (Select")

            if(qen.selectDistinct)
                sw.write(" distinct")

            sw.write(" row_number() over (order by ")
            val ob = qen.orderByClause.filter(e => ! e.inhibited)
            sw.writeNodesWithSeparator(ob, ",", true)
            sw.write(") as rowNo, ")
            sw.nextLine
            sw.writeIndented {
                sw.writeNodesWithSeparator(qen.selectList.filter(e => ! e.inhibited), ",", true)
            }
            sw.nextLine
            sw.write("From")
            sw.nextLine


            if(!qen.isJoinForm) {
                sw.writeIndented {
                    qen.outerJoinExpressionsDEPRECATED match {
                        case Nil =>
                            for(z <- qen.tableExpressions.zipi) {
                                z.element.write(sw)
                                sw.write(" ")
                                sw.write(sw.quoteName(z.element.alias))
                                if(!z.isLast) {
                                    sw.write(",")
                                    sw.nextLine
                                }
                            }
                            sw.pushPendingNextLine
                        case joinExprs =>
                            for(z <- qen.tableExpressions.zipi) {
                                if(z.isFirst) {
                                    z.element.write(sw)
                                    sw.write(" ")
                                    sw.write(sw.quoteName(z.element.alias))
                                    sw.indent
                                    sw.nextLine
                                } else {
                                    sw.write("inner join ")
                                    z.element.write(sw)
                                    sw.write(" as ")
                                    sw.write(sw.quoteName(z.element.alias))
                                    sw.nextLine
                                }
                            }
                            for(oje <- joinExprs.zipi) {
                                writeOuterJoinDEPRECATED(oje.element, sw)
                                if(oje.isLast)
                                    sw.unindent
                                sw.pushPendingNextLine
                            }
                    }
                }
            }
            else {
                val singleNonJoinTableExpression = qen.tableExpressions.filter(! _.isMemberOfJoinList)
                assert(singleNonJoinTableExpression.size == 1, "join query must have exactly one FROM argument, got : " + qen.tableExpressions)
                val firstJoinExpr = singleNonJoinTableExpression.head
                val restOfJoinExpr = qen.tableExpressions.filter(_.isMemberOfJoinList)
                firstJoinExpr.write(sw)
                sw.write(" ")
                sw.write(sw.quoteName(firstJoinExpr.alias))
                sw.nextLine

                for(z <- restOfJoinExpr.zipi) {
                    writeJoin(z.element, sw)
                    if(z.isLast)
                        sw.unindent
                    sw.pushPendingNextLine
                }
            }

            writeEndOfFromHint(qen, sw)

            if(qen.hasUnInhibitedWhereClause) {
                sw.write("Where")
                sw.nextLine
                sw.writeIndented {
                    qen.whereClause.get.write(sw)
                }
                sw.pushPendingNextLine
            }

            if(! qen.groupByClause.isEmpty) {
                sw.write("Group By")
                sw.nextLine
                sw.writeIndented {
                    sw.writeNodesWithSeparator(qen.groupByClause.filter(e => ! e.inhibited), ",", true)
                }
                sw.pushPendingNextLine
            }

            if(! qen.havingClause.isEmpty) {
                sw.write("Having")
                sw.nextLine
                sw.writeIndented {
                    sw.writeNodesWithSeparator(qen.havingClause.filter(e => ! e.inhibited), ",", true)
                }
                sw.pushPendingNextLine
            }

            writeEndOfQueryHint(qen, sw)

            writePaginatedQueryDeclaration(qen, sw)

        }
    }

    override def writePaginatedQueryDeclaration(qen: QueryExpressionElements, sw: StatementWriter) {
        if (qen.page != None) {
            assert(!qen.orderByClause.isEmpty, "\"order by\" clause must be defined for the paginal query")
            val page = qen.page.get
            val beginOffset = page._1
            val pageSize = page._2

            sw.write(")")
            sw.nextLine
            sw.write("Select top ", pageSize.toString)
            sw.nextLine
            sw.writeIndented {
                sw.writeLinesWithSeparator(qen.selectList.filter(e => ! e.inhibited).map(_.alias.replace(".", "_")), ",")
            }
            sw.nextLine
            sw.write("From ___row___")
            sw.nextLine
            sw.write("Where rowNo >= ", beginOffset.toString)
        }
    }
}
