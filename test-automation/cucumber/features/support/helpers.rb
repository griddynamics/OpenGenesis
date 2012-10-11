require "genesis_client"

module ModelHelpers
  def project(name, manager, description = nil)
    {:name => name, :projectManager => manager, :description => description}
  end

  def create_project(name, manager, description = nil)
    resource :projects do |resource|
      resource.post(project(name, manager, description))
    end
  end

  def create_credentials(provider, pair, identity, credential, project_id = 0)
    {:projectId => project_id, :cloudProvider => provider, :pairName => pair, :identity => identity, :credential => credential}
  end

  def create_environment(name, template, version, variables = {})
    {:envName => name, :templateName => template, :templateVersion => version, :variables => variables}
  end

  def create_user(username, email, first_name, last_name, job_title, password, groups)
    {:username => username, :email => email, :firstName => first_name, :lastName => last_name,
     :jobTitle => job_title, :password => password, :groups => groups.split(",")}
  end

  def in_project(name, &block)
    resource :projects do |projects|
      project = projects.find_by_name(name)
      project.should_not be_nil, "Expected to find project #{project}, but got none"
      block.call(project["id"])
    end
  end

  def project_resource(project, resource_path, &block)
    resource :projects do |projects|
      project = projects.find_by_name(project)
      project.should_not be_nil, "Expected to find project #{project}, but got none"
      nested_resource :projects, project["id"], resource_path do |resource|
        block.call(resource,  project["id"])
      end
    end
  end

  def credentials_resource(project, &block)
    project_resource project, :credentials, &block
  end

  def templates_resource(project, &block)
    project_resource project, :templates, &block
  end

  def environments_resource(project, &block)
    project_resource project, :envs, &block
  end

  def workflow_resource(project, template, version, workflow, &block)
    templates_resource project do |r, id|
      nested_resource r.path, "#{template}/v#{version}", "#{workflow}", &block
    end
  end

  def errors(response, code, &block)
    response.code.should eq(code.to_i), "Expected to get code #{code}, but really it's #{response.code}"
    r = Genesis::Hashed.new(JSON.parse(response.body))
    if block_given?
      block.call(r)
    else
      r
    end
  end

  def wait_for(count, &block)
    cond = block.call
    cur_count = count
    until cond
      raise "Too many tryouts #{count}" if cur_count > count
      sleep 1
      cond = block.call
      cur_count = count + 1
    end
  end
end
World(Genesis, ModelHelpers)