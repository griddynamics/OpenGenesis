package com.griddynamics.genesis.spring.security.acls

import org.springframework.security.acls.model.{ObjectIdentity, ObjectIdentityRetrievalStrategy, ObjectIdentityGenerator}
import java.io.Serializable
import java.lang.Class
import org.springframework.util.{Assert, ClassUtils}
import org.springframework.security.acls.domain.{ObjectIdentityImpl, IdentityUnavailableException}

class ScalaObjectIdentityGenerator extends ObjectIdentityRetrievalStrategy with ObjectIdentityGenerator {

  def getObjectIdentity(domainObject: AnyRef): ObjectIdentity = {
    ScalaObjectIdentityImpl(domainObject)
  }

  def createObjectIdentity(id: Serializable, `type`: String): ObjectIdentity = {
    ScalaObjectIdentityImpl(`type`, id)
  }
}


class ScalaObjectIdentityImpl(javaType: String,
                              identifier: Serializable) extends ObjectIdentityImpl(javaType, identifier) {
  override def getIdentifier = identifier
  override def getType = javaType
}

object ScalaObjectIdentityImpl {

  def apply(javaType: String, identitier: Serializable) = new ScalaObjectIdentityImpl(javaType, identitier)

  def apply(javaType: Class[_], identifier: Serializable): ObjectIdentity =
    new ScalaObjectIdentityImpl(javaType.getName, identifier)

  def apply(domainObject: AnyRef): ObjectIdentity = {
    val typeClass: Class[_] = ClassUtils.getUserClass(domainObject.getClass)
    val result = domainObject match {
      case x: { val id: Option[_] } =>
        x.id.getOrElse(throw new IllegalArgumentException("id is required to return a non-null value"))
      case _ =>
        throw new IdentityUnavailableException("Could not extract identity from object " + domainObject)
    }

    Assert.isInstanceOf(classOf[Serializable], result, "Getter must provide a return value of type Serializable")

    new ScalaObjectIdentityImpl(typeClass.getName, result.asInstanceOf[Serializable])
  }
}
