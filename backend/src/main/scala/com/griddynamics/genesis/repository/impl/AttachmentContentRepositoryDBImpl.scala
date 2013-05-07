package com.griddynamics.genesis.repository.impl

import com.griddynamics.genesis.repository.AttachmentContentRepository
import com.griddynamics.genesis.model.{AttachmentContent, DBAttachmentContent, GenesisSchema, Attachment}
import java.io.InputStream
import com.griddynamics.genesis.api
import com.griddynamics.genesis.util.Closeables
import org.squeryl.PrimitiveTypeMode._


class AttachmentContentRepositoryDBImpl extends AttachmentContentRepository {
  def saveAttachmentContent(attachment: Attachment, content: InputStream) {
      Closeables.using(content) { is =>
        val bytes = Stream.continually(is.read()).takeWhile(-1 !=).map(_.toByte).toArray
        GenesisSchema.attachmentContent.insert(new DBAttachmentContent(bytes, attachment.id))
      }
  }

  def getContent(attachment: api.Attachment): Option[AttachmentContent[_]] = {
      from(GenesisSchema.attachmentContent)(item => (where(item.attachmentId === attachment.id) select item)).headOption
  }
}
