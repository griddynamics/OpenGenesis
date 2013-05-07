package com.griddynamics.genesis.repository

import com.griddynamics.genesis.model.{AttachmentContent, Attachment}
import com.griddynamics.genesis.api
import java.io.InputStream

trait AttachmentContentRepository {
   def saveAttachmentContent(attachment: Attachment, content: InputStream)
   def getContent(attachment: api.Attachment): Option[AttachmentContent[_]]
}
