Feature: Workflow preconditons
  In order to use preconditions in templates
  I must be sure that preconditions are being checked before workflow description request
  and before workflow request as well

  Background:
    Given Genesis is running
    And I am valid admin user
    And I create a project with the name 'Preconditions' managed by 'manager'
    And There is a template 'Preconditions' version '0.1' for project 'Preconditions'
    And There is a template 'FailedCreate' version '0.1' for project 'Preconditions'

  Scenario: Preconditions on create environment request: failed precondition
    When I create an environment 'fred' in project 'Preconditions' with template 'FailedCreate' version '0.1'
    Then Compound service error with code 400 and error "One must be less than zero" should be present in answer

  Scenario: Preconditions on workflow description request: failed precondition
    Given I create an environment 'fred' in project 'Preconditions' with template 'Preconditions' version '0.1'
    When I request a description for workflow 'wont_run' for template 'Preconditions' version '0.1' in project 'Preconditions'
    Then Compound service error with code 400 and error "One must be less than zero" should be present in answer
    And I can delete environment 'fred' in project 'Preconditions'

  Scenario: Preconditions on workflow description request: satisfied precondition
    Given I create an environment 'barney' in project 'Preconditions' with template 'Preconditions' version '0.1'
    When I request a description for workflow 'should_run' for template 'Preconditions' version '0.1' in project 'Preconditions'
    Then I should get response with code '200'
    And I can delete environment 'barney' in project 'Preconditions'

  Scenario: Preconditions on request workflow: failed precondition
    Given I create an environment 'wilma' in project 'Preconditions' with template 'Preconditions' version '0.1'
    When I'm starting workflow 'wont_run' on environment 'wilma' in project 'Preconditions'
    Then Compound service error with code 400 and error "One must be less than zero" should be present in answer
    And I can delete environment 'wilma' in project 'Preconditions'

  Scenario: Request workflow: can't run create workflow on existing project
    Given I create an environment 'pebbles' in project 'Preconditions' with template 'Preconditions' version '0.1'
    When I'm starting workflow 'create' on environment 'pebbles' in project 'Preconditions'
    Then Service error with code 400 and error 'envName': 'It's not allowed to execute create workflow\['create'\] in existing environment 'pebbles'' should be returned
    And I can delete environment 'pebbles' in project 'Preconditions'

  Scenario: Preconditions on request workflow: satisfied precondition
    Given I create an environment 'betty' in project 'Preconditions' with template 'Preconditions' version '0.1'
    When I'm starting workflow 'should_run' on environment 'betty' in project 'Preconditions'
    Then I should get response with code '200'
    And I can delete environment 'betty' in project 'Preconditions'
    And I can delete project 'Preconditions'

