Meta:
@author ybaturina
@theme usergroups

Scenario: Preconditions - Create user group

GivenStories: com/griddynamics/genesis/stories/usergroups/PreconditionCreateTestGroup.story#{0}

When I send get user groups request
Then I expect to see 1 user groups in list

Examples:
|name| groupName| mail		        | description	        | usersList| 
|TEST1| gr1	  | gr1@mailinator.com	| descr		    | null  | 

Scenario: Wrong symbols in description

When I send request to edit usergroup with name <groupName> and specify description <description> mailing list <mail> and users <usersList>
Then I expect that response with error message "Description":"Must be present" was returned

Examples:
|name| groupName| mail		        | description	        | usersList| 
|TEST1| gr1	  | gr1@mailinator.com	| empty		    | null  | 

Scenario: Wrong symbols in email

When I send request to edit usergroup with name <groupName> and specify description <description> mailing list <mail> and users <usersList>
Then I expect that response with compound error message "Mailing list must contain E-mail address. Only lowercase letters are allowed" was returned

Examples:
|name| groupName| mail		        | description	        | usersList| 
|TEST1| gr1	  | g@mailinator.com	| descr		    | null  | 

Scenario: Postcondition - Delete Groups

When I send request to delete user group with name <groupName>
Then I expect that user group was deleted successfully

Examples:
|name| groupName| 
|TEST1| gr1	  | 