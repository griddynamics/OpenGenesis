/**
 * Copyright (c) 2010-2012 Grid Dynamics Consulting Services, Inc, All Rights Reserved
 *   http://www.griddynamics.com
 *
 *   This library is free software; you can redistribute it and/or modify it under the terms of
 *   the GNU Lesser General Public License as published by the Free Software Foundation; either
 *   version 2.1 of the License, or any later version.
 *
 *   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 *   AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 *   IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *   DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE
 *   FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 *   DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 *   SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 *   CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 *   OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 *   OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * @Project:     Genesis
 * @Description: Execution Workflow Engine
 */
package com.griddynamics.genesis.service.impl

import com.griddynamics.genesis.common.CRUDService
import com.griddynamics.genesis.validation.Validation
import com.griddynamics.genesis.repository.Repository
import org.springframework.transaction.annotation.Transactional
import com.griddynamics.genesis.api.RequestResult

abstract class AbstractCRUDService[A](repository: Repository[A]) extends CRUDService[A, Int] with Validation[A]  {

  @Transactional(readOnly = true)
  def list = repository.list

  @Transactional(readOnly = true)
  override def get(id: Int) = repository.get(id)

  @Transactional
  override def delete(entity: A) {
    repository.delete(entity)
    RequestResult(isSuccess = true)
  }

  @Transactional
  override def create(creds: A) = validCreate(creds, repository.insert(_))

  @Transactional
  override def update(creds: A) = validUpdate(creds, repository.update(_))

}