require 'httparty'
require 'genesis_client'


Given /^Genesis is running$/ do
  @client = GenesisClient.new()
  response = @client.ping
  response.code.should == 200
end

Given /^I am valid admin user$/ do
  #@client.auth = {:username => arg1, :password => arg2}
  response = @client.whoami
  struct = JSON.parse(response.body)
  struct["administrator"].should == true
end

When /^I create a project with the name '(.+)' managed by '(\w+)'$/ do |arg1, arg2|
  @last_response = @client.create_project(arg1, arg2)
end

When /^I create a project with (\d+) characters in the name managed by '(.+)'$/ do |arg1, arg2|
  name = 'a' * arg1.to_i
  @last_response = @client.create_project(name, arg2)
end

Then /^I should get response with code '(\d+)'$/ do |arg1|
  @last_response.code.should == arg1.to_i
end

Then /^Project '(.+)' must exist$/ do |arg1|
  @client.project_id(arg1).should_not be_nil
end

Then /^Project '(.+)' should not exist$/ do |arg1|
  @client.project_id(arg1).should be_nil
end

Then /^I can delete project '(.+)'$/ do |arg1|
  id = @client.project_id(arg1)
  id.should_not be_nil
  response = @client.delete_project(id)
  response.code.should == 200
end
