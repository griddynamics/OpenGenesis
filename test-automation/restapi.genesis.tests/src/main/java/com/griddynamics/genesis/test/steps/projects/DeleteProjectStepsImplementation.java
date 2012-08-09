package com.griddynamics.genesis.test.steps.projects;

import org.jbehave.core.annotations.Aliases;
import org.jbehave.core.annotations.Named;
import org.jbehave.core.annotations.Then;
import org.jbehave.core.annotations.When;
import org.testng.Assert;

public class DeleteProjectStepsImplementation extends ProjectSharedSources {

	@When("I send request to delete project with name <projectName>")
	@Aliases (values={"I send request to delete project with name $projectName"})
	public void whenISendDeleteProjectRequest(@Named("projectName") String name) {
		expProject.name=name;
		int id=getProjectIdByProjectName(name);
		if (id != -1) {
			request.setUrl(PROJECTS_PATH+"/" + id);
			request.delete();}
	}

	@Then("I expect that project was deleted successfully")
	public void thenIExpectSuccessfulProjectDeletion() {
		Assert.assertEquals(getProjectIdByProjectName(expProject.name), -1);
	}
}
