define ["genesis", "backbone", "modules/validation", "services/backend", "jquery", "jvalidate"], (genesis, Backbone, validation, backend, $) ->
  Agents = genesis.module()
  URL = "rest/agents"
  class Agents.Model extends Backbone.Model
    urlRoot: URL

  class Agents.Collection extends Backbone.Collection
    model: Agents.Model
    url: URL

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
      @currentView = new Agents.Views.Edit({model: model, el: @el})
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
        @render()

    render: ->
      @currentView?.render()

  class Agents.Views.ListView extends Backbone.View
    template: "app/templates/settings/agents/agents_list.html"
    events: "click .delete-agent": "deleteAgent"

    deleteAgent: (event) ->
      agentId = $(event.currentTarget).attr("data-agent-id")
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
        @$el.html tmpl(agents: @collection.toJSON())
        @dialog = @dialog or @initConfirmationDialog()

  class Agents.Views.Edit extends Backbone.View
    template: "app/templates/settings/agents/edit_agent.html"
    events:
      "click .cancel": "cancel"
      "click #save-agent": "save"

    initialize: (options) ->
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
        @trigger "back"

    render: ->
      $.when(genesis.fetchTemplate(@template), @model.fetch()).done (tmpl) =>
        @$el.html tmpl({agent: @model.toJSON()})
  Agents

