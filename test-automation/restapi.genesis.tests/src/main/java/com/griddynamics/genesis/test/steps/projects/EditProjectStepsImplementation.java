package com.griddynamics.genesis.test.steps.projects;

import org.jbehave.core.annotations.Aliases;
import org.jbehave.core.annotations.Named;
import org.jbehave.core.annotations.Then;
import org.jbehave.core.annotations.When;
import org.testng.Assert;

import com.griddynamics.genesis.tools.projects.ProjectDetails;

public class EditProjectStepsImplementation extends ProjectSharedSources {

	private static ProjectDetails expProject;

	@When("I send request to edit project with name <oldName> and specify new name <projectName> description <description> and manager <manager>")
	@Aliases(values={"I send request to edit project with name $oldName and specify new name $projectName description $description and manager $manager"})
	public void whenISendEditProjectRequest(
			@Named("oldName") String oldName,
			@Named("projectName") String projectName,
			@Named("description") String descr, @Named("manager") String manager) {
			expProject = new ProjectDetails(1, projectName, descr, manager);
			int id = getProjectIdByProjectName(oldName);
			if (id != -1) {
				request.setUrl(PROJECTS_PATH+"/" + id);
				request.put(expProject);
			}
	}
	
	@Then("I expect that project was changed successfully")
	public void thenIExpectSuccessfulProjectChanges() {
		Assert.assertNotEquals(getProjectIdByProjectName(expProject.name), -1);
	}
		
}
