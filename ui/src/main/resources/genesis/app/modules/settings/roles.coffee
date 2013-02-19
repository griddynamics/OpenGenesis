define [
  "genesis",
  "modules/status",
  "services/backend",
  "modules/validation",
  "backbone",
  "jquery",
  "jvalidate",
  "fcbcomplete"],
(genesis, status, backend, validation, Backbone, $) ->

  Roles = genesis.module()

  LANG =
    ROLE_GENESIS_ADMIN: "System Administrator"
    "ROLE_GENESIS_ADMIN.description": "Have full control over genesis application"
    ROLE_GENESIS_READONLY: "System Read-only"
    "ROLE_GENESIS_READONLY.description": "Have read-only access over all genesis application"
    ROLE_GENESIS_USER: "Genesis User"
    "ROLE_GENESIS_USER.description": "Can login into genesis application"
    ROLE_GENESIS_PROJECT_ADMIN: "Project Administrator"
    "ROLE_GENESIS_PROJECT_ADMIN.description": "Have full control over project<br/><br/>If environment security enabled, only project admin can create new environments and grant access to them to project users"
    ROLE_GENESIS_PROJECT_USER: "Project User"
    "ROLE_GENESIS_PROJECT_USER.description": "Have access to project environments<br/> <br/>if environment security disabled, can create and execute actions against all project's environments, <br/>otherwise access is restricted to specific environments"

  class Roles.Model extends genesis.Backbone.Model
    idAttribute: "name"
    linkType: backend.LinkTypes.Role

    initialize: (values, options) ->
      @projectId = options.projectId  if options.projectId
      @_editLink = _(options.links).find(@linkType.edit)

    url: ->
      if @projectId
        "rest/projects/" + @projectId + "/roles/" + @id
      else
        "rest/roles/" + @id

  class Roles.Views.Main extends Backbone.View
    events:
      "click .edit-link": "editRole"

    initialize: (options) ->
      _.bind @render, this
      @collection = new Backbone.Collection()
      rolesLoader = (if (options.projectId) then `_.partial(backend.AuthorityManager.projectRoles, options.projectId)` else backend.AuthorityManager.roles)
      $.when(rolesLoader()).done (roles) =>
        modelOptions = (if (options.projectId) then projectId: options.projectId else {})
        @roles = _(roles.items).map((item) ->
          new Roles.Model(name: item.name, `_.extend(modelOptions, {links: item.links})`)
        )
        @listView = new RolesList(
          collection: @collection
          el: @el
          projectId: options.projectId
          readonly: _(@roles).any((role) -> not role.canEdit())
        )
        @currentView = @listView
        @reloadRoles()

      @bind "opened", ->
        @reloadRoles()  if @currentView is @listView


    onClose: ->
      genesis.utils.nullSafeClose @currentView
      genesis.utils.nullSafeClose @listView

    reloadRoles: ->
      allLoad = _(@roles).map((item) ->
        item.fetch()
      )
      $.when.apply($, allLoad).done =>
        @collection.reset @roles
      .fail ->
        status.StatusPanel.error "Failed to load roles!"

    editRole: (event) ->
      roleName = $(event.currentTarget).attr("data-role-name")
      role = @listView.collection.get(roleName)
      @currentView = new RoleEdit(
        role: role
        el: @el
      )
      @currentView.bind "back", =>
        @currentView.unbind()
        @currentView.undelegateEvents()
        @currentView = @listView
        @reloadRoles()
        @render()

    render: ->
      @currentView.render()  if @currentView?

  class RolesList extends Backbone.View
    template: "app/templates/settings/roles/list.html"
    initialize: (options) ->
      @collection.bind "reset", @render, this
      @projectId = options.projectId
      @readonly = options.readonly || false

    render: ->
      $.when(genesis.fetchTemplate(@template)).done (tmpl) => #, this.collection.fetch()
        @$el.html tmpl(
          projectId: @projectId
          roles: @collection.toJSON()
          LANG: LANG,
          utils: genesis.utils,
          canEdit: not @readonly
        )

  class RoleEdit extends Backbone.View
    template: "app/templates/settings/roles/edit.html"
    events:
      "click a.back": "backToList"
      "click a.save": "saveChanges"

    initialize: (options) ->
      @role = options.role
      @title = options.title or LANG[@role.get("name")]
      @showButtons = (if options.showButtons isnt `undefined` then options.showButtons else true)
      @render()

    backToList: ->
      @trigger "back"

    pullGranties: ->
      users: @$("#users-select").val() or []
      groups: @$("#groups-select").val() or []

    saveChanges: ->
      $not_found = $('.holder tester:not(:empty)').text()?.replace /\s+/, ''
      if $not_found
        status.StatusPanel.error("Some changes will not be saved. Offending input is #{$not_found}. Clear it before saving or re-enter it with completion, please")
      else
        @role.
        save({users: @$("#users-select").val() or [], groups: @$("#groups-select").val() or []}, {suppressErrors: true}).
        done =>
          @backToList()
          status.StatusPanel.success "Changes have been saved"


    initCompletion: (hasGroups, hasUsers) ->
      @$("#groups-select").fcbkcomplete
        json_url: "rest/groups"
        cache: false
        filter_case: false
        filter_hide: true
        filter_selected: true
        newel: false
        width: ""
        input_name: "groups-select"
        complete_text: "Enter group name..."
        maxitems: 10000,
        input_min_size: 2

      @$("#users-select").fcbkcomplete
        json_url: "rest/users"
        cache: false
        filter_case: false
        filter_hide: true
        filter_selected: true
        newel: false
        width: ""
        input_name: "users-select"
        complete_text: "Enter username..."
        maxitems: 10000,
        input_min_size: 2


    render: ->
      self = this
      $.when(backend.UserManager.hasUsers(), backend.UserManager.hasGroups(), genesis.fetchTemplate(@template)).done (hasUsers, hasGroups, tmpl) =>
        @$el.html tmpl(
          role: @role.toJSON()
          LANG: LANG
          title: @title
          showButtons: self.showButtons,
          utils: genesis.utils
        )
        @initCompletion hasGroups[0], hasUsers[0]
        @status = new status.LocalStatus(el: self.$(".notification"))
        validation.bindValidation self.role, self.$("form"), @status

  Roles.Views.Edit = RoleEdit
  Roles

