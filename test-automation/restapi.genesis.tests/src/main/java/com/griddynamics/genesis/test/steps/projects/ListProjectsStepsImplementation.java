package com.griddynamics.genesis.test.steps.projects;

import org.jbehave.core.annotations.Aliases;
import org.jbehave.core.annotations.Named;
import org.jbehave.core.annotations.Then;
import org.jbehave.core.annotations.When;
import org.testng.Assert;

import com.google.gson.JsonSyntaxException;
import com.griddynamics.genesis.tools.projects.ProjectDetails;

public class ListProjectsStepsImplementation extends ProjectSharedSources {

	@When("I send get projects request")
	public static void whenISendGetProjectsRequest() {
		request.setUrl(PROJECTS_PATH);
		request.get();
		try {
			actListProjects =request.getResponseObject(ProjectDetails[].class); 
		} catch (JsonSyntaxException e) {
			actListProjects = new ProjectDetails[0];
		}
	}

	@Then("I expect to see project with name <projectName> description <description> and manager <manager> with result <result>")
	@Aliases(values = { "I expect to see project with name <projectName> description <description> and manager <manager> with result $result" })
	public static void thenIExpectToSeeProject(
			@Named("projectName") String name,
			@Named("description") String descr,
			@Named("manager") String manager, @Named("result") Boolean result) {
		boolean res = false;
		ProjectDetails expProject = new ProjectDetails(1, name, descr, manager);
		for (ProjectDetails actProj : actListProjects) {
			if (expProject.equals(actProj)) {
				res = true;
				break;
			}
		}
		Assert.assertEquals(res, result.booleanValue());
	}

	@Then("I expect to see <quantity> projects in list")
	@Aliases(values = { "I expect to see $quantity projects in list" })
	public static void thenIExpectToSeeProjects(@Named("quantity") int quantity) {
		request.setUrl(PROJECTS_PATH);
		request.get();
		actListProjects = request.getResponseObject(ProjectDetails[].class); 
		Assert.assertEquals(actListProjects.length, quantity);
	}

}
