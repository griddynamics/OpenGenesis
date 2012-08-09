package com.griddynamics.genesis.test.steps.templates;

import java.io.IOException;

import com.griddynamics.genesis.test.steps.GenesisUtilities;

import org.jbehave.core.annotations.Named;
import org.jbehave.core.annotations.Then;
import org.jbehave.core.annotations.When;
import org.testng.Assert;

import com.griddynamics.genesis.tools.templates.TemplateContentResponse;


public class GetTemplateContentSteps extends GenesisUtilities {
	TemplateContentResponse expectedTemplate = new TemplateContentResponse(); 

    @When("I send get template content request for project <projectName> with <templateName> and <templateVersion>")
    public void whenISendGetTemplateContentRequest(@Named("projectName") String projectName, 
    		                                           @Named("templateName") String templateName, 
                                                       @Named("templateVersion") String templateVersion) {
    	expectedTemplate.setName(templateName);
    	expectedTemplate.setVersion(templateVersion);

    	int projectId = getProjectIdByProjectName(projectName);
		Assert.assertTrue(projectId != -1, "Project " + projectName + " was not found");

		request.setUrl(String.format(GET_TEMPLATES_CONTENT_URL, projectId, templateName, templateVersion));
		request.get();
    }

    
    @Then("Template content should be returned")
    public void thenTemplateContentShouldReturned(@Named("createWorflowName") String createWorflowName, 
                                                  @Named("variables") String variables) throws IOException {
    	
    	Assert.assertTrue(request.checkStatusCode200(), "Status code is " + request.getResponse().getStatusCode() + ", but must be 200");

    	TemplateContentResponse tContent = request.getResponseObject(TemplateContentResponse.class);
    	expectedTemplate.setContent(getFileContentFromResourses("templates/" + expectedTemplate.getName() + ".genesis"));
     	Assert.assertEquals(tContent.getContent().trim(), expectedTemplate.getContent().trim(), "Template content comparision failed");
    }

}
