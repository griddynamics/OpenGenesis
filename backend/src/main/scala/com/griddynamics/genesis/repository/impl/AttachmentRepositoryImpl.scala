package com.griddynamics.genesis.repository.impl

import com.griddynamics.genesis.repository.AttachmentRepository
import com.griddynamics.genesis.model.{GenesisSchema, Attachment}
import com.griddynamics.genesis.api
import com.griddynamics.genesis.annotation.RemoteGateway
import org.squeryl.PrimitiveTypeMode._
import org.springframework.transaction.annotation.Transactional

@RemoteGateway("Genesis database access: GenesisVersionRepository")
class AttachmentRepositoryImpl extends AttachmentRepository{

  @Transactional(readOnly = true)
  def findByActionUUID(actionUUID: String) = {
    from(GenesisSchema.attachments)(item => (where(item.actionUUID === Some(actionUUID))) select(item)).toList.map(convert(_))
  }

  @Transactional(readOnly = false)
  def insert(attachment: Attachment) : Attachment = {
     GenesisSchema.attachments.insert(attachment)
  }

  @Transactional(readOnly = true)
  def get(key: Int) = from(GenesisSchema.attachments)(item => (where(item.id === key)) select(item)).headOption.map(convert(_))

  implicit def convert(model: Attachment) : api.Attachment = api.Attachment(model.id, model.description, model.attachmentType, model.stepId, model.actionUUID)
}
