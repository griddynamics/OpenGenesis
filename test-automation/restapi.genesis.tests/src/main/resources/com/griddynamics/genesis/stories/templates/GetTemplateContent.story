Meta:
@author mlykosova
@theme given

Scenario: Get template content

When I send get template content request for project <projectName> with <templateName> and <templateVersion>
Then Template content should be returned 
