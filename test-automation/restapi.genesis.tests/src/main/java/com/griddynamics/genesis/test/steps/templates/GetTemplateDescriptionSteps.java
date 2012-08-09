package com.griddynamics.genesis.test.steps.templates;


import com.griddynamics.genesis.test.steps.GenesisUtilities;
import com.griddynamics.genesis.tools.templates.CreateWorkflow;
import com.griddynamics.genesis.tools.templates.TemplateDescriptionResponse;

import org.jbehave.core.annotations.Named;
import org.jbehave.core.annotations.Then;
import org.jbehave.core.annotations.When;
import org.testng.Assert;


public class GetTemplateDescriptionSteps extends GenesisUtilities {
	TemplateDescriptionResponse expectedTemplate = new TemplateDescriptionResponse(); 

    @When("I send get template description request for project <projectName> with <templateName> and <templateVersion>")
    public void whenISendGetTemplateDescriptionRequest(@Named("projectName") String projectName, 
    		                                           @Named("templateName") String templateName, 
                                                       @Named("templateVersion") String templateVersion) {
    	
    	expectedTemplate.setName(templateName);
    	expectedTemplate.setVersion(templateVersion);

    	int projectId = getProjectIdByProjectName(projectName);
		Assert.assertTrue(projectId != -1, "Project " + projectName + " was not found");

		request.setUrl(String.format(GET_TEMPLATES_DESCRIPTION_URL, projectId, templateName, templateVersion));
		request.get();
    }

    
    @Then("Template description should be returned with <createWorflowName> and <variables>")
    public void thenTemplateDescriptionShouldReturned(@Named("createWorflowName") String createWorflowName, 
                                                      @Named("variables") String variables) {
    	
    	Assert.assertTrue(request.checkStatusCode200(), "Status code is " + request.getResponse().getStatusCode() + ", but must be 200");

    	TemplateDescriptionResponse tDescription = request.getResponseObject(TemplateDescriptionResponse.class);

    	CreateWorkflow workflow = new CreateWorkflow(createWorflowName, null);
    	expectedTemplate.setCreateWorkflow(workflow);
    	
     	Assert.assertEquals(tDescription, expectedTemplate, "Template description comparision failed");
    }
    
}
