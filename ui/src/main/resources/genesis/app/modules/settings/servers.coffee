define [
  "genesis"
  "services/backend"
  "modules/status"
  "modules/validation"
  "backbone"
  "jquery"
  "showLoading"
  "jvalidate"
], (genesis, backend, status, validation, Backbone, $) ->
  Servers = genesis.module()
  Servers.ArrayModel = genesis.Backbone.Model.extend(
    linkType: backend.LinkTypes.ServerArray
    urlRoot: ->
      projectId = @get("projectId")
      if projectId
        "rest/projects/" + @get("projectId") + "/server-arrays"
      else
        "rest/server-arrays"

    serversCollection: ->
      new Servers.ServersCollection([],
        projectId: @get("projectId")
        serverArrayId: @id
      )
  )
  Servers.ArrayCollection = genesis.Backbone.Collection.extend(
    model: Servers.ArrayModel
    linkType: backend.LinkTypes.ServerArray
    initialize: (elements, options) ->
      @projectId = options.projectId
      return

    url: ->
      if @projectId
        "rest/projects/" + @projectId + "/server-arrays"
      else
        "rest/server-arrays"
  )
  Servers.ServerModel = genesis.Backbone.Model.extend(linkType: backend.LinkTypes.Server)
  Servers.ServersCollection = genesis.Backbone.Collection.extend(
    model: Servers.ServerModel
    linkType: backend.LinkTypes.Server
    initialize: (elements, options) ->
      @projectId = options.projectId
      @serverArrayId = options.serverArrayId
      return

    url: ->
      if @projectId
        "rest/projects/" + @projectId + "/server-arrays/" + @serverArrayId + "/servers"
      else
        "rest/server-arrays/" + @serverArrayId  + "/servers"
  )
  CredentialsRef = Backbone.Collection.extend(
    initialize: (elements, options) ->
      @projectId = options.projectId
      return

    url: ->
      if @projectId
        "rest/projects/" + @projectId + "/credentials/?type=static"
      else
        "rest/credentials/?type=static"

    parse: (json) ->
      if json.items
        json.items
      else
        json
  )
  Servers.Views.Main = Backbone.View.extend(
    projectId: null
    events:
      "click #add-array": "createArray"
      "click .edit-array": "editArray"
      "click .show-array": "showArray"

    initialize: (options) ->
      @projectId = options.projectId
      @collection = new Servers.ArrayCollection({},
        projectId: @projectId
      )
      @listView = new ServerArrayList(
        collection: @collection
        el: @el
      )
      @setCurrentView @listView
      @collection.fetch()
      return

    showArray: (e) ->
      arrayId = $(e.currentTarget).attr("data-array-id")
      @setCurrentView new ServerArrayView(
        el: @el
        array: @collection.get(arrayId)
      )
      self = this
      @currentView.bind "back", ->
        self.setCurrentView self.listView
        return

      return

    onClose: ->
      genesis.utils.nullSafeClose @currentView
      return

    createArray: ->
      @setCurrentView new EditArrayView(
        el: @el
        projectId: @projectId
        model: new Servers.ArrayModel()
        collection: @collection
      )
      self = this
      @currentView.bind "back", ->
        self.setCurrentView self.listView
        self.collection.fetch()
        return

      return

    editArray: (e) ->
      arrayId = $(e.currentTarget).attr("data-array-id")
      @setCurrentView new EditArrayView(
        el: @el
        projectId: @projectId
        model: @collection.get(arrayId)
        collection: @collection
      )
      self = this
      @currentView.bind "back", ->
        self.$el.empty()
        self.setCurrentView self.listView
        self.collection.fetch()
        return

      return

    setCurrentView: (view) ->
      if @currentView?
        @currentView.undelegateEvents()
        @currentView.unbind()
        @currentView.onClose()  if @currentView.onClose
      @currentView = view
      @currentView.render()
      return

    render: ->
  )
  ServerArrayList = Backbone.View.extend(
    template: "app/templates/settings/servers/arrays_list.html"
    dialog: null
    events:
      "click .delete-array": "deleteArray"

    initialize: ->
      @collection.bind "reset", _.bind(@render, this)
      return

    onClose: ->
      @dialog.dialog "close"
      return

    initConfirmationDialog: ->
      @dialog or @$("#dialog-confirm-delete").dialog(title: "Confirmation")

    deleteArray: (elem) ->
      credentialId = $(elem.currentTarget).attr("data-array-id")
      self = this
      @dialog.dialog "option", "buttons",
        Yes: ->
          self.collection.get(credentialId).destroy().done ->
            self.collection.fetch()
            return

          $(this).dialog "close"
          return

        No: ->
          $(this).dialog "close"
          return

      @dialog.dialog "open"
      return

    render: ->
      self = this
      $.when(genesis.fetchTemplate(@template)).done (tmpl) ->
        self.$el.html tmpl(
          serverArrays: self.collection.toJSON()
          accessRights: self.collection.itemAccessRights()
          canCreate: self.collection.canCreate()
        )
        self.dialog = self.initConfirmationDialog()
        self.delegateEvents self.events
        return

      return
  )
  EditArrayView = Backbone.View.extend(
    template: "app/templates/settings/servers/edit_array.html"
    events:
      "click .cancel": "cancel"
      "submit #edit-server-array": "create"
      "click #save-array": "create"

    initialize: (options) ->
      @projectId = options.projectId
      return

    cancel: ->
      @trigger "back"
      return

    create: (e) ->
      e and e.preventDefault()
      return  unless @$("#edit-server-array").valid()
      array = @model.clone()
      array.set
        projectId: parseInt(@projectId)
        name: @$("input[name='name']").val()
        description: @$("textarea[name='description']").val()

      validation.bindValidation array, @$("#edit-server-array"), @status
      self = this
      array.save().done ->
        self.trigger "back"
        return

      return

    render: ->
      self = this
      $.when(genesis.fetchTemplate(@template)).done (tmpl) ->
        self.$el.html tmpl(
          array: self.model.toJSON()
          canEdit: self.model.canEdit() or self.collection.canCreate()
        )
        self.status = new status.LocalStatus(el: self.$(".notification"))
        return

      return
  )
  ServerArrayView = Backbone.View.extend(
    template: "app/templates/settings/servers/servers-list.html"
    events:
      "click .back": "back"
      "click #add-server:not(.disabled)": "showAddServerForm"
      "click .delete-server": "deleteServer"
      "click .show-server": "showServer"

    initialize: (options) ->
      @array = options.array
      @servers = @array.serversCollection()
      @servers.bind "all", @render, this
      @servers.fetch()
      return

    onClose: ->
      @dialog.dialog "destroy"  if @dialog
      return

    initConfirmationDialog: ->
      @dialog or @$(".dialog-confirm-delete").dialog(
        modal: true
        title: "Delete server"
        dialogClass: "dialog-without-header"
        minHeight: 120
        width: 420
        autoOpen: false
      )

    back: ->
      @trigger "back"
      return

    deleteServer: (e) ->
      serverId = $(e.currentTarget).attr("data-server-id")
      server = @servers.get(serverId)
      self = this
      server.fetch().done ->
        $(".server-usage").html server.get("usage").length
        self.dialog.dialog "option", "buttons",
          Yes: ->
            self.servers.get(serverId).destroy()
            $(this).dialog "close"
            return

          No: ->
            $(this).dialog "close"
            return

        self.dialog.dialog "open"
        return

      return

    showAddServerForm: ->
      $addButton = @$("#add-server")
      $addButton.addClass "disabled"
      addServerView = new AddServerView(
        el: $("<div/>").appendTo(@$("#add-server-form"))
        collection: @servers
        projectId: @array.get("projectId")
      )
      addServerView.bind "closed", ->
        $addButton.removeClass "disabled"
        return

      addServerView.render()
      return

    showServer: (e) ->
      serverId = $(e.currentTarget).attr("data-server-id")
      server = @servers.get(serverId)
      view = new ServerUsageView(
        el: @el
        model: server
        array: @array
      )
      self = this
      view.bind "back", ->
        view.unbind()
        self.$el.empty()
        self.render()
        return

      return

    render: ->
      self = this
      $.when(genesis.fetchTemplate(@template)).done (tmpl) ->
        self.$el.html tmpl(
          array: self.array.toJSON()
          servers: self.servers.toJSON()
          canCreate: self.servers.canCreate()
          accessRights: self.servers.itemAccessRights()
        )
        self.delegateEvents self.events
        self.dialog = self.initConfirmationDialog()
        return

      return
  )
  AddServerView = Backbone.View.extend(
    template: "app/templates/settings/servers/add_server.html"
    events:
      "click .save-server": "saveServer"
      "click .cancel": "closeForm"

    initialize: (options) ->
      @projectId = options.projectId
      return

    saveServer: ->
      return  unless @$("form").valid()
      obj = new Servers.ServerModel(
        instanceId: @$("input[name='instanceId']").val()
        address: @$("input[name='address']").val()
        credentialsId: @$("#credentials").val()
      )
      self = this
      obj.bind "sync", ->
        self.closeForm()
        return

      obj.bind "sync error", ->
        self.$el.hideLoading()
        return

      validation.bindValidation obj, @$("form"), @status
      @$el.showLoading()
      @collection.create obj,
        wait: true

      return

    closeForm: ->
      self = this
      @$el.slideUp "fast", ->
        self.trigger "closed"
        self.close()
        return

      return

    render: ->
      credentials = new CredentialsRef([],
        projectId: @projectId
      )
      self = this
      $.when(genesis.fetchTemplate(@template), credentials.fetch()).done (tmpl) ->
        self.$el.html tmpl()
        $credsSelect = self.$("#credentials")
        credentials.each (cred) ->
          $credsSelect.append "<option value ='" + cred.get("id") + "'>" + cred.get("pairName") + "</option>"
          return

        $credsSelect.removeAttr "disabled"
        self.$el.slideDown "fast"
        self.status = new status.LocalStatus(el: self.$(".notification"))
        return

      return
  )
  ServerUsageView = Backbone.View.extend(
    template: "app/templates/settings/servers/server_usage.html"
    events:
      "click .back-to-server": "back"

    initialize: (options) ->
      @array = options.array
      $.when(@model.fetch()).done _.bind(@render, this)
      return

    back: ->
      @trigger "back"
      return

    render: ->
      self = this
      $.when(genesis.fetchTemplate(@template)).done (tmpl) ->
        self.$el.html tmpl(
          server: self.model.toJSON()
          serverArray: self.array.toJSON()
          utils: genesis.utils
        )
        return

      return
  )
  Servers

