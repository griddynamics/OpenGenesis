include Genesis
Given /^There is a template '(.+)' version '(.+)' for project '(.+)'$/ do |template, version, project|
  templates_resource project do |resource, id|
    t = resource.find_by_name_and_version(template, version)
    t.should_not be_nil, "Expected to have template #{template} v. #{version} in project #{project}, but got none"
  end
end

Given /^there is no environment '(.+)' in project '(.+)'$/ do |env, project|
  environments_resource project do |resource, id|
    env = resource.find_by_name(env)
    env.should be_nil, "Expected that environment #{env} is not exists, but it's there"
  end
end

When /^I create an environment '(.+)' in project '(.+)' with template '(.+)' version '(.+)'$/ do |env, project, template, version|
  @last_response = nil
  puts @last_response
  @last_response = environments_resource project do |resource, id|
     resource.post(create_environment(env, template, version))
  end
  puts @last_response
end



Then /^there must be an environment '(.+)' in project '(.+)'$/ do |env, project|
  environments_resource project do |resource, id|
    env = resource.find_by_name(env)
    env.should_not be_nil, "Expected that environment #{env} exists, but it's not found"
  end
end

When /^I'm renaming environment '(.+)' to '(.+)' in project '(.+)'$/ do |old_name, new_name, project|
  @last_response = environments_resource project do |resource, id|
     env = resource.find_by_name(old_name)
     env.should_not be_nil, "Expected that environment #{env} exists, but it's not found"
     wait_for_env_status(env['name'], project, 'Ready')
     resource.put(env['id'], {:environment => {:name => new_name}})
  end
end



When /^I can (?:remove|delete) environment '(.*)' in project '(.+)'$/ do |env_name, project|
  environments_resource project do |resource, id|
    env = Hashed.new(resource.find_by_name(env_name))
    wait_for(20) do
      Hashed.new(resource.find_by_name(env.name)).status != 'Busy'
    end
    resource.delete(env.id)
    wait_for_env_status(env.name, project, 'Destroyed')
  end
end