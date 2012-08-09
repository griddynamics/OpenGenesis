Meta:
@author mlykosova
@theme given

Scenario: Create servers array

When I send create servers array request with parameters: <arrayName>, <arrayDescription> for project <projectName> 
Then New server array should be created
