Given /^There is a template '(.+)' version '(.+)' for project '(.+)'$/ do |template, version, project|
  id = @client.project_id(project)
  response = @client.get_template(id, template, version)
  response.code.should == 200
end

Given /^there is no environment '(.+)' in project '(.+)'$/ do |env, project|
  id = @client.project_id(project)
  env_id = @client.env_id(id, env)
  env_id.should be_nil
end

When /^I create an environment '(.+)' in project '(.+)' with template '(.+)' version '(.+)'$/ do |env, project, template, version|
  id = @client.project_id(project)
  @last_response = @client.create_env(id, env, template, version)
end

Then /^error like '(.+)'$/ do |message|
  error = JSON.parse(@last_response.body)
  service_errors = error["serviceErrors"]
  service_errors.should have_key("envName")
  service_errors["envName"].should =~ /^#{message}/
end

Then /^there must be an environment '(.+)' in project '(.+)'$/ do |env, project|
  id = @client.project_id(project)
  env_id = @client.env_id(id, env)
  env_id.should_not be_nil
end
