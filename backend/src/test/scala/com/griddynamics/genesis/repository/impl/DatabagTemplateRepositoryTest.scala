package com.griddynamics.genesis.repository.impl

import org.scalatest.junit.AssertionsForJUnit
import org.scalatest.mock.MockitoSugar
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.FunSpec
import com.griddynamics.genesis.service.impl.DSLTestUniverse
import com.griddynamics.genesis.repository.DatabagTemplateRepository

class DatabagTemplateRepositoryTest extends AssertionsForJUnit with MockitoSugar with ShouldMatchers with FunSpec with DSLTestUniverse {
   val repository = new DatabagTemplateRepositoryImpl("../resources/databags", "*.dbtemplate")
   describe("Databag template repository") {
     it ("Should load templates from classpath") {
       val templates = repository.list
       templates.isEmpty should equal(false)
       templates.size should equal(2)
     }

     it ("Should find template with scope defined in templates") {
       val templates = repository.list(DatabagTemplateRepository.ProjectScope)
       templates.size should equal(1)
     }

     it ("Should not find template with scope not defined in templates") {
       val templates = repository.list(DatabagTemplateRepository.ProjectScope)
       templates.size should equal(1)
     }

     it ("Should find template with id defined in template") {
       val template = repository.get("1")
       template should not be None
       template.map(template => {
          template.id should equal("1")
       })
     }

     it ("Should not find template with unknown id") {
       val template = repository.get("foo")
       template should be(None)
     }
   }
}
