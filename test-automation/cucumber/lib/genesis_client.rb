require 'httparty'
require 'yaml'

class GenesisClient
  include HTTParty
  attr_accessor :auth

  def initialize()
    config = YAML::load(File.open(File.dirname(__FILE__) + "/../config.yml"))
    @host = config["genesis"]["host"]
    @port = config["genesis"]["port"]
    @auth = {:username => config["genesis"]["user"], :password => config["genesis"]["password"] } 
  end


  def ping
    self.class.get(genesis_path + "/")
  end

  def whoami
    get("/whoami")
  end

  def project_id(name)
    response = get('/projects')
    resp = JSON.parse(response.body)
    found = resp.select { |r| r["name"] == name }
    if found.size > 0
      found[0]["id"]
    else
      nil
    end
  end

  def create_project(name, manager, description = nil)
    project = {:name => name, :projectManager => manager, :description => description}
    post('/projects', :body=>project.to_json)
  end


  def delete_project(id)
    self.class.delete(path("/projects/#{id}"), :basic_auth => auth)
  end

  def get_template(project, template, version)
    get("/projects/#{project}/templates/#{template}/v#{version}")
  end

  def env_id(project, name)
    response = get("/projects/#{project}/envs")
    found = JSON.parse(response.body).select { |e| e["name"] == name }
    if found.size > 0
      found[0]
    end
  end

  def create_env(id, name, template, version, variables = {})
    env = {:envName => name, :templateName => template, :templateVersion => version, :variables => variables }
    post("/projects/#{id}/envs", :body=> env.to_json)
  end

  def delete_projects
    resp = get('/projects') 
    JSON.parse(resp.body).map { |p| delete_project(p["id"]) }
  end

  private
    def genesis_path
      "http://#{@host}:#{@port}"
    end
    
    def get(p, options = {})
      options.merge!({:basic_auth => auth}) unless auth.nil?
      self.class.get(path(p), options)
    end

    def post(p, options = {})
      options.merge!({:basic_auth => auth, :headers => {'Content-Type' => 'application/json'}}) unless auth.nil?
      self.class.post(path(p), options)
    end

    def put(p, options = {})
      options.merge!({:basic_auth => auth, :headers => {'Content-Type' => 'application/json'}}) unless auth.nil?
      self.class.put(path(p), options)
    end

    def delete(p, options = {})
      options.merge!({:basic_auth => auth}) if auth
      self.class.delete(path(p), options)
    end

    def path(p)
      genesis_path + '/rest/' + p
    end
end
