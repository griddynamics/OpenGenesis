When /^I request url "(.*)"$/ do |url|
  @response_obj = resource url do |r|
    body = r.get.body
    JSON.parse(body)
  end
end

When /^User "(.+)" with password "(.*)" requests url "(.*)"$/ do |user, password, url|
  @response_obj = resource url, {:username => user, :password => password} do |r|
    body = r.get.body
    JSON.parse(body)
  end
end

Then /^(?:I|He|She) get links as "(.*)", "(.*)", "(.*)", "(.*)"$/ do |url, type, rel, methods|
  obj = @response_obj.find {|x| x["href"] == full_url(url)}
  obj.should_not be_nil
  expected_methods = methods.split(",")
  expected_methods.each do |method|
    obj["methods"].should include(method)
  end
  obj["type"].should eq(type)
  obj["rel"].should eq(rel)
end

Then /^(?:I|He|She) get no links as "(.*)"$/ do |url|
  obj = @response_obj.find {|x| x["href"] == full_url(url)}
  puts obj
  obj.should be_nil
end