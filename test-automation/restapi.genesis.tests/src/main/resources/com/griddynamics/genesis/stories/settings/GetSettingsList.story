Meta:
@author mlykosova
@theme given

Scenario: Get list of settings

When I send get list of settings request with <prefix> 
Then I should get a list of settings included <name>, <defValue>, <readOnly>, <description> 

