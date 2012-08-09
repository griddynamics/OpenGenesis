Meta:
@author mlykosova
@theme templates

Scenario: Template successful path 

GivenStories: com/griddynamics/genesis/stories/projects/PreconditionCreateTestProject.story#{0},
              com/griddynamics/genesis/stories/templates/GetTemplatesList.story#{0},
              com/griddynamics/genesis/stories/templates/GetTemplateDescription.story#{0},
              com/griddynamics/genesis/stories/templates/GetTemplateContent.story#{0},
              com/griddynamics/genesis/stories/projects/DeleteProject.story#{0}

Given Running given stories

Examples:
|projectName   | description          | manager  | templateName    | templateVersion | createWorflowName | variables |
|TemplatesTest | Created by autotest  | Manager  | NovaProvision   | 0.1             | create            | null      |

