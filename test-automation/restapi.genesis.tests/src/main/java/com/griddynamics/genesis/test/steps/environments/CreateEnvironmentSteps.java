package com.griddynamics.genesis.test.steps.environments;

import com.griddynamics.genesis.tools.CommonResultObjectResponse;
import com.griddynamics.genesis.tools.environments.CreateEnvironmentRequest;
import com.griddynamics.genesis.tools.environments.Environment;

import org.jbehave.core.annotations.Named;
import org.jbehave.core.annotations.Then;
import org.jbehave.core.annotations.When;
import org.testng.Assert;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


public class CreateEnvironmentSteps extends EnvironmentBaseSteps{
    Environment env = new Environment();
	CreateEnvironmentRequest requestBody = new CreateEnvironmentRequest();
   

	@When("I send an create environment request with <envName>, <templateName>, <templateVersion>, <var1> in the project <projectName>")
    public void SendCreateEnvironmentRequest(@Named("envName") String envName, 
    		                                 @Named("templateName") String templateName, 
                                             @Named("templateVersion") String templateVersion, 
                                             @Named("var1") String var1,
                                             @Named("projectName") String projectName) {
		
		projectId = getProjectIdByProjectName(projectName);
		Assert.assertTrue(projectId != -1, "Project " + projectName + " was not found");

		env.setName(envName);
    	Map<String, String> variables = new HashMap<String, String>();
        variables.put("genesis_url", var1);

        requestBody = new CreateEnvironmentRequest(envName, templateName, templateVersion, null);

        request.setUrl(String.format(CREATE_ENV_URL, projectId));
        request.post(requestBody);
    }
   
    @Then("New environment will be created")
    public void thenNewEnviromentCreated() throws IOException {
		CommonResultObjectResponse actualResponse = request.getResponseObject(CommonResultObjectResponse.class);
		CommonResultObjectResponse expectedResponse = CommonResultObjectResponse
				.getResultFromString(true, false, "", "", "", "");
		Assert.assertEquals(actualResponse, expectedResponse);

    	Assert.assertTrue(request.checkStatusCode200(), "Status code is " + request.getResponse().getStatusCode() + ", but must be 200");
    	Assert.assertTrue(isEnvironmentExists(String.format(GET_ENVS_URL, projectId), requestBody.getEnvName()), "[ENV is not existed]"); 
    	waitForEnvStatus(env.getName(), 620000, "Ready");     	
    }
        
}
