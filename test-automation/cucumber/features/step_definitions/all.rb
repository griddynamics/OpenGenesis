Given /^Genesis is running$/ do
  resource "" do |r|
    response = r.get
    response.code.should eq(200), "Expected response code 200, but really got #{response.code}"
  end
end

Given /^I am valid admin user$/ do
  resource "/" do |r|
    JSON.parse(r.get.body)["administrator"].should eq(true), "It should run as admin user"
  end
end

Given /^There is a project '(.+)'$/ do |project_name|
  wait_for(20) do
    resource :projects do |projects|
      project = projects.find_by_name(project_name)
      ! project.nil?
    end
  end
end

Then /^Variable error with code (\d+) and error "([^"]*)": "([^"]*)" should be returned$/ do |code, field, message|
  error = errors(@last_response, code)
#  error.variablesErrors.respond_to?(field).should eq(true)
  actual = error.variablesErrors.send(field)
  actual.should eq(message), "Expected to have message #{message}, but actually it's not here: #{actual}"
end

Then /^Service error with code (\d+) and error '(.+)': '(.+)' should be returned$/ do |code, field, message|
  error = errors(@last_response, code)
  error.serviceErrors.respond_to?(field).should eq(true)
  actual = error.serviceErrors.send(field)
  actual.should match(message)
end

Then /^Compound service error with code (\d+) and error "([^"]*)" should be present in answer$/ do |code, message|
  errors(@last_response, code) do |error|
    error.compoundServiceErrors.should have_at_least(1).item, "Compound service errors must be present"
    messages = error.compoundServiceErrors.join(", ")
    error.compoundServiceErrors.should include(message), "Expected to have message #{message}, but actually it's not here: #{messages}"
  end
end
Then /^User "([^"]*)" must be able to authenticate itself with password "([^"]*)"$/ do |user, password|
  resource "/", :username => user, :password => password do |resource|
    r = resource.get
    r.code.should eq(200)
  end
end