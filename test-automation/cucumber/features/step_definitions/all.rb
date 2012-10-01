Given /^Genesis is running$/ do
  @client = GenesisClient.new()
  resource "" do |r|
    response = r.get
    response.code.should eq(200), "Expected response code 200, but really got #{response.code}"
  end
end

Given /^I am valid admin user$/ do
  resource :whoami do |r|
    JSON.parse(r.get.body)["administrator"].should eq(true), "It should run as admin user"
  end
end