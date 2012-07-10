/**
 * Copyright (c) 2010-2012 Grid Dynamics Consulting Services, Inc, All Rights Reserved
 * http://www.griddynamics.com
 *
 * This library is free software; you can redistribute it and/or modify it under the terms of
 * the GNU Lesser General Public License as published by the Free Software Foundation; either
 * version 2.1 of the License, or any later version.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * @Project:     Genesis
 * @Description: Execution Workflow Engine
 */
package com.griddynamics.genesis.repository.impl

import com.griddynamics.genesis.repository.AbstractGenericRepository
import com.griddynamics.genesis.{repository, api, model}
import api._
import api.Failure
import api.Success
import model.{GenesisSchema => GS, ProjectContextEntry => PP}
import org.squeryl.PrimitiveTypeMode._
import org.springframework.transaction.annotation.Transactional
import com.griddynamics.genesis.util.Logging

class ProjectPropertyRepository extends AbstractGenericRepository[model.ProjectContextEntry, api.ProjectProperty](GS.projectProperties)
with repository.ProjectPropertyRepository with Logging {

    val namePattern = """^([a-zA-Z0-9.]{1,1024})$""".r

    @Transactional(readOnly = true)
    def listForProject(projectId: Int): List[api.ProjectProperty] = {
        val modelProperties = from(table)(pp => where(pp.projectId === projectId) select(pp) orderBy(pp.name asc)).toList
        modelProperties.map(convert(_))
    }

    @Transactional
    def delete(pid: Int, keys: List[String]) = {
        val count: Int = keys.map(p =>
            table.deleteWhere(pp => pp.projectId === pid and pp.name === p)
        ).reduceLeft(_ + _)
        Success(count)
    }

    @Transactional
    def create(pid: Int, properties: List[api.ProjectProperty]) : ExtendedResult[Int] = {
        val validated = properties.groupBy(_.name).map(e => if (e._2.size > 1)
            Failure(compoundServiceErrors = Seq("Project property name '%s' is duplicated".format(e._1)))
        else
            validate(e._2.head) ++ uniqueName(e._2.head, pid)).fold(Success(List()))((total, n) => n :: total)
        validated.map(list => {
            val iterable: Iterable[PP] = list.asInstanceOf[Iterable[PP]]
            table.insert(iterable.map(pp => new PP(pid, pp.name, pp.value)))
            iterable.size
        })
    }

    @Transactional
    def updateForProject(pid: Int, properties : List[api.ProjectProperty]): ExtendedResult[Int] = {
        val left = properties.groupBy(_.name).map(e => if (e._2.size > 1)
            Failure(compoundServiceErrors = Seq("Project property name '%s' is duplicated".format(e._1)))
        else
            validate(e._2.head)).fold(Success(List()))((total, n) => n :: total)
        left.map(list => {
            table.deleteWhere(pp => pp.projectId === pid)
            val model: List[PP] = list.asInstanceOf[Iterable[PP]].map(pp => new PP(pid, pp.name, pp.value)).toList
            table.insert(model)
            model.size
        })
    }

    @Transactional
    def read(pid: Int, key: String) = {
      from(table)(pp => where(pp.projectId === pid and pp.name === key) select(pp)).headOption.map(_.value)
    }


    override implicit def convert(entity: PP): api.ProjectProperty = {
        new api.ProjectProperty(entity.id, entity.projectId, entity.name, entity.value)
    }

    override implicit def convert(dto: api.ProjectProperty): PP = {
        val projectProperty = new PP(dto.projectId, dto.name, dto.value)
        projectProperty.id = dto.id
        projectProperty
    }

    def uniqueName(property: PP, pid: Int) : ExtendedResult[PP] =
        from(table)(pp => where(pp.name === property.name and pp.projectId === pid).select(pp)).headOption match {
            case None => Success(property)
            case _ => Failure(compoundServiceErrors = Seq("Duplicated property name %s".format(property.name)))
        }

    def validate(property: PP): ExtendedResult[PP] = {
        val nameValid = if ((property.name == null) || property.name.isEmpty) {
            Failure(compoundServiceErrors = Seq("Project property name is empty"))
        } else {
            Success(property)
        }

        val notEmpty = if (property.value == null || property.value.trim.isEmpty)  {
            Failure(compoundServiceErrors =  Seq("Property %s is empty".format(String.valueOf(property.name))))
        } else {
            Success(property)
        }

        val patternValid = if (namePattern.findFirstIn(property.name) == None) {
            Failure(compoundServiceErrors = Seq("Invalid format. Use a combination of latin letters, numbers and dots. Length must be from 1 to 1024".format(property.name)))
        } else {
            Success(property)
        }
        nameValid ++ notEmpty ++ patternValid
    }
}
