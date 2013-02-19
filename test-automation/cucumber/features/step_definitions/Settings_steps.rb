Then /^User "([^"]*)" with credential "([^"]*)" can list system settings$/ do |user, password|
  resource :settings, :username => user, :password => password do |r|
    r.get.code.should eq(200)
  end
end

When /^User "([^"]*)" with credential "([^"]*)" can't create a setting "([^"]*)" set to "([^"]*)"$/ do |user, password, setting, value|
  resource :settings, :username => user, :password => password do |r|
    r.put({setting => value}).code.should eq(403)
  end
end

When /^I list system settings$/ do
  @last_response = resource :settings do |r|
    r.get
  end
end

Then /^I should get a list of settings, including '(.+)'$/ do |prop_name|
  @last_response.code.should eq(200)
  prop = @last_response["items"].find {|c| c["name"] == prop_name}
  prop.should_not be_nil, "Expected to get property with name #{prop_name} but got none"
end

Then /^I should get setting "([^"]*)" with value "([^"]*)"$/ do |setting, value|
  actualValue = genesis_settings_value(setting)
  actualValue.should eq(value), "Expected to get value=#{value} but got #{actualValue}"
end

When /^I create a setting "([^"]*)" set to "([^"]*)"$/ do |setting, value|
  @last_response = resource :settings do |r|
    r.post({setting => value})
  end
end

When /^I update a setting "([^"]*)" set to "([^"]*)"$/ do |setting, value|
  @last_response = resource :settings do |r|
    r.put({setting => value})
  end
end

Given /^setting "(.*?)" is read-only$/ do |setting|
  prop = genesis_setting(setting)
  prop["readOnly"].should eq(true), "Expected to #{setting} is read-only"
end

Then /^I can revert setting "(.*?)"$/ do |setting|
  response = resource :settings do |resource|
    resource.delete(setting)
  end
  response.code.should eq(200)
end
