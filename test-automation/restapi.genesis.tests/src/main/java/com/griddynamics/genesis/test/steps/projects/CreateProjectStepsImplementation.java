package com.griddynamics.genesis.test.steps.projects;
import org.jbehave.core.annotations.Aliases;
import org.jbehave.core.annotations.Named;
import org.jbehave.core.annotations.Then;
import org.jbehave.core.annotations.When;
import org.testng.Assert;

import com.griddynamics.genesis.tools.projects.ProjectDetails;
import com.griddynamics.genesis.tools.projects.SuccessfulProjectCreationResponse;

public class CreateProjectStepsImplementation extends ProjectSharedSources{

	@When("I send request to create project with name <projectName> description <description> and manager <manager>")
	@Aliases (values={"I send request to create project with name $projectName description $description and manager $manager"})
	public static void whenISendCreateProjectRequest(
			@Named("projectName") String name, @Named("description") String descr,
			@Named("manager") String manager) {
			expProject=new ProjectDetails(1, name, descr, manager);
			request.setUrl(PROJECTS_PATH);
			request.post(expProject);
	}
	
	@Then("I expect that project was created successfully")
	public static void thenIExpectSuccessfulProjectCreation(){
		SuccessfulProjectCreationResponse actResponse = request.getResponseObject(SuccessfulProjectCreationResponse.class);
		SuccessfulProjectCreationResponse expResponse=new SuccessfulProjectCreationResponse(true, expProject);

		Assert.assertEquals(actResponse, expResponse);
		Assert.assertEquals(request.checkStatusCode200(), true);
	}
}
