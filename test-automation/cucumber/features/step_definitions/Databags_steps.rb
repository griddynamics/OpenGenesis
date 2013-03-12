When /^I create databag ["|'](.+)["|'] in project ["|'](.+)["|'] with values$/ do |databag_name, project_name, table|
  databag_values = table.rows_hash
  pending
end

When /^I create databag "(.+)" with values$/ do |databag_name, table|
  databag_values = table.rows_hash
  resource :databags do |resource|
    resource.post({:name => databag_name, :values => databag_values})
  end
end

When /^I can delete databag "([^"]*)"$/ do |db_name|
  resource :databags do |resource|
    db = resource.find_by_name(db_name)
    db.should_not be_nil
    delete = resource.delete db["id"]
    delete.code.should eq(200)
  end
end