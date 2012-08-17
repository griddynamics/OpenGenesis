Feature: Unified name rules for projects and environment

  Background:
    Given Genesis is running
    And I am valid admin user

  Scenario: Valid project name
    Given Project 'project' should not exist
    When I create a project with the name 'project' managed by 'manager'
    Then I should get response with code '200'
    And Project 'project' must exist
    And I can delete project 'project'

  Scenario: Trimming leading spaces from project name
    Given Project 'leading' should not exist
    When I create a project with the name '   leading' managed by 'manager'
    Then I should get response with code '200'
    And Project 'leading' must exist
    And I can delete project 'leading'

  Scenario: Trimming trailing spaces from project name
    Given Project 'trailing' should not exist
    When I create a project with the name 'trailing   ' managed by 'manager'
    Then I should get response with code '200'
    And Project 'trailing' must exist
    And I can delete project 'trailing'

  Scenario: Trimming all spaces from project name
    Given Project 'spaces' should not exist
    When I create a project with the name '  spaces  ' managed by 'manager'
    Then I should get response with code '200'
    And Project 'spaces' must exist
    And I can delete project 'spaces'

  Scenario: Invalid symbols in project name
    When I create a project with the name '&$%' managed by 'manager'
    Then I should get response with code '400'
    And Project '&$%' should not exist

  Scenario: Empty project name
    When I create a project with 0 characters in the name managed by 'manager'
    Then I should get response with code '400'

  Scenario: Too long project name
    When I create a project with 65 characters in the name managed by 'manager'
    Then I should get response with code '400'
