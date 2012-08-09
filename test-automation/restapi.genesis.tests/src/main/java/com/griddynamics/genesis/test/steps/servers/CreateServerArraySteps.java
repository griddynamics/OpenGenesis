package com.griddynamics.genesis.test.steps.servers;


import com.griddynamics.genesis.test.steps.GenesisUtilities;

import org.jbehave.core.annotations.Named;
import org.jbehave.core.annotations.Then;
import org.jbehave.core.annotations.When;
import org.testng.Assert;

import com.griddynamics.genesis.tools.servers.CreateServerArrayResponse;
import com.griddynamics.genesis.tools.servers.ServerArray;


public class CreateServerArraySteps extends GenesisUtilities {
	ServerArray requestBody = new ServerArray();

    @When("I send create servers array request with parameters: <arrayName>, <arrayDescription> for project $projectName")
    public void SendCreateServersRequest(@Named("arrayName") String arrayName, 
    		                                 @Named("arrayDescription") String arrayDescription, 
                                             @Named("projectName") String projectName) {
    	
    	int projectId = getProjectIdByProjectName(projectName);
		Assert.assertTrue(projectId != -1, "Project " + projectName + " was not found");

		requestBody.setProjectId(projectId);
		requestBody.setName(arrayName);
		requestBody.setDescription(arrayDescription);
    	
		request.setUrl(String.format(CREATE_SERVER_ARRAY_URL, projectId));
		request.post(requestBody);
    }

    
    @Then("New server array should be created")
    public void thenNewCredentialsWillBeCreated() {
    	Assert.assertTrue(request.checkStatusCode200(), "Status code is " + request.getResponse().getStatusCode() + ", but must be 200");
        
    	CreateServerArrayResponse actualResponse = request.getResponseObject(CreateServerArrayResponse.class); 
    	ServerArray actualArray = actualResponse.getArray();
    	
    	Assert.assertEquals(actualResponse.isSuccess, true);
    	Assert.assertEquals(actualArray.getName(), requestBody.getName());
    	Assert.assertEquals(actualArray.getDescription(), requestBody.getDescription());
    	Assert.assertEquals(actualArray.getProjectId(), requestBody.getProjectId());

    }
}
