Feature: Agent registry CRUD tests
  In order to be able to use remote agents, I must have an agent registry on a main Genesis server

  Background:
    Given Genesis is running
    And I am valid admin user

  Scenario: Successful agent creation/deletion
    When I create an agent as
      |hostname|localhost|
      |port    |2053     |
      |tags    |dev      |
    Then I should get response with code '200'
    And I can delete an agent for host "localhost" and port 2053

  Scenario Outline: Invalid input
    When I create an agent with <hostname>, <port>, <tags>
    Then Variable error with code 400 and error "<field>": "<message>" should be returned
    Examples:
    |hostname|port    |tags   |field        |message                                  |
    |&acs        |       0|test   |hostname     |Invalid format. Hostname must be a host name according to RFC 952 or valid IP v4 address                        |
    |host    |       0|test   |port         |must be greater than or equal to 1       |
    |host    |   40000|test   |port         |must be less than or equal to 32767      |