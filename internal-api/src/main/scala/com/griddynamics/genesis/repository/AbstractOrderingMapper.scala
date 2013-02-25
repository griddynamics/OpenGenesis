/*
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
 * Project:     Genesis
 * Description:  Continuous Delivery Platform
 */
package com.griddynamics.genesis.repository

import com.griddynamics.genesis.api
import api.Directions
import com.griddynamics.genesis.model.GenesisEntity
import org.squeryl.PrimitiveTypeMode._
import org.squeryl.dsl.ast.TypedExpressionNode
import org.squeryl.KeyedEntity

trait AbstractOrderingMapper[Model <: KeyedEntity[GenesisEntity.Id]] {
  protected def mapFieldsAstField(model: Model): Map[String, TypedExpressionNode[_]]

  private[genesis] def order(model: Model, ordering: api.Ordering) = {
    val mapping = mapFieldsAstField(model)

    if (!mapping.contains(ordering.field))
      throw new IllegalArgumentException("Unsupported ordering field '%s'".format(ordering.field))

    val field = mapping(ordering.field)
    if (ordering.direction == Directions.ASC) field.asc else field.desc
  }
}
