Meta:
@author mlykosova
@theme given

Scenario: Get existing environments

When I send get existing environments request with <projectName>
Then I should get environments list
 
