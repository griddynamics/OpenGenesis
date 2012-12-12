When /^I create an agent as$/ do |table|
  data = table.rows_hash
  step "I create an agent with #{data[:hostname]}, #{data[:port]}, #{data[:tags]}"
end

When /^I can delete an agent for host "([^"]*)" and port (\d+)$/ do |host, port|
  resource :agents do |resource|
    agent = resource.find_by_hostname_and_port(host, port.to_i)
    agent.should_not be_nil, "Expected to have at least one agent for host #{host} and port #{port}"
    response = resource.delete agent["id"]
    response.code.should eq(200), "Expected to have 200 result for agent deletion"
    response = resource.get agent["id"]
    response.code.should eq(404), "Expected not to meet deleted agent #{agent[:id]}, but got response #{response.code}"
  end
end
When /^I create an agent with (.*), (.*), (.*)$/ do |hostname, port, tags|
  @last_response = resource :agents do |resource|
    resource.post([:hostname => hostname, :port => port.to_i, :tags => tags.split(",")])
  end
end