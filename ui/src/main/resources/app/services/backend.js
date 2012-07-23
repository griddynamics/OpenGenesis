define([
  "genesis",
  "jquery"
],

function(genesis, $) {
  /**
   * @const
   */
  var DEFAULT_TIMEOUT = 30000;

  var backend = genesis.module();

  backend.WorkflowManager = {

    /**
     * @param {string} environment name of the environment
     */
    cancelWorkflow: function(projectId, environment) {
      return $.ajax({
        url: "/rest/projects/" + projectId +  "/envs/" + environment + "/actions",
        type: "POST",
        dataType: "json",
        data: JSON.stringify({action: 'cancel'}),
        timeout: DEFAULT_TIMEOUT
      });
    },

    /**
     *
     * @param {string} environment environment name
     * @param {string} workflow workflow name
     * @param variables workflow vars to be sent
     */
    executeWorkflow: function(projectId, environment, workflow, variables) {
      return $.ajax({
        url: '/rest/projects/' + projectId + '/envs/' + environment + '/actions',
        dataType: "json",
        type: "POST",
        data: JSON.stringify({
          action: 'execute',
          parameters: {
            workflow: workflow,
            variables: variables
          }
        }),
        timeout: DEFAULT_TIMEOUT,
        processData: false
      });
    }
  };

  backend.ProjectManager = {
    removeProject: function(projectId, success, error) {
      $.ajax({
        url: "/rest/projects/" + projectId,
        dataType: "json",
        type: "DELETE",
        timeout: DEFAULT_TIMEOUT,
        success: success,
        error: error
      });
    }
  };

  backend.EnvironmentManager = {
    createEnvironment: function(projectId, environment) {
      return $.ajax({
        url: "/rest/projects/" + projectId + "/envs",
        dataType: "json",
        type: "POST",
        data: JSON.stringify(environment.toJSON()),
        timeout: DEFAULT_TIMEOUT,
        processData: false
      });
    },

    resetEnvStatus: function(projectId, environment) {
      return $.ajax({
        url: "/rest/projects/" + projectId + "/envs/" + environment + "/actions",
        dataType: "json",
        type: "POST",
        data: JSON.stringify({action: 'resetEnvStatus'}),
        timeout: DEFAULT_TIMEOUT
      });
    }
  };

  backend.UserManager = {
    hasUsers: function() {
      return $.ajax({
        url: "/rest/users?available",
        dataType: "json",
        type: "GET",
        timeout: DEFAULT_TIMEOUT
      });
    },
    hasGroups: function() {
      return $.ajax({
        url: "/rest/groups?available",
        dataType: "json",
        type: "GET",
        timeout: DEFAULT_TIMEOUT
      });
    },

    whoami: function() {
      return $.ajax({
        url: "/rest/whoami",
        dataType: "json",
        type: "GET",
        timeout: DEFAULT_TIMEOUT
      });
    },

      removeUser: function(username) {
          return $.ajax({
              url: "/rest/users/" + username,
              dataType: "json",
              type: "DELETE",
              timeout: DEFAULT_TIMEOUT,
              processData: true
          })
      },

    getUserGroups: function(username) {
      return $.ajax({
        url: "/rest/users/" + username + "/groups",
        dataType: "json",
        type: "GET",
        timeout: DEFAULT_TIMEOUT,
        processData: true
      });
    }
  };

  backend.AuthorityManager = {
    roles: function() {
      var def = new $.Deferred();
      if (this._rolesListCache) {
        return def.resolve(this._rolesListCache);
      }

      var self = this;
      $.ajax({
        url: "/rest/roles",
        dataType: "json",
        type: "GET",
        timeout: DEFAULT_TIMEOUT,
        processData: true
      }).done(function(roles) {
        self._rolesListCache = roles;
        def.resolve(self._rolesListCache);
      }).fail(function(jqXHR) {
        def.reject(jqXHR)
      });

      return def.promise();
    },

    projectRoles: function() {
      var def = new $.Deferred();
      if (this._projectRolesListCache) {
        return def.resolve(this._projectRolesListCache);
      }

      var self = this;
      $.ajax({
        url: "/rest/projectRoles",
        dataType: "json",
        type: "GET",
        timeout: DEFAULT_TIMEOUT,
        processData: true
      }).done(function(roles) {
          self._projectRolesListCache = _(roles).pluck("name");
          def.resolve(self._projectRolesListCache);
        }).fail(function(jqXHR) {
          def.reject(jqXHR)
        });

      return def.promise();
    },

    saveUserRoles: function(username, roles) {
      return $.ajax({
        url: "/rest/users/" + username +  "/roles",
        dataType: "json",
        type: "PUT",
        data: JSON.stringify(roles),
        timeout: DEFAULT_TIMEOUT
      });
    },

    saveGroupRoles: function(groupName, roles) {
      return $.ajax({
        url: "/rest/groups/" + groupName +  "/roles",
        dataType: "json",
        type: "PUT",
        data: JSON.stringify(roles),
        timeout: DEFAULT_TIMEOUT
      });
    },

    getGroupRoles: function(groupName) {
      return $.ajax({
        url: "/rest/groups/" + groupName + "/roles",
        dataType: "json",
        type: "GET",
        timeout: DEFAULT_TIMEOUT,
        processData: true
      });
    },

    haveAdministratorRights: function(projectId) {
      var def = new $.Deferred();
      $.ajax({
        url: "/rest/projects/" + projectId + "/permissions",
        dataType: "json",
        type: "GET",
        timeout: DEFAULT_TIMEOUT,
        processData: true
      }).done(function(permissions) {
          def.resolve(_(permissions).indexOf("ROLE_GENESIS_PROJECT_ADMIN") != -1)
        }).fail(function(jqXHR) {
          def.reject(jqXHR)
        });
      return def;
    },

    getUserRoles: function(username) {
      return $.ajax({
        url: "/rest/users/" + username + "/roles",
        dataType: "json",
        type: "GET",
        timeout: DEFAULT_TIMEOUT,
        processData: true
      });
    }

  };

  return backend;
});
