package com.griddynamics.genesis.test.steps.settings;


import static com.jayway.restassured.path.json.JsonPath.from;
import org.jbehave.core.annotations.Then;
import org.jbehave.core.annotations.When;
import org.testng.Assert;

import com.griddynamics.genesis.test.steps.GenesisUtilities;

public class ResetSettingsSteps extends GenesisUtilities {
	       
    @When("I send a reset all settings request")
    public void whenISendResetSettingsRequest() {
  	   request.setUrl(RESET_SETTINGS_URL);
  	   request.delete();
    }

    
    @Then("Reset settings request returns successful result")
    public void thenTheSettingsWillBeReset() {
    	Assert.assertTrue(request.checkStatusCode200(), "Status code is " + request.getResponse().getStatusCode() + ", but must be 200");
    	
    	boolean isSuccess = from(request.getResponse().asString()).getBoolean("isSuccess");
    	Assert.assertEquals(isSuccess, true, "[Error] isSuccess == false");
    }

}
