define([
  "genesis",
  "use!backbone",
  "modules/status",
  "services/backend",
  "jquery",
  "use!showLoading",
  "use!jvalidate"
],

function(genesis, Backbone, status, backend, $) {
  var Users = genesis.module({Collections: {}});
  var URL = "/rest/users";

  Users.Model = Backbone.Model.extend({
    url: function () {
      return this.isNew() ? URL : URL + "/" + this.id;
    },
    idAttribute: "username"
  });

  Users.Collections.Users = Backbone.Collection.extend({
    model: Users.Model,
    url: URL
  });

  Users.Collections.Groups = Backbone.Collection.extend({
    url: "/rest/groups"
  });

  var EmptyUser = new Users.Model({
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
      var user = EmptyUser.clone();
      user.isNew = function () {
        return true;
      }
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
          backend.UserManager.removeUser(username);
          self.collection.fetch().done(function () {
            self.render();
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
          resizable: true,
          modal: true,
          title: 'Confirmation',
          dialogClass: 'dialog-without-header',
          minHeight: 120,
          width: 420,
          autoOpen: false
        });
      }
      this.confirmationDialog.dialog("option", "buttons", buttons);
      this.confirmationDialog.dialog('open');
    }
  });

  var UsersList = Backbone.View.extend({
    template: "app/templates/settings/users/user_list.html",

    render: function(done) {
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

      this.groups.fetch().done(_.bind(this.render, this));

      if (!this.user.isNew()) {
        var self = this;
        $.when(
          backend.AuthorityManager.getUserRoles(this.user.get('username')),
          backend.UserManager.getUserGroups(this.user.get('username'))
        ).done(function (userRoles, userGroups) {
            self.userRoles = userRoles[0];
            self.userGroups = userGroups[0];
            self.render();
        });
      }
    },

    render: function (done) {
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
        self.status = new status.LocalStatus({el: self.$(".notification")})
      });
    },

    saveUser: function () {
      if (!this.$("#user-attributes").valid()) {
        return;
      }

      var groups = $("input[name='groups']:checked").map(function () { return this.value; }).get();

      this.user.set({
        username: $("input[name='name']").val(),
        firstName: $("input[name='first_name']").val(),
        lastName: $("input[name='last_name']").val(),
        email: $("input[name='e-mail']").val(),
        jobTitle: $("input[name='job_title']").val(),
        groups: groups,
        password: $("input[name='password']").val()
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
        if(error.savingFail) {
          self.status.error(error.error)
        } else {
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
