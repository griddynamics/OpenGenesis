  Feature: Configuration security test

  Background:
    Given Genesis is running
    And Environment security is enabled
    And I am valid admin user
    And I create a project with the name 'Configurations' managed by 'manager'
    And User 'test' is a project user of a project 'Configurations'

  Scenario: User gets access to environment if he has access to configuration
    When  I'm creating configuration as 'config1' in the project 'Configurations'
      And I grant permissions to configuration 'config1' to user 'test' in project 'Configurations'
      And I create an environment simple 'environment1' in project 'Configurations' with configuration 'config1'
    Then  User 'test' should have access to environment 'environment1' in project 'Configurations'
      And User 'test' should see environment 'environment1' in environments list in project 'Configurations'
      And I can remove environment 'environment1' in project 'Configurations'
      And I can delete project 'Configurations'

  Scenario: User doesn't have access to environment if he has no access to configuration
    When  I'm creating configuration as 'config2' in the project 'Configurations'
      And User 'test' doesn't have permissions to config 'config2' in the project 'Configurations'
      And  I create an environment simple 'aaa' in project 'Configurations' with configuration 'config2'
    Then  User 'test' should not have access to environment 'aaa' in the project 'Configurations'
      And User 'test' should not see environment 'aaa' in environments list in the project 'Configurations'
      And I can remove environment 'aaa' in project 'Configurations'
      And I can delete project 'Configurations'

  Scenario: Configs list should be restricted to permited configs
    When I'm creating configuration as 'config3' in the project 'Configurations'
     And I'm creating configuration as 'config4' in the project 'Configurations'
     And I grant permissions to configuration 'config3' to user 'test' in project 'Configurations'
    Then User 'test' should see 'config3' in configs list of project 'Configurations'
     But User 'test' should not see 'config4' in configs list of project 'Configurations'
    And I can delete project 'Configurations'

  Scenario: User can't create environment with configuration he doesn't have access to
    When  I'm creating configuration as 'config3' in the project 'Configurations'
      And User 'test' doesn't have permissions to config 'config3' in the project 'Configurations'
      And User 'test' creates simple environment 'ccc' in the project 'Configurations' with configuration 'config3'
    Then  User gets 403 http response code
      And I can delete project 'Configurations'

  Scenario: User can create environment if he has access to configuration
    When  I'm creating configuration as 'config4' in the project 'Configurations'
     And  I grant permissions to configuration 'config4' to user 'test' in project 'Configurations'
     And  User 'test' creates simple environment 'zzz' in the project 'Configurations' with configuration 'config4'
    Then User gets 200 http response code
     And there must be an environment 'zzz' in project 'Configurations'
     And I can remove environment 'zzz' in project 'Configurations'
     And I can delete project 'Configurations'

  Scenario: Changing access restrictions to configuration affects existing environments
    When  I'm creating configuration as 'config6' in the project 'Configurations'
      And User 'test' doesn't have permissions to config 'config6' in the project 'Configurations'
      And  I create an environment simple 'rrr' in project 'Configurations' with configuration 'config6'
    Then  User 'test' should not have access to environment 'rrr' in the project 'Configurations'
    When  I grant permissions to configuration 'config6' to user 'test' in project 'Configurations'
    Then  User 'test' should have access to environment 'rrr' in project 'Configurations'
    When  I remove permissions to configuration 'config6' from user 'test' in project 'Configurations'
    Then  User 'test' should not have access to environment 'rrr' in the project 'Configurations'
      And I can remove environment 'rrr' in project 'Configurations'
      And I can delete project 'Configurations'
