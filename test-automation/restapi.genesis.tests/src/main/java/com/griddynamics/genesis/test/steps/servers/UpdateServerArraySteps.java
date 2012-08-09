package com.griddynamics.genesis.test.steps.servers;

import static com.jayway.restassured.path.json.JsonPath.from;

import java.io.IOException;

import com.griddynamics.genesis.test.steps.GenesisUtilities;

import org.jbehave.core.annotations.Named;
import org.jbehave.core.annotations.Then;
import org.jbehave.core.annotations.When;
import org.testng.Assert;

public class UpdateServerArraySteps  extends GenesisUtilities {
	
    @When("I send update servers array request with parameters: <newArrayName>, <newArrayDescription> for project <projectName>")
    public void whenISendUpdateServerArrayRequest(@Named("newArrayName") String newArrayName, 
    		                                 @Named("newArrayDescription") String newArrayDescription){
    	
    	request.setUrl(String.format(UPDATE_SETTINGS_URL, newArrayName));
    	request.put("{\"value\":\"" + newArrayDescription + "\"}");
    }
    
    @Then("New value of setting should be set")
    public void thenNewValueIsSet() throws IOException {
    	Assert.assertTrue(request.checkStatusCode200(), "Status code error");
    	
    	String json = request.getResponse().asString(); 
    	boolean isSuccess = from(json).getBoolean("isSuccess");
    	Assert.assertEquals(isSuccess, true);
    }

}