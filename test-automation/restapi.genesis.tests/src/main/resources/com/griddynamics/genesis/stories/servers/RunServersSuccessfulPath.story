Meta:
@author mlykosova
@theme servers
@skip

Scenario: Successful path

GivenStories: com/griddynamics/genesis/stories/projects/PreconditionCreateTestProject.story#{0},
              com/griddynamics/genesis/stories/servers/CreateServerArray.story#{0},
              com/griddynamics/genesis/stories/servers/GetServersArrayList.story#{0},
              com/griddynamics/genesis/stories/servers/UpdateServerArray.story#{0},
              com/griddynamics/genesis/stories/servers/DeleteServerArray.story#{0},
              com/griddynamics/genesis/stories/projects/DeleteProject.story#{0}

Given Running given stories


Examples:
|projectName      | description         | manager  | arrayName | arrayDescription | newArrayName  | newArrayDescription |
|ServerArraysTest | Created by autotest | Manager  | array     | empty            | newArray      | newDescription      |

