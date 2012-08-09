Meta:
@author ybaturina
@theme given

Scenario: Precondition Create Users

When I send request to create user with userName <userName> email <email> firstName <firstName> lastName <lastName> jobTitle <title> password <password> and groups <groups>
Then I expect that user was created successfully