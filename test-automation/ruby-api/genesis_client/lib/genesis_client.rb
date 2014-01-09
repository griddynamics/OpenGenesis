require 'rubygems'
require 'httparty'
require 'yaml'

module Genesis
  # Generate nested resource under root parent/:parent_id
  def nested_resource(parent, parent_id, path, auth = {}, &block)
    full_path = "#{parent}/#{parent_id}/#{path}"
    auth = @auth if auth.empty?
    r = Genesis::Resource.new(full_path, @config, auth)
    if block_given?
       yield r
    else
      r
    end
  end
  # Generate top-level resource under /rest prefix.
  # However, if path == "", /rest prefix is skipped
  def resource(path, auth = {},  &block)
    auth = @auth if auth.empty?
    r = Genesis::Resource.new(path, @config, auth)
    if block_given?
       yield r
    else
      r
    end
  end

  def run_as(auth = {}, &block)
     @auth = auth
     yield
     @auth = {}
  end

  class Resource
    include HTTParty
    attr_reader :auth
    attr_reader :path

    def initialize(path, config = {}, auth = {})
      @path = path
      begin
        @host = config["host"]
        @port = config["port"]
        if auth.nil? || auth.empty?
          @auth = {:username => config["user"], :password => config["password"]}
        else
          @auth = auth
        end
      rescue
        raise "You must provide a config hash with keys host, port, user, password"
      end
    end

    def method_missing(name, *arguments, &block)
      if match = /find_by_(.+)/.match(name.to_s)
        names = match.captures.first
        attributes = names && names.split("_and_")
        return super unless (attributes.size == arguments.size)
        method_body = <<-EOS
        def #{name}(*arguments)
              criteria = concat_arguments('#{attributes.join(",")}', arguments)
              response = get
              arr = []
              begin
                arr = JSON.parse(response.body)
              rescue
                raise "Response is not valid JSON: " + response.body
              end
              if arr.class == Hash && arr.has_key?("items")
                 arr = arr["items"]
              end
              criteria.each do |k,v|
                arr = arr.reject {|g| g[k.to_s] != v}
              end
              if (arr.size > 0)
                arr[0]
              end
            end
        EOS
        instance_eval method_body, __FILE__, __LINE__ + 1
        send(name, *arguments)
      else
        return super
      end
    end



    [:get, :post, :put, :delete].each do |verb|
       send :define_method, verb do |*args|
         path = @path
         body = Hash.new
         if args
           first = args.shift
           if first.class == String || first.class == Fixnum
             path = "#{@path}/#{first}"
             body = args.shift
           else
             body = first
           end
         end
         options = {:headers => {'Content-Type' => 'application/json'}}
         options.merge!({:body => body.to_json}) unless body.nil?
         options.merge!({:basic_auth => auth}) unless auth.nil?
         send "_#{verb}", path, options
       end
    end

    def long_read(response)
      if response.code == 202
        body = JSON.parse(response.body)
        if body.has_key?("location")
          sleep 1
          location = body["location"]
          _get(location, {:basic_auth => @auth})
        else
          response
        end
      else
        response
      end
    end

    def find(&condition)
      response = get
      begin
        found = JSON.parse(response.body).select {|g| condition.call(g) }
        if found.size > 0
          found[0]
        end
      rescue
        raise "Response is not valid json #{response.body}"
      end
    end


    private
    def concat_arguments(fields, arguments)
      Hash[fields.split(",").zip(arguments)]
    end

    def _genesis_path
      "http://#{@host}:#{@port}"
    end

    def _get(p, options = {})
      long_read self.class.get(_path(p), options)
    end

    def _post(p, options = {})
      long_read self.class.post(_path(p), options)
    end

    def _put(p, options = {})
      long_read self.class.put(_path(p), options)
    end

    def _delete(p, options = {})
      long_read self.class.delete(_path(p), options)
    end

    def _path(p)
      if p.to_s.start_with?("http://")
        p
      else
        gp = _genesis_path()
        if p.to_s.size > 0
          "#{gp}/rest/#{p}"
        else
          gp
        end
      end
    end
  end

  class Hashed
    def initialize(hash)
      hash.each do |k,v|
        elements = k.split('.')
        create_methods_from_elements(elements, v)
      end
    end

    def method_missing(name, *arguments)
      method_name = name.to_s
      elements = method_name.split('.')
      if elements.size < 2
        raise NoMethodError.new("Only names with dots are expected in Hashed.method_missing, but got #{name}", name)
      end
      obj = self.send(elements.first)
      if obj.class == self.class
        obj.send(elements.drop(1).join("."))
      else
        obj
      end
    end

    private
    def create_methods_from_elements(elements, v)
      if (elements.size == 1)
        create_methods(elements.first, v)
      else
        create_methods_from_elements([elements.first], {elements.drop(1).join(".") => v})
      end
    end

    def create_methods(k, v)
      if v.class == Hash
        self.instance_variable_set("@#{k}", Hashed.new(v))
      else
        self.instance_variable_set("@#{k}", v)
      end
      # Here we create singleton methods because otherwise we'll create methods
      # for all instances of a Hashed class. For example, having key as 'some.simple.val'
      # we'll create methods :some, :simple and :val for every instance of Hashed class,
      # though only top-level class should have method :some, and so on
      singleton = class << self; self end
      singleton.send :define_method, k, proc { self.instance_variable_get("@#{k}") }
    end
  end
end

