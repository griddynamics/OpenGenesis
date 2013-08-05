define ["genesis", "backbone", "cs!modules/settings/plugins", "cs!modules/settings/configs", "cs!modules/settings/groups", "cs!modules/settings/users", "cs!modules/settings/roles", "cs!modules/settings/databags", "cs!modules/settings/agents", "cs!modules/dashboard/running", "services/backend"], (genesis, Backbone, Plugins, SystemConfigs, Groups, Users, Roles, Databags, Agents, Running, backend) ->
  AppSettings = genesis.module()

  class AppSettings.SystemSettings extends Backbone.Model
    initialize: (project) -> @project = project

    url: "rest/settings/root"


  class AppSettings.SystemActions extends Backbone.Model
    url: "rest/system/root"


  class AppSettings.Views.Main extends Backbone.View
    template: "app/templates/settings.html"
    pluginsView: null
    configsView: null
    groupsView: null
    usersView: null
    agentsView: null
    events:
      "click #plugin-panel-tab-header": "showPluginsTab"
      "click #settings-panel-tab-header": "showSettings"
      "click #group-panel-tab-header": "showGroupsTab"
      "click #user-panel-tab-header": "showUsersTab"
      "click #roles-panel-tab-header": "showRolesTab"
      "click #databags-panel-tab-header": "showDatabags"
      "click #agents-panel-tab-header": "showAgents"
      "click #system-restart": "restartSystem"
      "click #system-stop": "stopSystem"

    initialize: (options) ->
      @model = new AppSettings.SystemSettings
      @actionsModel = new AppSettings.SystemActions
      @confirmations = {}

    onClose: ->
      genesis.utils.nullSafeClose @pluginsView
      genesis.utils.nullSafeClose @configsView
      genesis.utils.nullSafeClose @groupsView
      genesis.utils.nullSafeClose @usersView
      genesis.utils.nullSafeClose @rolesView
      genesis.utils.nullSafeClose @databagsView
      genesis.utils.nullSafeClose @agentsView

    showPluginsTab: ->
      unless @pluginsView?
        @pluginsView = new Plugins.Views.Main(
          el: @$("#plugin-panel")
          main: this
        )

    showSettings: ->
      unless @configsView?
        @configsView = new SystemConfigs.Views.Main(
          el: @$("#config-panel")
          main: this
        )
      @toggleRestart()

    showGroupsTab: ->
      @groupsView = new Groups.Views.Main(el: @$("#group-panel"))  unless @groupsView?

    showUsersTab: ->
      @usersView = new Users.Views.Main(el: @$("#user-panel"))  unless @usersView?

    showRolesTab: ->
      unless @rolesView?
        @rolesView = new Roles.Views.Main(el: @$("#roles-panel"))
      else
        @rolesView.trigger "opened"

    showDatabags: ->
      @databagsView = new Databags.Views.Main(el: @$("#databags-panel"))  unless @databagsView?

    showAgents: ->
      @agentsView = new Agents.Views.Main(el: @$("#agents-panel")) unless @agentsView?

    toggleRestart: ->
      $.when(backend.SettingsManager.restartRequired()).done (restart) =>
        @$("#restart").toggle restart

    adviceStop: (elem, elemRunning, action)->
      stat = new Running.JobStats
      $.when(stat.fetch()).done ()=>
        if (stat.size() > 0)
          @showConfirm("#" + elemRunning, action, () =>
            genesis.app.router.navigate('jobs_running', {trigger: true});
          )
        else
          @showConfirm("#" + elem, action)

    restartSystem: ->
      @adviceStop('dialog-confirm-system-restart', 'dialog-confirm-system-restart-running', backend.SystemManager.restart)

    stopSystem: ->
      @adviceStop('dialog-confirm-system-stop',  'dialog-confirm-system-stop-running', backend.SystemManager.stop)

    showConfirm: (id, action, list) ->
      unless @confirmations[id]
        buttons =
          Yes: ->
            action()
            $(this).dialog "close"
          No: ->
            $(this).dialog "close"
        if list?
          buttons['Jobs'] = () ->
            $(this).dialog "close"
            list()
        @confirmations[id] = @$(id).dialog(
          title: "Confirmation"
          buttons: buttons
        )
      @confirmations[id].dialog "open"

    render: ->
      $.when(@model.fetch(), @actionsModel.fetch(), genesis.fetchTemplate(@template)).done (linksAggregator, actions, tmpl) =>
        typeToLink = _(@model.get("links")).groupBy 'type'
        actionLinks = @actionsModel.get("links")
        @$el.html tmpl(
          users: typeToLink[backend.LinkTypes.User.name]
          groups: typeToLink[backend.LinkTypes.UserGroup.name]
          roles: typeToLink[backend.LinkTypes.Role.name]
          plugins: typeToLink[backend.LinkTypes.Plugin.name]
          databags: typeToLink[backend.LinkTypes.DataBag.name]
          agents: typeToLink[backend.LinkTypes.RemoteAgent.name]
          restart: _(actionLinks).find((l) => l.href.indexOf('restart') != -1)
          stop: _(actionLinks).find((l) => l.href.indexOf('stop') != -1)
        )
        @showSettings()

  AppSettings

