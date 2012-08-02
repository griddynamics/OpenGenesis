package com.griddynamics.genesis.service.impl

import com.griddynamics.genesis.template._
import org.springframework.core.convert.support.ConversionServiceFactory
import com.griddynamics.genesis.util.IoUtil
import org.mockito.Mockito
import org.junit.{Before, Test}
import com.griddynamics.genesis.service.VariableDescription
import org.scalatest.junit.AssertionsForJUnit
import org.scalatest.mock.MockitoSugar
import com.griddynamics.genesis.template.VersionedTemplate
import com.griddynamics.genesis.repository.DatabagRepository
import net.sf.ehcache.CacheManager

class DatasourcesTest  extends AssertionsForJUnit with MockitoSugar  {

    val templateRepository = mock[TemplateRepository]
    val databagRepository = mock[DatabagRepository]
    val templateService = new GroovyTemplateService(templateRepository,
        List(new DoNothingStepBuilderFactory), ConversionServiceFactory.createDefaultConversionService(),
        Seq(new ListVarDSFactory, new DependentListVarDSFactory, new NoArgsDSFactory), databagRepository, CacheManager.getInstance())

    val body = IoUtil.streamAsString(classOf[GroovyTemplateServiceTest].getResourceAsStream("/groovy/DataSources.genesis"))
    val bodyWithInlining = IoUtil.streamAsString(classOf[GroovyTemplateServiceTest].getResourceAsStream("/groovy/InlineDatasources.genesis"))

    Mockito.when(templateRepository.listSources).thenReturn(
      Map(
        VersionedTemplate("DataSources", "0.1") -> body,
        VersionedTemplate("InlineSources", "0.1") -> bodyWithInlining
      )
    )

    private def testTemplate = templateService.findTemplate(0, "DataSources", "0.1").get

    private def testInlineTemplate = templateService.findTemplate(0, "InlineSources", "0.1").get

    @Before def setUp() {
      CacheManager.getInstance().clearAll()
    }

    @Test def testOneOfVariable() {
        val template = testTemplate
        val validate = template.createWorkflow.validate(Map("nodesCount"-> 1, "list" -> 10))
        assert(validate.isDefinedAt(0))
        assert(validate(0).variableName == "list")
    }

    @Test def testOneOfDS() {
        val template = testTemplate
        val varDesc =  template.createWorkflow.variableDescriptions
        assert(varDesc.nonEmpty)
        val listDS12 = varDesc.find(_.name == "listDS12")
        assert(listDS12.isDefined)
        expect(Map("value1"->"value1", "value2" -> "value2", "value3" -> "value3"))(listDS12.get.values)
    }

    @Test def testNoArgsSource() {
        val template = testTemplate
        val varDesc =  template.createWorkflow.variableDescriptions
        assert(varDesc.nonEmpty)
        val listDS12 = varDesc.find(_.name == "noArgs")
        assert(listDS12.isDefined)
        expect(Seq("a", "b", "c").zip(Seq("a", "b", "c")).toMap)(listDS12.get.values)
    }

    @Test def testIndependentDataSource() {
        val template = testTemplate
        val varDesc =  template.createWorkflow.variableDescriptions
        assert(varDesc.nonEmpty)
        val listDS1 = varDesc.find(_.name == "listDS1")
        assert(listDS1.isDefined)
        expect(Seq("value1", "value2").map(v => (v,v)).toMap)(listDS1.get.values)
    }

    @Test def testDependent() {
        val template = testTemplate
        val varDesc =  template.createWorkflow.variableDescriptions
        assert(varDesc.nonEmpty)
        val listDS1 = varDesc.find(_.name == "dependent")
        assert(listDS1.isDefined)
        val S1 = Map()
        expect(S1)(listDS1.get.values)
        val partial: Seq[VariableDescription] = template.createWorkflow.partial(Map("nodesCount" -> 1))
        val descAfterApply = partial.find(_.name == "dependent")
        assert(descAfterApply.isDefined)
        assert(! descAfterApply.get.values.isEmpty)
        expect(Map("11" -> "1", "31" -> "3", "41" -> "4"))(descAfterApply.get.values)
    }

    @Test def testDoubleDependent() {
        val template = testTemplate
        val varDesc =  template.createWorkflow.variableDescriptions
        assert(varDesc.nonEmpty)
        val listDS1 = varDesc.find(_.name == "doubleDep")
        assert(listDS1.isDefined)
        val S1 = Map()
        expect(S1)(listDS1.get.values)
        val partial: Seq[VariableDescription] = template.createWorkflow.partial(Map("dependent" -> "z", "nodesCount" -> 1))
        val descAfterApply = partial.find(_.name == "doubleDep")
        assert(descAfterApply.isDefined)
        assert(! descAfterApply.get.values.isEmpty)
        expect(Map("1 < nc:1 < dp:z" -> "1", "3 < nc:1 < dp:z" -> "3", "4 < nc:1 < dp:z" -> "4"))(descAfterApply.get.values)
    }

    @Test def testTripleDependent() {
        implicit val projectId: Int = 1
        val template = testTemplate
        val varDesc =  template.createWorkflow.variableDescriptions
        assert(varDesc.nonEmpty)
        val listDS1 = varDesc.find(_.name == "triple")
        assert(listDS1.isDefined)
        val S1 = Map()
        expect(S1)(listDS1.get.values)
        val partial = template.createWorkflow.partial(Map("list" -> 13, "dependent" -> 'z', "nodesCount" -> 1))
        assert(partial.length == 8)
        val descAfterApply = partial.find(_.name == "triple")
        assert(descAfterApply.isDefined)
        assert(! descAfterApply.get.values.isEmpty)
        expect(Map("1<13<1<z" -> "1", "3<13<1<z" -> "3", "4<13<1<z" -> "4"))(descAfterApply.get.values)
    }

    @Test def testInlineDSDeclarationWithDependency() {
        implicit val projectId: Int = 1
        val template = testInlineTemplate

        val varDesc =  template.createWorkflow.variableDescriptions

        val listDS1 = varDesc.find(_.name == "list")
        assert(listDS1.isDefined)

        expect(Map())(listDS1.get.values)
        expect(Some(List("key")))(listDS1.get.dependsOn)

        val partial = template.createWorkflow.partial(Map("key" -> 666))

        val descAfterApply = partial.find(_.name == "list")
        assert(descAfterApply.isDefined)
        expect(Map("666" -> "666"))(descAfterApply.get.values)
    }

    @Test def testInlineDSDeclaration() {
        implicit val projectId: Int = 1
        val template = testInlineTemplate

        val varDesc =  template.createWorkflow.variableDescriptions

        val listDS1 = varDesc.find(_.name == "independant")
        assert(listDS1.isDefined)

        expect(None)(listDS1.get.dependsOn)

        val partial = template.createWorkflow.partial(Map())

        val descAfterApply = partial.find(_.name == "independant")
        assert(descAfterApply.isDefined)
        expect(Map("1" -> "1", "2" -> "2"))(descAfterApply.get.values)
    }
}

class DependentListDataSource extends ListVarDataSource with DependentDataSource {
    override def getData(param: Any) = values.map(entry => (entry._1 + param.toString, entry._1))
    def getData(nodesCount: Any, dependent: Any) =
      values.map(entry => (entry._1 + " < nc:%s".format(nodesCount) + " < dp:%s".format(dependent), entry._1))
    /*
    *  A method for triple dependent variable. It has three arguments, but
    *  you don't have to declare parameters as Any since you're providing a
    *  correct data types as input
    */
    def getData(list: /*String*/Int, nodesCount: Int, dependent: Char)
    = values.map(entry => (entry._1 + "<%s".format(list) + "<%d".format(nodesCount) + "<%s".format(dependent), entry._1))
    override def getData = Map()
}

class DependentListVarDSFactory extends DataSourceFactory {
    val mode = "dependentList"
    def newDataSource = new DependentListDataSource
}

class NoArgsDSFactory extends DataSourceFactory {
    val mode = "noArgsList"
    def newDataSource = {
        val source = new ListVarDataSource {
            override def config(map: Map[String, Any]){}
        }
        source.values = Seq("a", "b", "c").map(e => (e, e)).toMap
        source
    }
}
