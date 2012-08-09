Meta:
@author mlykosova
@theme given

Scenario: Get templates list

When I send get template list request for project <projectName>
Then I should get a template list included <templateName>, <templateVersion> 

