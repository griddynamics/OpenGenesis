package com.griddynamics.genesis.test.steps.credentials;

import java.io.IOException;

import com.griddynamics.genesis.test.steps.GenesisUtilities;

import org.jbehave.core.annotations.Named;
import org.jbehave.core.annotations.Then;
import org.jbehave.core.annotations.When;
import org.testng.Assert;

import com.griddynamics.genesis.tools.credentials.Credentials;


public class GetCredentialsListSteps extends GenesisUtilities {
    @When("I send get existing credentials request for <projectName>")
    public void whenISendGetCredentialsListRequest(@Named("projectName") String projectName) {

    	int projectId = getProjectIdByProjectName(projectName);
		Assert.assertTrue(projectId != -1, "Project " + projectName + " was not found");

		request.setUrl(String.format(GET_CREDENTIALS_URL, projectId)); 
		request.get();

    }

    @Then("I should get a list of credentials, included <pairName>, <cloudProvider>, <identity>, <credential>")
    public void thenIGetCredentialsList(@Named("pairName") String pairName, 
                                        @Named("cloudProvider") String cloudProvider, 
                                        @Named("identity") String identity) throws IOException {

    	Assert.assertTrue(request.checkStatusCode200(), "Status code is " + request.getResponse().getStatusCode() + ", but must be 200");
    	Credentials[] credentialsList = request.getResponseObject(Credentials[].class);

    	boolean res = false;
    	
        for (int i = 0; i < credentialsList.length; i++) {
            if (credentialsList[i].getPairName().equals(pairName) &  
            		credentialsList[i].getCloudProvider().equals(cloudProvider) &
            		  credentialsList[i].getIdentity().equals(identity)) {

            	res = true;
            	break;
            } else {
            	continue;
            }
        }
        
        Assert.assertTrue(res);    
    }


}
