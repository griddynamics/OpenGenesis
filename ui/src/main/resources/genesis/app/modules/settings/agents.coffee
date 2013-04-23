define [
  "genesis",
  "backbone",
  "modules/status",
  "modules/validation",
  "services/backend",
  "utils/poller",
  "jquery",
  "jvalidate"
], (genesis, Backbone, status, validation, backend, poller, $) ->
  'use strict'

  Agents = genesis.module()

  URL = "rest/agents"

  class Agents.Model extends genesis.Backbone.Model
    urlRoot: URL
    linkType: backend.LinkTypes.RemoteAgent

  class Agents.Collection extends genesis.Backbone.Collection
    model: Agents.Model
    url: URL
    linkType: backend.LinkTypes.RemoteAgent

  class Agents.SettingsModel extends genesis.Backbone.Model
    idAttribute: "name"

  class Agents.Settings extends genesis.Backbone.Collection
    url: ->
      "rest/agents/" + @agentId + "/settings"
    agentId: null
    model: Agents.SettingsModel
    initialize: (options) ->
      @agentId = options.id

    parse: (json) ->
      if json.result?
        json.result
      else
        {}

    save: () ->
      payload = _.reduce @models, ((map, elem) -> map[elem.get('name')] = elem.get('value'); return map), {}
      Backbone.sync('update', @, url: @url(), contentType: 'application/json', data: JSON.stringify(payload))


  class Agents.Views.Main extends Backbone.View
    events:
      "click .add-agent": "createAgent"
      "click .edit-agent": "editAgent"

    initialize: (options) ->
      _.bind @render, this
      @collection = new Agents.Collection
      @refresh()

    createAgent: ->
      @showEditView new Agents.Model(
        hostname: "",
        port: 0,
        tags: []
      )

    editAgent: (event) ->
      agentId = $(event.currentTarget).attr("data-agent-id");
      @showEditView(@collection.get(agentId));

    showEditView: (model) ->
      @currentView = new Agents.Views.Edit(model: model, collection: @collection, el: @el)
      self = this
      @currentView.bind "back", ->
        self.currentView.unbind()
        self.currentView.undelegateEvents()
        self.currentView = self.listView
        self.render()


    refresh: =>
      @collection.fetch().done =>
        @listView = new Agents.Views.ListView(collection: @collection, el: @el)
        @currentView = @listView

        @poll = @collection.clone()
        poller.PollingManager.start @poll, { delay: 10000 }
        @poll.bind "reset", @checkStatusUpdates, @

        @render()

    onClose: ->
      poller.PollingManager.stop @poll

    render: ->
      @currentView?.render()

    checkStatusUpdates: ->
      hasStatusChanges = ()=>
        @poll.find (m) => @collection.get(m.id)?.get("status").name != m.get("status").name or
        @collection.get(m.id)?.get("stats")?.runningJobs != m.get("stats")?.runningJobs or
        @collection.get(m.id)?.get("stats")?.totalJobs != m.get("stats")?.totalJobs

      if(@poll.size() != @collection.size() or hasStatusChanges())
        @collection.reset @poll.models
        @render() if @currentView == @listView

  class Agents.Views.ListView extends Backbone.View
    template: "app/templates/settings/agents/agents_list.html"
    events: "click .delete-agent": "deleteAgent"

    deleteAgent: (event) ->
      agentId = @$(event.currentTarget).attr("data-agent-id")
      self = this
      @dialog.dialog "option", "buttons",
        Yes: ->
          self.collection.get(agentId).destroy().done ->
            self.render()

          $(this).dialog "close"

        No: ->
          $(this).dialog "close"

      @dialog.dialog "open"

    initConfirmationDialog: ->
      @$("#dialog-confirm-agent-delete").dialog title: "Confirmation"

    render: =>
      $.when(genesis.fetchTemplate(@template), @collection.fetch()).done (tmpl) =>
        @$el.html tmpl(
          agents: @collection.toJSON()
          canCreate: @collection.canCreate()
          accessRights: @collection.itemAccessRights()
        )
        @dialog = @dialog or @initConfirmationDialog()

  class Agents.Views.Edit extends Backbone.View
    template: "app/templates/settings/agents/edit_agent.html"
    events:
      "click .cancel": "cancel"
      "click #save-agent": "save"

    initialize: (options) ->
      @settings = new Agents.Settings(id: @model.get('id'))
      if (@settings.agentId? && @model.get('status').name == 'Active')
        $.when(@settings.fetch()).always (settings) =>
          @render(settings)
      else
        @render()

    cancel: ->
      @trigger "back"

    save: ->
      return unless @$("#edit-agent").valid()
      port = parseInt(@$("input[name='port']").val().trim())
      port = 0 if isNaN(port)
      agent = @model.clone().set(
        hostname: @$("input[name='hostname']").val().trim()
        port: port
        tags: @$("textarea[name='tags']").val().split(" ")
      )
      validation.bindValidation agent, @$("#edit-agent"), @status
      agent.save().done =>
        if @settings.agentId?
          @saveSettings(agent)
        else
          @trigger "back"

    saveSettings: (agent) ->
      agent.fetch().done () =>
        if (agent.get("status").name == 'Active')
          data = _.reduce @$('input[rel=settings]'), ((acc, item) -> acc[$(item).attr('name')] = $(item).val(); return acc), {}
          for name, value of data
            @settings.get(name).set "value", value
          validation.bindValidation @settings, @$("#edit-agent"), @status
          @settings.save().done () =>
            @trigger "back"
        else
          status.StatusPanel.attention("Cannot save agent plugins configuration because agent is not active")
          @trigger "back"



    render: (settings) ->
      $.when(genesis.fetchTemplate(@template), @model.fetch()).done (tmpl) =>
        @$el.html tmpl(
          agent: @model.toJSON()
          canEdit: @model.canEdit() or @collection.canCreate(),
          settings: settings.result if settings?.result?
        )
  Agents

