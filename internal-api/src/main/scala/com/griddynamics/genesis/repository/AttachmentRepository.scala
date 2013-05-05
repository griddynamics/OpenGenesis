package com.griddynamics.genesis.repository

import com.griddynamics.genesis.api.{Attachment => Api}
import com.griddynamics.genesis.model.Attachment

trait AttachmentRepository {
   def findByActionUUID(actionUUID: String) : Seq[Api]
   def insert(attachment: Attachment) : Attachment
   def get(key: Int): Option[Api]
}
