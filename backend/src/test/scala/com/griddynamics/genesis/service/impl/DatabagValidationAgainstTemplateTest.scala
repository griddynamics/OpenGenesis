package com.griddynamics.genesis.service.impl

import org.scalatest.junit.AssertionsForJUnit
import org.scalatest.mock.MockitoSugar
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.FunSpec
import com.griddynamics.genesis.repository.impl.DatabagTemplateRepositoryImpl
import com.griddynamics.genesis.api.{Failure, DataItem, ExtendedResult, DataBag}
import com.griddynamics.genesis.validation.IntValidator

class DatabagValidationAgainstTemplateTest extends AssertionsForJUnit with MockitoSugar with ShouldMatchers with FunSpec with DSLTestUniverse {
  val repository = new DatabagTemplateRepositoryImpl("../resources/databags", "*.dbtemplate")
  val service = new DataBagServiceImpl(null, repository, Map("int_nonnegative" -> new IntValidator(0, Int.MaxValue)))
  val emptyDatabag =  new DataBag(None, "", Seq(), None, Some("1"))

  val databagWithRequiredKey = emptyDatabag.copy(items = Seq(
    new DataItem(None, "other-long-key", "value", None)
  ))

  val databagWithErrorInValues = emptyDatabag.copy(items = Seq(
    new DataItem(None, "other-long-key", "value", None),
    new DataItem(None, "some-long-key", "aaa", None)
  ))

  val validDatabag = emptyDatabag.copy(items = Seq(
    new DataItem(None, "other-long-key", "value", None),
    new DataItem(None, "some-long-key", "123", None)
  ))

  describe("Databag validation against template") {
    it ("Should report missing keys") {
      val missingErrors = service.validateRequiredKeys(repository.get("1").get, emptyDatabag)
      missingErrors.isSuccess should equal(false)
      missingErrors.asInstanceOf[Failure].serviceErrors.get("other-long-key") should not be None
    }

    it ("Should not report missing keys when no required key is specified in template") {
      val missingErrors = service.validateRequiredKeys(repository.get("2").get, emptyDatabag)
      missingErrors.isSuccess should equal(true)
    }

    it ("Should not report missing keys when databag contains them") {
      val missingErrors = service.validateRequiredKeys(repository.get("1").get, databagWithRequiredKey)
      missingErrors.isSuccess should equal(true)
    }

    it ("Should report validation errors when databag value is invalid") {
      val missingErrors = service.validAccordingToTemplate(databagWithErrorInValues)
      missingErrors.isSuccess should equal(false)
    }

    it ("Should not report validation errors when databag value is valid") {
      val missingErrors = service.validAccordingToTemplate(databagWithErrorInValues)
      missingErrors.isSuccess should equal(false)
    }

  }
}
