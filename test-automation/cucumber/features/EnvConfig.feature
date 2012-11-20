Feature: Environment configuration properties
  In order to use environment configuration properties in templates
  I must be sure that environment configuration properties can be used in create workflow preconditions

  Background:
    Given Genesis is running
    And I am valid admin user
    And I create a project with the name 'EnvConfig' managed by 'manager'
    And There is a template 'EnvConfig' version '0.1' for project 'EnvConfig'

  Scenario: Create first instance in default empty environment: success
    When I create an environment 'inst1' in project 'EnvConfig' with template 'EnvConfig' version '0.1'
    Then I should get response with code '200'

  Scenario: Create second instance in default environment: failed precondition
    When I create an environment 'inst2' in project 'EnvConfig' with template 'EnvConfig' version '0.1'
    Then Compound service error with code 400 and error "Environment must contain no active instances" should be present in answer
    And I can delete environment 'inst1' in project 'EnvConfig'
  
  Scenario: Create instance in default environment having destroyed instance: success
    When I create an environment 'inst2' in project 'EnvConfig' with template 'EnvConfig' version '0.1'
    Then I should get response with code '200'
    And I can delete environment 'inst2' in project 'EnvConfig'
    And I can delete project 'EnvConfig'

