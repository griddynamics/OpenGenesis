Meta:
@author mlykosova
@theme given

Scenario: Successful environment creation

When I send an create environment request with <envName>, <templateName>, <templateVersion>, <var1> in the project <projectName>
Then New environment will be created


