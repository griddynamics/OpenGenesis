Meta:
@author ybaturina
@theme users

Scenario: Wrong symbols in username

When I send request to create user with userName <userName> email <email> firstName <firstName> lastName <lastName> jobTitle <title> password <password> and groups <groups>
Then I expect that response with error message "User name":"Invalid format. Use a combination of latin lowercase letters, numbers, dots, hyphens and underscores. Length must be from 2 to 32" was returned
And I expect to see user with userName <userName> email <email> firstName <firstName> lastName <lastName> and jobTitle <title> with result false

Examples:
|name| userName| email				| firstName	| lastName | title   | password| groups    |
|TEST1| user name	  | user@mailinator.com	| name 		| surname  | title   | u1	   | null	   |
|TEST2| u	  | user@mailinator.com	| name 		| surname  | title   | u1	   | null	   |
|TEST3|	  | user@mailinator.com	| name 		| surname  | title   | u1	   | null	   |
|TEST4| &lt;	  | user@mailinator.com	| name 		| surname  | title   | u1	   | null	   |
|TEST5| аб	  | user@mailinator.com	| name 		| surname  | title   | u1	   | null	   |
|TEST6| <script language="javascript">alert("bug!")</script>	  | user@mailinator.com	| name 		| surname  | title   | u1	   | null	   |
|TEST7| drop table users	  | user@mailinator.com	| name 		| surname  | title   | u1	   | null	   |
|TEST8| qwertyuiopqwertyuiopqwertyuiopqwe	  | user@mailinator.com	| name 		| surname  | title   | u1	   | null	   |

Scenario: Wrong symbols in first name

When I send request to create user with userName <userName> email <email> firstName <firstName> lastName <lastName> jobTitle <title> password <password> and groups <groups>
Then I expect that response with error message "First Name":"Invalid format. Use a combination of letters and spaces. Length must be from 2 to 64" was returned
And I expect to see user with userName <userName> email <email> firstName <firstName> lastName <lastName> and jobTitle <title> with result false

Examples:
|name| userName| email				| firstName	| lastName | title   | password| groups    |
|TEST1| username	  | user@mailinator.com	| 123name 		| surname  | title   | u1	   | null	   |
|TEST2| username	  | user@mailinator.com	| n 		| surname  | title   | u1	   | null	   |
|TEST3|	username  | user@mailinator.com	| 		| surname  | title   | u1	   | null	   |
|TEST4| username	  | user@mailinator.com	| &lt; 		| surname  | title   | u1	   | null	   |
|TEST5| username	  | user@mailinator.com	| <script language="javascript">alert("bug!")</script> 		| surname  | title   | u1	   | null	   |
|TEST6| username	  | user@mailinator.com	| qwertyuiopqwertyuiopqwertyuiopqwqwertyuiopqwertyuiopqwertyuiopqwy | surname  | title   | u1	   | null	   |

Scenario: Wrong symbols in last name

When I send request to create user with userName <userName> email <email> firstName <firstName> lastName <lastName> jobTitle <title> password <password> and groups <groups>
Then I expect that response with error message "Last Name":"Invalid format. Use a combination of letters and spaces. Length must be from 2 to 64" was returned
And I expect to see user with userName <userName> email <email> firstName <firstName> lastName <lastName> and jobTitle <title> with result false

Examples:
|name| userName| email				| lastName	| firstName | title   | password| groups    |
|TEST1| username	  | user@mailinator.com	| 123name 		| surname  | title   | u1	   | null	   |
|TEST2| username	  | user@mailinator.com	| n 		| surname  | title   | u1	   | null	   |
|TEST3|	username  | user@mailinator.com	| 		| surname  | title   | u1	   | null	   |
|TEST4| username	  | user@mailinator.com	| &lt; 		| surname  | title   | u1	   | null	   |
|TEST5| username	  | user@mailinator.com	| <script language="javascript">alert("bug!")</script> 		| surname  | title   | u1	   | null	   |
|TEST6| username	  | user@mailinator.com	| qwertyuiopqwertyuiopqwertyuiopqwqwertyuiopqwertyuiopqwertyuiopqwy | surname  | title   | u1	   | null	   |

Scenario: Wrong symbols in email

When I send request to create user with userName <userName> email <email> firstName <firstName> lastName <lastName> jobTitle <title> password <password> and groups <groups>
Then I expect that response with error message "E-Mail":"Invalid format. Note that only lowercase letters are allowed. Length must be from 7 to 64." was returned
And I expect to see user with userName <userName> email <email> firstName <firstName> lastName <lastName> and jobTitle <title> with result false

Examples:
|name| userName| email				| firstName	| lastName | title   | password| groups    |
|TEST1| username	  | u@mailinatorcom	| first name 		| surname  | title   | u1	   | null	   |
|TEST2| username	  | аб@mailinator.com	| first name 		| surname  | title   | u1	   | null	   |
|TEST3|	username  |  	| first name		| surname  | title   | u1	   | null	   |
|TEST4| username	  | user@.com	| first name 		| surname  | title   | u1	   | null	   |
|TEST5| username	  | <script language="javascript">alert("bug!")</script>	| first name 		| surname  | title   | u1	   | null	   |

Scenario: Wrong symbols in password

When I send request to create user with userName <userName> email <email> firstName <firstName> lastName <lastName> jobTitle <title> password <password> and groups <groups>
Then I expect that response with error message "Password":"Must be present" was returned
And I expect to see user with userName <userName> email <email> firstName <firstName> lastName <lastName> and jobTitle <title> with result false

Examples:
|name| userName| email				| firstName	| lastName | title   | password| groups    |
|TEST1| username	  | user@mailinator.com	| first name 		| surname  |   | 	   | null	   |

Scenario: Try to create user with duplicated username

GivenStories: com/griddynamics/genesis/stories/users/PreconditionCreateTestUser.story#{0}

When I send request to create user with userName username email newuser@mailinator.com and password u1
Then I expect that response with compound error message "User with username [username] is already registered" was returned

Examples:
|name| userName| email				| firstName	| lastName | title   | password| groups    |
|TEST1| username	  | user@mailinator.com	| name 		| surname  | title   | u1	   | null	   |

Scenario: Try to create user with duplicated email

When I send request to create user with userName newusername email user@mailinator.com and password u1
Then I expect that response with compound error message "User with email [user@mailinator.com] is already registered" was returned

Scenario: Postcondition - Delete Users

When I send request to delete user with name username
Then I expect that user was deleted successfully

Examples:
|name| userName| email				| firstName	| lastName | title   | password| groups    |
|TEST1| username	  | user@mailinator.com	| name 		| surname  | title   | u1	   | null	   |