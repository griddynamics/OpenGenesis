Feature: Unified name rules for projects and environment. Environment part

  Background:
    Given Genesis is running
    And I am valid admin user
    And I create a project with the name 'Environments' managed by 'manager'
    And I create a project with the name 'Environments2' managed by 'manager' 
    And There is a template 'Simple' version '0.1' for project 'Environments'
    And There is a template 'Simple' version '0.1' for project 'Environments2'

  Scenario:
    Given there is no environment 'environment1' in project 'Environments'
    When I create an environment 'environment1' in project 'Environments' with template 'Simple' version '0.1'
    Then I should get response with code '200'
    And there must be an environment 'environment1' in project 'Environments'

  Scenario:
    Given there must be an environment 'environment1' in project 'Environments'
    When I create an environment 'environment1' in project 'Environments' with template 'Simple' version '0.1'
    Then I should get response with code '400'
    And error like 'Environment with the same name already exists in project'
  
  Scenario:
    Given there is no environment 'environment1' in project 'Environments2'
    And there must be an environment 'environment1' in project 'Environments'
    When I create an environment 'environment1' in project 'Environments2' with template 'Simple' version '0.1'
    And there must be an environment 'environment1' in project 'Environments2'

  Scenario Outline:
    When I create an environment '<name>' in project 'Environments' with template 'Simple' version '0.1'
    Then I should get response with code '<code>'
    And there must be an environment '<name>' in project 'Environments'

    Examples:
      | name   | code |
      | name   | 200  |
      | Name   | 200  |
      | nAmE   | 200  |
      | Q1w2e4 | 200  |
