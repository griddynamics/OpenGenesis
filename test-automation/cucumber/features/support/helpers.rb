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
end
World(Genesis, ModelHelpers)