require 'rubygems'
require 'httparty'
require 'yaml'


class GenesisClient
  include HTTParty
  attr_accessor :auth

  def initialize()
    config = YAML::load(File.open(File.dirname(__FILE__) + "/../config.yml"))
    @host = config["genesis"]["host"]
    @port = config["genesis"]["port"]
    @auth = {:username => config["genesis"]["user"], :password => config["genesis"]["password"]}
  end


  def ping
    self.class.get(genesis_path + "/", {:basic_auth => auth})
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
    post('/projects', :body => project.to_json)
  end


  def delete_project(id)
    delete("/projects/#{id}")
  end

  def get_template(project, template, version)
    get("/projects/#{project}/templates/#{template}/v#{version}")
  end

  def env_id(project, name)
    response = get("/projects/#{project}/envs")
    found = JSON.parse(response.body).select { |e| e["name"] == name }
    if found.size > 0
      found[0]["id"]
    end
  end

  def create_env(id, name, template, version, variables = {})
    env = {:envName => name, :templateName => template, :templateVersion => version, :variables => variables}
    post("/projects/#{id}/envs", :body => env.to_json)
  end

  def delete_projects
    resp = get('/projects')
    JSON.parse(resp.body).map { |p| delete_project(p["id"]) }
  end

  def create_credentials(project_id, pair, provider, identity, credential)
    creds = {:projectId => project_id, :cloudProvider => provider, :pairName => pair, :identity => identity, :credential => credential}
    post("/projects/#{project_id}/credentials", :body => creds.to_json)
  end

  def list_credentials(project_id)
    get("/projects/#{project_id}/credentials")
  end

  def delete_credential(project_id, credential_id)
    delete("/projects/#{project_id}/credentials/#{credential_id}")
  end

  def rename_env(id, env_id, newname)
    req = {:environment => {:name => newname}}
    put("/projects/#{id}/envs/#{env_id}", :body => req.to_json)
  end

  def create_group(name, description, mail, users)
    req = {:name => name, :description => description, :mailingList => mail, :users => users}
    post("/groups", :body => req.to_json)
  end

  def group_id(name)
    response = get("/groups")
    found = JSON.parse(response.body).select { |g| g["name"] == name }
    if found.size > 0
      found[0]["id"]
    end
  end

  def delete_group(id)
    delete("/groups/#{id}")
  end

  private
  def genesis_path
    "http://#{@host}:#{@port}"
  end

  def get(p, options = {})
    options.merge!({:basic_auth => auth}) unless auth.nil?
    res = self.class.get(path(p), options)
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

module Genesis

  class Resource
    include HTTParty
    attr_accessor :auth

    def initialize(path)
      @path = path
      config = YAML::load(File.open(File.dirname(__FILE__) + "/../config.yml"))
      @host = config["genesis"]["host"]
      @port = config["genesis"]["port"]
      @auth = {:username => config["genesis"]["user"], :password => config["genesis"]["password"]}
    end

    [:get, :post, :put, :delete].each do |verb|
       send :define_method, verb do |*args|
         path = @path
         if args
           first = args.shift
           if first.class == Hash
             body = first
           else
             path = "#{@path}/#{first}"
             body = args.shift || Hash.new
           end
         else
           body = Hash.new
         end
         options = {:headers => {'Content-Type' => 'application/json'}}
         options.merge!({:body => body.to_json}) unless body.empty?
         options.merge!({:basic_auth => auth}) unless auth.nil?
         send "_#{verb}", path, options
       end
    end

    def find(&condition)
      response = get
      found = JSON.parse(response.body).select {|g| condition.call(g) }
      if found.size > 0
        found[0]
      end
    end

    private
    def _genesis_path
      "http://#{@host}:#{@port}"
    end

    def _get(p, options = {})
      self.class.get(_path(p), options)
    end

    def _post(p, options = {})
      self.class.post(_path(p), options)
    end

    def _put(p, options = {})
      self.class.put(_path(p), options)
    end

    def _delete(p, options = {})
      self.class.delete(_path(p), options)
    end

    def _path(p)
      gp = _genesis_path()
      "#{gp}/rest/#{p}"
    end
  end
end

