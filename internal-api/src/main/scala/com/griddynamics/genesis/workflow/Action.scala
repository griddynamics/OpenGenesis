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
 *   Project:     Genesis
 *   Description:  Continuous Delivery Platform
 */
package com.griddynamics.genesis.workflow

import java.util
import com.griddynamics.genesis.model.{Attachment, ActionTrackingStatus}
import java.io.{FileInputStream, File, InputStream}

/* Marker trait for any particular action */
trait Action {
  def desc: String

  final val uuid = util.UUID.randomUUID().toString
}

/* Trait for any action able to be executed on remote agent*/
trait RemoteAgentExec {
   def tag: String
}

/* Base trait for result of particular action */
trait ActionResult {
    def action: Action

    def desc: String

    def outcome: ActionTrackingStatus.ActionStatus = ActionTrackingStatus.Succeed
}

trait ActionFailed extends ActionResult {
    override def outcome = ActionTrackingStatus.Failed
}

trait ActionInterrupted extends ActionResult {
    override def outcome = ActionTrackingStatus.Interrupted
}

trait ResultWithAttachment {
  def attachments: Seq[Attachable]
}

trait Attachable {
  def getContent: Array[Byte]
  def getName: String
  def getType: String
}

class FileAttachable(file: File, `type`: String = "text/plain") extends Attachable with Serializable {
  val content = {
    val is = new FileInputStream(file)
    try {
      Stream.continually(is.read()).takeWhile(-1 !=).map(_.toByte).toArray
    } finally {
      is.close()
    }
  }
  val getContent = content
  def getName = file.getName
  def getType = `type`

}

object FileAttachable {
  def apply(s: String) = new FileAttachable(new File(s))
}


