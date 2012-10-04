When /^I'm creating user "([^"]*)" with email "([^"]*)", firstName "([^"]*)" lastName "([^"]*)" jobTitle "([^"]*)" password "([^"]*)" and groups "([^"]*)"$/ do
  |username, email, firstName, lastName, jobTitle, password, groups|
  @last_response = resource :users do |resource|
     resource.post(create_user(username, email, firstName, lastName, jobTitle, password, groups))
  end
end

Then /^I can delete user "([^"]*)"$/ do |username|
  resource :users do |resource|
    resource.delete(username)
    resource.find_by_username(username).should be_nil, "Expected not to get any user for #{username}, but got one."
  end
end
Given /^I successfully created user "([^"]*)" with email "([^"]*)", firstName "([^"]*)" lastName "([^"]*)" jobTitle "([^"]*)" password "([^"]*)" and groups "([^"]*)"$/ do
|username, email, firstName, lastName, jobTitle, password, groups|
  resource :users do |resource|
    response = resource.post(create_user(username, email, firstName, lastName, jobTitle, password, groups))
    response.code.should eq(200), "Expected to get success response, but got #{response.code}: #{response.body}"
    resource.find_by_username(username).should_not be_nil, "Expected to find user #{username}"
  end
end

When /^I'm updating user "([^"]*)" with "([^"]*)" set to "([^"]*)"$/ do |username, f, v|
  @last_response = resource :users do |resource|
    user = resource.find_by_username(username)
    user.should_not be_nil, "Expected to find user #{username}, but got none"
    user.merge! Hash[f => v]
    resource.put username, user
  end
end