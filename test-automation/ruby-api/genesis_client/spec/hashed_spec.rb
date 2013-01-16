require 'rubygems'
require 'genesis_client'
require 'test/unit'
require 'rspec'

class HashedSpec < Test::Unit::TestCase
  include Genesis
  describe Hashed do

     it "should be created from plain map" do
       hashed = Hashed.new({"errors" => "Simple error"})
       hashed.respond_to?("errors").should == true
       hashed.errors.should eq("Simple error")
     end

     it "should be created from multilevel map" do
       hashed = Hashed.new({"errors" => {"first" => "Simple error"}})
       hashed.respond_to?("errors").should eq(true)
       hashed.errors.class.should eq(Hashed)
       hashed.errors.respond_to?("first").should eq(true)
       hashed.errors.first.should eq("Simple error")
     end

     it "should support dot in key names" do
       hashed = Hashed.new({"errors" => {"compound.error" => "Simple error"}})
       hashed.respond_to?("errors").should eq(true)
       hashed.errors.class.should eq(Hashed)
       hashed.errors.respond_to?("compound").should eq(true)
       hashed.errors.compound.respond_to?("error").should eq(true)
     end

     it "should support dots in key names" do
       hashed = Hashed.new({"errors" => {"compound.simple.error" => "Simple error"}})
       hashed.respond_to?("errors").should eq(true)
       hashed.errors.class.should eq(Hashed)
       hashed.errors.respond_to?("compound").should eq(true)
       hashed.errors.compound.respond_to?("simple").should eq(true)
       hashed.errors.compound.simple.respond_to?("error").should eq(true)
     end
  end
end