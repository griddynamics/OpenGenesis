define([
  "genesis",
  "use!backbone",
  "modules/environments"  //todo: dependency smell
],

function(genesis, Backbone, Environments) {

  /**
   * @const
   */
  var LANG = {
    "create_new_env": "Create new environment",
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
      router.bind("route:createProjectProperties", this.projectProperties);
      router.bind("route:environments", this.environments);
      router.bind("route:environmentDetails", this.environmentsDetails);
      router.bind("route:createEnvironment", this.createEnvironment);
      router.bind("route:listSettings", this.settings);
      router.bind("route:createProject", this.createProject);
    },

    createEnvironment: function(projectId) {
      var locationList = [
        _homeLocation,
        this._project(projectId),
        _locationItem("project/" + projectId + "/createEnv", LANG["create_new_env"])
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

    environmentsDetails: function(projectId, envName) {
      var locationList = [_homeLocation, this._project(projectId)];
      var view = this;
      var environments = new Environments.Collection({}, {project: this.projectRepository.get(projectId)});
      $.when(environments.fetch()).done(function () {
        if (environments.get(envName)) {
          locationList.push(_locationItem(Backbone.history.fragment, environments.get(envName).get('name')));
        }
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

    render: function(locationList) {
      this.$el.html(this.template({breadcrumbs: locationList}));
    }
  });


  return Breadcrumbs;
});
