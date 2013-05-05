package com.griddynamics.genesis.service

import com.griddynamics.genesis.api
import com.griddynamics.genesis.model
import java.io.InputStream

trait AttachmentService{
  def findForAction(actionUUID: String): Seq[api.Attachment]
  def get(key: Int): Option[api.Attachment]
  def insert(attachment: model.Attachment, content: InputStream): model.Attachment
}
