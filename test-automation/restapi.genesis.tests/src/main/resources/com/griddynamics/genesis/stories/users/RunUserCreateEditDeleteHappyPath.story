Meta:
@author ybaturina
@theme users

Scenario: Create User

When I send request to create user with userName <userName> email <email> firstName <firstName> lastName <lastName> jobTitle <title> password <password> and groups <groups>
Then I expect that user was created successfully
And I expect to see user with userName <userName> email <email> firstName <firstName> lastName <lastName> and jobTitle <title> with result <result>

Examples:
|name| userName| email				| firstName	| lastName | title   | password| groups    |result|
|TEST1| u1	  | u1@mailinator.com	| name 		| surname  | title   | u1	   | null	   |true  |
|TEST2| u2	  | u2@mailinator.com	| fname 		| sname  |    	 | u2	   | null	   |true  |
|TEST3| u3	  | u3@mailinator.com	| name 		| surname  | title   | u3	   | null	   |true  |
|TEST4| u4	  | u4@mailinator.com	| drop table users 		| surname  |    	 | u4	   | null	   |true  |

Scenario: Edit User

When I send request to edit user with userName <userName> and specify email <email> firstName <firstName> lastName <lastName> jobTitle <title> and groups <groups>
Then I expect that user was changed successfully
And I expect to see user with userName <userName> email <email> firstName <firstName> lastName <lastName> and jobTitle <title> with result <result>


Examples: 
|name| userName| email				| firstName	    | lastName  | title	  | groups	   |result|
|TEST1| u1	  | u11@mailinator.com	| fname 		| ssurname  |    | null	   |true  |
|TEST2| u2	  | u22@mailinator.com	| fname 		| ssurname  | title   | null	   |true  |
|TEST3| u3	  | u33@mailinator.com	| fname 		| ssurname  |    | null	   |true  |
|TEST4| u4	  | u44@mailinator.com	| fname 		| ssurname  | title   | null	   |true  | 

Scenario: Delete User


When I send request to delete user with name <userName>
Then I expect that user was deleted successfully
And I expect to see user with userName <userName> email <email> firstName <firstName> lastName <lastName> and jobTitle <title> with result false

Examples:
|name| userName| email				| firstName	    | lastName  | title	  |
|TEST1| u1	  | u11@mailinator.com	| fname 		| ssurname  |    |
|TEST2| u2	  | u22@mailinator.com	| fname 		| ssurname  | title   |
|TEST3| u3	  | u33@mailinator.com	| fname 		| ssurname  |    |
|TEST4| u4	  | u44@mailinator.com	| fname 		| ssurname  | title   |
 