define ["genesis", "modules/status", "backbone", "jquery", "jvalidate"], (genesis, status, Backbone, $) ->
  Plugins = genesis.module()
  class Plugins.Model extends Backbone.Model

  class Plugins.Collection extends Backbone.Collection
    model: Plugins.Model
    url: "rest/plugins"

  class Plugins.Views.Main extends Backbone.View
    template: "app/templates/settings/plugins.html"
    events:
      "click .edit-settings": "editSettings"

    initialize: (options) ->
      _.bind @render, this
      @collection = new Plugins.Collection()
      @collection.fetch().done =>
        @listView = new PluginsList(
          collection: @collection
          el: @el
        )
        @currentView = @listView
        @render()

      @mainView = options.main

    onClose: ->
      genesis.utils.nullSafeClose @currentView
      genesis.utils.nullSafeClose @listView

    editSettings: (element) ->
      id = $(element.currentTarget).attr("data-plugin-id")
      plugin = @collection.get(id)
      @currentView = new PluginSettings(
        model: plugin
        el: @el
      )
      @currentView.bind "back", =>
        @currentView.unbind()
        @currentView.undelegateEvents()
        @currentView = @listView
        @render()


    render: ->
      if @currentView?
        @currentView.render()
        @mainView.toggleRestart()

  class PluginsList extends Backbone.View
    template: "app/templates/settings/plugins.html"
    render: ->
      $.when(genesis.fetchTemplate(@template)).done (tmpl) =>
        @$el.html tmpl(plugins: @collection.toJSON())

  class PluginSettings extends Backbone.View
    template: "app/templates/settings/plugin_settings.html"
    events:
      "click a.back": "backToPlugins"
      "click a.save": "savePluginSettings"

    initialize: (options) ->
      @plugin = @model
      @plugin.fetch().done =>
        @configMap = _(@plugin.get("configuration")).reduce((memo, config) ->
          memo[config.name] = config.value
          memo
        , {})
        @render()


    backToPlugins: ->
      @trigger "back"

    savePluginSettings: ->
      configuration = _(@plugin.get("configuration")).map(_.clone)
      _(configuration).each (item) ->
        item.value = $("input[name='" + item.name + "']").val()  unless item.readOnly

      @plugin.set "configuration", configuration,
        silent: true

      if @plugin.hasChanged("configuration")
        @plugin.save().done(=>
          status.StatusPanel.success "Plugin settings updated"
          @trigger "back"
        ).error ->
          status.StatusPanel.error "Failed to process request"

      else
        @trigger "back"

    render: ->
      $.when(genesis.fetchTemplate(@template)).done (tmpl) =>
        settings = _(@plugin.get("configuration")).groupBy((item) ->
          (if item.readOnly then "constants" else "properties")
        )
        @$el.html tmpl(
          plugin: @plugin.toJSON()
          settings: settings
        )

  Plugins

