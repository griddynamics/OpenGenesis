When /^I request list of template scopes$/ do
  @last_response = resource(:dbtemplates).get
end

When /^Response should contain link to scope$/ do |table|
  scopes = table.rows_hash
  scopes.keys do |scope|
    link = @last_response["items"].find {|c| c["href"] =~ /\/scope$/ && c["rel"] == scopes[scope]}
    link.should_not be_nil, "Expected to get scope #{scope}, but got none"
  end
end

When /^I request a list of templates for (?:invalid|valid) scope '(.+)'$/ do |scope|
  @last_response = resource("dbtemplates/#{scope}").get
end
When /^I request a template with (?:in)?valid scope '(.+)' and (?:in)?valid id '(.+)'$/ do |scope, id|
  @last_response = resource("dbtemplates/#{scope}/#{id}").get
end

When /^I create databag "([^"]*)" based with template "([^"]*)" and no values$/ do |bag_name, template|
  @last_response = resource :databags do |resource|
    resource.post({:name => bag_name, :templateId => template})
  end
end