Meta:
@author ybaturina
@theme given

Scenario: Create Test Project

When I send request to create project with name <projectName> description <description> and manager <manager>
Then I expect that project was created successfully