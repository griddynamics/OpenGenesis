Meta:
@author mlykosova
@theme given

Scenario: Delete server array

When I send delete server array request for array <arrayName>, <arrayDescription> in project <projectName>   
Then The server array should be deleted successfully 

