Meta:
@author mlykosova
@theme given

Scenario: Successful environment deletion

When I send a delete environment request with <projectName>, <envName>
Then Environment will be deleted

