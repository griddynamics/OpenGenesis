Meta:
@author mlykosova
@theme given

Scenario: Update server array

When I send update servers array request with parameters: <newArrayName>, <newArrayDescription> for project <projectName> 
Then Server array should be updated

