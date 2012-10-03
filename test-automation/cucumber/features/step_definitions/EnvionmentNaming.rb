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
  @last_response = environments_resource project do |resource, id|
     resource.post(create_environment(env, template, version))
  end
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
     resource.put(env["id"], {:environment => {:name => new_name}})
  end
end