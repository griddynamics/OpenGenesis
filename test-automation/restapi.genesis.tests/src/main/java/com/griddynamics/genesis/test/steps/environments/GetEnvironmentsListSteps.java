package com.griddynamics.genesis.test.steps.environments;

import org.jbehave.core.annotations.Named;
import org.jbehave.core.annotations.Then;
import org.jbehave.core.annotations.When;
import org.testng.Assert;

public class GetEnvironmentsListSteps extends EnvironmentBaseSteps  {
	 
    @When("I send get existing environments request with <projectName>")
    public void whenISendGetEnvironmentsListRequest(@Named("projectName") String projectName) {
    	
		projectId = getProjectIdByProjectName(projectName);
		Assert.assertTrue(projectId != -1, "Project " + projectName + " was not found");

        request.setUrl(String.format(GET_ENVS_URL, projectId));
        request.get();
    }
    
    @Then("I should get environments list")
    public void thenIGetEnvironmentsList() {
    	Assert.assertTrue(request.checkStatusCode200(), "Status code is " + request.getResponse().getStatusCode() + ", but must be 200");
    }
}
