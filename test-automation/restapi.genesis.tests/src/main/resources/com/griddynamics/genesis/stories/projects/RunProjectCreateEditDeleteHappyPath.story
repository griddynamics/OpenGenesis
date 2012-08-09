Meta:
@author ybaturina
@theme projects 

Scenario: Create Project

When I send request to create project with name <projectName> description <description> and manager <manager>
Then I expect that project was created successfully

When I send get projects request 
Then I expect to see project with name <projectName> description <description> and manager <manager> with result <result>

Examples:
com/griddynamics/genesis/paramtables/projects/NewProject.table

Scenario: Edit Project

When I send request to edit project with name <oldName> and specify new name <projectName> description <description> and manager <manager>
Then I expect that project was changed successfully

When I send get projects request 
Then I expect to see project with name <projectName> description <description> and manager <manager> with result <result> 

Examples:
com/griddynamics/genesis/paramtables/projects/ModifiedProject.table

Scenario: Delete project

When I send request to delete project with name <projectName>
Then I expect that project was deleted successfully

When I send get projects request 
Then I expect to see project with name <projectName> description <description> and manager <manager> with result false

Examples:
| name				| projectName |	description		|manager	|oldName |
| TEST 1			|new	 	  |new description	|new manager	|old	 	  |



 