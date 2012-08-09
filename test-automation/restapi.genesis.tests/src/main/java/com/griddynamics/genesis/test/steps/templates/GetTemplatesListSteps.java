package com.griddynamics.genesis.test.steps.templates;

import java.util.Arrays;
import java.util.List;

import com.griddynamics.genesis.test.steps.GenesisUtilities;

import org.jbehave.core.annotations.Named;
import org.jbehave.core.annotations.Then;
import org.jbehave.core.annotations.When;
import org.testng.Assert;

import com.griddynamics.genesis.tools.templates.Template;


public class GetTemplatesListSteps extends GenesisUtilities {
	private static String GET_TEMPLATES_LIST_URL  = endpointProperties.getProperty("templates.list");
//	Response response;
	 
	@When("I send get template list request for project <projectName>")
    public void whenISendGetTemplatesListRequest(@Named("projectName") String projectName) {

    	int projectId = getProjectIdByProjectName(projectName);
    	Assert.assertTrue(projectId != -1,  "Project " + projectName + " not found");
    	
    	request.setUrl(String.format(GET_TEMPLATES_LIST_URL, projectId));
    	request.get();
    }


	@Then("I should get a template list included <templateName>, <templateVersion>")
    public void thenIGetShouldGetTemplateList(@Named("templateName") String templateName, 
                                              @Named("templateVersion") String templateVersion) {
    	
    	Assert.assertTrue(request.checkStatusCode200(), "Status code is " + request.getResponse().getStatusCode() + ", but must be 200");

    	List<Template> settingsList = Arrays.asList(request.getResponseObject(Template[].class));
     	Template expectedTemplate = new Template(templateName, templateVersion);
     	Assert.assertTrue(settingsList.contains(expectedTemplate));
    }
}
