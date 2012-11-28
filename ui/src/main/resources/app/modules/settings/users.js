define([
  "genesis",
  "use!backbone",
  "modules/status",
  "modules/validation",
  "services/backend",
  "jquery",
  "use!jvalidate"
],

function(genesis, Backbone, status, validation, backend, $) {
  var Users = genesis.module({Collections: {}});

  var URL = "rest/users";

  Users.Model = Backbone.Model.extend({
    urlRoot: URL,
    idAttribute: "username"
  });

  Users.Collections.Users = Backbone.Collection.extend({
    model: Users.Model,
    url: URL
  });

  Users.Collections.Groups = Backbone.Collection.extend({
    url: "rest/groups"
  });

  var EMPTY_USER = new Users.Model({
    username: "",
    firstName: "",
    lastName: "",
    email: "",
    jobTitle: "",
    password: ""
  });

  Users.Views.Main = Backbone.View.extend({

    events: {
      "click a.add-user": "addUser",
      "click a.edit-user": "editUser",
      "click a.delete-user": "deleteUser"
    },

    initialize: function () {
      _.bind(this.render, this);
      this.collection = new Users.Collections.Users();
      this.refresh();
    },

    refresh: function () {
      var self = this;
      this.collection.fetch().done(function () {
        self.listView = new UsersList({collection: self.collection, el: self.el});
        self.currentView = self.listView;
        self.render();
      });
    },

    onClose: function () {
      genesis.utils.nullSafeClose(this.currentView);
      genesis.utils.nullSafeClose(this.listView);
    },

    addUser: function () {
      var user = EMPTY_USER.clone();
      user.isNew = function () {
        return true;
      };
      this.currentView = new UsersEdit({user: user, groups: new Users.Collections.Groups(), el: this.el});
      var self = this;
      this.currentView.bind("back", function () {
        self.currentView.unbind();
        self.currentView.undelegateEvents();
        self.currentView = self.listView;
        self.render();
      });

    },

    editUser: function (event) {
      var username = event.currentTarget.getAttribute("data-index");
      var user = this.listView.collection.get(username);
      this.currentView = new UsersEdit({user: user, groups: new Users.Collections.Groups(), el: this.el});
      var self = this;
      this.currentView.bind("back", function () {
        self.currentView.unbind();
        self.currentView.undelegateEvents();
        self.currentView = self.listView;
        self.render();
      });

    },

    deleteUser: function (event) {
      var username = event.currentTarget.getAttribute("data-index");
      var self = this;
      self.showConfirmationDialog({
        "Yes": function () {
          $.when(self.collection.get(username).destroy()).done(function() {
            self.collection.fetch().done(function () {
              self.render();
            });
          });
          self.confirmationDialog.dialog("close");
        },
        "No": function () {
          self.confirmationDialog.dialog("close");
        }
      });
    },

    render: function () {
      if (this.currentView != null) {
        this.currentView.render();
      }
    },

    showConfirmationDialog: function (buttons) {
      if (!this.confirmationDialog) {
        this.confirmationDialog = this.$("#dialog-user").dialog({
          title: 'Confirmation'
        });
      }
      this.confirmationDialog.dialog("option", "buttons", buttons);
      this.confirmationDialog.dialog('open');
    }
  });

  var UsersList = Backbone.View.extend({
    template: "app/templates/settings/users/user_list.html",

    render: function() {
      var view = this;
      $.when(genesis.fetchTemplate(this.template), this.collection.fetch())
        .done(function (tmpl) {
          view.$el.html(tmpl({"users": view.collection.toJSON()}));
        });
    }
  });

  var UsersEdit = Backbone.View.extend({
    template: "app/templates/settings/users/edit_user.html",

    events: {
      "click a.back": "cancel",
      "click a.save": "saveUser"
    },

    initialize: function (options) {
      this.user = options.user;
      this.groups = options.groups;
      this.userRoles = [];
      this.userGroups = [];

      if (!this.user.isNew()) {
        var self = this;
        $.when(
          backend.AuthorityManager.getUserRoles(this.user.get('username')),
          backend.UserManager.getUserGroups(this.user.get('username')),
          this.groups.fetch()
        ).done(function (userRoles, userGroups) {
            self.userRoles = userRoles[0];
            self.userGroups = userGroups[0];
            self.render();
        });
      } else {
        this.groups.fetch().done(_.bind(this.render, this));
      }
    },

    render: function () {
      var self = this;
      $.when(genesis.fetchTemplate(this.template), backend.AuthorityManager.roles()).done(function (tmpl, availableRoles) {
        var userRolesLookupMap = genesis.utils.toBoolean(self.userRoles);
        var userGroupsLookupMap = genesis.utils.toBoolean(_(self.userGroups).pluck("id"));
        self.$el.html(tmpl({
          user: self.user.toJSON(),
          groups: self.groups.toJSON(),
          roles: availableRoles,
          userRoles: userRolesLookupMap,
          userGroups: userGroupsLookupMap
        }));
        self.status = new status.LocalStatus({el: self.$(".notification")});
        validation.bindValidation(self.user, self.$("#user-attributes"), self.status);
      });
    },

    saveUser: function () {
      if (!this.$("#user-attributes").valid()) {
        return;
      }

      var groups = $("input[name='groups']:checked").map(function () { return this.value; }).get();

      this.user.set({
        username:  $("input[name='username']").val(),
        firstName: $("input[name='firstName']").val(),
        lastName:  $("input[name='lastName']").val(),
        email:     $("input[name='email']").val(),
        jobTitle:  $("input[name='jobTitle']").val(),
        password:  $("input[name='password']").val(),
        groups:    groups
      });

      var user = this.user,
          self = this;
      $.when(user.save()).pipe(
        function success(){
          var roles = $("input[name='roles']:checked").map(function () {return this.value;}).get();
          if(!user.isNew() || roles.length > 0) {
            return backend.AuthorityManager.saveUserRoles(user.get("username"), roles);
          }
        },
        function error(jqXHR) {
          return new $.Deferred().reject({savingFail: true, error: jqXHR});
        }
      ).done(function (created) {
        status.StatusPanel.success("User Account " + (user.isNew() ? "created" : "saved"));
        self.backToList();
      }).fail(function (error) {
        if(!error.savingFail) {
          status.StatusPanel.error("User Account changes were saved, but ROLES changes were not applied");
          self.backToList();
        }
      });
    },

    cancel: function () {
      status.StatusPanel.hide();
      this.backToList();
    },

    backToList: function () {
      this.trigger("back");
    }
  });

  return Users;
});
