Feature: Unified name rules for projects and environment. Environment part

  Background:
    Given Genesis is running
    And I am valid admin user
    And I create a project with the name 'Environments' managed by 'manager'
    And There is a template 'Simple' version '0.1' for project 'Environments'


  Scenario: I can create an environment in project
    Given there is no environment 'environment1' in project 'Environments'
    When I create an environment 'environment1' in project 'Environments' with template 'Simple' version '0.1'
    Then I should get response with code '200'
    And there must be an environment 'environment1' in project 'Environments'
    And I can remove environment 'environment1' in project 'Environments'
    And I can delete project 'Environments'

  Scenario: I can't create two environments with the same name in the same project
    Given I create an environment 'environment1' in project 'Environments' with template 'Simple' version '0.1'
    When I create an environment 'environment1' in project 'Environments' with template 'Simple' version '0.1'
    Then Service error with code 400 and error 'envName': 'Environment with the same name already exists in project' should be returned
    And I can remove environment 'environment1' in project 'Environments'
    And I can delete project 'Environments'
  
  Scenario: I can create environments with the same names in different projects
    Given I create a project with the name 'Environments2' managed by 'manager'
    And There is a template 'Simple' version '0.1' for project 'Environments2'
    And there is no environment 'environment1' in project 'Environments2'
    And I create an environment 'environment1' in project 'Environments' with template 'Simple' version '0.1'
    When I create an environment 'environment1' in project 'Environments2' with template 'Simple' version '0.1'
    And there must be an environment 'environment1' in project 'Environments2'
    And I can remove environment 'environment1' in project 'Environments'
    And I can remove environment 'environment1' in project 'Environments2'
    And I can delete project 'Environments'
    And I can delete project 'Environments2'

  Scenario Outline: Allowed symbols for environment name
    When I create an environment '<name>' in project 'Environments' with template 'Simple' version '0.1'
    Then I should get response with code '<code>'
    And there must be an environment '<name>' in project 'Environments'
    And I can remove environment '<name>' in project 'Environments'
    And I can delete project 'Environments'
    Examples:
      | name   | code |
      | name   | 200  |
      | Name   | 200  |
      | nAmE   | 200  |
      | Q1w2e4 | 200  |

  Scenario: I can rename an existing environment, if new name is not duplicated
    Given I create an environment 'name' in project 'Environments' with template 'Simple' version '0.1'
    And there is no environment 'newname' in project 'Environments'
    When I'm renaming environment 'name' to 'newname' in project 'Environments'
    Then I should get response with code '200'
    And there must be an environment 'newname' in project 'Environments'
    But there is no environment 'name' in project 'Environments'
    And I can remove environment 'newname' in project 'Environments'
    And I can delete project 'Environments'

  Scenario: I can't rename an existing environment, if new name is duplicated
    Given I create an environment 'Name' in project 'Environments' with template 'Simple' version '0.1'
    And I create an environment 'newname' in project 'Environments' with template 'Simple' version '0.1'
    When I'm renaming environment 'Name' to 'newname' in project 'Environments'
    Then I should get response with code '400'
    And I can remove environment 'newname' in project 'Environments'
    And I can remove environment 'Name' in project 'Environments'
    And I can delete project 'Environments'

  Scenario: I can't rename an existing environment if new name is old name + spaces
    Given I create an environment 'Name' in project 'Environments' with template 'Simple' version '0.1'
    And I create an environment 'newname' in project 'Environments' with template 'Simple' version '0.1'
    When I'm renaming environment 'Name' to 'newname ' in project 'Environments'
    Then I should get response with code '400'
    And I can remove environment 'Name' in project 'Environments'
    And I can remove environment 'newname' in project 'Environments'
    And I can delete project 'Environments'

  Scenario: I can't rename an existing environment if new name contains only spaces
    Given I create an environment 'Name' in project 'Environments' with template 'Simple' version '0.1'
    When I'm renaming environment 'Name' to '   ' in project 'Environments'
    Then I should get response with code '400'
    And I can remove environment 'Name' in project 'Environments'
    And I can delete project 'Environments'