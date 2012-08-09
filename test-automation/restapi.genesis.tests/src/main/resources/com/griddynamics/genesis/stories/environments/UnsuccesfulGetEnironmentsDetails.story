Meta:
@author mlykosova
@theme environments

Scenario: Successful environment creation


GivenStories: com/griddynamics/genesis/stories/environments/CreateEnvironment.story

Given Running given stories

Scenario: Successful environment deletion

GivenStories: com/griddynamics/genesis/stories/environments/DeleteEnvironment.story

Given Running given stories


