package com.griddynamics.genesis.test.steps.credentials;


import com.griddynamics.genesis.test.steps.GenesisUtilities;
import com.griddynamics.genesis.tools.credentials.AddCredentialsResponse;
import com.griddynamics.genesis.tools.credentials.Credentials;

import org.jbehave.core.annotations.Named;
import org.jbehave.core.annotations.Then;
import org.jbehave.core.annotations.When;
import org.testng.Assert;

import java.io.IOException;


public class AddCredentialsSteps extends GenesisUtilities {

	Credentials newCredentials = new Credentials();

    @When("I send an create credentials request with parameters: <pairName>, <cloudProvider>, <identity>, <credential> in the project <projectName>")
    public void SendCreateCredentialRequest(@Named("pairName") String pairName, 
    		                                 @Named("cloudProvider") String cloudProvider, 
                                             @Named("identity") String identity, 
                                             @Named("credential") String credential,
                                             @Named("projectName") String projectName) throws IOException {
    	
    	String pathToCredential = null;	

    	if (System.getProperty("credentials.path") != null)  
    		pathToCredential = System.getProperty("credentials.path");
    	Assert.assertTrue(pathToCredential!=null, "Credentials path property was not set");
    	
    	int projectId = getProjectIdByProjectName(projectName);
		Assert.assertTrue(projectId != -1, "Project " + projectName + " was not found");

		newCredentials = new Credentials(pairName, cloudProvider, identity, getKeyFromFile(pathToCredential, credential));
		
    	request.setUrl(String.format(ADD_CREDENTIALS_URL, projectId)); 
    	request.post(newCredentials);

    	newCredentials.setProjectId(projectId);
    }

    
    @Then("New credentials will be added")
    public void thenNewCredentialsWillBeCreated() throws IOException {
    	Credentials actualCredentials = request.getResponseObject(AddCredentialsResponse.class).getCredentials();
        
    	Assert.assertTrue(findCredentialsById(actualCredentials.getId(), String.valueOf(newCredentials.getProjectId())), 
    			"ERROR: New credentials was not found");

    	Assert.assertEquals(actualCredentials.getPairName(), newCredentials.getPairName());
    	Assert.assertEquals(actualCredentials.getCloudProvider(), newCredentials.getCloudProvider());
    	Assert.assertEquals(actualCredentials.getIdentity(), newCredentials.getIdentity());
    	Assert.assertEquals(actualCredentials.getProjectId(), newCredentials.getProjectId());
     }
}
