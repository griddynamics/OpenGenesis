When /^I'm listing predefined roles$/ do
  @last_response = resource :roles do |r|
     r.get
  end
end

Then /^Role "(.+)" must be in response$/ do  |role|
  parsed = JSON.parse(@last_response.body)
  parsed.class.should eq(Hash)
  found = parsed['items'].find{|x| x["name"] == role}
  found.should_not be_nil, "Role #{role} must be present"
end

When /^User "([^"]*)" should have role "([^"]*)"$/ do |user, role|
  nested_resource :users, user, :roles do |resource|
     resp = resource.get
     JSON.parse(resp.body).should include(role)
  end
end

When /^User ["|'](.+)["|'] with credential ["|'](.+)["|'] can read, but not update or delete project ["|'](.+)["|']$/ do |username, password, project_name|
  resource :projects, :username => username, :password => password do |resource|
    project = resource.find_by_name project_name
    project.should_not be_nil, "Expected to have a project for name #{project_name}, but got none"
    details = resource.get project["id"]
    details.code.should eq(200)
    JSON.parse(details.body)["id"].should eq(project["id"])
    response = resource.put project["id"], {:name => project_name, :manager => username}
    response.code.should eq(403), "User #{username} should not be allowed to update project #{project_name}"
    response = resource.delete project["id"]
    response.code.should eq(403), "User #{username} should not be allowed to delete project #{project_name}"
  end
end

Then /^User ["|']([^"]*)["|'] with credential ["|']([^"]*)["|'] can read, but not update or delete user ["|']([^"]*)["|']$/ do |user, password, read_user|
  resource :users, :username => user, :password => password do |resource|
    resource.get.code.should eq(200)
    user = resource.get read_user
    user.code.should eq(200)
    user_rec = JSON.parse(user.body)
    update = resource.put user, user_rec
    update.code.should eq(403)
    delete = resource.delete user
    delete.code.should eq(403)
  end
end

Then /^User "([^"]*)" with credential "([^"]*)" can read, but not update or delete user group "([^"]*)"$/ do |user, password, group_name|
  resource :groups, :username => user, :password => password do |resource|
    resource.get.code.should eq(200)
    group = resource.find_by_name group_name
    group.should_not be_nil
    update = resource.put group[:id], group
    update.code.should eq(403)
    delete = resource.delete group[:id]
    delete.code.should eq(403)
  end
end

Then /^User "([^"]*)" with credential "([^"]*)" can read, but not update or delete databag "([^"]*)"$/ do |user, password, databag|
  resource :databags, :username => user, :password => password do |resource|
    resource.get.code.should eq(200)
    db = resource.find_by_name(databag)
    db.should_not be_nil
    read = resource.get db["id"]
    read.code.should eq(200)
    db = JSON.parse(read.body)
    update = resource.put db["id"], {:name => "#{databag}_changed"}
    update.code.should eq(403)
    delete = resource.delete db["id"]
    delete.code.should eq(403)
  end
end

Then /^User "([^"]*)" with credential "([^"]*)" can read environment "([^"]*)" in project "([^"]*)", but can't run workflow "([^"]*)" neither delete it$/ do |user, password, env_name, project_name, workflow_name|
  run_as :username => user, :password => password do
    resource :projects do |resource|
      p = resource.find_by_name project_name
      p.should_not be_nil
      nested_resource :projects, p["id"], :envs do |r|
        env = r.find_by_name env_name
        env.should_not be_nil
        #todo: support for query string in client
        nested_resource r.path, env['id'], "history?page_offset=0&page_length=10" do |nr|
          response = nr.get
          response.code.should eq(200)
        end
        nested_resource r.path, env['id'], "actions" do |nr|
          response = nr.post({:action=>'execute', :parameters => {:workflow => workflow_name}})
          response.code.should eq(403)
        end
        response = r.delete env["id"]
        response.code.should eq(403)
      end
    end
  end
end

Then /^User "([^"]*)" with credential "([^"]*)" can't create an environment "([^"]*)" in project "([^"]*)" with template "([^"]*)" version "([^"]*)"$/ do
  |user, password, env_name, project_name, template, version|
  run_as :username => user, :password => password do
    project_resource project_name, :envs do |resource|
      response = resource.post(create_environment(env_name, template, version))
      response.code.should eq(403)
    end
  end
end