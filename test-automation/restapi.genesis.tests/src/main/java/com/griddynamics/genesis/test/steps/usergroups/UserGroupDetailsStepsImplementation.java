package com.griddynamics.genesis.test.steps.usergroups;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.jbehave.core.annotations.Aliases;
import org.jbehave.core.annotations.Named;
import org.jbehave.core.annotations.Then;
import org.jbehave.core.annotations.When;
import org.testng.Assert;

import com.griddynamics.genesis.tools.CommonTools;
import com.griddynamics.genesis.tools.usergroups.UserGroupDetails;

public class UserGroupDetailsStepsImplementation extends UserGroupSharedSources {

	@When("I send request to view user group with name <name>")
	public static void whenISendViewGroupRequest(@Named("name") String name) {
		request.setUrl(GROUPS_PATH + "/" + getUserGroupId(name));
		request.get();
		actGroup = request.getResponseObject(UserGroupDetails.class);
	}

	@Then("I expect that user group has name <groupName> description <description> mailing list <mail> and users <usersList>")
	public void thenIExpectFullUserGroupDetails(
			@Named("groupName") String groupName, @Named("description") String descr,
			@Named("mail") String mail, @Named("usersList") String usersList) {
		expGroup=new UserGroupDetails(1, groupName, descr, mail, new String[0]);
		expGroup.users=CommonTools.processStringValue(usersList);
		Assert.assertEquals(expGroup, actGroup);
	}

	@Then("I expect that user group has users <users>")
	@Aliases(values={"I expect that user group has users $users"})
	public void thenIExpectUsersInGroup(@Named("users") String users) {
		Set<String> actUsersList=new HashSet<String>(Arrays.asList(actGroup.users));
		String[] expectedUsers=CommonTools.processStringValue(users);
		Set<String> expUsersList=new HashSet<String>(Arrays.asList(expectedUsers));
		Assert.assertEquals(actUsersList, expUsersList);
	}
}
