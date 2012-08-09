Meta:
@author ybaturina
@theme usergroups

Scenario: Precondition - create test users and project

GivenStories: com/griddynamics/genesis/stories/users/PreconditionCreateTestUser.story#{0},
              com/griddynamics/genesis/stories/users/PreconditionCreateTestUser.story#{1},
              com/griddynamics/genesis/stories/projects/PreconditionCreateTestProject.story#{0}
              
When I send get users request
Then I expect to see 2 users in list 

When I send get projects request
Then I expect to see 1 projects in list

Examples:
|name| userName| email				| firstName	| lastName | title   | password| groups    |result|projectName |	description		|manager	|
|TEST1| u1	  | u1@mailinator.com	| name 		| surname  | title   | u1	   | null	   |true  |old	 	  |testdescription	|manager	|
|TEST2| u2	  | u2@mailinator.com	| name 		| surname  | title   | u2	   | null	   |true  |old	 	  |testdescription	|manager	|

Scenario: Preconditions - assign ROLE_GENESIS_USER to users

When I send request to edit role with name ROLE_GENESIS_USER and specify users u1, u2
Then I expect that role was changed successfully

Scenario: Verify that users with  ROLE_GENESIS_USER have limited access to application

Given I log in system with username <userName> and password <password>
When I send get projects request 
Then I expect to see <quantity> projects in list

Given I log in system with username <userName> and password <password>
When I send request to create project with name <projectName> description <description> and manager <manager>
Then I expect the request rejected as forbidden

Examples:
|name| userName| password|projectName |	description		|manager	|quantity|
|TEST1| u1	   | u1	     |proj1	 	  |testdescription	|manager	|0|
|TEST2| u2	   | u2	     |proj2	 	  |testdescription	|manager	|0|

Scenario: Preconditions - Create test user groups

Given I log in system
When I send request to create user group with name <groupName> description <description> mailing list <mail> and users <usersList>
Then I expect that user group was created successfully

Examples:
|name| groupName| mail		        | description	        | usersList| 
|TEST1| gr1	  | gr1@mailinator.com	| descr		    | u1, u2  | 

Scenario: Preconditions - assign ROLE_GENESIS_ADMIN to the group

When I send request to edit role with name ROLE_GENESIS_ADMIN and specify groups gr1
Then I expect that role was changed successfully

Scenario: Verify that users with admin role have full access to application

Given I log in system with username <userName> and password <password>
When I send get projects request 
Then I expect to see <quantity> projects in list

Given I log in system with username <userName> and password <password>
When I send request to create project with name <projectName> description <description> and manager <manager>
Then I expect that project was created successfully

Examples:
|name| userName| password|projectName |	description		|manager	|quantity|
|TEST1| u1	   | u1	     |proj1	 	  |testdescription	|manager	|1|
|TEST2| u2	   | u2	     |proj2	 	  |testdescription	|manager	|2|

Scenario: Postcondition - delete users

Given I log in system
When I send request to delete user with name <userName>
Then I expect that user was deleted successfully

Examples:
|name| userName| 
|TEST1| u1	  |
|TEST2| u2	  |

Scenario: Postcondition - Delete User Group

When I send request to delete user group with name gr1
Then I expect that user group was deleted successfully

Scenario: Postcondition - delete project

When I send request to delete project with name <projectName>
Then I expect that project was deleted successfully

Examples:
|name| projectName|
|TEST1| old|
|TEST2| proj1|
|TEST3| proj2|