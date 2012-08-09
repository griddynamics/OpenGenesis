Meta:
@author mlykosova
@theme environments

Scenario: Precondition of the story - set OpenStack plugin settings

GivenStories: com/griddynamics/genesis/stories/settings/UpdateSettings.story#{0},
              com/griddynamics/genesis/stories/settings/UpdateSettings.story#{1},
              com/griddynamics/genesis/stories/settings/UpdateSettings.story#{2},
              com/griddynamics/genesis/stories/settings/UpdateSettings.story#{3}

Given Running given stories

Examples:
|name                              |newValue |
|genesis.plugin.jclouds.endpoint   |http://172.18.41.1:5000  |
|genesis.plugin.jclouds.identity   |genesis:genesis  |
|genesis.plugin.jclouds.credential |320ba983-f835-42ae-bbbd-0d8d774b4020|
|genesis.plugin.jclouds.provider   |gdnova  | 


Scenario: Environment successful path

GivenStories: com/griddynamics/genesis/stories/projects/PreconditionCreateTestProject.story#{0},
              com/griddynamics/genesis/stories/credentials/AddCredentials.story#{0},
              com/griddynamics/genesis/stories/environments/CreateEnvironment.story#{0},
              com/griddynamics/genesis/stories/environments/GetEnvironmentDetails.story#{0},
              com/griddynamics/genesis/stories/environments/GetListOfEnvironments.story#{0},
              com/griddynamics/genesis/stories/environments/DeleteEnvironment.story#{0},
              com/griddynamics/genesis/stories/projects/DeleteProject.story#{0}

Given Running given stories

Examples:
com/griddynamics/genesis/paramtables/environments/NewEnvironment.table
