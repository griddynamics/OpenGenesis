define([
  "genesis",
  "backbone",
  "services/backend",
  "modules/projects",
  "modules/project_properties/env_configurations",
  "modules/project_properties/credentials",
  "modules/project_properties/servers",
  "cs!modules/settings/roles",
  "cs!modules/settings/databags",
  "modules/project_properties/template_repo"
],

function(genesis, Backbone, backend, Projects, EnvConfigs, Credentials, Servers, Roles, Databags, TemplateRepo) {
  var ProjectProperties = genesis.module();

  ProjectProperties.Views.Main = Backbone.View.extend({
    template: "app/templates/project_properties.html",

    events: {
      "click #main-settings-tab" : "showMainSettings",
      "click #credentials-settings-tab" : "showCredentialsSettings",
      "click #project-roles-tab" : "showProjectRoles",
      "click #project-servers-tab" : "showServers",
      "click #project-databags-tab" : "showProjectDatabags",
      "click #project-templaterepo-tab" : "showProjectTemplateRepo",
      "click #environments-configurations-tab" : "showEnvironmentConfigurations"
    },

    initialize: function(options) {
      this.project = options.project;

      this.tabs = {
        mainSettingsView: null,
        credentialsView: null,
        otherSettings: null,
        projectRolesView: null,
        serversView: null,
        templateRepoView: null
      }
    },

    onClose: function() {
      _(this.tabs).values(function(view) { genesis.utils.nullSafeClose(view); } );
    },

    showCredentialsSettings: function() {
      if (this.tabs.credentialsView == null) {
        this.tabs.credentialsView = new Credentials.Views.Main({el: this.$("#credentials-settings-panel"), projectId: this.project.id});
      }
    },

    showMainSettings: function() {
      if (this.tabs.mainSettingsView == null) {
        this.tabs.mainSettingsView = new Projects.Views.CreateProject({el: this.$("#main-settings-panel"), project: this.project});
      }
    },

    showProjectDatabags: function() {
      if (this.tabs.otherSettingsView == null) {
        this.tabs.otherSettingsView = new Databags.Views.Main({el: this.$("#project-databags-panel"), projectId: this.project.id});
      }
    },

    showProjectRoles: function() {
      if(this.tabs.projectRolesView == null) {
        this.tabs.projectRolesView = new Roles.Views.Main({el: this.$("#project-roles-panel"), projectId: this.project.id});
      }
    },

    showServers: function() {
      if(this.tabs.serversView == null) {
        this.tabs.serversView = new Servers.Views.Main({el: this.$("#project-servers-panel"), projectId: this.project.id});
      }
    },

    showProjectTemplateRepo: function() {
      if(this.tabs.templateRepoView == null) {
        this.tabs.templateRepoView = new TemplateRepo.Views.Main({el: this.$("#project-templaterepo-panel"), projectId: this.project.id});
      }
    },

    showEnvironmentConfigurations: function() {
      if(this.tabs.envConfigView == null) {
        this.tabs.envConfigView = new EnvConfigs.Views.Main({el: this.$("#environments-config-panel"), projectId: this.project.id});
      }
    },

    render: function() {
      var self = this;
      $.when(backend.AuthorityManager.haveAdministratorRights(this.project.id), genesis.fetchTemplate(self.template)).done(function(isAdmin, tmpl){
        if (!isAdmin) {
          genesis.app.trigger("server-communication-error", "You don't have enough permissions to access this page", "/project/" + self.project.id);
          self.delegateEvents({})
        } else {
          self.$el.html(tmpl());
          self.showMainSettings();
        }
      });
    }
  });

  return ProjectProperties;
});
