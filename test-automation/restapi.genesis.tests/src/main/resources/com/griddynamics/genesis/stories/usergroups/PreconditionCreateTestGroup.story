Meta:
@author ybaturina
@theme given

Scenario: Precondition Create Groups

When I send request to create user group with name <groupName> description <description> mailing list <mail> and users <usersList>
Then I expect that user group was created successfully