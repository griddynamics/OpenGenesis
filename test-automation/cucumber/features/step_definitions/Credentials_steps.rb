require 'httparty'
require 'genesis_client'

When /^I'm creating credential as '(.+)', '(.+)', '(.+)', '(.+)' in the project '(.+)'$/ do |pair, provider, identity, credential, project|
  @last_response = @client.create_credentials(@client.project_id(project), pair, provider, identity, credential)
end

When /^I'm listing existing credentials in the project '(.+)'$/ do |project|
  @last_response = @client.list_credentials(@client.project_id(project))
end

Then /^I should get a list of credentials, including '(.+)', '(.+)' for the project '(.+)'$/ do |pair, provider, project|
  cred = find_credential(JSON.parse(@last_response.body), pair, provider, @client.project_id(project))
  cred.should_not be_nil
end

When /^I'm deleting credentials '(.+)', '(.+)' in the project '(.+)'$/ do |pair, provider, project|
  project_id = @client.project_id(project)
  resp = @client.list_credentials(project_id)
  cred = find_credential(JSON.parse(resp.body), pair, provider, project_id)
  cred.should_not be_nil
  @client.delete_credential(project_id, cred["id"])
end

Then /^The credentials should be deleted successfully from project '(.+)'$/ do |project|
  project_id = @client.project_id(project)
  resp = @client.list_credentials(project_id)
  JSON.parse(resp.body).size.should == 0
end

def find_credential(creds, pair, provider, project_id)
  creds.detect { |c| c["cloudProvider"] == provider and c["pairName"] == pair and c["projectId"] == project_id}
end
