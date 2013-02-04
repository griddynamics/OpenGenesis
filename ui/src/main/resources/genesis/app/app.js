require([
  "genesis",

  // Libs
  "jquery",
  "backbone",
  "underscore",
  //services
  "services/backend",
  // Modules
  "modules/status",
  "modules/projects",
  "modules/environments",
  "modules/env_details/env_details",
  "modules/createenv",
  "modules/breadcrumbs",
  "modules/project_properties",
  "cs!modules/settings/main",
//jquery plugins
  "bootstrap",
  "tabs"
],

function(genesis, jQuery, Backbone, _, backend, status, Projects, Environments, EnvironmentDetails, CreateEnvironment, Breadcrumbs, ProjectProperties, AppSettings) {

  var app = genesis.app;

  var Router = Backbone.Router.extend({

    currentView: { close: function(){} }, // null-object

    routes: {
      "": "index",
      "project/:projectId" : "environments",
      "project/:projectId/inst/:envId": "environmentDetails",
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

  jQuery(function($) {
    var errorDialog = $("<div style='margin-top: 10px' id='server-communication-error-dialog'></div>").dialog({
      title: 'Failed to complete request',
      dialogClass: 'error-notification-dialog',
      width: 400,
      minHeight: 80
    });

    genesis.app.bind("server-communication-error", function(message, url) {
      if (!errorDialog.dialog('isOpen')) {
        $("#server-communication-error-dialog").html(message);
        errorDialog.dialog("option", "buttons", {
          "OK": function() {
            $(this).dialog("close");
            if(!_.isUndefined(url)) {
              if(_.isFunction(url)){
                url();
              } else {
                genesis.app.router.navigate(url, {trigger: true});
              }
            }
          }
        }).dialog('open');
      }
    });


    (function initializeErrorHandler(doc) {
      var errorHandler = {
        401: function (event, xhr, settings) {
          if(app.currentConfiguration.logout_disabled) {
            var retry = settings.retry || 1;
            if(retry < 2) {
              settings.retry = retry + 1;
              $.ajax(settings);
            } else {
              genesis.app.trigger("server-communication-error", "Server authentication has expired <br/><br/> Press OK to reload the page", function(){ location.reload(true); });
            }
          } else {
            window.location.href = "login.html?expire=true";
          }
        },

        403: function () {
          genesis.app.trigger("server-communication-error", "You don't have enough permissions to access this page", "/")
        },

        404: function () {
          genesis.app.trigger("server-communication-error", "Requested resource wasn't found", "/");
        },

        500: function (event, xhr, settings) {
          var errorMsg = "";
          try {
            var error = JSON.parse(xhr.responseText).error;
            errorMsg = "Internal server error: " + error;
          } catch (e) {
            errorMsg = "Internal server error occurred.";
          }
          genesis.app.trigger("server-communication-error", errorMsg + (!app.currentUser.administrator ? "<br/><br/> Please contact system administrator" : ""));
        },

        503: function () {
          if (!errorDialog.dialog('isOpen')) {
            errorDialog.dialog("option", "buttons", {});
            $("#server-communication-error-dialog").
              html("Backend service became unreachable (or took to long to respond). <br/><br/>" +
                "Please try again later or contact administrator if the problem persists.");
            errorDialog.dialog('open');
          }
        }
      };

      $(doc).ajaxError(function (event, xhr, settings) {
        if (!settings.suppressErrors) {
          genesis.app.trigger("page-view-loading-completed");
          (errorHandler[xhr.status] || function () {}) (event, xhr, settings);
        }
      });

      $.ajaxPrefilter(function( options, originalOptions, jqXHR ) {
        options.error = function (xhr, status, thrown) {
          if(xhr.status != 401 && originalOptions.error) {
            originalOptions.error(xhr, status, thrown);
          }
        }
      });

    })(document || {});

    $.when(backend.UserManager.whoami()).done(function(restRoot) {
      initCurrentUser(restRoot);
      Environments.fixFilter();

      var projectsUrlRoot = _(restRoot.links).find( backend.LinkTypes.Project.any );

      var userProjects = new Projects.Collection([], {url: projectsUrlRoot.href });

      userProjects.fetch().done(function() {
        var projectsDropdownTmpl = _.template($("#project-dropdown-list-tmpl").html());

        userProjects.bind("all", function () {
          $(".project-list").html(projectsDropdownTmpl({
            projects: userProjects.toJSON()
          }));
        });

        genesis.app.bind("projects-changed", function() {
          userProjects.fetch();
        });

        app.router = new Router({projects: userProjects});
        app.breadcrumbs = new Breadcrumbs.View({router: app.router, projects: userProjects});

        app.router.bind('all', function () {
          var path = /project\/(\d*)/g.exec(Backbone.history.fragment);
          var currentProject = "Projects";
          if(path && path[1] && userProjects.get(path[1])) {
            currentProject = userProjects.get(path[1]).get('name');
          }
          $("#current-project").html(currentProject);
        });

        genesis.app.trigger("page-view-loading-completed");
        Backbone.history.start();
      });
    }).fail(function(jxhr){
       if (jxhr.status == 403) {
          status.StatusPanel.error("You are not allowed to access this page");
          genesis.app.trigger("page-view-loading-completed");
       }
    });

    $.when(backend.SettingsManager.coreDetails()).done(function(details) {
      $(".core-details").data("details", details).text("v" + details.build.version + "-sha:" + details.revision.short)
    });

    $.when(backend.SettingsManager.distributionDetails()).done(function(details) {
      var hasVersion = details.build && details.build.version;
      var hasRevision = details.revision && details.revision.short;

      var info = details.name || 'Unknown';
      if (hasVersion) info = info + " v" + details.build.version;
      if (hasRevision) info = info + (hasVersion ? "-" : " ") + "sha:" + details.revision.short;

      $(".distr-details").data("details", details).text(info)
    });

    $(".genesis-version").click(function(e) {
      if (e.shiftKey && window) {
        window.prompt("Copy to clipboard: Ctrl+C, Enter",
          JSON.stringify({
            "core": $(".core-details").data("details") || null,
            "distribution": $(".distr-details").data("details") || null
          }));
        return false;
      }
    });

    function initCurrentUser(user){
      app.currentUser = user;
      app.currentConfiguration = user.configuration || {};

      var logoutLink = _(user.links).find(function(link) { return link.rel == "logout"});
      app.currentConfiguration["logout_disabled"] = _.isUndefined(logoutLink);

      var systemSettingsLink = _(user.links).find(backend.LinkTypes.SystemSettings.any);

      $('.user-name').text(user.user);
      if (systemSettingsLink) {
        $(".system-settings")
          .show()
          .attr("data-settings-url", systemSettingsLink.href);
      }

      $(document).on("focus", ".readonly input", function(){
        if (! $(this).is('[data-access-all]'))
          $(this).attr('disabled', 'disabled');
      });
      $(document).on("focus", ".readonly textarea", function(){
        if (! $(this).is('[data-access-all]'))
          $(this).attr('disabled', 'disabled');
      });
      $(document).on("mouseenter focus", ".readonly select", function(){
        if (! $(this).is('[data-access-all]'))
          $(this).attr('disabled', 'disabled');
      });

      if ( !logoutLink ) {
        $("#logout_elt a").css("cursor", "default");
        $("#logout_elt .caret").remove();
        $("#logout_elt .dropdown-menu").remove();
      } else {
        $(".logout-link").attr("href", logoutLink.href)
      }
    }

    $("#connection-error").ajaxError(function(event, jqXHR) {
      if (((jqXHR.status === 0 && jqXHR.statusText !== "abort") || jqXHR.status === 12029) && $(this).is(":hidden")) {
        if(jqXHR.statusText === "timeout") {
          genesis.app.trigger("page-view-loading-completed");
          genesis.app.trigger("server-communication-error", "Timeout. Server took too long to respond.")
        } else {
          $(this).show();
          pollServerStatus($(this));
        }
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
