Before do
  @groups = Genesis::Resource.new("groups")
end

When /^I'm creating user group "(.*)" described as "(.*)" with mailing list "(.*)" and users "(.*)"$/ do |name, description, mail, users |
  @last_response = @groups.post(group(name, description, mail, users))
end

Then /^Variable error "([^"]*)" :"([^"]*)" should be returned$/ do |field, message|
  @last_response.code.should == 400
  error = JSON.parse(@last_response.body)
  service_errors = error["variablesErrors"]
  service_errors.should have_key(field)
  service_errors[field].should =~ /^#{message}$/
end

Given /^I successfully created user group "(.*)" described as "(.*)" with mailing list "(.*)" and users "(.*)"$/ do |name, description, mail, users |
  resource :groups do |groups|
     r = groups.post(group(name, description, mail, users))
     r.code.should eq(200), "Expected to get code 200, but got #{r.code}"
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

Then /^Compound service error "([^"]*)" should be present in answer$/ do |message|
  @last_response.code.should == 400
  error = JSON.parse(@last_response.body)
  service_errors = error["compoundServiceErrors"]
  service_errors[0].should == message
end

def group(name, description, mail, users)
  {:name => name, :description => description, :mailingList => mail, :users => users.split(",")}
end