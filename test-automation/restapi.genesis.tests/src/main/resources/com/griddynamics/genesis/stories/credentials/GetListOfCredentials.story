Meta:
@author mlykosova
@theme given

Scenario: Get existing credentials

When I send get existing credentials request for <projectName>
Then I should get a list of credentials, included <pairName>, <cloudProvider>, <identity>, <credential>

