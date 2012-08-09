Meta:
@author ybaturina
@theme projectroles

Scenario: Precondition - create test users and project

GivenStories: com/griddynamics/genesis/stories/users/PreconditionCreateTestUser.story#{0},
              com/griddynamics/genesis/stories/users/PreconditionCreateTestUser.story#{1},
              com/griddynamics/genesis/stories/projects/PreconditionCreateTestProject.story#{0},
              com/griddynamics/genesis/stories/projects/PreconditionCreateTestProject.story#{1}
              
When I send request to edit role with name ROLE_GENESIS_USER and specify users u1, u2
Then I expect that role was changed successfully

Examples:
|name| userName| email				| firstName	| lastName | title   | password| groups    |result|projectName |	description		|manager	|
|TEST1| u1	  | u1@mailinator.com	| name 		| surname  | title   | u1	   | null	   |true  |old	 	  |testdescription	|manager	|
|TEST2| u2	  | u2@mailinator.com	| name 		| surname  | title   | u2	   | null	   |true  |new	 	  |testdescription	|manager	|

Scenario: Preconditions - assign project User role to the users

When I send request to edit project role with name ROLE_GENESIS_PROJECT_USER in project old and specify users u1, u2
Then I expect that project role was changed successfully

When I send request to edit project role with name ROLE_GENESIS_PROJECT_USER in project new and specify users u1
Then I expect that project role was changed successfully

Scenario: Preconditions - Create test user groups

GivenStories: com/griddynamics/genesis/stories/usergroups/PreconditionCreateTestGroup.story#{0},
			  com/griddynamics/genesis/stories/usergroups/PreconditionCreateTestGroup.story#{1}

When I send get user groups request
Then I expect to see 2 user groups in list

Examples:
|name| groupName| mail		        | description	        | usersList| 
|TEST1| gr1	  | gr1@mailinator.com	| descr		    | null  | 
|TEST1| gr2	  | gr2@mailinator.com	| descr		    | null  | 

Scenario: Preconditions - assign project roles to the groups

When I send request to edit project role with name ROLE_GENESIS_PROJECT_ADMIN in project old and specify groups gr1, gr2
Then I expect that project role was changed successfully

When I send request to edit project role with name ROLE_GENESIS_PROJECT_ADMIN in project new and specify groups gr2
Then I expect that project role was changed successfully

Scenario: Precondition - add credentials to project
              
When I send an create credentials request with parameters: <pairName>, <cloudProvider>, <identity>, <credential> in the project <projectName>
Then New credentials will be added

Examples:
|projectName     | cloudProvider | pairName	     | identity | credential         |
|old            | gdnova        | genesis-key   | root     | cloud4gd.pem       |
|new            | gdnova        | genesis-key   | root     | cloud4gd.pem       |

Scenario: Assign users to groups

Given I log in system
When I send request to edit usergroup with name <groupName> and specify users <usersList>
Then I expect that user group was changed successfully

Examples:
|name| usersList| groupName|
|TEST1| u1, u2	   | gr1	     |
|TEST2| u1	   | gr2	     |

Scenario: View Project admin role

When I send request to view project role with name ROLE_GENESIS_PROJECT_ADMIN in project old
Then I expect that project role has groups gr1, gr2

When I send request to view project role with name ROLE_GENESIS_PROJECT_ADMIN in project new
Then I expect that project role has groups gr2

Scenario: Check if projects are visible
  
Given I log in system with username <userName> and password <password>
When I send get projects request 
Then I expect to see <quantity> projects in list

Examples:
|name| userName| password|quantity|
|TEST1| u1	   | u1	     |2|
|TEST2| u2	   | u2	     |1|

Scenario: Check user permissions in project
  
Given I log in system with username <userName> and password <password>
When I send get user permissions in project <projectName> request
Then I expect to see user permissions <permissions>

Examples:
|name| userName| password|projectName|permissions|
|TEST1| u1	   | u1	     |old|ROLE_GENESIS_PROJECT_USER, ROLE_GENESIS_PROJECT_ADMIN|
|TEST2| u2	   | u2	     |old|ROLE_GENESIS_PROJECT_USER, ROLE_GENESIS_PROJECT_ADMIN|
|TEST1| u1	   | u1	     |new|ROLE_GENESIS_PROJECT_USER, ROLE_GENESIS_PROJECT_ADMIN|

Scenario: Check user permissions in project to which he has no access

Given I log in system with username u2 and password u2
When I send get user permissions in project new request
Then I expect the request rejected as forbidden

Scenario: Try to create environment

Given I log in system with username <userName> and password <password>
!--When I send an create environment request with <envName>, <templateName>, <templateVersion>, <var1> in the project <projectName>
!--Then New environment will be created

Examples:
|name| userName| password|projectName     | envName		    | templateName      |templateVersion	| var1   |
|TEST1| u1	   | u1	     |new|env1|NovaProvision     | 0.1		        | not-set|
|TEST2| u1	   | u1	     |old|env2|NovaProvision     | 0.1		        | not-set|
|TEST1| u2	   | u2	     |old|env3|NovaProvision     | 0.1		        | not-set|

Scenario: Try to delete environment

Given I log in system with username <userName> and password <password>
!--When I send a delete environment request with <projectName>, <envName>
!--Then Environment will be deleted

Examples:
|name| userName| password|projectName     | envName		    | 
|TEST1| u1	   | u1	     |new|env1|
|TEST2| u1	   | u1	     |old|env2|
|TEST1| u2	   | u2	     |old|env3|

Scenario: Try to change project's info

Given I log in system with username <userName> and password <password>
When I send request to edit project with name <oldName> and specify new name <projectName> description <description> and manager <manager>
Then I expect that project was changed successfully

Examples:
| name				| userName| password|projectName |	description		|manager	|oldName |
| TEST 1			|u1	      | u1	    |changedold	 	  |changed description	|changed manager	|old	 	  |
| TEST 2			|u1	      | u1	    |changednew	 	  |changed description	|changed manager	|new	 	  |
| TEST 3			|u2	      | u2	    |doublechangedold	 	  |double changed description	|doublechanged manager	|changedold	 	  |

Scenario: Try to change project's info in which user has no access

Given I log in system with username u2 and password u2
When I send request to edit project with name changednew and specify new name projectName description description and manager manager
Then I expect the request rejected as forbidden

Scenario: Postcondition - delete users

Given I log in system
When I send request to delete user with name <userName>
Then I expect that user was deleted successfully

Examples:
|name| userName| 
|TEST1| u1	  |
|TEST2| u2	  |

Scenario: Postcondition - delete project

When I send request to delete project with name <projectName>
Then I expect that project was deleted successfully

Examples:
|name| projectName|
|TEST1| doublechangedold|
|TEST3| changednew|

Scenario: Postcondition - Delete User Group

When I send request to delete user group with name <groupName>
Then I expect that user group was deleted successfully

Examples:
|name| groupName| 
|TEST1| gr1	  |
|TEST2| gr2	  |