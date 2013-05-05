package com.griddynamics.genesis.model

class Attachment(val actionUUID: Option[String], val stepId: Option[Int], val attachmentType: String, val description: String)
  extends GenesisEntity {
}

class DBAttachmentContent(val content: Array[Byte], val attachmentId: GenesisEntity.Id) extends AttachmentContent[GenesisEntity.Id] with GenesisEntity

trait AttachmentContent[B] {
  def content: Array[Byte]
  def attachmentId: B
}
