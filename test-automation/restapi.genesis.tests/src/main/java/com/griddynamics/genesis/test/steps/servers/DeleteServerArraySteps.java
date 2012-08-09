package com.griddynamics.genesis.test.steps.servers;

import java.io.IOException;

import com.griddynamics.genesis.test.steps.GenesisUtilities;

import org.jbehave.core.annotations.Named;
import org.jbehave.core.annotations.Then;
import org.jbehave.core.annotations.When;
import org.testng.Assert;

import com.jayway.restassured.response.Response;


public class DeleteServerArraySteps extends GenesisUtilities {
	Response response;
	int projectId;
	
    @When("I send delete server array request for array <arrayName>, <arrayDescription> in project <projectName>")
    public void SendCreateEnvironmentRequest(@Named("projectName") String projectName,
    		                                 @Named("arrayName") String arrayName,
    		                                 @Named("arrayDescription") String arrayDescription) {
    	
    	projectId = getProjectIdByProjectName(projectName);
		Assert.assertTrue(projectId != -1, "Project " + projectName + " was not found");

		int serverArrayId = getServerArrayId(projectId, arrayName);
    	request.setUrl(String.format(DELETE_SERVER_ARRAY_URL, projectId, serverArrayId));
    	request.delete();
    }

    
    @Then("The server array should be deleted successfully")
    public void thenTheCredentialsWillBeDeletedSuccessfully() throws IOException {
    	Assert.assertTrue(request.checkStatusCode200());
    	
    	//TODO Make sure that server array was deleted 
    }

}
