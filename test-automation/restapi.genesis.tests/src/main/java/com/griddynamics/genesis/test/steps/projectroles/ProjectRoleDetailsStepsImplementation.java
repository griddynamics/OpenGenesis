package com.griddynamics.genesis.test.steps.projectroles;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.jbehave.core.annotations.Aliases;
import org.jbehave.core.annotations.Named;
import org.jbehave.core.annotations.Then;
import org.jbehave.core.annotations.When;
import org.testng.Assert;

import com.griddynamics.genesis.tools.CommonTools;
import com.griddynamics.genesis.tools.projectroles.ProjectRoleDetails;
import com.griddynamics.genesis.tools.projectroles.ProjectRoleDetailsResponse;

public class ProjectRoleDetailsStepsImplementation extends ProjectRoleSharedSources {
	
	private static ProjectRoleDetailsResponse roleDetailsResponse;

	@When("I send request to view project role with name <roleName> in project <projectName>")
	@Aliases(values={"I send request to view project role with name $roleName in project $projectName"})
	public static void whenISendViewProjectRoleRequest(@Named("roleName") String name, @Named("projectName") String projectName) {
		int id=getProjectIdByProjectName(projectName);

		request.setUrl(String.format(PROJECT_ROLE_DETAILS_PATH,id,name)); 
		request.get();
		roleDetailsResponse = request.getResponseObject(ProjectRoleDetailsResponse.class);

		actProjectRole=roleDetailsResponse.result;
	}

	@Then("I expect that project role has users <users> and groups <groups>")
	@Aliases(values={"I expect that role has users $users and groups $groups"})
	public void thenIExpectFullProjectRoleDetails(@Named("users") String users, @Named("groups") String groups) {
		expProjectRole = new ProjectRoleDetails(CommonTools.processStringValue(users), CommonTools.processStringValue(groups));
		Assert.assertEquals(expProjectRole, actProjectRole);
	}

	@Then("I expect that project role has users <users>")
	@Aliases(values={"I expect that project role has users $users"})
	public static void thenIExpectUsersInProjectRole(@Named("users") String users) {
		Set<String> actUsersList=new HashSet<String>(Arrays.asList(actProjectRole.users));
		String[] expectedUsers=CommonTools.processStringValue(users);
		Set<String> expUsersList=new HashSet<String>(Arrays.asList(expectedUsers));
		Assert.assertEquals(actUsersList, expUsersList);
	}

	@Then("I expect that project role has groups <groups>")
    @Aliases(values={"I expect that project role has groups $groups"})
	public void thenIExpectGroupsInProjectRole(@Named("groups") String groups) {
		Set<String> actGroupsList=new HashSet<String>(Arrays.asList(actProjectRole.groups));
		String[] expectedGroups=CommonTools.processStringValue(groups);
		Set<String> expGroupsList=new HashSet<String>(Arrays.asList(expectedGroups));
		Assert.assertEquals(actGroupsList, expGroupsList);
	}

}
