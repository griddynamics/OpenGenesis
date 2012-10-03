Feature: Users group test

  Background:
    Given Genesis is running
    And I am valid admin user

  Scenario Outline: Creation with invalid group fields
    When I'm creating user group "<groupName>" described as "<description>" with mailing list "<mail>" and users "<users>"
    Then Variable error with code 400 and error "<field>": "<message>" should be returned

    Examples:
      | groupName                         | mail		        | description	        | users | field | message                       |
      | 		                          | gr1@mailinator.com	| description 		    |       | name  | size must be between 2 and 32 |
      | g	                              | gr1@mailinator.com	| description 		    |       | name  | size must be between 2 and 32 |
      | qwertyuiopqwertyuiopqwertyuiopqwe | gr1@mailinator.com	| description 		    |       | name  | size must be between 2 and 32 |
      | Users                             | gr1@mailinator.com	| description 		    |       | name  | Invalid format. Use a combination of latin lowercase letters, numbers, dots, hyphens and underscores. |
      | <script>alert("a!")</script>      | gr1@mailinator.com	| description 		    |       | name  | Invalid format. Use a combination of latin lowercase letters, numbers, dots, hyphens and underscores. |
      | &lt;                              | gr1@mailinator.com	| description 		    |       | name  | Invalid format. Use a combination of latin lowercase letters, numbers, dots, hyphens and underscores. |
      | аб	                              | gr1@mailinator.com	| description 		    |       | name  | Invalid format. Use a combination of latin lowercase letters, numbers, dots, hyphens and underscores. |
      | name	                          | root        	    | description 		    |       | mailingList | not a well-formed email address |
      | name	                          | root        	    |  		                |       | description | may not be empty |

  Scenario: I can't create a group with duplicated name
    Given I successfully created user group "group" described as "description" with mailing list "list@example.com" and users ""
    When I'm creating user group "group" described as "description 2" with mailing list "list@example.com" and users ""
    Then Compound service error with code 400 and error "Group with name 'group' already exists" should be present in answer
    And I can delete group "group"

  Scenario: Create a group with users - Success path
    Given I successfully created user "user" with email "user@example.con", firstName "John" lastName "Doe" jobTitle "" password "password" and groups ""
    When  I'm creating user group "group" described as "description" with mailing list "list@example.com" and users "user"
    Then I should get response with code '200'
    And I can delete group "group"
    And I can delete user "user"

  Scenario: Create a group with non-existent users
    When  I'm creating user group "group" described as "description" with mailing list "list@example.com" and users "user"
    Then Compound service error with code 404 and error "Some of usernames in list is not found" should be present in answer
