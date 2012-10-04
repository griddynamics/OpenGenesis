Feature: Users tests

  Background:
    Given Genesis is running
    And I am valid admin user
    And I create a project with the name 'Users' managed by 'manager'

  Scenario: Creation and deletion of user - Success path
    When I'm creating user "username" with email "mail@example.com", firstName "John" lastName "Doe" jobTitle "User" password "password" and groups ""
    Then I should get response with code '200'
    And I can delete user "username"

  Scenario: Creation and deletion of user with group - Success path
    Given I successfully created user group "users" described as "test group" with mailing list "list@example.com" and users ""
    When I'm creating user "username" with email "mail@example.com", firstName "John" lastName "Doe" jobTitle "User" password "password" and groups "users"
    Then I should get response with code '200'
    And I can delete user "username"
    And I can delete group "users"

  Scenario: Creation of user with group - Nonexistent group
    Given There are no user group "users"
    When I'm creating user "username" with email "mail@example.com", firstName "John" lastName "Doe" jobTitle "User" password "password" and groups "users"
    Then Service error with code 404 and error 'groups': 'Group users does not exist' should be returned
    And I can delete user "username"

  Scenario: Duplicate username
    Given I'm creating user "fred" with email "mail@example.com", firstName "John" lastName "Doe" jobTitle "user" password "password" and groups ""
    When I'm creating user "fred" with email "other@example.com", firstName "Jane" lastName "Doe" jobTitle "user" password "passw0rd" and groups ""
    Then Compound service error with code 400 and error "User with username [fred] is already registered" should be present in answer
    And I can delete user "fred"

  Scenario: Duplicate email
    Given I'm creating user "fred" with email "mail@example.com", firstName "John" lastName "Doe" jobTitle "user" password "password" and groups ""
    When I'm creating user "barney" with email "mail@example.com", firstName "Jane" lastName "Doe" jobTitle "user" password "passw0rd" and groups ""
    Then Compound service error with code 400 and error "User with email [mail@example.com] is already registered" should be present in answer
    And I can delete user "fred"

  Scenario Outline: Wrong symbols in username
    When I'm creating user "<userName>" with email "<email>", firstName "<firstName>" lastName "<lastName>" jobTitle "<title>" password "<password>" and groups "<groups>"
    Then Variable error with code 400 and error "<field>": "<message>" should be returned
    Examples:
    | userName                          | email				    | firstName	| lastName | title   | password| groups| field     | message                                                                                              |
    | user name	                        | user@mailinator.com	| name 		| surname  | title   | u1	   | 	   | username  | Invalid format. Use a combination of latin lowercase letters, numbers, dots, hyphens and underscores.|
    | u	                                | user@mailinator.com	| name 		| surname  | title   | u1	   | 	   | username  | size must be between 2 and 32  |
    |	                                | user@mailinator.com	| name 		| surname  | title   | u1	   | 	   | username  | size must be between 2 and 32  |
    | &lt;                              | user@mailinator.com	| name 		| surname  | title   | u1	   | 	   | username  | Invalid format. Use a combination of latin lowercase letters, numbers, dots, hyphens and underscores. |
    | аб	                            | user@mailinator.com	| name 		| surname  | title   | u1	   | 	   | username  | Invalid format. Use a combination of latin lowercase letters, numbers, dots, hyphens and underscores. |
    | <script>alert('bug!')</script>    | user@mailinator.com	| name 		| surname  | title   | u1	   | 	   | username  | Invalid format. Use a combination of latin lowercase letters, numbers, dots, hyphens and underscores. |
    | drop table users                  | user@mailinator.com	| name 		| surname  | title   | u1	   | 	   | username  | Invalid format. Use a combination of latin lowercase letters, numbers, dots, hyphens and underscores. |
    | qwertyuiopqwertyuiopqwertyuiopqwe | user@mailinator.com	| name 		| surname  | title   | u1	   | 	   | username  | size must be between 2 and 32 |

  Scenario Outline: Wrong symbols in last name
    When I'm creating user "<userName>" with email "<email>", firstName "<firstName>" lastName "<lastName>" jobTitle "<title>" password "<password>" and groups "<groups>"
    Then Variable error with code 400 and error "<field>": "<message>" should be returned
  Examples:
    | userName | email				    | firstName	| lastName | title   | password| groups| field     | message                                                                                              |
    | username | user@mailinator.com	| name 		| 123name  | title   | u1	   | 	   | lastName  | Invalid format. Only letters and spaces are allowed.|
    | username | user@mailinator.com	| name 		|          | title   | u1	   | 	   | lastName  | may not be empty |
    | username | user@mailinator.com	| name 		| &lt;     | title   | u1	   | 	   | lastName  | Invalid format. Only letters and spaces are allowed. |
    | username | user@mailinator.com	| name 		| <script>alert('bug!')</script>  | title   | u1	   | 	   | lastName  | Invalid format. Only letters and spaces are allowed. |

  Scenario Outline: Wrong symbols in first name
    When I'm creating user "<userName>" with email "<email>", firstName "<firstName>" lastName "<lastName>" jobTitle "<title>" password "<password>" and groups "<groups>"
    Then Variable error with code 400 and error "<field>": "<message>" should be returned
  Examples:
    | userName  | email				    | lastName	| firstName | title   | password| groups| field     | message                                                                                              |
    | username | user@mailinator.com	| name 		| 123name  | title   | u1	   | 	   | firstName  | Invalid format. Only letters and spaces are allowed.|
    | username | user@mailinator.com	| name 		|          | title   | u1	   | 	   | firstName  | size must be between 1 and 256  |
    | username | user@mailinator.com	| name 		| &lt;     | title   | u1	   | 	   | firstName  | Invalid format. Only letters and spaces are allowed. |
    | username | user@mailinator.com	| name 		| <script>alert('bug!')</script>  | title   | u1	   | 	   | firstName  | Invalid format. Only letters and spaces are allowed. |

  Scenario Outline: Wrong symbols in email
    When I'm creating user "<userName>" with email "<email>", firstName "<firstName>" lastName "<lastName>" jobTitle "<title>" password "<password>" and groups "<groups>"
    Then Variable error with code 400 and error "<field>": "<message>" should be returned
  Examples:
    | userName | email				            | lastName	| firstName| title   | password| groups| field  | message                                                                                              |
    | username | .u@mailinatorcom	            | name 		| surname  | title   | u1	   | 	   | email  | not a well-formed email address |
    | username | аб@mailinator.com	            | name 		| surname  | title   | u1	   | 	   | email  | not a well-formed email address |
    | username | just mail        	            | name 		| surname  | title   | u1	   | 	   | email  | not a well-formed email address |
    | username | <script>alert('bug!')</script>	| name 		| surname  | title   | u1	   | 	   | email  | not a well-formed email address |

  Scenario: Password is mandatory
    When I'm creating user "username" with email "mail@example.com", firstName "John" lastName "Doe" jobTitle "User" password "" and groups ""
    Then Variable error with code 400 and error "password": "size must be between 3 and 64" should be returned

  Scenario Outline: Update user
    Given I successfully created user "user" with email "mail@example.com", firstName "John" lastName "Doe" jobTitle "User" password "passw0wd" and groups ""
    When I'm updating user "user" with "<field>" set to "<value>"
    Then I should get response with code '<code>'
    And I can delete user "user"
    Examples:
      |field     |value                         |code|
      |email     |                              |400 |
      |email     |email                         |400 |
      |firstName |                              |400 |
      |firstName |<script>alert('bug!')</script>|400 |
      |lastName  |                              |400 |
      |lastName  |<script>alert('bug!')</script>|400 |