require 'rubygems'
require 'httparty'
require 'yaml'

module Genesis

  def nested_resource(parent, parent_id, path, &block)
    full_path = "#{parent}/#{parent_id}/#{path}"
    r = Genesis::Resource.new(full_path)
    if block_given?
      block.call(r)
    else
      r
    end
  end

  def resource(path, &block)
    r = Genesis::Resource.new(path)
    if block_given?
       block.call(r)
    else
      r
    end
  end

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

    def method_missing(name, *arguments, &block)
      if name =~ /find_by_(.+)/
         names = $1
         attributes = names && names.split("_and_")
         return super unless (attributes.size == arguments.size)
         criteria = Hash[attributes.zip(arguments)]
         instance_eval <<-EOS, __FILE__, __LINE__ + 1
            def #{name}(criteria)
              response = get
              arr = JSON.parse(response.body)
              criteria.each do |k,v|
                arr.reject! {|g| g[k] != v}
              end
              if (arr.size > 0)
                arr[0]
              end
            end
         EOS
         send(name, criteria)
      else
        super
      end
    end



    [:get, :post, :put, :delete].each do |verb|
       send :define_method, verb do |*args|
         path = @path
         if args
           first = args.shift
           if first.class == Hash
             body = first
           else
             path = "#{@path}/#{first}" if first
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
      if p.length > 0
        "#{gp}/rest/#{p}"
      else
        gp
      end
    end
  end
end

