When /^I request a description for workflow '(.+)' for template '(.+)' version '(.+)' in project '(.+)'$/ do |workflow, template_name, template_version, project|
  @last_response = workflow_resource project, template_name, template_version, workflow do |r|
      r.get
  end
end

When /^I'm starting workflow '(.+)' on environment '(.+)' in project '(.+)'$/ do |workflow, env_name, project|
  @last_response = environments_resource project do |resource, id|
    env = resource.find_by_name(env_name)
    env.should_not be_nil, "Expected to find env #{env_name} in project #{id}: #{project}, but got none"
    nested_resource resource.path, env['id'], "actions" do |r|
      r.post({:action=>'execute', :parameters => {:workflow => workflow}})
    end
  end
end