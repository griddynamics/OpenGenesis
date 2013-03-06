package com.griddynamics.genesis.repository.impl

import org.scalatest.junit.AssertionsForJUnit
import org.scalatest.mock.MockitoSugar
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.FunSpec
import com.griddynamics.genesis.service.impl.DSLTestUniverse
import com.typesafe.config.{ConfigResolveOptions, ConfigParseOptions, ConfigFactory}
import com.griddynamics.genesis.model.DataBagItem

class DatabagTemplateReaderTest extends AssertionsForJUnit with MockitoSugar with ShouldMatchers with FunSpec with DSLTestUniverse  {


  describe("Databag template reader") {
    it("Should read a test template") {
      val withName = ConfigFactory.load("databags/test.dbtemplate", ConfigParseOptions.defaults(), ConfigResolveOptions.noSystem())
      val template1 = DatabagTemplateReader.read(withName)
      template1.name should equal("Test databag template")
      template1.id should equal("1")
      template1.defaultName should equal(Some("test"))
      template1.scope should equal("project")
      template1.values.size should equal(2)
      val databag = template1.databag
      databag.name should equal("test")
      databag.tags should equal(List("foo", "bar", "baz").mkString(","))
      val items = template1.items
      val longKey: Option[DataBagItem] = items.find(item => item.itemKey == "some-long-key")
      val otherKey: Option[DataBagItem] = items.find(item => item.itemKey == "other-long-key")
      longKey should not be None
      otherKey should not be None
    }

    it("Should read a test template without default name and tags") {
      val withoutName = ConfigFactory.load("databags/test-no-name.dbtemplate", ConfigParseOptions.defaults(), ConfigResolveOptions.noSystem())
      val template2 = DatabagTemplateReader.read(withoutName)
      template2.id should equal("2")
      template2.defaultName should equal(None)
      val databag2 = template2.databag
      databag2.name should equal("")
      databag2.tags should equal("")
    }
  }
}
