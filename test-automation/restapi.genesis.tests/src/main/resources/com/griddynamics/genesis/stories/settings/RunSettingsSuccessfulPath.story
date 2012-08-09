Meta:
@author mlykosova
@theme settings

Scenario: Settings successful path 

GivenStories: com/griddynamics/genesis/stories/settings/ResetSettings.story#{0}, 
              com/griddynamics/genesis/stories/settings/GetSettingsList.story#{0},
              com/griddynamics/genesis/stories/settings/UpdateSettings.story#{0},
              com/griddynamics/genesis/stories/settings/GetSettingsList.story#{1}

Given Running given stories

Examples:
|prefix |name                            |defValue  |readOnly | description                      |newValue |
|null   |genesis.system.flow.timeout.ms  |3600000   |false    | Workflow execution timeout in ms.|4200000  |
|null   |genesis.system.flow.timeout.ms  |4200000   |false    | Workflow execution timeout in ms.|3600000  |

GivenStories: com/griddynamics/genesis/stories/settings/ResetSettings.story#{0}
              com/griddynamics/genesis/stories/settings/GetSettingsList.story#{0}
              
Given Running given stories

Examples:
|prefix |name                            |defValue  |readOnly | description                      |newValue |
|null   |genesis.system.flow.timeout.ms  |3600000   |false    | Workflow execution timeout in ms.|4200000  |




Scenario: Get settings list

GivenStories: com/griddynamics/genesis/stories/settings/ResetSettings.story, 
              com/griddynamics/genesis/stories/settings/GetSettingsList.story#{0},
              com/griddynamics/genesis/stories/settings/GetSettingsList.story#{1},
              com/griddynamics/genesis/stories/settings/GetSettingsList.story#{2}


Given Running given stories

Examples:
|prefix                     |name                                   |defValue      |readOnly | description                                                         |
|null                       |genesis.system.flow.timeout.ms         |3600000       |false    | Workflow execution timeout in ms.                                   |
|genesis.plugin.jclouds     |genesis.plugin.jclouds.nodename.prefix |GN            |false    | null                                                                |
|genesis.system             |genesis.system.jdbc.driver             |org.h2.Driver |true     | JDBC driver class name to use for access to the application database|
