require 'rubygems'
require 'httparty'
require 'yaml'

module Genesis
  # Generate nested resource under root parent/:parent_id
  def nested_resource(parent, parent_id, path, auth = {}, &block)
    full_path = "#{parent}/#{parent_id}/#{path}"
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
    r = Genesis::Resource.new(path, @config, auth)
    if block_given?
       yield r
    else
      r
    end
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
        if auth.empty?
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
              arr = JSON.parse(response.body)
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

    def find(&condition)
      response = get
      found = JSON.parse(response.body).select {|g| condition.call(g) }
      if found.size > 0
        found[0]
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
      if p.to_s.size > 0
        "#{gp}/rest/#{p}"
      else
        gp
      end
    end
  end

  class Hashed
    def initialize(hash)
      hash.each do |k,v|
        if v.class == Hash
          self.instance_variable_set("@#{k}", Hashed.new(v))
        else
          self.instance_variable_set("@#{k}", v)
        end
        self.class.send :define_method, k, proc { self.instance_variable_get("@#{k}") }
      end
    end
  end
end

