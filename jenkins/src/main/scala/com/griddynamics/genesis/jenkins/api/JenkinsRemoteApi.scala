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
 * Project:     Genesis
 * Description:  Continuous Delivery Platform
 */
package com.griddynamics.genesis.jenkins.api

import org.apache.http.client.methods.{HttpRequestBase, HttpGet, HttpPost}
import java.io.InputStream
import org.apache.http.{HttpResponse, HttpEntity}
import org.apache.http.util.EntityUtils

class JenkinsRemoteApi(specification: JenkinsConnectSpecification) {

  def post[B <: AnyRef](s: String)(f: InputStream => B) = exec(new HttpPost(s))(f)

  def postNoResult(s: String) {
    execNoResult(new HttpPost(s))
  }

  def get[B <: AnyRef](s: String)(f: InputStream => B) = exec(new HttpGet(s))(f)

  private def exec[B <: AnyRef](method: HttpRequestBase)(f: InputStream => B): B = {
    execute(method) match {
      case Left(code) => throw new IllegalStateException("Request failed, status code is " + code)
      case Right(r) => {
        val entity: HttpEntity = r.getEntity
        try {
          f(entity.getContent)
        } finally {
          EntityUtils.consume(entity)
        }
      }
    }
  }

  private def execNoResult(method: HttpRequestBase) {
    execute(method) match {
      case Left(code) => throw new IllegalStateException("Request failed, status code is " + code)
      case _ =>
    }
  }


  private def execute(method: HttpRequestBase): Either[Int, HttpResponse] = {
    val response = specification.context match {
      case None => specification.client.execute(method)
      case Some(c) => specification.client.execute(method, c)
    }
    val code: Int = response.getStatusLine.getStatusCode
    if (code >= 400) {
      EntityUtils.consume(response.getEntity)
      Left(code)
    } else {
      Right(response)
    }
  }
}
