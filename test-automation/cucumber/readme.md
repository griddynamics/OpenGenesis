Genesis cucumber tests
======================

You should have ruby and rubygems (or jruby) installed.

Steps to run
------------

### Install bundler:

```
gem install bundler
```

### Install tests dependencies:

```
budle install
```

### Copy config.yml.sample to config.yml and edit it to reflect your settings:

```yaml
genesis:
  host: 127.0.0.1 #host running genesis
  port: 8080 #port that genesis listen on
  user: user #user to use for tests
  password: pass #pass to use for tests
```

### Run tests:

```
bundle exec cucumber
```

or just 

```
cucumber
```

Note: with JRuby commands should be prefixed with

```
jruby -S ...
```


Expectations
------------

- Template Simple.genesis in subdir templates should be accessible for genesis instance
- It's better to run tests in empty database to avoid possible interference
