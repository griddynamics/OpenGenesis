define [
  "genesis",
  "backbone",
  "modules/status",
  "modules/common/properties",
  "modules/validation",
  "services/backend",
  "jquery",
  "jvalidate"],
(genesis, Backbone, status, property, validation, backend, $) ->
  Databags = genesis.module()
  URL = "rest/databags"

  linkTypes = backend.LinkTypes

  class Databags.Model extends genesis.Backbone.Model
    linkType: backend.LinkTypes.DataBag
    initialize: (options) ->
      @projectId = options.projectId if options.projectId

    urlRoot: ->
      if @projectId
        "rest/projects/" + @projectId + "/databags"
      else
        URL

  class Databags.Collection extends genesis.Backbone.Collection
    model: Databags.Model,
    linkType: backend.LinkTypes.DataBag
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
      @collection.fetch().done =>
        @listView = new DatabagsList(
          collection: @collection
          el: @el
        )
        @currentView = @listView
        @render()


    createDatabag: =>
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
        collection: @collection
        el: @el
      )
      @currentView.bind "back", =>
        @currentView.unbind()
        @currentView.undelegateEvents()
        @currentView = @listView
        @render()


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
      $.when(genesis.fetchTemplate(@template), @collection.fetch()).done (tmpl) =>
#        accessRights = @collection
#          .chain()
#          .map((item) -> {id: item.id, canEdit: item.canEdit(), canDelete: item.canDelete()})
#          .groupBy("id")
#          .value()
#        debugger;
        @$el.html tmpl(
          databags: @collection.toJSON()
          canCreate: @collection.canCreate()
          accessRights: @collection.itemAccessRights()
        )
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
      bag.save().done =>
        @trigger "back"


    render: ->
      $.when(genesis.fetchTemplate(@template), @model.fetch()).done (tmpl) =>
        items = _(@model.get("items")).map((item) ->
          new DatabagItem(item)
        )
        itemsCollection = new ItemsCollection(items)
        @$el.html tmpl(
          databag: @model.toJSON()
          canSave: @model.canEdit() or @collection.canCreate()
        )
        @propertyView = new property.Views.PropertyEditor(
          collection: itemsCollection
          el: @$("#properties")
        )
        @status = new status.LocalStatus(el: @$(".notification"))

  Databags

