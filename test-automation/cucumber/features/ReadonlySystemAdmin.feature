Feature: Readonly System Admin role
  As a Genesis/project administrator I want to grant users and groups read-only administration
  rights to the system, to allow them to view system, project and environment settings and data,
  and still be sure that they will not modify settings or run deployments.

  Background:
    Given Genesis is running
    And I am valid admin user

  Scenario: Readonly role must be present in list of possible roles
    When I'm listing predefined roles
    Then Role "ROLE_GENESIS_READONLY" must be in response
    
  Scenario: It should be possible to create user with readonly admin role
    When I successfully created user with parameters
      |username |foo                        |
      |password |pass                       |
      |firstname|john                       |
      |lastname |Doe                        |
      |email    |john@example.com           |
      |groups   |                           |
      |roles    |ROLE_GENESIS_READONLY|
    Then User "foo" should have role "ROLE_GENESIS_READONLY"
    And I can delete user "foo"

  Scenario: Readonly admin must be able to log in
    When I successfully created user with parameters
      |username |foo                        |
      |password |pass                       |
      |firstname|john                       |
      |lastname |Doe                        |
      |email    |john@example.com           |
      |groups   |                           |
      |roles    |ROLE_GENESIS_READONLY|
    Then User "foo" must be able to authenticate itself with password "pass"
    And I can delete user "foo"

  Scenario: Readonly admin must be able to read project list
    When I successfully created user with parameters
      |username |readonly                        |
      |password |password                       |
      |firstname|john                       |
      |lastname |Doe                        |
      |email    |john@example.com           |
      |groups   |                           |
      |roles    |ROLE_GENESIS_READONLY|
    And I create a project with the name 'test_admin' managed by 'manager'
    Then User 'readonly' with credential 'password' can read, but not update or delete project 'test_admin'
    And I can delete project 'test_admin'
    And I can delete user "readonly"

  Scenario: Readonly admin must be able to read user list
    When I successfully created user with parameters
      |username |readonly                        |
      |password |password                       |
      |firstname|john                       |
      |lastname |Doe                        |
      |email    |john@example.com           |
      |groups   |                           |
      |roles    |ROLE_GENESIS_READONLY|
    And I successfully created user with parameters
      |username |user                        |
      |password |password                       |
      |firstname|john                       |
      |lastname |Doe                        |
      |email    |user@example.com           |
      |groups   |                           |
      |roles    ||
    Then User "readonly" with credential "password" can read, but not update or delete user "user"
    And I can delete user "readonly"
    And I can delete user "user"

  Scenario: Readonly admin must be able to read group list
    When I successfully created user with parameters
      |username |readonly                        |
      |password |password                       |
      |firstname|john                       |
      |lastname |Doe                        |
      |email    |john@example.com           |
      |groups   |                           |
      |roles    |ROLE_GENESIS_READONLY|
    And I successfully created user group "test_group" described as "test" with mailing list "list@example.com" and users ""
    Then User "readonly" with credential "password" can read, but not update or delete user group "test_group"
    And I can delete user "readonly"
    And I can delete group "test_group"

  Scenario: Readonly admin must be able to read system databags
    When I successfully created user with parameters
      |username |readonly                        |
      |password |password                       |
      |firstname|john                       |
      |lastname |Doe                        |
      |email    |john@example.com           |
      |groups   |                           |
      |roles    |ROLE_GENESIS_READONLY|
    And I create databag "test" with values
      |foo|bar|
    Then User "readonly" with credential "password" can read, but not update or delete databag "test"
    And I can delete databag "test"
    And I can delete user "readonly"

  Scenario: Readonly admin must be able to read system settings
    When I successfully created user with parameters
      |username |readonly                        |
      |password |password                       |
      |firstname|john                       |
      |lastname |Doe                        |
      |email    |john@example.com           |
      |groups   |                           |
      |roles    |ROLE_GENESIS_READONLY|
    Then User "readonly" with credential "password" can list system settings
    But User "readonly" with credential "password" can't create a setting "com.griddynamics.foo" set to "false"
    And I can delete user "readonly"

  Scenario: Readonly admin must not be able to create new projects
    When I successfully created user with parameters
      |username |readonly                        |
      |password |password                       |
      |firstname|john                       |
      |lastname |Doe                        |
      |email    |john@example.com           |
      |groups   |                           |
      |roles    |ROLE_GENESIS_READONLY|
    Then User "readonly" with credential "password" cannot create project "ro_project" managed by "manager"
    And I can delete user "readonly"

  Scenario: Readonly admin must be able to read existing environments, but not run workflows
    When I successfully created user with parameters
      |username |readonly                        |
      |password |password                       |
      |firstname|john                       |
      |lastname |Doe                        |
      |email    |john@example.com           |
      |groups   |                           |
      |roles    |ROLE_GENESIS_READONLY|
    And I create a project with the name 'ro_envs' managed by 'manager'
    And I create an environment 'ro_env' in project 'ro_envs' with template 'Preconditions' version '0.1'
    Then User "readonly" with credential "password" can read environment "ro_env" in project "ro_envs", but can't run workflow "should_run" neither delete it
    And I can delete user "readonly"
    And I can delete environment 'ro_env' in project 'ro_envs'
    And I can delete project 'ro_envs'

  Scenario: Readonly admin can't create new environments
    When I successfully created user with parameters
      |username |readonly                        |
      |password |password                       |
      |firstname|john                       |
      |lastname |Doe                        |
      |email    |john@example.com           |
      |groups   |                           |
      |roles    |ROLE_GENESIS_READONLY|
    And I create a project with the name 'ro_envs' managed by 'manager'
    Then User "readonly" with credential "password" can't create an environment "ro_env" in project "ro_envs" with template "Preconditions" version "0.1"
    And I can delete project 'ro_envs'
    And I can delete user "readonly"
