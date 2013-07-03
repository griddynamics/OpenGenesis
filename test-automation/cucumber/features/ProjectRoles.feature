Feature: Project roles changes

  Background:
    Given Genesis is running
    And I am valid admin user

  Scenario: Try to get access when user is not assigned to project
    When I create a project with the name 'TestUsers' managed by 'manager'
    And I successfully created user with parameters
      |username |foo                        |
      |password |pass                       |
      |firstname|john                       |
      |lastname |Doe                        |
      |email    |john@example.com           |
      |groups   |                           |
      |roles    |ROLE_GENESIS_USER          |
    Then User "foo" with credential "pass" cannot find project 'TestUsers'
    And I can delete user "foo"
    And I can delete project 'TestUsers'

  Scenario: Try to get access when user is assigned to project
    When I create a project with the name 'TestUsers2' managed by 'manager'
    And I successfully created user with parameters
      |username |foo                        |
      |password |pass                       |
      |firstname|john                       |
      |lastname |Doe                        |
      |email    |john@example.com           |
      |groups   |                           |
      |roles    |ROLE_GENESIS_USER          |
    And User "foo" with credential "pass" cannot find project 'TestUsers2'
    But If user 'foo' becomes a project user of a project 'TestUsers2'
    Then User "foo" with credential "pass" can find project 'TestUsers2'
    And I can delete user "foo"
    And I can delete project 'TestUsers2'
