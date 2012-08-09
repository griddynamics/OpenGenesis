Meta:
@author ybaturina
@theme usergroups

Scenario: Wrong symbols in groupname

When I send request to create user group with name <groupName> description <description> mailing list <mail> and users <usersList>
Then I expect that response with error message "Group name":"Invalid format. Use a combination of latin lowercase letters, numbers, dots, hyphens and underscores. Length must be from 2 to 32" was returned

Examples:
|name| groupName| mail		        | description	        | usersList| 
|TEST1| g	  | gr1@mailinator.com	| description 		    | null  | 
|TEST2| &lt;		  | gr1@mailinator.com	| description 		    | null  | 
|TEST3| 		  | gr1@mailinator.com	| description 		    | null  | 
|TEST4|  <script language="javascript">alert("bug!")</script> | gr1@mailinator.com	| description 		    | null  | 
|TEST5|qwertyuiopqwertyuiopqwertyuiopqwe | gr1@mailinator.com	| description 		    | null  | 
|TEST6| аб	  | gr1@mailinator.com	| description 		    | null  |

Scenario: Wrong symbols in description

When I send request to create user group with name <groupName> description <description> mailing list <mail> and users <usersList>
Then I expect that response with error message "Description":"Must be present" was returned

Examples:
|name| groupName| mail		        | description	        | usersList| 
|TEST1| gr1	  | gr1@mailinator.com	| empty		    | null  | 

Scenario: Wrong symbols in email

When I send request to create user group with name <groupName> description <description> mailing list <mail> and users <usersList>
Then I expect that response with compound error message "Mailing list must contain E-mail address. Only lowercase letters are allowed" was returned

Examples:
|name| groupName| mail		        | description	        | usersList| 
|TEST1| gr1	  | g@mailinator.com	| descr		    | null  | 

Scenario: Try to create user group with duplicated groupname

GivenStories: com/griddynamics/genesis/stories/usergroups/PreconditionCreateTestGroup.story#{0}

When I send request to create user group with name gr1 description descr mailing list gr1@mailinator.com and users null
Then I expect that response with compound error message "Group name must be unique" was returned

Examples:
|name| groupName| mail		        | description	        | usersList| 
|TEST1| gr1	  | gr1@mailinator.com	| descr		    | null  | 


Scenario: Postcondition - Delete Groups

When I send request to delete user group with name <groupName>
Then I expect that user group was deleted successfully

Examples:
|name| groupName| 
|TEST1| gr1	  | 