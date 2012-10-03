When /^I create a project with the name '(.+)' managed by '(\w+)'$/ do |name, manager|
  @last_response = create_project(name, manager)
end

When /^I create a project with (\d+) characters in the name managed by '(.+)'$/ do |count, manager|
  name = 'a' * count.to_i
  @last_response = create_project(name, manager)
end

Then /^I should get response with code '(\d+)'$/ do |code|
  @last_response.code.should eq(code.to_i), "Expected to get response with code #{code}, but got #{@last_response.code}: #{@last_response.body}"
end

Then /^Project '(.+)' must exist$/ do |name|
  resource(:projects) do |r|
    r.find_by_name(name).should_not be_nil, "Expected to find project #{name}, but got none"
  end
end

Then /^Project '(.+)' should not exist$/ do |name|
  resource(:projects) do |r|
    r.find_by_name(name).should be_nil, "Expected to not find project #{name}, but got one"
  end
end

Then /^I can delete project '(.+)'$/ do |name|
  resource(:projects) do |r|
    project = r.find_by_name(name)
    project.should_not be_nil, "Expected to find project #{name}, but got none"
    delete = r.delete(project["id"])
    delete.code.should eq(200), "Expected to get code 200 when deleting project #{name}, but got #{delete.code}"
  end
end
