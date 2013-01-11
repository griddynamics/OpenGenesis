define([
  "genesis",
  "services/backend",
  "cs!modules/settings/roles",
  "backbone",
  "jquery",
  "underscore",
  "jqueryui",
  "jvalidate"
],

function (genesis, backend, roles, Backbone, $, _) {
  var EnvironmentAccess = genesis.module();

  EnvironmentAccess.Model = Backbone.Model.extend({
    initialize: function(attr, options){
      this.envId = options.envId;
      this.projectId = options.projectId;
    },

    url: function() {
      return "rest/projects/" + this.projectId + "/envs/" + this.envId + "/access"
    },

    isNew: function() {
      return false;
    }
  });

  EnvironmentAccess.View = Backbone.View.extend({
    template: "app/templates/project_properties/env_configs/access_list.html",

    events: {
      "click a.modify-access" : "modifyAccess"
    },

    initialize: function(options){
      this.accessConfiguration = options.accessConfiguration;
      var self = this;
      if (genesis.app.currentConfiguration['environment_security_enabled']) {
        backend.AuthorityManager.haveAdministratorRights(options.projectId).done(function(isAdmin) {
          if (isAdmin) {
            options.tabHeader.show();
            $.when(self.accessConfiguration.fetch()).done(
              _.bind(self.render, self)
            );
          }
        });
      }
    },

    modifyAccess: function() {
      var editView = new roles.Views.Edit({
        el: this.el,
        role: this.accessConfiguration,
        title: "Grant access to environment"
      });
      var self = this;
      editView.bind("back", function() {
        editView.unbind();
        editView.undelegateEvents();
        $.when(self.accessConfiguration.fetch()).done(
          _.bind(self.render, self)
        );
      });
    },

    render: function(){
      var self = this;
      $.when(genesis.fetchTemplate(this.template)).done(function(tmpl) {
        self.$el.html(tmpl({
          accessConfiguration: self.accessConfiguration.toJSON(),
          utils: genesis.utils
        }));
      });
    }
  });

  return EnvironmentAccess;
});
