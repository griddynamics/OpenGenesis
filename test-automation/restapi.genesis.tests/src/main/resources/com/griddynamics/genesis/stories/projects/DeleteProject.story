Meta:
@author ybaturina
@theme given

Scenario: Successful project deletion

When I send request to delete project with name <projectName>
Then I expect that project was deleted successfully


 