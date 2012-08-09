package com.griddynamics.genesis.test.steps.credentials;

import java.io.IOException;

import com.griddynamics.genesis.test.steps.GenesisUtilities;

import org.jbehave.core.annotations.Named;
import org.jbehave.core.annotations.Then;
import org.jbehave.core.annotations.When;
import org.testng.Assert;


public class DeleteCredentialsSteps extends GenesisUtilities {
	       
	String projectId;
	String credentialId;
	
    @When("I send a delete credentials request for credentials  <pairName>, <cloudProvider>, <identity> in project <projectName>")
    public void SendCreateEnvironmentRequest(@Named("projectName") String projectName,
    		                                 @Named("pairName") String pairName,
    		                                 @Named("cloudProvider") String cloudProvider,
    		                                 @Named("identity") String identity) {
    	
    	projectId  = String.valueOf(getProjectIdByProjectName(projectName));
    	credentialId = String.valueOf(getCredentialId(projectId, pairName, cloudProvider,identity));
    	
		request.setUrl(String.format(DELETE_CREDENTIALS_URL, projectId, credentialId)); 
		request.delete();
    }

    
    @Then("The credentials will be deleted successfully")
    public void thenTheCredentialsWillBeDeletedSuccessfully() throws IOException {
    	Assert.assertTrue(request.checkStatusCode200(), "Status code is " + request.getResponse().getStatusCode() + ", but must be 200");
    	Assert.assertTrue(!findCredentialsById(Integer.valueOf(credentialId), projectId), "ERROR: Deleted credentials was found");
    }

}
