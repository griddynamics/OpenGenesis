require 'rubygems'
require 'genesis_client'
include Genesis
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

  def create_user(username, email, first_name, last_name, job_title, password, groups = "", roles = "")
    {:username => username, :email => email, :firstName => first_name, :lastName => last_name,
     :jobTitle => job_title, :password => password, :groups => groups.split(","), :roles => roles.split(",")}
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

  def configuration_resource(project, &block)
    project_resource project, :configs, &block
  end

  def genesis_settings_value(name)
    config = resource "/settings?prefix=#{name}" do |r|
      r.get
    end
    JSON.parse(config.body)[0]["value"]
  end

  def find_config(config_name, project, username)
    r = resource("projects/#{project_id(project)}/configs", :username => username, :password => username)
    r.find_by_name(config_name)
  end


  def update_config_access(project, config_id, users)
    project_resource project, "/configs/#{config_id}/access" do |r|
      resp = r.put({:users => users, :groups => []})
      resp.code.should eq(200), "Failed to update access to env configuration. Response: #{resp}"
    end
  end

  def find_config_in_project(name, project)
    configuration_resource project do |r|
      r.find_by_name(name)
    end
  end

  def project_id(project)
    resource :projects do |projects|
      projects.find_by_name(project)["id"]
    end
  end

  def environment_id(env_name, project)
    environments_resource project do |resource, id|
      env = resource.find_by_name(env_name)
      env.should_not be_nil, "Expected that environment #{env_name} exists, but it's not found"
      env["id"]
    end
  end

  def get_env_as_user(env_name, project, username)
    env_id = environment_id(env_name, project)
    project_id = project_id(project)

    r = resource("projects/#{project_id}/envs/#{env_id}", :username => username, :password => username)
    r.get
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
    cond = block.call()
    cur_count = 0
    until cond
      raise "Too many tryouts #{count}" if cur_count > count
      sleep 1
      cond = block.call()
      cur_count += 1
    end
  end


end

class GenesisWorld
  include ModelHelpers
  include Genesis
  def initialize
    yaml = YAML::load(File.open(File.dirname(__FILE__) + "/../../config.yml"))
    @config = yaml["genesis"]
  end
end

World do
  GenesisWorld.new
end
