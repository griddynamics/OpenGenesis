Then(/^User "([^"]*)" with credential "([^"]*)" cannot find project '([^"]*)'$/) do |user, pass, project|
  project_id = project_id(project)
  run_as :username => user, :password => pass do
    resource :projects do |resource|
      p = resource.find_by_name project
      p.should be_nil, "User can not list project he don't have access to"
    end
  end
end

Then(/^User "([^"]*)" with credential "([^"]*)" can find project '([^"]*)'$/) do |user, pass, project|
  project_id = project_id(project)
  run_as :username => user, :password => pass do
    resource :projects do |resource|
      p = resource.find_by_name project
      p.should_not be_nil, "User must be able to list project he have access to"
    end
  end
end