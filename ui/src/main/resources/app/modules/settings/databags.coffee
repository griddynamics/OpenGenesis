define ["genesis", "backbone", "modules/status", "modules/common/properties", "modules/validation", "services/backend", "jquery", "jvalidate"], (genesis, Backbone, status, property, validation, backend, $) ->
  Databags = genesis.module()
  URL = "rest/databags"
  class Databags.Model extends Backbone.Model
    initialize: (options) ->
      @projectId = options.projectId if options.projectId

    urlRoot: ->
      if @projectId
        "rest/projects/" + @projectId + "/databags"
      else
        URL

  class Databags.Collection extends Backbone.Collection
    model: Databags.Model
    initialize: (options) ->
      @projectId = options.projectId if options.projectId?

    url: ->
      if @projectId
        "rest/projects/" + @projectId + "/databags"
      else
        URL

  class DatabagItem extends Backbone.Model

  class ItemsCollection extends Backbone.Collection
    mode: DatabagItem
    fetch: -> #fake fetch
      def = new $.Deferred()
      @trigger "reset"
      def.resolve()

  class Databags.Views.Main extends Backbone.View
    events:
      "click .add-databag": "createDatabag"
      "click .edit-databag": "editDatabag"

    initialize: (options) ->
      _.bind @render, this
      @projectId = options.projectId if options.projectId
      @collection = new Databags.Collection(projectId: @projectId)
      @refresh()

    refresh: ->
      self = this
      @collection.fetch().done ->
        self.listView = new DatabagsList(
          collection: self.collection
          el: self.el
        )
        self.currentView = self.listView
        self.render()


    createDatabag: =>
      self = this
      @showEditView new Databags.Model(
        name: ""
        tags: []
        projectId: @projectId
      )

    editDatabag: (e) ->
      bagId = $(e.currentTarget).attr("data-databag-id")
      @showEditView @collection.get(bagId)

    showEditView: (model) ->
      @currentView = new EditDatabagView(
        model: model
        el: @el
      )
      self = this
      @currentView.bind "back", ->
        self.currentView.unbind()
        self.currentView.undelegateEvents()
        self.currentView = self.listView
        self.render()


    render: ->
      @currentView.render()  if @currentView?

  class DatabagsList extends Backbone.View
    template: "app/templates/settings/databags/bags_list.html"
    events:
      "click .delete-databag": "deleteDatabag"

    deleteDatabag: (e) ->
      bagId = $(e.currentTarget).attr("data-databag-id")
      self = this
      @dialog.dialog "option", "buttons",
        Yes: ->
          self.collection.get(bagId).destroy().done ->
            self.render()

          $(this).dialog "close"

        No: ->
          $(this).dialog "close"

      @dialog.dialog "open"

    initConfirmationDialog: ->
      @$("#dialog-confirm-databag-delete").dialog title: "Confirmation"

    render: (done) =>
      self = this
      $.when(genesis.fetchTemplate(@template), @collection.fetch()).done (tmpl) =>
        @$el.html tmpl(databags: @collection.toJSON())
        @dialog = @dialog or @initConfirmationDialog()


  class EditDatabagView extends Backbone.View
    template: "app/templates/settings/databags/edit_databag.html"
    events:
      "click .cancel": "cancel"
      "click #save-databag": "save"

    initialize: (options) ->
      @render()

    cancel: ->
      @trigger "back"

    save: ->
      return  unless @$("#edit-databag").valid()
      properties = @propertyView.pullCollection()
      toBeRemoved = properties.filter((item) ->
        item.get("removed") or not item.get("name")
      )
      properties.remove toBeRemoved,
        silent: true

      bag = @model.clone().set(
        name: @$("input[name='name']").val().trim()
        tags: @$("textarea[name='tags']").val().split(" ")
        items: properties.toJSON()
      )
      validation.bindValidation bag, @$("#edit-databag"), @status
      self = this
      bag.save().done ->
        self.trigger "back"


    render: ->
      self = this
      $.when(genesis.fetchTemplate(@template), @model.fetch()).done (tmpl) ->
        items = _(self.model.get("items")).map((item) ->
          new DatabagItem(item)
        )
        itemsCollection = new ItemsCollection(items)
        self.$el.html tmpl(databag: self.model.toJSON())
        self.propertyView = new property.Views.PropertyEditor(
          collection: itemsCollection
          el: self.$("#properties")
        )
        self.status = new status.LocalStatus(el: self.$(".notification"))

  Databags

