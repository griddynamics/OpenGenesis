package com.griddynamics.genesis.test.steps.settings;

import java.util.Arrays;
import java.util.List;

import com.griddynamics.genesis.test.steps.GenesisUtilities;
import com.griddynamics.genesis.tools.settings.Settings;

import org.jbehave.core.annotations.Alias;
import org.jbehave.core.annotations.Named;
import org.jbehave.core.annotations.Then;
import org.jbehave.core.annotations.When;
import org.testng.Assert;


public class GetSettingsListSteps extends GenesisUtilities {
 
    @When("I send get list of settings request with <prefix>")
    @Alias("I send get list of settings request") 
    public void whenISendGetSettingsRequest(@Named("prefix") String prefix) {
    	String requestURL = (prefix == null) ? GET_SETTINGS_URL : GET_SETTINGS_URL + "?prefix=" + prefix; 

    	request.setUrl(requestURL);
    	request.get();
    }

    @Then("I should get a list of settings included <name>, <defValue>, <readOnly>, <description>")
    public void thenIGetCredentialsList(@Named("name") String name, 
                                        @Named("defValue") String defValue, 
                                        @Named("readOnly") Boolean readOnly, 
                                        @Named("description") String description){
    	
    	Assert.assertTrue(request.checkStatusCode200(), "Status code is " + request.getResponse().getStatusCode() + ", but must be 200");

    	List<Settings> settingsList = Arrays.asList(request.getResponseObject(Settings[].class));
    	Settings expectedSettings = new Settings(name, defValue, readOnly, description);
     	Assert.assertTrue(settingsList.contains(expectedSettings));
    }

}
