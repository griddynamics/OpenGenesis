define [
  "genesis",
  "backbone",
  "modules/status",
  "cs!modules/common/databag_templates",
  "modules/common/properties",
  "modules/validation",
  "services/backend",
  "jquery",
  "jvalidate"],
(genesis, Backbone, status, templates, property, validation, backend, $) ->
  Databags = genesis.module()
  URL = "rest/databags"

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
      "click .add-from-template": "selectTemplate"
      "click .edit-databag": "editDatabag"

    initialize: (options) ->
      _.bind @render, this
      @projectId = options.projectId if options.projectId
      @collection = new Databags.Collection(projectId: @projectId)
      @scope = if @projectId
        "project"
      else
        "system"
      @refresh()

    refresh: ->
      @collection.fetch().done =>
        @listView = new DatabagsList(
          collection: @collection
          el: @el
        )
        @currentView = @listView
        @scope = if @projectId
          "project"
        else
          "system"
        @selectTemplateDialog = new templates.SelectTemplateDialog(
          $el: $("#select-template-dialog"),
          collection: new templates.Collection(scope: @scope)
        )
        @render()

    createFromTemplate: (id) =>
      created = new templates.Model(
         scope: @scope
         id: id
      )
      $.when(created.fetch()).done (obj) =>
        @showEditView new Databags.Model(
          name: obj.defaultName
          tags: obj.tags
          projectId: @projectId
          templateId: obj.id
          items: obj.properties
        )

    createDatabag: =>
      @showEditView new Databags.Model(
        name: ""
        tags: []
        projectId: @projectId
        templateId: templateId?
      )

    selectTemplate: =>
      @selectTemplateDialog.bind('databag-template-selected', (val) =>
        @createFromTemplate(val)
      ).show()

    editDatabag: (e) ->
      bagId = $(e.currentTarget).attr("data-databag-id")
      @showEditView @collection.get(bagId)

    showEditView: (model) ->
      @currentView = new EditDatabagView(
        model: model
        collection: @collection
        el: @el,
        scope: @scope
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
      @scope = options.scope
      if @model.isNew()
        @render()
      else
        $.when(@model.fetch()).done () => @render()


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
        templateId: @$("input[name='templateId']").val()
        items: properties.toJSON()
      )
      validation.bindValidation bag, @$("#edit-databag"), @status, true
      bag.save().done =>
        @trigger "back"


    render: ->
      $.when(genesis.fetchTemplate(@template)).done (tmpl) =>
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
          el: @$("#properties"),
          dbtemplate: new templates.Model(
            id: @model.get("templateId"),
            scope: @scope
          )
        )
        @status = new status.LocalStatus(el: @$(".notification"))

  Databags

