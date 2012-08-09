Meta:
@author ybaturina
@theme projects

Scenario: View list of projects

GivenStories: com/griddynamics/genesis/stories/projects/RunProjectCreateEditDeleteHappyPath.story

When I send get projects request 
Then I expect to see <quantity> projects in list


Examples:
| name				|quantity	|
| TEST 1			|0			|




