define([
  "genesis",
  "backbone",
  "modules/env_details/env_details"  //todo: dependency smell
],

function(genesis, Backbone, Environments) {

  /**
   * @const
   */
  var LANG = {
    "create_new_inst": "Create new instance",
    "project_list": "Project list",
    "system_settings": "System settings",
    "project_properties": "Properties",
    "create_project": "Create"
  };

  var Breadcrumbs = genesis.module();

  function _locationItem(url, title) {
    return {"url": url, "title": title};
  }

  var _homeLocation = _locationItem("/", LANG["project_list"]);

  var _settingsLocation = _locationItem("settings", LANG["system_settings"]);

  var _createProjectLocation = _locationItem("admin/create/project", LANG["create_project"]);

  Breadcrumbs.View = Backbone.View.extend ({
    el: '#breadcrumbs',
    template: _.template($("#breadcrumbs_list_template").html()),

    _project: function(projectId) {
      if (this.projectRepository.get(projectId)) {
        return _locationItem("project/" + projectId, this.projectRepository.get(projectId).get('name'));
      } else {
        return {};
      }
    },

    initialize: function (options) {
      _.bindAll(this);
      var router = options.router;
      this.projectRepository = options.projects;

      router.bind("route:index", this.projectsList);
      router.bind("route:projectProperties", this.projectProperties);
      router.bind("route:environments", this.environments);
      router.bind("route:environmentDetails", this.environmentsDetails);
      router.bind("route:workflowDetails", this.workflowDetails);
      router.bind("route:createEnvironment", this.createEnvironment);
      router.bind("route:listSettings", this.settings);
      router.bind("route:createProject", this.createProject);
      genesis.app.bind("breadcrumb:changed", this.updateLastLocation)
    },

    createEnvironment: function(projectId) {
      var locationList = [
        _homeLocation,
        this._project(projectId),
        _locationItem("project/" + projectId + "/createEnvInst", LANG["create_new_inst"])
      ];
      this.render(locationList)
    },

    createProject: function() {
      this.render([_homeLocation, _createProjectLocation]);
    },

    environments: function(projectId) {
      var locationList = [
        _homeLocation,
        this._project(projectId)
      ];
      this.render(locationList)
    },

    environmentsDetails: function(projectId, envId, workflowId) {
      var locationList = [_homeLocation, this._project(projectId)]
        , view = this
        , environment = new Environments.Model({id: envId, projectId: projectId});

      $.when(environment.fetch()).done(function () {
        locationList.push(_locationItem(Backbone.history.fragment, environment.get('name')));
        view.render(locationList)
      });
    },

    workflowDetails: function(projectId, envId, workflowId) {
      var locationList = [_homeLocation, this._project(projectId)]
        , view = this
        , environment = new Environments.Model({id: envId, projectId: projectId});

      $.when(environment.fetch()).done(function () {
        locationList.push(_locationItem("/project/" + projectId + "/inst/" + envId, environment.get('name')));
        locationList.push(_locationItem(Backbone.history.fragment, "Workflow details"));
        view.render(locationList)
      });
    },

    projectsList: function() {
      this.render([_homeLocation]);
    },

    projectProperties: function(projectId) {
      this.render([_homeLocation,
                   this._project(projectId),
                   _locationItem("/project/" + projectId + "/properties", LANG["project_properties"])]);
    },

    settings: function() {
      this.render([_settingsLocation]);
    },

    updateLastLocation: function(changed) {
      var locationList = [_homeLocation, this._project(changed.projectId), _locationItem(Backbone.history.fragment, changed.name)];
      this.render(locationList);
    },

    render: function(locationList) {
      this.$el.html(this.template({breadcrumbs: locationList}));
    }
  });


  return Breadcrumbs;
});
