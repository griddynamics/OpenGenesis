define([
  "genesis",

  // Libs
  "backbone",
  "underscore",
  // Modules
  "modules/projects",
  "modules/environments",
  "modules/env_details/env_details",
  "modules/createenv",
  "modules/project_properties",
  "cs!modules/settings/main",
//jquery plugins
  "bootstrap",
  "tabs"
],

function(genesis, Backbone, _, Projects, Environments, EnvironmentDetails, CreateEnvironment, ProjectProperties, AppSettings) {

  var rmodule = genesis.module();

  rmodule.Router = Backbone.Router.extend({

    currentView: { close: function(){} }, // null-object

    routes: {
      "": "index",
      "project/:projectId" : "environments",
      "project/:projectId/inst/:envId": "environmentDetails",
      "project/:projectId/inst/:envId/workflow/:wfId": "workflowDetails",
      "project/:projectId/createEnvInst": "createEnvironment",
      "project/:projectId/properties": "projectProperties",
      "admin/create/project": "createProject",
      "settings": "listSettings",
      ":hash": "index",
      "*invalid": "index"
    },

    initialize: function(options) {
      this.projects = options.projects;
      this.$wrapperContent = $("#wrapper-content");
    },

    setCurrentView: function(view) {
      this.currentView.close();
      this.currentView = view;
      this.currentView.render();
    },

    $viewDiv: function(){
      return $("<div/>").appendTo(this.$wrapperContent);
    },

    createProject: function(){
      var view = new Projects.Views.CreateProject({
        el: this.$viewDiv(),
        project: new Projects.Model({name: "", description: "", projectManager: ""})
      });
      this.setCurrentView(view);
    },

    projectProperties: function(projectId) {
      var project = this.projects.get(projectId);
      if(project){
        var self = this;
        $.when(project.fetch()).done(function(){
          self.setCurrentView(new ProjectProperties.Views.Main({project: project, el: self.$viewDiv()}));
        });
      } else {
        genesis.app.trigger("server-communication-error", "Requested project wasn't found", "/")
      }
    },

    environments: function(projectId) {
      if(this.projects.get(projectId)){
        var self = this;
        var project = this.projects.get(projectId);
        project.fetch().done(function() {
          var environments = new Environments.Collection({}, {"project" : project});
          var view = new Environments.Views.List({collection: environments, el: self.$viewDiv()});
          self.setCurrentView(view);
        })
      } else {
        genesis.app.trigger("server-communication-error", "Requested project wasn't found", "/")
      }
    },

    workflowDetails: function(projectId, envId, workflowId) {
      this.setCurrentView(
        new EnvironmentDetails.Views.SingleWorkflowView({projectId: projectId, envId : envId, el: this.$viewDiv(), workflowId: parseInt(workflowId)})
      );
    },

    environmentDetails: function(projectId, envId) {
      genesis.app.trigger("page-view-loading-started");
      this.setCurrentView(new EnvironmentDetails.Views.Details({projectId: projectId, envId : envId, el: this.$viewDiv()}));
    },

    createEnvironment: function(projectId) {
      if(this.projects.get(projectId)){
        this.setCurrentView(new CreateEnvironment.Views.Main({project : this.projects.get(projectId), el: this.$viewDiv()}));
      } else {
        genesis.app.trigger("server-communication-error", "Requested project wasn't found", "/")
      }
    },

    listSettings: function() {
      this.setCurrentView(new AppSettings.Views.Main({el: this.$viewDiv()}));
    },

    index: function(hash) {
      var route = this;
      this.currentView.close();
      this.currentView = new Projects.Views.ProjectsOverview({collection: this.projects});

      this.currentView.render(function(el) {
        route.$wrapperContent.html(el);
        // Fix for hashes in pushState and hash fragment
        if (hash && !route._alreadyTriggered) {
          Backbone.history.navigate("", false);
          location.hash = hash;
          // Set an internal flag to stop recursive looping
          route._alreadyTriggered = true;
        }
      });
    }
  });

  return rmodule;
});