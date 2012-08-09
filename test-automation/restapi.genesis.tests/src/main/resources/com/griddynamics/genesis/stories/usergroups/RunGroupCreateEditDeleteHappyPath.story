Meta:
@author ybaturina
@theme usergroups

Scenario: Create Group

When I send request to create user group with name <groupName> description <description> mailing list <mail> and users <usersList>
Then I expect that user group was created successfully
And I expect to see user group with name <groupName> description <description> and mailing list <mail> with result <result>

Examples:
|name| groupName| mail		        | description	        | usersList| result|
|TEST2| gr2	  | gr2@mailinator.com	| description 		    | null  | true|
|TEST3| gr3	  | empty	            | fhfhf 		            | null  | true|
|TEST4| gr4	  | null	            | <script language="javascript">alert("bug!")</script> 		| null  | true|

Scenario: Edit Group

When I send request to edit usergroup with name <groupName> and specify description <description> mailing list <mail> and users <usersList>
Then I expect that user group was changed successfully
And I expect to see user group with name <groupName> description <description> and mailing list <mail> with result <result>


Examples: 
|name| groupName| mail		        | description	        | usersList| result|
|TEST2| gr2	  | gr02@mailinator.com	| description 		    | empty  | true|
|TEST3| gr3	  | null	            | fhfggfh 		            | empty  | true|
|TEST4| gr4	  | empty	            | <script language="javascript">alert("bug!")</script> 		| empty  | true|

Scenario: Delete User Group

When I send request to delete user group with name <groupName>
Then I expect that user group was deleted successfully
And I expect to see user group with name <groupName> description <description> and mailing list <mail> with result <result>

Examples: 
|name| groupName| mail		        | description	        | usersList| result|
|TEST2| gr2	  | gr02@mailinator.com	| description 		    | empty  | false|
|TEST3| gr3	  | null	            | fghfgh 		            | empty  | false|
|TEST4| gr4	  | empty	            | <script language="javascript">alert("bug!")</script> 		| empty  | false|
 