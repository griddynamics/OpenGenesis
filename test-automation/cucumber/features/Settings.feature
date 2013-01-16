Feature: System and plugins settings create, read, update, delete

  Background:
    Given Genesis is running
    And I am valid admin user

  Scenario: Read system settings
    When I list system settings
    Then I should get a list of settings, including 'genesis.system.bind.port'

  Scenario: Create new setting
    When I create a setting "my.new.property" set to "my_value"
    Then I should get response with code '405'

  Scenario: Update non-existent setting
    When I update a setting "my.new.property" set to "my_value"
    Then I should get response with code '404'

  Scenario: Update existing read-only setting
    Given setting "genesis.system.bind.port" is read-only
    When I update a setting "genesis.system.bind.port" set to "!@#$%"
    Then Compound service error with code 400 and error "Could not modify read-only property: genesis.system.bind.port" should be present in answer

  Scenario Outline: Update existing settings to invalid values
    When I update a setting "<propName>" set to "<value>"
    Then settings error with code 400 and error "<propName>": "<message>" should be returned
    Examples:
    | propName                                      | value				    | message |
    | genesis.system.bind.host                      | !@#$^%  	            | Invalid format. Hostname must be a host name according to RFC 952 or valid IP v4 address|
    | genesis.system.shutdown.timeout.sec           | -1 	                | Must be integer value >= 0|
    | genesis.system.beat.period.ms                 | 12345ValueVeryVeryLongValueVeryVeryLongValueVeryVeryLongValueVeryVeryLongValueVeryVeryLongValueVeryVeryLongValueVeryVeryLongValue | Length must be from 0 to 128|
    | genesis.system.flow.executor.sync.threads.max | 0                      | Must be positive integer value |
    | genesis.plugin.jclouds.endpoint               | xxx:                   | Endpoint must be a valid URL |
    | genesis.plugin.notification.sender.email      | _687678                | Not a well-formed email address |
    | genesis.plugin.notification.sender.email      |                        | E-mail address is required |

  Scenario: Update existent setting to valid value and reset back to initial value
    When I update a setting "genesis.system.flow.timeout.ms" set to "5000"
    Then I should get response with code '200'
    And I should get setting "genesis.system.flow.timeout.ms" with value "5000"
    And I can revert setting "genesis.system.flow.timeout.ms"
    And I should get setting "genesis.system.flow.timeout.ms" with value "3600000"
