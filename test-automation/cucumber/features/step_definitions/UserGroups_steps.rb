
When /^I'm creating user group "(.*)" described as "(.*)" with mailing list "(.*)" and users "(.*)"$/ do |name, description, mail, users |
  @last_response = resource :groups do |r|
    r.post(group(name, description, mail, users))
  end
end

Given /^I successfully created user group "(.*)" described as "(.*)" with mailing list "(.*)" and users "(.*)"$/ do |name, description, mail, users |
  resource :groups do |groups|
     r = groups.post(group(name, description, mail, users))
     r.code.should eq(200), "Expected to get code 200, but got #{r.code}: #{r.body}"
  end
end

Then /^I can delete group "(.+)"$/ do |groupName|
  resource :groups do |groups|
     g = groups.find_by_name groupName
     g.should_not be_nil, "Expected to get group #{groupName}, but none found"
     r = groups.delete(g["id"])
     r.code.should eq(200), "Expected to get code 200, but got #{r.code}"
  end
end



def group(name, description, mail, users)
  {:name => name, :description => description, :mailingList => mail, :users => users.split(",")}
end
Given /^There are no user group "([^"]*)"$/ do |name|
  resource :groups do |groups|
    groups.find_by_name(name).should be_nil, "Expected to not find group #{name}, but got one"
  end
end