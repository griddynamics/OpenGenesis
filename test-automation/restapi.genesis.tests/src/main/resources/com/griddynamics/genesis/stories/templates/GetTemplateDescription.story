Meta:
@author mlykosova
@theme given

Scenario: Get template description

When I send get template description request for project <projectName> with <templateName> and <templateVersion>
Then Template description should be returned with <createWorflowName> and <variables>


 