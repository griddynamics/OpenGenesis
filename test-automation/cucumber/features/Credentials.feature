Feature: Credentials test

  Background:
    Given Genesis is running
    And I am valid admin user
    And I create a project with the name 'Credentials' managed by 'manager'

  Scenario: Successful credential creation
    When I'm creating credential as 'pairname', 'provider', 'identity', 'credential' in the project 'Credentials'
    Then I should get response with code '200'

  Scenario: Credential is unique by pair name and provider across the same project
    When I'm creating credential as 'pairname', 'provider', 'root', 'password' in the project 'Credentials'
    Then I should get response with code '400'

  Scenario: List credentials
    When I'm listing existing credentials in the project 'Credentials'
    Then I should get a list of credentials, including 'pairname', 'provider' for the project 'Credentials'

  Scenario: Delete credentials
    When I'm deleting credentials 'pairname', 'provider' in the project 'Credentials'
    Then The credentials should be deleted successfully from project 'Credentials'
    And I can delete project 'Credentials'