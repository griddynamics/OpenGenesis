Then /^User "([^"]*)" with credential "([^"]*)" can list system settings$/ do |user, password|
  resource :settings, :username => user, :password => password do |r|
    r.get.code.should eq(200)
  end
end

When /^User "([^"]*)" with credential "([^"]*)" can't create a setting "([^"]*)" set to "([^"]*)"$/ do |user, password, setting, value|
  resource :settings, :username => user, :password => password do |r|
    r.put({setting:value}).code.should eq(403)
  end
end
