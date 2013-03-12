Feature: Databag templates

  Background:
    Given Genesis is running
    And I am valid admin user

  Scenario:
    When I request list of template scopes
    Then I should get response with code '200'
    And  Response should contain link to scope
      |system|collection|
      |project|collection|
      |environments|collection|

  Scenario:
    When I request a list of templates for invalid scope 'foo'
    Then I should get response with code '404'

  Scenario:
    When I request a list of templates for valid scope 'system'
    Then I should get response with code '200'

  Scenario:
    When I request a template with valid scope 'system' and valid id '1-system'
    Then I should get response with code '200'

  Scenario:
    When I request a template with invalid scope 'foo' and valid id '1-system'
    Then I should get response with code '404'

  Scenario:
    When I request a template with valid scope 'system' and invalid id '4-system'
    Then I should get response with code '404'
    
  Scenario:
    When I create databag "test" based with template "1-system" and no values
    Then Service error with code 400 and error 'required': 'Required key required not found in databag' should be returned