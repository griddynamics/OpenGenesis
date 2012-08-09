package com.griddynamics.genesis.test.steps.environments;


import org.jbehave.core.annotations.Named;
import org.jbehave.core.annotations.Then;
import org.jbehave.core.annotations.When;
import org.testng.Assert;

public class GetEnvironmentDetailsSteps extends EnvironmentBaseSteps {
    int projectId;

    @When("I send get environments details request with <projectName>, <envName>")
    public void whenISendGetEnvironmentDetailsRequest(@Named("projectName") String projectName,
    		                                 @Named("envName") String envName) {
		
    	projectId = getProjectIdByProjectName(projectName);
		Assert.assertTrue(projectId != -1, "Project " + projectName + " was not found");
    	
		request.setUrl(String.format(GET_ENV_DETAILS_URL, projectId, envName));
        request.get();
    }

    @Then("Environment details will be return")
    public void whenISendGetEnvironmentDetailsRequest() {
    	Assert.assertTrue(request.checkStatusCode200(), "Status code is " + request.getResponse().getStatusCode() + ", but must be 200");
    }


}