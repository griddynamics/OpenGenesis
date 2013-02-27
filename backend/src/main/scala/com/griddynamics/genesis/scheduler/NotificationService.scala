/**
 *   Copyright (c) 2010-2012 Grid Dynamics Consulting Services, Inc, All Rights Reserved
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
 *   Description: Continuous Delivery Platform
 */ package com.griddynamics.genesis.scheduler

import com.griddynamics.genesis.configuration.MailServiceContext
import com.griddynamics.genesis.users.UserService
import com.griddynamics.genesis.model.Environment
import com.griddynamics.genesis.util.Logging
import com.griddynamics.genesis.service.ProjectService
import scala.collection.JavaConversions._
import org.springframework.beans.factory.annotation.Autowired

class NotificationService(adminUsername: Option[String],
                          adminEmail: Option[String],
                          userService: UserService,
                          projectService: ProjectService) extends Logging {

  @Autowired var emailService: MailServiceContext = _

  def creatorEmail(env: Environment): Option[String] = {
    if (adminUsername == Option(env.creator) && adminEmail.isDefined) {
      adminEmail
    } else {
      userService.findByUsername(env.creator).map(_.email)
    }
  }

  def notifyCreator(env: Environment, subject: String, message: String) {
    val email = creatorEmail(env)
    try {
      emailService.getEmailService.sendEmail(email.toSeq, subject, message )
    } catch {
      case e: Exception =>
        val emailStr = email.map{ c => if(c.isEmpty) "<EMPTY>" else c }.getOrElse("<NONE>")
        log.error(e, s"Failed to send notification message $subject: '$message' to user ${env.creator} (email: $emailStr)")
    }
  }

  def notifyAdmins(env: Environment, subject: String, message: String) {
    val managerEmail = projectService.get(env.id).flatMap(p => userService.findByUsername(p.projectManager) ).map(_.email)
    val projectAdmins = projectService.getProjectAdmins(env.projectId)
    val adminEmails = userService.findByUsernames(projectAdmins).map(_.email) ++ managerEmail

    try {
      emailService.getEmailService.sendEmail(adminEmails.toList, subject, message )
    } catch {
      case e: Exception => log.error(e, s"Failed to send notification message $subject: '$message' to admins ($adminEmails)")
    }
  }

  def notifySystemAdmin(subject: String, message: String) {
    //todo
  }


}
