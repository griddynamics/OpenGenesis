package com.griddynamics.genesis.service.impl

import com.griddynamics.genesis.service.AttachmentService
import com.griddynamics.genesis.api.Attachment
import com.griddynamics.genesis.repository.{AttachmentContentRepository, AttachmentRepository}
import com.griddynamics.genesis.model
import org.springframework.transaction.annotation.Transactional
import java.io.InputStream

class AttachmentServiceImpl(repository: AttachmentRepository, contentRepository: AttachmentContentRepository) extends AttachmentService {
  @Transactional(readOnly = true)
  def findForAction(actionUUID: String): Seq[Attachment] = repository.findByActionUUID(actionUUID)
  @Transactional(readOnly = true)
  def get(key: Int): Option[Attachment] = repository.get(key)
  @Transactional(readOnly = false)
  def insert(attachment: model.Attachment, content: InputStream): model.Attachment = {
    val saved = repository.insert(attachment)
    contentRepository.saveAttachmentContent(saved, content)
    saved
  }
  @Transactional(readOnly = true)
  def getContent(attachment: Attachment): Array[Byte] = contentRepository.getContent(attachment).map(_.content).getOrElse(Array())
}
