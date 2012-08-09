Meta:
@author ybaturina
@theme users

Scenario: Edit Preconditions - create user

GivenStories: com/griddynamics/genesis/stories/users/PreconditionCreateTestUser.story#{0},
              com/griddynamics/genesis/stories/users/PreconditionCreateTestUser.story#{1}

When I send get users request
Then I expect to see 2 users in list

Examples:
|name| userName| email				| firstName	| lastName | title   | password| groups    |
|TEST1| u1	  | u1@mailinator.com	| name 		| surname  | title   | u1	   | null	   |
|TEST2| u2	  | u2@mailinator.com	| fn 		| sn  |    	 | u2	   | null	   |

Scenario: Edit - Wrong symbols in first name

When I send request to edit user with userName <userName> and specify email <email> firstName <firstName> lastName <lastName> jobTitle <title> and groups <groups>
Then I expect that response with error message "First Name":"Invalid format. Use a combination of letters and spaces. Length must be from 2 to 64" was returned

Examples:
|name| userName| email				| firstName	| lastName | title   | password| groups    |
|TEST1| u2	  | u2@mailinator.com	| 123name 		| sn  |    	 | u2	   | null	   |
|TEST2| u2	  | u2@mailinator.com	| n 		| sn  |    	 | u2	   | null	   |
|TEST3| u2	  | u2@mailinator.com	| empty 		| sn  |    	 | u2	   | null	   |
|TEST4| u2	  | u2@mailinator.com	| &lt; 		| sn  |    	 | u2	   | null	   |
|TEST5| u2	  | u2@mailinator.com	| <script language="javascript">alert("bug!")</script> 		| sn  |    	 | u2	   | null	   |
|TEST6| u2	  | u2@mailinator.com	| qwertyuiopqwertyuiopqwertyuiopqwqwertyuiopqwertyuiopqwertyuiopqwy 		| sn  |    	 | u2	   | null	   |

Scenario: Edit - Wrong symbols in last name

When I send request to edit user with userName <userName> and specify email <email> firstName <firstName> lastName <lastName> jobTitle <title> and groups <groups>
Then I expect that response with error message "Last Name":"Invalid format. Use a combination of letters and spaces. Length must be from 2 to 64" was returned

Examples:
|name| userName| email				| lastName	| firstName | title   | password| groups    |
|TEST1| u2	  | u2@mailinator.com	| 123name 		| fn  |    	 | u2	   | null	   |
|TEST2| u2	  | u2@mailinator.com	| n 		| fsns  |    	 | u2	   | null	   |
|TEST3| u2	  | u2@mailinator.com	| empty 		| fn  |    	 | u2	   | null	   |
|TEST4| u2	  | u2@mailinator.com	| &lt; 		| gn |    	 | u2	   | null	   |
|TEST5| u2	  | u2@mailinator.com	| <script language="javascript">alert("bug!")</script> 		| sfsf  |    	 | u2	   | null	   |
|TEST6| u2	  | u2@mailinator.com	| qwertyuiopqwertyuiopqwertyuiopqwqwertyuiopqwertyuiopqwertyuiopqwy 		| sdfsf  |    	 | u2	   | null	   |

Scenario: Edit - Wrong symbols in email

When I send request to edit user with userName <userName> and specify email <email> firstName <firstName> lastName <lastName> jobTitle <title> and groups <groups>
Then I expect that response with error message "E-Mail":"Invalid format. Note that only lowercase letters are allowed. Length must be from 7 to 64." was returned

Examples:
|name| userName| email				| firstName	| lastName | title   | password| groups    |
|TEST1| u2	  | u@mailinator.com	| name 		| surname  | title   | u1	   | null	   |
|TEST2| u2	  | аб@mailinator.com	| fn 		| sn  |    	 | u2	   | null	   |
|TEST3| u2	  | user@.com	| fn 		| sn  |    	 | u2	   | null	   |
|TEST4| u2	  | first name	| fn 		| sn  |    	 | u2	   | null	   |
|TEST5| u2	  | <script language="javascript">alert("bug!")</script>	| fn 		| sn  |    	 | u2	   | null	   |

Scenario: Edit - Try to create user with duplicated email

When I send request to edit user with userName <userName> and specify email <email> firstName <firstName> lastName <lastName> jobTitle <title> and groups <groups>
Then I expect that response with compound error message "Email [u1@mailinator.com] is already registered for other user" was returned

Examples:
|name| userName| email				| firstName	| lastName | title   | password| groups    |
|TEST1| u2	  | u1@mailinator.com	| first name 		| surname  |   | 	u2   | null	   |

Scenario: Edit Postcondition - Delete Users

When I send request to delete user with name <userName>
Then I expect that user was deleted successfully

Examples:
|name| userName| email				| firstName	| lastName | title   | password| groups    |
|TEST1| u1	  | u1@mailinator.com	| name 		| surname  | title   | u1	   | null	   |
|TEST2| u2	  | u2@mailinator.com	| fn 		| sn  |    	 | u2	   | null	   |