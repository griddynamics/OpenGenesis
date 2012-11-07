Then /^User "([^"]*)" with credential "([^"]*)" cannot create project "([^"]*)" managed by "([^"]*)"$/ do |user, password, project_name, manager|
  resource :projects, :username => user, :password => password do |r|
    post = r.post({:name => project_name, :projectManager => manager})
    post.code.should eq(403), post.body
  end
end