Meta:
@author mlykosova
@theme given

Scenario: Successful credentials deletion

When I send a delete credentials request for credentials <pairName>, <cloudProvider>, <identity> in project <projectName> 
Then The credentials will be deleted successfully


