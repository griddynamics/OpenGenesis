define([
  "genesis",
  "use!backbone",
  "modules/settings/plugins",
  "modules/settings/configs",
  "modules/settings/groups",
  "modules/settings/users",
  "modules/settings/roles",
  "modules/settings/databags",
  "services/backend"
],

function(genesis, Backbone, Plugins, SystemConfigs, Groups, Users, Roles, Databags, backend) {

  var AppSettings = genesis.module();

  AppSettings.Views.Main = Backbone.View.extend({
    template: "app/templates/settings.html",
    pluginsView: null,
    configsView: null,
    groupsView: null,
    usersView: null,

    events: {
      "click #plugin-panel-tab-header": "showPluginsTab",
      "click #settings-panel-tab-header": "showSettings",
      "click #group-panel-tab-header": "showGroupsTab",
      "click #user-panel-tab-header": "showUsersTab",
      "click #roles-panel-tab-header": "showRolesTab",
      "click #databags-panel-tab-header": "showDatabags"
    },

    onClose: function() {
      genesis.utils.nullSafeClose(this.pluginsView);
      genesis.utils.nullSafeClose(this.configsView);
      genesis.utils.nullSafeClose(this.groupsView);
      genesis.utils.nullSafeClose(this.usersView);
      genesis.utils.nullSafeClose(this.rolesView);
      genesis.utils.nullSafeClose(this.databagsView);
    },

    showPluginsTab: function() {
      if(this.pluginsView == null) {
        this.pluginsView = new Plugins.Views.Main({el: this.$("#plugin-panel")});
      }
    },

    showSettings: function() {
      if(this.configsView  == null) {
        this.configsView = new SystemConfigs.Views.Main({el: this.$("#config-panel")});
      }
    },

    showGroupsTab: function() {
      if(this.groupsView  == null) {
        this.groupsView = new Groups.Views.Main({el: this.$("#group-panel")});
      }
    },

    showUsersTab: function() {
      if(this.usersView  == null) {
        this.usersView = new Users.Views.Main({el: this.$("#user-panel")});
      }
    },

    showRolesTab: function() {
      if(this.rolesView == null) {
        this.rolesView = new Roles.Views.Main({el: this.$("#roles-panel")});
      } else {
        this.rolesView.trigger("opened");
      }
    },

    showDatabags: function() {
      if(this.databagsView == null) {
        this.databagsView = new Databags.Views.Main({el: this.$("#databags-panel")});
      }
    },

    render: function() {
      var view = this;
      $.when(backend.UserManager.hasUsers(), backend.UserManager.hasGroups(), genesis.fetchTemplate(this.template))
      .done(function(hasUsers, hasGroups, tmpl) {
        view.$el.html( tmpl({users: hasUsers[0], groups: hasGroups[0]}) );
        view.showSettings();
      });
    }
  });

  return AppSettings;
});
