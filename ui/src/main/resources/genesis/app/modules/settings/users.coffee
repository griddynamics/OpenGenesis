define [
  "genesis",
  "backbone",
  "modules/status",
  "modules/validation",
  "services/backend",
  "jquery",
  "jvalidate"],
(genesis, Backbone, status, validation, backend, $) ->
  Users = genesis.module(Collections: {})

  URL = "rest/users"

  class Users.Model extends genesis.Backbone.Model
    urlRoot: URL
    idAttribute: "username"
    linkType: backend.LinkTypes.User

  class Users.Collections.People extends genesis.Backbone.Collection
    url: URL
    model: Users.Model
    linkType: backend.LinkTypes.User

  class Users.Collections.Groups extends genesis.Backbone.Collection
    url: "rest/groups"
    linkType: backend.LinkTypes.UserGroup

  EMPTY_USER = new Users.Model(
    username: ""
    firstName: ""
    lastName: ""
    email: ""
    jobTitle: ""
    password: ""
  )

  class Users.Views.Main extends Backbone.View
    events:
      "click a.add-user": "addUser"
      "click a.edit-user": "editUser"
      "click a.delete-user": "deleteUser"

    initialize: ->
      _.bind @render, this
      @collection = new Users.Collections.People()
      @refresh()

    refresh: ->
      @collection.fetch().done =>
        @listView = new UsersList(
          collection: @collection
          el: @el
        )
        @currentView = @listView
        @render()


    onClose: ->
      genesis.utils.nullSafeClose @currentView
      genesis.utils.nullSafeClose @listView

    addUser: ->
      user = EMPTY_USER.clone()
      user.isNew = ->
        true

      @currentView = new UsersEdit(
        user: user
        groups: new Users.Collections.Groups()
        usersCollection: @collection
        el: @el
      )
      @currentView.bind "back", =>
        @currentView.unbind()
        @currentView.undelegateEvents()
        @currentView = @listView
        @render()


    editUser: (event) ->
      username = event.currentTarget.getAttribute("data-index")
      user = @listView.collection.get(username)
      @currentView = new UsersEdit(
        user: user
        usersCollection: @listView.collection
        groups: new Users.Collections.Groups()
        el: @el
      )
      @currentView.bind "back", =>
        @currentView.unbind()
        @currentView.undelegateEvents()
        @currentView = @listView
        @render()


    deleteUser: (event) ->
      username = event.currentTarget.getAttribute("data-index")
      self = this
      self.showConfirmationDialog
        Yes: ->
          $.when(self.collection.get(username).destroy()).done ->
            self.collection.fetch().done ->
              self.render()


          self.confirmationDialog.dialog "close"

        No: ->
          self.confirmationDialog.dialog "close"


    render: ->
      @currentView.render()  if @currentView?

    showConfirmationDialog: (buttons) ->
      @confirmationDialog = @$("#dialog-user").dialog(title: "Confirmation")  unless @confirmationDialog
      @confirmationDialog.dialog "option", "buttons", buttons
      @confirmationDialog.dialog "open"

  class UsersList extends Backbone.View
    template: "app/templates/settings/users/user_list.html"
    render: ->
      $.when(genesis.fetchTemplate(@template), @collection.fetch()).done (tmpl) =>
        @$el.html tmpl(
          users: @collection.toJSON()
          canCreate: @collection.canCreate()
          accessRights: @collection.itemAccessRights()
        )

  class UsersEdit extends Backbone.View
    template: "app/templates/settings/users/edit_user.html"
    events:
      "click a.back": "cancel"
      "click a.save": "saveUser"

    initialize: (options) ->
      @user = options.user
      @groups = options.groups
      @userRoles = []
      @userGroups = []
      @usersCollection = options.usersCollection
      unless @user.isNew()
        $.when(backend.AuthorityManager.getUserRoles(@user.get("username")), backend.UserManager.getUserGroups(@user.get("username")), @groups.fetch()).done (userRoles, userGroups) =>
          @userRoles = userRoles[0]
          @userGroups = userGroups[0]
          @render()

      else
        @groups.fetch().done _.bind(@render, this)

    render: ->
      $.when(genesis.fetchTemplate(@template), backend.AuthorityManager.roles()).done (tmpl, availableRoles) =>
        userRolesLookupMap = genesis.utils.toBoolean(@userRoles)
        userGroupsLookupMap = genesis.utils.toBoolean(_(@userGroups).pluck("id"))
        @$el.html tmpl(
          user: @user.toJSON()
          groups: @groups.toJSON()
          roles: _(availableRoles.items).pluck("name")
          userRoles: userRolesLookupMap
          userGroups: userGroupsLookupMap
          canEdit: @user.canEdit() or @usersCollection.canCreate()
        )
        @status = new status.LocalStatus(el: @$(".notification"))
        validation.bindValidation @user, @$("#user-attributes"), @status


    saveUser: ->
      return  unless @$("#user-attributes").valid()
      groups = $("input[name='groups']:checked").map(->
        @value
      ).get()
      @user.set
        username: $("input[name='username']").val()
        firstName: $("input[name='firstName']").val()
        lastName: $("input[name='lastName']").val()
        email: $("input[name='email']").val()
        jobTitle: $("input[name='jobTitle']").val()
        password: $("input[name='password']").val()
        groups: groups

      user = @user
      $.when(user.save()).pipe(success = =>
        roles = $("input[name='roles']:checked").map(->
          @value
        ).get()
        backend.AuthorityManager.saveUserRoles user.get("username"), roles  if not user.isNew() or roles.length > 0
      , error = (jqXHR) ->
        new $.Deferred().reject
          savingFail: true
          error: jqXHR

      ).done((created) =>
        status.StatusPanel.success "User Account " + ((if user.isNew() then "created" else "saved"))
        @backToList()
      ).fail (error) =>
        unless error.savingFail
          status.StatusPanel.error "User Account changes were saved, but ROLES changes were not applied"
          @backToList()


    cancel: ->
      status.StatusPanel.hide()
      @backToList()

    backToList: ->
      @trigger "back"
  Users

