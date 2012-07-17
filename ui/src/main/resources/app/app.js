require([
  "genesis",

  // Libs
  "jquery",
  "use!backbone",
  //services
  "services/backend",
  // Modules
  "modules/status",
  "modules/projects",
  "modules/environments",
  "modules/createenv",
  "modules/breadcrumbs",
  "modules/project_properties",
  "modules/settings/main",
//jquery plugins
  "use!bootstrap",
  "use!tabs"
],

function(genesis, jQuery, Backbone, backend, status, Projects, Environments, CreateEnvironment, Breadcrumbs, ProjectProperties, AppSettings) {

  var app = genesis.app;

  var Router = Backbone.Router.extend({

    currentView: { close: function(){} }, // null-object

    routes: {
      "": "index",
      "project/:projectId" : "environments",
      "project/:projectId/env/:envName": "environmentDetails",
      "project/:projectId/createEnv": "createEnvironment",
      "project/:projectId/properties": "createProjectProperties",
      "admin/create/project": "createProject",
      "settings": "listSettings",
      ":hash": "index"
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
      this.setCurrentView(new Projects.Views.CreateProject({el: this.$viewDiv(),
                                                            project: new Projects.Model({name: "", description: "", projectManager: ""})}));
    },

    createProjectProperties: function(projectId) {
      this.setCurrentView(new ProjectProperties.Views.Main({project: this.projects.get(projectId), el: this.$viewDiv()}));
    },

    environments: function(projectId) {
      var environments = new Environments.Collection({}, {"project" : this.projects.get(projectId)});
      var view = new Environments.Views.List({collection: environments, el: this.$viewDiv()});
      this.setCurrentView(view);
    },

    environmentDetails: function(projectId, envName) {
      genesis.app.trigger("page-view-loading-started");
      this.setCurrentView(new Environments.Views.Details({projectId: projectId, envName : envName, el: this.$viewDiv()}));
    },

    createEnvironment: function(projectId) {
      this.setCurrentView(new CreateEnvironment.Views.Main({project : this.projects.get(projectId), el: this.$viewDiv()}));
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

  jQuery(function($) {
    var errorDialog = $("<div style='margin-top: 10px' id='server-communication-error-dialog'></div>").dialog({
      modal: true,
      title: 'Failed to complete request',
      dialogClass: 'error-notification-dialog',
      width: 400,
      autoOpen: false,
      resizable: false
    });

    $.ajaxSetup({cache: false, statusCode: {
      404: function() {
        genesis.app.trigger("page-view-loading-completed");
        if (!errorDialog.dialog('isOpen')) {
          errorDialog.dialog("option", "buttons", {
              "Yes": function () {
                $(this).dialog("close");
                genesis.app.router.navigate("/", {trigger: true});
              },
              "No": function () {
                $(this).dialog("close");
              }
          });
          $("#server-communication-error-dialog").html("Requested resource does not exist.<br/><br/> Redirect to home page?");
          errorDialog.dialog('open');
        }
      },

      503: function() {
        genesis.app.trigger("page-view-loading-completed");
        if (!errorDialog.dialog('isOpen')) {
          errorDialog.dialog("option", "buttons", {});
          $("#server-communication-error-dialog").
            html("Backend service became unreachable (or took to long to respond). <br/><br/>" +
                 "Please try again later or contact administrator if the problem persists.");
          errorDialog.dialog('open');
        }
      }
    }});

    var userProjects = new Projects.Collection();

    var projectsDropdownTmpl = _.template($("#project-dropdown-list-tmpl").html());
    userProjects.bind("all", function () {
      $(".project-list").html(projectsDropdownTmpl({
        projects: userProjects.toJSON()
      }));
    });

    genesis.app.bind("projects-changed", function() {
      userProjects.fetch();
    });

    $.when(backend.UserManager.whoami(), userProjects.fetch()).done(function(user) {
      initCurrentUser(user[0]);
      app.router = new Router({projects: userProjects});
      app.breadcrumbs = new Breadcrumbs.View({router: app.router, projects: userProjects});


      app.router.bind('all', function (trigger, args) {
        var path = /project\/(\d*)/g.exec(Backbone.history.fragment);
        var currentProject = "Projects";
        if(path && path[1]) {
          currentProject = userProjects.get(path[1]).get('name');
        }
        $("#current-project").html(currentProject);
      });

      app.router.bind("all", status.StatusPanel.hide, status.StatusPanel);

      genesis.app.trigger("page-view-loading-completed");
      Backbone.history.start();
    }).fail(function(jxhr){
       if (jxhr.status == 403) {
          status.StatusPanel.error("You are not allowed to access this page");
          genesis.app.trigger("page-view-loading-completed");
       }
    });

    function initCurrentUser(user){
      app.currentUser = user;
      $('.user-name').text(user.user);
      if (user.administrator) {
        $(".system-settings").show();
      }

      if (user.logout_disabled) {
        $("#logout_elt .caret").remove();
        $("#logout_elt .dropdown-menu").remove();
      }
    }

    $("#connection-error").ajaxError(function(event, jqXHR, ajaxSettings, thrownError) {
      if (((jqXHR.status === 0 && jqXHR.statusText !== "abort") || jqXHR.status === 12029) && $(this).is(":hidden")) {
        $(this).show();
        pollServerStatus($(this));
      }
      if(jqXHR.status === 401) {
        window.location.href = "login.html?expire=true";
      }
    });

    function pollServerStatus($errorPanel) {
      setTimeout(function () {
        $.when(backend.UserManager.whoami()).fail(function (jqXHR) {
          if (jqXHR.status === 0) {
            pollServerStatus($errorPanel);
          } else {
            $errorPanel.text("Server communication error");
          }
        }).done(function() {
            $errorPanel.hide()
        })
      }, 5000)
    }

    var $loadingSpinner = $("#page-view-loading");
    genesis.app.bind("page-view-loading-started", function() {
      $loadingSpinner.show();
    });
    genesis.app.bind("page-view-loading-completed", function() {
      $loadingSpinner.hide();
    });
  });

  // All navigation that is relative should be passed through the navigate
  // method, to be processed by the router.  If the link has a data-bypass
  // attribute, bypass the delegation completely.
  $(document).on("click", "a:not([data-bypass])", function(evt) {
    var href = $(this).attr("href");
    var protocol = this.protocol + "//";
    if (href && href !== "#" &&
        href.slice(0, protocol.length) !== protocol &&
        href.indexOf("javascript:") !== 0) {
      evt.preventDefault();
      app.router.navigate(href, true);
    }
  });

});
