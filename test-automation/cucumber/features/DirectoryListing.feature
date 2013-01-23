Feature: In order to better navigate on REST api I need to see top-level links from REST calls

  Background:
    Given Genesis is running
    And I am valid admin user

  Scenario Outline: System admin should see links to projects and to settings
    When I request url "/"
    Then I get links as "<url>", "<type>", "<rel>", "<methods>"
    Examples:
      |url      |type                                 |rel       |methods|
      |/rest/projects|application/vnd.griddynamics.genesis.Project+json|collection|get   |
      |/rest/settings|application/vnd.griddynamics.genesis.Link+json   |collection|get   |

  Scenario Outline: System readonly should see links to projects and to settings
    When I successfully created user with parameters
      |username |rouser                        |
      |password |pass                       |
      |firstname|john                       |
      |lastname |Doe                        |
      |email    |john@example.com           |
      |groups   |                           |
      |roles    |ROLE_GENESIS_READONLY|
    And User "rouser" with password "pass" requests url "/"
    Then He get links as "<url>", "<type>", "<rel>", "<methods>"
    And I can delete user "rouser"
  Examples:
    |url      |type                                 |rel       |methods|
    |/rest/projects|application/vnd.griddynamics.genesis.Project+json|collection|get   |
    |/rest/settings|application/vnd.griddynamics.genesis.Link+json   |collection|get   |

  Scenario Outline: User without special privileges should see link to projects
    When I successfully created user with parameters
      |username |linkuser                        |
      |password |password                       |
      |firstname|john                       |
      |lastname |Doe                        |
      |email    |linkuser@example.com           |
      |groups   |                           |
      |roles    |ROLE_GENESIS_USER|
    And User "linkuser" with password "password" requests url "/"
    Then He get links as "<url>", "<type>", "<rel>", "<methods>"
    And I can delete user "linkuser"
  Examples:
    |url      |type                                 |rel       |methods|
    |/rest/projects|application/vnd.griddynamics.genesis.Project+json|collection|get   |

  Scenario Outline: User without special privileges must not see link to settings
    When I successfully created user with parameters
      |username |linkuser                        |
      |password |password                       |
      |firstname|john                       |
      |lastname |Doe                        |
      |email    |linkuser@example.com           |
      |groups   |                           |
      |roles    |ROLE_GENESIS_USER|
    And User "linkuser" with password "password" requests url "/"
    Then He get no links as "<url>"
    And I can delete user "linkuser"
  Examples:
    |url           |
    |/rest/settings|