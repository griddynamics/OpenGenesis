define [
  "genesis"
  "services/backend"
  "modules/status"
  "modules/validation"
  "backbone"
  "jquery"
  "jvalidate"
], (genesis, backend, status, validation, Backbone, $) ->
  Credentials = genesis.module()
  Credentials.Model = genesis.Backbone.Model.extend(
    linkType: backend.LinkTypes.Credentials
    urlRoot: ->
      projectId = @get("projectId")
      if projectId
        "rest/projects/" + @get("projectId") + "/credentials"
      else
        "rest/credentials"
  )
  Credentials.Collection = genesis.Backbone.Collection.extend(
    model: Credentials.Model
    projectId: null
    linkType: backend.LinkTypes.Credentials
    initialize: (elements, options) ->
      @projectId = options.projectId
      return

    url: ->
      if @projectId
        "rest/projects/" + @projectId + "/credentials"
      else
        "rest/credentials"
  )
  Credentials.Views.Main = Backbone.View.extend(
    events:
      "click #add-credentials": "createCredentials"

    initialize: (options) ->
      @projectId = options.projectId
      @collection = new Credentials.Collection({},
        projectId: @projectId
      )
      @listView = new CredentialsList(
        collection: @collection
        el: @el
      )
      @setCurrentView @listView
      @collection.fetch()
      return

    onClose: ->
      genesis.utils.nullSafeClose @currentView
      return

    createCredentials: ->
      @setCurrentView new CreateView(
        el: @el
        projectId: @projectId
        collection: @collection
      )
      self = this
      @currentView.bind "back", ->
        self.setCurrentView self.listView
        self.collection.fetch()
        return

      return

    setCurrentView: (view) ->
      if @currentView?
        @currentView.undelegateEvents()
        @currentView.unbind()
      @currentView = view
      @currentView.render()
      return

    render: ->
  )
  CredentialsList = Backbone.View.extend(
    template: "app/templates/settings/credentials/credentials_list.html"
    dialog: null
    events:
      "click .delete-credential": "deleteCredentials"

    initialize: (options) ->
      @collection.bind "reset", _.bind(@render, this)
      return

    onClose: ->
      @dialog.dialog "close"
      return

    initConfirmationDialog: ->
      @$("#dialog-confirm-delete").dialog title: "Confirmation"

    deleteCredentials: (elem) ->
      credentialId = $(elem.currentTarget).attr("data-credential-id")
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
          credentials: self.collection.toJSON()
          canCreate: self.collection.canCreate()
          accessRights: self.collection.itemAccessRights()
        )
        self.dialog = self.initConfirmationDialog()
        self.delegateEvents self.events
        return

      return
  )
  CreateView = Backbone.View.extend(
    template: "app/templates/settings/credentials/create.html"
    projectId: null
    events:
      "click .cancel": "cancel"
      "click #save-credentials": "create"

    initialize: (options) ->
      @projectId = options.projectId
      return

    cancel: ->
      @trigger "back"
      return

    create: ->
      return  unless @$("#install-credentials").valid()
      credentials = new Credentials.Model(
        projectId: parseInt(@projectId)
        cloudProvider: @$("input[name='cloudProvider']").val()
        pairName: @$("input[name='pairName']").val()
        identity: @$("input[name='identity']").val()
        credential: @$("textarea[name='credentials']").val()
      )
      validation.bindValidation credentials, @$("#install-credentials"), @status
      self = this
      credentials.save().done ->
        self.trigger "back"
        return

      return

    render: ->
      self = this
      $.when(genesis.fetchTemplate(@template)).done (tmpl) ->
        self.$el.html tmpl(canEdit: self.collection.canCreate())
        self.$("textarea[name='credentials']").on "keyup", ->
          $this = $(this)
          if $this.css("height") isnt "350px" and $this.val().indexOf("\n") isnt -1
            $this.animate
              height: "350px"
            , "fast"
          return

        self.status = new status.LocalStatus(el: self.$(".notification"))
        return

      return
  )
  Credentials

