Meta:
@author mlykosova
@theme given

Scenario: Successful credential creation

When I send an create credentials request with parameters: <pairName>, <cloudProvider>, <identity>, <credential> in the project <projectName>
Then New credentials will be added



 