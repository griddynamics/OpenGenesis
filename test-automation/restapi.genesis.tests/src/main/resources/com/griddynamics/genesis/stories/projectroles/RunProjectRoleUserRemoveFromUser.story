Meta:
@author ybaturina
@theme projectroles

Scenario: Precondition - create users and project, Assign Genesis User role to users

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

Scenario: Preconditions - Assign project user role to users

When I send request to edit project role with name ROLE_GENESIS_PROJECT_USER in project old and specify users u1, u2
Then I expect that project role was changed successfully

When I send request to edit project role with name ROLE_GENESIS_PROJECT_USER in project new and specify users u1
Then I expect that project role was changed successfully

Scenario: Check user permissions in project
  
Given I log in system with username <userName> and password <password>
When I send get user permissions in project <projectName> request
Then I expect to see user permissions <permissions>

Examples:
|name| userName| password|projectName|permissions|
|TEST1| u1	   | u1	     |old|ROLE_GENESIS_PROJECT_USER|
|TEST2| u2	   | u2	     |old|ROLE_GENESIS_PROJECT_USER|
|TEST1| u1	   | u1	     |new|ROLE_GENESIS_PROJECT_USER|

Scenario: Unassign project user role from users

Given I log in system
When I send request to edit project role with name <roleName> in project <projectName> and specify users <users>
Then I expect that project role was changed successfully

Examples:
|name| roleName| projectName|users		    | 
|TEST1| ROLE_GENESIS_PROJECT_USER	   | old	     |empty|
|TEST2| ROLE_GENESIS_PROJECT_USER	   | new	     |empty|

Scenario: Check if projects are invisible
  
Given I log in system with username <userName> and password <password>
When I send get projects request 
Then I expect to see <quantity> projects in list

Examples:
|name| userName| password|quantity|
|TEST1| u1	   | u1	     |0|
|TEST2| u2	   | u2	     |0|

Scenario: Postcondition - delete users

Given I log in system
When I send request to delete user with name <userName>
Then I expect that user was deleted successfully

Examples:
|name| userName| 
|TEST1| u1	  |
|TEST2| u2	  |

Scenario: Postcondition - delete project

Given I log in system
When I send request to delete project with name <projectName>
Then I expect that project was deleted successfully

Examples:
|name| projectName|
|TEST1| old|
|TEST2| newName|
|TEST3| new|