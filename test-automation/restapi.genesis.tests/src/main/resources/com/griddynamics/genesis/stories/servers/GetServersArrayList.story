Meta:
@author mlykosova
@theme given

Scenario: Get servers array list

When I send get servers array list request for <projectName> 
Then I should get servers array list included <arrayName>, <arrayDescription> 

