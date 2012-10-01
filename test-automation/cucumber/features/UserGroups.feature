Feature: Users group test

  Background:
    Given Genesis is running
    And I am valid admin user

  Scenario Outline: Creation with invalid group fields
    When I'm creating user group "<groupName>" described as "<description>" with mailing list "<mail>" and users "<users>"
    Then Variable error "<field>" :"<message>" should be returned

    Examples:
      |name | groupName                         | mail		            | description	        | users | field | message                       |
      |TEST1| 		                            | gr1@mailinator.com	| description 		    |       | name  | size must be between 2 and 32 |
      |TEST2| g	                                | gr1@mailinator.com	| description 		    |       | name  | size must be between 2 and 32 |
      |TEST3| qwertyuiopqwertyuiopqwertyuiopqwe | gr1@mailinator.com	| description 		    |       | name  | size must be between 2 and 32 |
      |TEST4| <script>alert("a!")</script>      | gr1@mailinator.com	| description 		    |       | name  | Invalid format. Use a combination of latin lowercase letters, numbers, dots, hyphens and underscores. |
      |TEST5| &lt;                              | gr1@mailinator.com	| description 		    |       | name  | Invalid format. Use a combination of latin lowercase letters, numbers, dots, hyphens and underscores. |
      |TEST6| аб	                            | gr1@mailinator.com	| description 		    |       | name  | Invalid format. Use a combination of latin lowercase letters, numbers, dots, hyphens and underscores. |
      |TEST7| name	                            | root        	        | description 		    |       | mailingList | not a well-formed email address |
      |TEST8| name	                            | root        	        |  		                |       | description | may not be empty |

  Scenario: I can't create a group with duplicated name
    Given I successfully created user group "group" described as "description" with mailing list "list@example.com" and users ""
    When I'm creating user group "group" described as "description 2" with mailing list "list@example.com" and users ""
    Then Compound service error "Group with name 'group' already exists" should be present in answer
    And I can delete group "group"