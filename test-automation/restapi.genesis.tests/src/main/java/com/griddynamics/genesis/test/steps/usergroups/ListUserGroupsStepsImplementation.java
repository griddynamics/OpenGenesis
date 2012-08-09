package com.griddynamics.genesis.test.steps.usergroups;

import org.jbehave.core.annotations.Aliases;
import org.jbehave.core.annotations.Named;
import org.jbehave.core.annotations.Then;
import org.jbehave.core.annotations.When;
import org.testng.Assert;

import com.griddynamics.genesis.tools.usergroups.UserGroupDetails;

public class ListUserGroupsStepsImplementation extends UserGroupSharedSources {

	@When("I send get user groups request")
	public static void whenISendGetUserGroupsRequest() {
		request.setUrl(GROUPS_PATH);
		request.get();
			actListGroups = request.getResponseObject(UserGroupDetails[].class);
	}

	@Then("I expect to see user group with name <groupName> description <description> and mailing list <mail> with result <result>")
	@Aliases (values={"I expect to see user group with name <groupName> description <description> and mailing list <mail> with result $result"})
	public void thenIExpectToSeeUserGroup(@Named("groupName") String name,
			@Named("description") String descr,
			@Named("mail") String mail, @Named("result") Boolean result) {
		boolean res = false;
		UserGroupDetails expGroup = new UserGroupDetails(1, name, descr, mail, new String[0]);
		whenISendGetUserGroupsRequest();
		for (UserGroupDetails actGroup : actListGroups) {
			if (expGroup.equals(actGroup)) {
				res = true;
				break;
			}
		}
		Assert.assertEquals(res, result.booleanValue());
	}
	
	@Then("I expect to see <quantity> user groups in list")
	@Aliases(values={"I expect to see $quantity user groups in list"})
	public void thenIExpectToSeeUserGroups(@Named("quantity") int quantity) {
		whenISendGetUserGroupsRequest();
			Assert.assertEquals(quantity, actListGroups.length);
	}	
}
