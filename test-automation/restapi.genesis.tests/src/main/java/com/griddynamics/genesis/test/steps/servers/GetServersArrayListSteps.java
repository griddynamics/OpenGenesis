package com.griddynamics.genesis.test.steps.servers;

import java.util.Arrays;
import java.util.List;

import org.jbehave.core.annotations.Named;
import org.jbehave.core.annotations.Then;
import org.jbehave.core.annotations.When;
import org.testng.Assert;

import com.griddynamics.genesis.test.steps.GenesisUtilities;
import com.griddynamics.genesis.tools.servers.ServerArray;
import com.jayway.restassured.response.Response;

public class GetServersArrayListSteps extends GenesisUtilities {
	
	protected Response response;
	int projectId; 
	 
    @When("I send get servers array list request for <projectName>")
    public void whenISendGetArrayListRequest(@Named("projectName") String projectName) {
   	    System.out.println(projectName);
       	projectId = getProjectIdByProjectName(projectName);
    	Assert.assertTrue(projectId != -1,  "Project " + projectName + " not found");
        
    	request.setUrl(String.format(GET_SERVERS_ARRAY_LIST_URL, projectId));
    	request.get();
    }

    
    @Then("I should get servers array list included <arrayName>, <arrayDescription>")
    public void thenIGetCredentialsList(@Named("arrayName") String arrayName, 
                                        @Named("arrayDescription") String arrayDescription) {
    	
    	List<ServerArray> serverArraysList = Arrays.asList(request.getResponseObject(ServerArray[].class));
    	
    	ServerArray expectedServerArray = new ServerArray();
    	
    	expectedServerArray.setName(arrayName);
    	expectedServerArray.setDescription(arrayDescription);
    	expectedServerArray.setProjectId(projectId);
    	
    	Assert.assertTrue(serverArraysList.contains(expectedServerArray));
    }
 
}
