Meta:
@author ybaturina
@theme roles

Scenario: Precondition - create users and project, Assign Genesis User role to users

GivenStories: com/griddynamics/genesis/stories/users/PreconditionCreateTestUser.story#{0},
              com/griddynamics/genesis/stories/users/PreconditionCreateTestUser.story#{1}
              
When I send request to edit role with name ROLE_GENESIS_USER and specify users u1, u2
Then I expect that role was changed successfully

Examples:
|name| userName| email				| firstName	| lastName | title   | password| groups    |result|
|TEST1| u1	  | u1@mailinator.com	| name 		| surname  | title   | u1	   | null	   |true  |
|TEST2| u2	  | u2@mailinator.com	| name 		| surname  | title   | u2	   | null	   |true  |

Scenario: Check if projects are visible
  
Given I log in system with username <userName> and password <password>
When I send get projects request 
Then I expect to see 0 projects in list

Examples:
|name| userName| password|
|TEST1| u1	   | u1	     |
|TEST2| u2	   | u2	     |

Scenario: Remove Genesis role from users
            
Given I log in system
When I send request to edit role with name <roleName> and specify users <users>
Then I expect that role was changed successfully

Examples:
|name| roleName| users|
|TEST1| ROLE_GENESIS_USER	   | empty	     |


Scenario: Check if projects are visible
  
Given I log in system with username <userName> and password <password>
When I send get projects request 
Then I expect the request rejected as unathorized

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