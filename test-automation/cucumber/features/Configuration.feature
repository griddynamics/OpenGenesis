Feature: Configuration test

  Background:
    Given Genesis is running
      And I am valid admin user
      And I create a project with the name 'Configurations' managed by 'manager'

  Scenario: Successful configuration creation
    When I'm creating configuration as 'config1' in the project 'Configurations'
    Then I should get response with code '200'

  Scenario: List configurations
    When I'm listing existing configurations in the project 'Configurations'
    Then I should get a list of configurations, including 'config1' for the project 'Configurations'

  Scenario: Duplicate names are not allowed
    Given There is a configuration 'config1' in the project 'Configurations'
    When I'm creating configuration as 'config1' in the project 'Configurations'
    Then I should get response with code '400'

  Scenario: Delete configurations
    When I'm deleting configuration 'config1' in the project 'Configurations'
    Then The configuration 'config1' should be deleted successfully from project 'Configurations'
     And I can delete project 'Configurations'

