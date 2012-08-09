package com.griddynamics.genesis.test.steps.settings;

import com.griddynamics.genesis.test.steps.GenesisUtilities;
import com.griddynamics.genesis.tools.CommonResultObjectResponse;

import org.jbehave.core.annotations.Named;
import org.jbehave.core.annotations.Then;
import org.jbehave.core.annotations.When;
import org.testng.Assert;

import java.io.IOException;

public class UpdateSettingsSteps extends GenesisUtilities {
	
    @When("I send an update settings request <name> with <newValue>")
    public void whenISendUpdateSettingsRequest(@Named("name") String name, 
    		                                 @Named("newValue") String newValue){
    	
    	request.setUrl(String.format(UPDATE_SETTINGS_URL, name));
    	request.put("{\"value\":\"" + newValue + "\"}");
    }
    
    @Then("New value of setting should be set")
    public void thenNewValueIsSet() throws IOException {
    	Assert.assertTrue(request.checkStatusCode200(), "Status code is " + request.getResponse().getStatusCode() + ", but must be 200");

		CommonResultObjectResponse actualResponse = request.getResponseObject(CommonResultObjectResponse.class);
		CommonResultObjectResponse expectedResponse = CommonResultObjectResponse
				.getResultFromString(true, false, "", "", "", "");
		Assert.assertEquals(actualResponse, expectedResponse);
    }

}
