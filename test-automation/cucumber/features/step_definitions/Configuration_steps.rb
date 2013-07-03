When /^I'm creating configuration as '(.+)' in the project '(.+)'$/ do |name, project|
  @last_response = configuration_resource project do |r, id|
    r.post({:projectId => id, :name => name, :items => {:blabla => "blabla"}})
  end
end


When /^I'm listing existing configurations in the project '(.+)'$/ do |project|
  @last_response = configuration_resource project do |r, id|
    r.get
  end
end

Then /^I should get a list of configurations, including '(.+)' for the project '(.+)'$/ do |name, project|
  conf = @last_response["items"].find {|c| c["name"] == name}
  conf.should_not be_nil, "Expected to get config with name #{name} but got none"
end


When /^I'm deleting configuration '(.+)' in the project '(.+)'$/ do |name, project|
  config = find_config_in_project(name, project)
  config.should_not be_nil, "Expected to get config with name #{name} but got none"

  configuration_resource project do |r, id|
    r.delete(config["id"])
  end
end

Given /^There is a configuration '(.+)' in the project '(.+)'$/ do |name, project|
  config = find_config_in_project(name, project)
  config.should_not be_nil, "Expected to get config with name #{name} but got none"
end

Then /^The configuration '(.+)' should be deleted successfully from project '(.+)'$/ do |name, project|
  config = find_config_in_project(name, project)
  config.should be_nil, "Configuration with #{name} should not be found in project #{project}. But it was"
end

Given /^Environment security is enabled$/ do
  config = genesis_settings_value("genesis.system.security.environment.restriction.enabled")
  config.should eq("true")
end

Given /(?:If\s+)?[u|U]ser '(.+)' (?:is|becomes) a project user of a project '(.+)'/ do |username, project|
  resource :users do |r|
    user = r.find_by_username(username)
    if user.nil?
      response = r.post(create_user(username, "blabla@blabla.com", "blabla", "blabla", "blabla", username, ""))
      response.code.should eq(200), "Expected to get success response, but got #{response.code}: #{response.body}"
    end
  end

  resource "/roles/ROLE_GENESIS_USER" do |r|
    assig = JSON.parse(r.get.body)
    user = assig["users"].find {| u | u == username }
    if user.nil?
      resp = r.put({:name => "ROLE_GENESIS_USER", :groups => [], :users => [username]})
      resp.code.should eq(200), "Failed to assign user to genesis user role. Response: #{resp}"
    end
  end

  project_resource project, "/roles/ROLE_GENESIS_PROJECT_USER" do |r|
    response = JSON.parse(r.get.body)
    user = response["users"].find {|u| u == username}
    if user.nil?
      resp = r.put({:users => [username], :groups => []})
      resp.code.should eq(200), "Failed to assign user to genesis project user role. Response: #{resp}"
    end
  end
end

When /^I grant permissions to configuration '(.+)' to user '(.+)' in project '(.+)'$/ do |name, username, project|
  config = find_config_in_project(name, project)
  update_config_access(project, config["id"], [username])
end

When /^I remove permissions to configuration '(.+)' from user '(.+)' in project '(.+)'$/ do |name, username, project|
  config = find_config_in_project(name, project)
  update_config_access(project, config["id"], [])
end

When /^I create an environment simple '(.+)' in project '(.+)' with configuration '(.+)'$/ do |env_name, project, config_name|
  conf = find_config_in_project(config_name, project)
  @last_response = environments_resource project do |resource, id|
    resource.post(create_environment(env_name, "Simple", "0.1").merge({:configId => conf["id"]}))
  end

  @last_response.code.should eq(200), "Failed to create environment. Response: #{@last_response}"
end

Then /^User '(.+)' should have access to environment '(.+)' in project '(.+)'$/ do |username, env_name, project|
  result = get_env_as_user(env_name, project, username)
  result.code.should eq(200), "User should be able to read environment info, but got following error instead: #{result}"
end


Then /^User '(.+)' should see environment '(.+)' in environments list in project '(.+)'$/ do |username, env_name, project|
  project_id = project_id(project)
  r = resource("projects/#{project_id}/envs", :username => username, :password => username)
  result = r.get
  result.code.should eq(200), "User should be able to read environment info, but got following error instead: #{result}"
  env_from_list = r.find_by_name(env_name)
  env_from_list.should_not be_nil, "Failed to find env #{env_name} in envs list for user #{username}"
end

When /^User '(.+)' doesn't have permissions to config '(.+)' in the project '(.+)'$/ do |username, config_name, project|
  config = find_config_in_project(config_name, project)
  project_resource project, "configs/#{config['id']}/access" do |resource|
    user = JSON.parse(resource.get.body)["users"].find{|u| u == username }
    user.should be_nil, "User #{username} should not has access to config #{config_name}, but has"
  end
end

Then /^User '(.+)' should not have access to environment '(.+)' in the project '(.+)'$/ do |username, env_name, project|
  result = get_env_as_user(env_name, project, username)
  result.code.should eq(403), "User #{username} should not be able to read environment info, but got following error instead: #{result}"
end


Then /^User '(.+)' should not see environment '(.+)' in environments list in the project '(.+)'$/ do |username, env_name, project|
  project_id = project_id(project)
  r = resource("projects/#{project_id}/envs", :username => username, :password => username)
  r.get.code.should eq(200), "Failed to get envs for project #{project_id} via user account #{username}"
  env = r.find_by_name(env_name)
  env.should be_nil, "User can see account he doesn't have access to"
end

Then /^User '(.+)' can't see configuration '(.+)' in the project '(.+)' as a workflow parameter$/ do |username, configuration, project|
  configurations = find_var_for_workflow(project, "Simple", "0.1", "create", "$envConfig", username, username)
  puts configurations
  configurations["values"].key(configuration).should be_nil, "User #{username} should not see config #{configuration}"
end

When /^User '(.+)' creates simple environment '(.+)' in the project '(.+)' with configuration '(.+)'$/ do |username, env_name, project, configuration|
  conf = find_config_in_project(configuration, project)
  project_id = project_id(project)

  r = resource("projects/#{project_id}/envs", :username => username, :password => username)
  @last_response = r.post(create_environment(env_name, "Simple", "0.1").merge({:configId => conf["id"]}))
end

Then /^User gets (.+) http response code$/ do |code|
  @last_response.code.should eq(code.to_i), "Unexpected response: #{@last_response}"
end

Then /^User '(.+)' should see '(.+)' in configs list of project '(.+)'$/ do |username, config_name, project|
  config = find_config(config_name, project, username)
  config.should_not be_nil, "Failed to find config #{config_name} via user #{username} account"
end

Then /^User '(.+)' should not see '(.+)' in configs list of project '(.+)'$/ do |username, config_name, project|
  config = find_config(config_name, project, username)
  config.should be_nil, "Config #{config_name} should not be seen via user #{username} account"
end

