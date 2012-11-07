require 'rubygems'
require 'genesis_client'
require 'test/unit'
require 'rspec'

class GenesisClientTest < Test::Unit::TestCase
  include Genesis
  describe Resource do
    include Genesis

    before :each do
      @config = {"host" => 'localhost', "port" => '8081', "user" => 'genesis', "password" => 'genesis'}
    end

    it "should contains path identical to constructor arg" do
      client = resource :projects
      client.path.should == :projects
    end

    it "should have default auth when using with additional auth parameter" do
      client = resource :projects
      client.auth.should  == {:username => 'genesis', :password => 'genesis' }
    end

    it "should change auth when using with additional auth parameter" do
      client = resource :projects, :username => 'foo', :password => 'bar'
      client.auth.should  == {:username => 'foo', :password => 'bar' }
    end

    it "should change auth when using with using from run_as block" do
      run_as :username => 'foo', :password => 'bar' do
        client = resource :projects
        client.auth.should  == {:username => 'foo', :password => 'bar' }
      end
    end
  end
end
