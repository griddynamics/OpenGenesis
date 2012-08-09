package com.griddynamics.genesis.test.steps.roles;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.jbehave.core.annotations.Aliases;
import org.jbehave.core.annotations.Named;
import org.jbehave.core.annotations.Then;
import org.jbehave.core.annotations.When;
import org.testng.Assert;

import com.griddynamics.genesis.tools.CommonTools;
import com.griddynamics.genesis.tools.roles.RoleDetails;

public class RoleDetailsStepsImplementation extends RoleSharedSources {

	@When("I send request to view role with name <roleName>")
	@Aliases(values={"I send request to view role with name $roleName"})
	public static void whenISendViewRoleRequest(@Named("roleName") String name) {
		request.setUrl(ROLES_PATH + "/" + name);
		request.get();
		actRole = request.getResponseObject(RoleDetails.class);
	}

	@Then("I expect that role has name <roleName> users <users> and groups <groups>")
	@Aliases(values={"I expect that role has name $roleName users $users and groups $groups"})
	public void thenIExpectFullRoleDetails(@Named("roleName") String name,
			@Named("users") String users, @Named("groups") String groups) {
		expRole = new RoleDetails(name, new String[0], new String[0]);
		expRole.users=CommonTools.processStringValue(users);
		expRole.groups=CommonTools.processStringValue(groups);
		Assert.assertEquals(expRole, actRole);
	}

	@Then("I expect that role has users <users>")
	@Aliases(values={"I expect that role has users $users"})
	public void thenIExpectUsersInRole(@Named("users") String users) {
		Set<String> actUsersList=new HashSet<String>(Arrays.asList(actRole.users));
		String[] expectedUsers=CommonTools.processStringValue(users);
		Set<String> expUsersList=new HashSet<String>(Arrays.asList(expectedUsers));
		Assert.assertEquals(actUsersList, expUsersList);
	}

	@Then("I expect that role has groups <groups>")
    @Aliases(values={"I expect that role has groups $groups"})
	public void thenIExpectGroupsInRole(@Named("groups") String groups) {
		Set<String> actGroupsList=new HashSet<String>(Arrays.asList(actRole.groups));
		String[] expectedGroups=CommonTools.processStringValue(groups);
		Set<String> expGroupsList=new HashSet<String>(Arrays.asList(expectedGroups));
		Assert.assertEquals(actGroupsList, expGroupsList);
	}

}
