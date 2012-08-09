package com.griddynamics.genesis.test.steps.usergroups;

import org.jbehave.core.annotations.Named;
import org.jbehave.core.annotations.Then;
import org.jbehave.core.annotations.When;
import org.jbehave.core.annotations.Aliases;
import org.testng.Assert;

import com.griddynamics.genesis.tools.CommonTools;
import com.griddynamics.genesis.tools.usergroups.SuccessfulUserGroupCreationResponse;
import com.griddynamics.genesis.tools.usergroups.UserGroupDetails;

public class EditUserGroupStepsImplementation extends UserGroupSharedSources {

	@When("I send request to edit usergroup with name <groupName> and specify description <description> mailing list <mail> and users <usersList>")
	public void whenISendEditGroupRequest(@Named("groupName") String groupName,
			@Named("description") String descr, @Named("mail") String mail,
			@Named("usersList") String usersList) {
		ListUserGroupsStepsImplementation.whenISendGetUserGroupsRequest();
		int id = getUserGroupId(groupName);
		expGroup = new UserGroupDetails(id, groupName, descr, mail,
				new String[0]);
		expGroup.users=CommonTools.processStringValue(usersList);
		if (id != 0) {			
			request.setUrl(GROUPS_PATH + "/" + id);
			request.put(expGroup);
		}
	}

	@Then("I expect that user group was changed successfully")
	public void thenIExpectSuccessfulUserGroupChanges() {
		SuccessfulUserGroupCreationResponse actResponse = request.getResponseObject(SuccessfulUserGroupCreationResponse.class);
		SuccessfulUserGroupCreationResponse expResponse=new SuccessfulUserGroupCreationResponse(true, expGroup);
		Assert.assertEquals(actResponse, expResponse);
		Assert.assertEquals(request.checkStatusCode200(), true);
	}
	
	@When("I send request to edit usergroup with name <groupName> and specify users <usersList>")
	@Aliases(values={"I send request to edit usergroup with name $groupName and specify users $usersList"})
	public void whenISendEditGroupShortRequestWithUsers(@Named("groupName") String groupName,
			@Named("usersList") String usersList) {
		ListUserGroupsStepsImplementation.whenISendGetUserGroupsRequest();
		int id = getUserGroupId(groupName);
		if (id != 0) {
			UserGroupDetailsStepsImplementation.whenISendViewGroupRequest(groupName);
			expGroup = new UserGroupDetails(actGroup.id, actGroup.name, actGroup.description, actGroup.mailingList,
					new String[0]);
			expGroup.users=CommonTools.processStringValue(usersList);
			request.setUrl(GROUPS_PATH + "/" + id);
			request.put(expGroup);
		} 
	}

}
