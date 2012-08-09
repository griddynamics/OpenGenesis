package com.griddynamics.genesis.test.steps.projectroles;

import org.jbehave.core.annotations.Aliases;
import org.jbehave.core.annotations.Named;
import org.jbehave.core.annotations.Then;
import org.jbehave.core.annotations.When;
import org.testng.Assert;

import com.griddynamics.genesis.tools.CommonResultObjectResponse;
import com.griddynamics.genesis.tools.CommonTools;
import com.griddynamics.genesis.tools.projectroles.ProjectRoleDetails;

public class EditProjectRoleStepsImplementation extends ProjectRoleSharedSources {

	@When("I send request to edit project role with name <roleName> in project <projectName> and specify users <users> and groups <groups>")
	@Aliases(values = { "I send request to edit project role with name $roleName in project $projectName and specify users $users and groups $groups" })
	public void whenISendEditFullProjectRoleRequest(
			@Named("roleName") String name, @Named("projectName") String projectName, @Named("users") String users,
			@Named("groups") String groups) {
		int id=getProjectIdByProjectName(projectName);
		expProjectRole = new ProjectRoleDetails(CommonTools.processStringValue(users), CommonTools.processStringValue(groups));
		
		request.setUrl(String.format(PROJECT_ROLE_DETAILS_PATH,id,name));
		request.put(expProjectRole);
	}

	@Then("I expect that project role was changed successfully")
	public static void thenIExpectSuccessfulProjectRoleChanges() {
		CommonResultObjectResponse actResponse = request.getResponseObject(CommonResultObjectResponse.class);
		CommonResultObjectResponse expResponse = CommonResultObjectResponse
				.getResultFromString(true, false, "", "", "", "");
		Assert.assertEquals(request.checkStatusCode200(), true);
		Assert.assertEquals(actResponse, expResponse);
	}

	@When("I send request to edit project role with name <roleName> in project <projectName> and specify users <users>")
	@Aliases(values = { "I send request to edit project role with name $roleName in project $projectName and specify users $users" })
	public void whenISendEditUsersInProjectRoleRequest(
			@Named("roleName") String name, @Named("projectName") String projectName, @Named("users") String users) {
		int id=getProjectIdByProjectName(projectName);
		ProjectRoleDetailsStepsImplementation.whenISendViewProjectRoleRequest(name, projectName);
		actProjectRole.users = CommonTools.processStringValue(users);
		
		request.setUrl(String.format(PROJECT_ROLE_DETAILS_PATH,id,name));
		request.put(actProjectRole);
	}

	@When("I send request to edit project role with name <roleName> in project <projectName> and specify groups <groups>")
	@Aliases(values = { "I send request to edit project role with name $roleName in project $projectName and specify groups $groups" })
	public void whenISendEditGroupsInProjectRoleRequest(
			@Named("roleName") String name, @Named("projectName") String projectName, @Named("groups") String groups) {
		int id=getProjectIdByProjectName(projectName);
		ProjectRoleDetailsStepsImplementation.whenISendViewProjectRoleRequest(name, projectName);
		actProjectRole.groups = CommonTools.processStringValue(groups);
		
		request.setUrl(String.format(PROJECT_ROLE_DETAILS_PATH,id,name));
		request.put(actProjectRole);
	}

}
