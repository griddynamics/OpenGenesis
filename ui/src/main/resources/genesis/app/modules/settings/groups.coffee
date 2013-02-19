define [
  "genesis",
  "backbone",
  "cs!modules/settings/users",
  "modules/status",
  "modules/validation",
  "services/backend",
  "jquery",
  "jvalidate"],
(genesis, Backbone, Users, status, validation, backend, $) ->
  Groups = genesis.module()
  URL = "rest/groups"

  class Groups.Model extends genesis.Backbone.Model
    urlRoot: URL
    linkType: backend.LinkTypes.UserGroup

  class GroupUsers extends Backbone.Collection
    initialize: (models, groupId) ->
      @groupId = groupId

    url: ->
      URL + "/" + @groupId + "/users"

  class GroupCollection extends Users.Collections.Groups
    model: Groups.Model

  EmptyGroup = new Groups.Model(
    name: ""
    description: ""
    mailingList: ""
  )

  class Groups.Views.Main extends Backbone.View
    events:
      "click .create-group": "createGroup"
      "click .edit-group": "editGroup"
      "click .delete-group": "deleteGroup"

    initialize: ->
      _.bind @render, this
      @collection = new GroupCollection()
      @collection.fetch().done =>
        @listView = new GroupsList(
          collection: @collection
          el: @el
        )
        @currentView = @listView
        @render()


    onClose: ->
      @confirmationDialog.dialog("destroy").remove()  if @confirmationDialog
      genesis.utils.nullSafeClose @currentView
      genesis.utils.nullSafeClose @listView

    createGroup: ->
      @currentView = new GroupsEdit(
        group: EmptyGroup.clone()
        groupCollection: @collection
        el: @el
      )
      @currentView.bind "back", =>
        @currentView.unbind()
        @currentView.undelegateEvents()
        @currentView = @listView
        @render()


    editGroup: (event) ->
      groupId = event.currentTarget.getAttribute("data-index")
      group = @listView.collection.get(groupId)
      @currentView = new GroupsEdit(
        group: group,
        groupCollection: @listView.collection,
        el: @el
      )
      @currentView.bind "back", =>
        @currentView.unbind()
        @currentView.undelegateEvents()
        @currentView = @listView
        @render()


    deleteGroup: (event) ->
      self = this
      groupId = event.currentTarget.getAttribute("data-index")
      self.showConfirmationDialog
        Yes: ->
          model = self.collection.get(groupId)
          model.destroy
            success: ->
              self.collection.fetch().done ->
                self.render()


            error: (model, result, xhr) ->
              status.StatusPanel.error xhr
              self.render()

          self.confirmationDialog.dialog "close"

        No: ->
          self.confirmationDialog.dialog "close"


    render: ->
      @currentView.render()  if @currentView?

    showConfirmationDialog: (buttons) ->
      @confirmationDialog = @$("#dialog-confirm").dialog(title: "Confirmation")  unless @confirmationDialog
      @confirmationDialog.dialog "option", "buttons", buttons
      @confirmationDialog.dialog "open"

  class GroupsList extends Backbone.View
    template: "app/templates/settings/groups/group_list.html"
    render: =>
      $.when(genesis.fetchTemplate(@template), @collection.fetch()).done (tmpl) =>
        @$el.html tmpl(
          groups: @collection.toJSON()
          canCreate: @collection.canCreate()
          accessRights: @collection.itemAccessRights()
        )

  class GroupsEdit extends Backbone.View
    template: "app/templates/settings/groups/edit_group.html"
    events:
      "click a.back": "cancel"
      "click a.save": "saveChanges"

    initialize: (options) ->
      @group = options.group
      @groupUsers = new GroupUsers([], @group.id)
      @users = new Users.Collections.People()
      @groupRolesArray = []
      @groupCollection = options.groupCollection

      unless @group.isNew()
        $.when(@groupUsers.fetch(), @users.fetch(), backend.AuthorityManager.getGroupRoles(@group.get("name"))).done (a, b, groupRoles) =>
          @groupRolesArray = groupRoles[0]
          @render()
      else
        $.when(@users.fetch()).done _.bind(@render, this)

    render: ->
      $.when(genesis.fetchTemplate(@template), backend.AuthorityManager.roles()).done (tmpl, rolesArray) =>
        usersInGroupMap = genesis.utils.toBoolean(@groupUsers.pluck("username"))
        groupRolesMap = genesis.utils.toBoolean(_(@groupRolesArray))
        @$el.html tmpl(
          group: @group.toJSON()
          users: @users.toJSON()
          usersInGroup: usersInGroupMap
          roles: _(rolesArray.items).pluck("name")
          groupRoles: groupRolesMap
          canEdit: @group.canEdit() or @groupCollection.canCreate()
        )
        @status = new status.LocalStatus(el: @$(".notification"))
        validation.bindValidation @group, @$("#group-attributes"), @status


    saveChanges: ->
      self = this
      return  unless @$("#group-attributes").valid()
      users = $("input[name='users']:checked").map(->
        @value
      ).get()
      @group.set
        name: $("input[name='name']").val()
        description: $("textarea[name='description']").val()
        mailingList: $("input[name='mailingList']").val()
        users: users

      isNew = @group.isNew()
      $.when(@group.save()).pipe(success = ->
        roles = $("input[name='roles']:checked").map(->
          @value
        ).get()
        backend.AuthorityManager.saveGroupRoles self.group.get("name"), roles  if not isNew or roles.length > 0
      , error = (error) ->
        new $.Deferred().reject
          savingFail: true
          error: error

      ).done(->
        status.StatusPanel.success "Group is " + ((if isNew then "created" else "saved"))
        self.backToList()
      ).fail (error) ->
        unless error.savingFail
          status.StatusPanel.error "Group changes were saved, but ROLES changes were not applied"
          self.backToList()


    cancel: ->
      status.StatusPanel.hide()
      @backToList()

    backToList: ->
      @trigger "back"

  Groups

