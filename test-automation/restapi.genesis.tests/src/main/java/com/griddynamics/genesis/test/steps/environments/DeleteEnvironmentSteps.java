package com.griddynamics.genesis.test.steps.environments;

import java.io.IOException;

import org.jbehave.core.annotations.Named;
import org.jbehave.core.annotations.Then;
import org.jbehave.core.annotations.When;
import org.testng.Assert;

import com.griddynamics.genesis.tools.CommonResultObjectResponse;
import com.griddynamics.genesis.tools.environments.Environment;


public class DeleteEnvironmentSteps extends EnvironmentBaseSteps{
    Environment env = new Environment();
   
    @When("I send a delete environment request with <projectName>, <envName>")
    public void whenISendCreateEnvironmentRequest(@Named("projectName") String projectName,
    		                                 @Named("envName") String envName) {
    	
    	env.setName(envName);
    	projectId = getProjectIdByProjectName(projectName);
		Assert.assertTrue(projectId != -1, "Project " + projectName + " was not found");

		request.setUrl(String.format(DELETE_ENV_URL, projectId, envName));
		request.delete();
    }
    
    @Then("Environment will be deleted")
    public void thenEnviromentWillBeDeleted() throws IOException {
		CommonResultObjectResponse actualResponse = request.getResponseObject(CommonResultObjectResponse.class);
		CommonResultObjectResponse expectedResponse = CommonResultObjectResponse
				.getResultFromString(true, false, "", "", "", "");
		Assert.assertEquals(request.checkStatusCode200(), true);
		Assert.assertEquals(actualResponse, expectedResponse);

		Assert.assertTrue(request.checkStatusCode200(), "Status code is " + request.getResponse().getStatusCode() + ", but must be 200");
    	waitForEnvStatus(env.getName(), 620000, "Destroyed");     	
    }
        	
}
