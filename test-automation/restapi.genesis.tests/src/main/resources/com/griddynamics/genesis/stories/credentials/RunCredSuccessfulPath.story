Meta:
@author mlykosova
@theme credentials

Scenario: Successful path

GivenStories: com/griddynamics/genesis/stories/projects/PreconditionCreateTestProject.story#{1},
              com/griddynamics/genesis/stories/credentials/AddCredentials.story#{1},
              com/griddynamics/genesis/stories/credentials/GetListOfCredentials.story#{1},
              com/griddynamics/genesis/stories/credentials/DeleteCredentials.story#{1},
              com/griddynamics/genesis/stories/projects/DeleteProject.story#{0}

Given Running given stories

Examples:
com/griddynamics/genesis/paramtables/credentials/NewCredentials.table

