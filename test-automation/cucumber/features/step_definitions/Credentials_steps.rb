When /^I'm creating credential as '(.+)', '(.+)', '(.+)', '(.+)' in the project '(.+)'$/ do |pair, provider, identity, credential, project|
  @last_response = credentials_resource project do |r, id|
    r.post(create_credentials(provider, pair, identity, credential, id))
  end
end

When /^I'm listing existing credentials in the project '(.+)'$/ do |project|
  @last_response = credentials_resource project do |r, id|
      r.get
  end
end

Then /^I should get a list of credentials, including '(.+)', '(.+)' for the project '(.+)'$/ do |pair, provider, project|
  cred = credentials_resource project do |r, id|
      r.find_by_pairName_and_cloudProvider_and_projectId(pair, provider, id)
  end
  cred.should_not be_nil, "Expected to get credentials for #{pair}, #{provider}, #{project}, but got none"
end

When /^I'm deleting credentials '(.+)', '(.+)' in the project '(.+)'$/ do |pair, provider, project|
  credentials_resource project do |r, id|
    cred = r.find_by_pairName_and_cloudProvider_and_projectId(pair, provider, id)
    cred.should_not be_nil, "Expected to get credentials for #{pair}, #{provider}, #{project}, but got none"
    r.delete(cred["id"])
  end
end

Then /^The credentials should be deleted successfully from project '(.+)'$/ do |project|
  credentials_resource project do |r, id|
      cred = JSON.parse(r.get.body)
      cred.size.should eq(0), "There should'nt be any credentials in project #{project}, but there is #{cred.size} credentials defined"
  end
end
