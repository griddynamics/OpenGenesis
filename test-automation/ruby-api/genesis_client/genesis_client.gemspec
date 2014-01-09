$LOAD_PATH << File.join(File.dirname(__FILE__), 'lib')
require 'genesis_client/version'

Gem::Specification.new do |s|
  s.name        = 'genesis_client'
  s.version     = Genesis::VERSION
  s.platform    = Gem::Platform::RUBY
  s.authors     = ['Svyatoslav Reyentenko']
  s.email       = %q{rsvato@gmail.com}
  s.homepage    = 'http://open-genesis.org'
  s.summary     = %q{Simple OpenGenesis client}
  s.description = %q{Simple OpenGenesis client for Cucumber tests}
  s.files         = `git ls-files`.split("\n")
  s.require_paths = ['lib']
  s.add_dependency 'httparty', '>= 0'
  s.add_dependency 'json'
end
