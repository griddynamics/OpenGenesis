Meta:
@author ybaturina
@theme roles

Scenario: Precondition - create users and project, Assign Genesis User role to users

GivenStories: com/griddynamics/genesis/stories/users/PreconditionCreateTestUser.story#{0},
              com/griddynamics/genesis/stories/users/PreconditionCreateTestUser.story#{1},
              com/griddynamics/genesis/stories/projects/PreconditionCreateTestProject.story#{0}
              
When I send request to edit role with name ROLE_GENESIS_USER and specify users u1, u2
Then I expect that role was changed successfully

Examples:
|name| userName| email				| firstName	| lastName | title   | password| groups    |result|projectName |	description		|manager	|
|TEST1| u1	  | u1@mailinator.com	| name 		| surname  | title   | u1	   | null	   |true  |old	 	  |testdescription	|manager	|
|TEST2| u2	  | u2@mailinator.com	| name 		| surname  | title   | u2	   | null	   |true  |old	 	  |testdescription	|manager	|

Scenario: View Genesis User role

When I send request to view role with name ROLE_GENESIS_USER
Then I expect that role has users u1, u2

Scenario: Check if projects are visible
  
Given I log in system with username <userName> and password <password>
When I send get projects request 
Then I expect to see 0 projects in list

Examples:
|name| userName| password|
|TEST1| u1	   | u1	     |
|TEST2| u2	   | u2	     |

Scenario: Check if project can be created

Given I log in system with username <userName> and password <password>
When I send request to create project with name newName description newDescription and manager newmanager
Then I expect the request rejected as forbidden

Examples:
|name| userName| password|
|TEST1| u1	   | u1	     |
|TEST2| u2	   | u2	     |

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