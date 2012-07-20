define([
    "genesis",
    "use!backbone",
    "modules/settings/users",
    "modules/status",
    "services/backend",
    "jquery",
    "use!showLoading",
    "use!jvalidate"
],

function(genesis, Backbone, Users, status, backend, $) {
  var DEFAULT_TIMEOUT = 4000;

  var Groups = genesis.module();

  var URL = "/rest/groups";

  Groups.Model = Backbone.Model.extend({
    url: function () {
      return !this.id ? URL : URL + "/" + this.id;
    }
  });

  var GroupUsers = Backbone.Collection.extend({
    initialize: function(groupId) {
      this.groupId = groupId;
    },

    url: function () {
      return URL + "/" + this.groupId + "/users";
    }
  });

  var EmptyGroup = new Groups.Model({
    name: "",
    description: "",
    mailingList: ""
  });

  Groups.Views.Main = Backbone.View.extend({
    events: {
      "click .create-group": "createGroup",
      "click .edit-group": "editGroup",
      "click .delete-group": "deleteGroup"
    },

    initialize: function() {
      _.bind(this.render, this);
      this.collection = new Users.Collections.Groups();

      var self = this;
      this.collection.fetch().done(function() {
        self.listView = new GroupsList({collection: self.collection, el: self.el});
        self.currentView = self.listView;
        self.render();
      });
    },

    onClose: function() {
      if (this.confirmationDialog) {
        this.confirmationDialog.dialog('destroy').remove();
      }
      genesis.utils.nullSafeClose(this.currentView);
      genesis.utils.nullSafeClose(this.listView);
    },

    createGroup: function() {
      this.currentView = new GroupsEdit({group: EmptyGroup.clone(), el: this.el});
      var self = this;
      this.currentView.bind("back", function() {
        self.currentView.unbind();
        self.currentView.undelegateEvents();
        self.currentView = self.listView;
        self.render();
      });
    },

    editGroup: function(event) {
      var groupId = event.currentTarget.getAttribute("data-index");
      var group = this.listView.collection.get(groupId);
      this.currentView = new GroupsEdit({group: group, el: this.el});
      var self = this;
      this.currentView.bind("back", function() {
        self.currentView.unbind();
        self.currentView.undelegateEvents();
        self.currentView = self.listView;
        self.render();
      });
    },

    deleteGroup: function(event) {
      var self = this;
      var groupId = event.currentTarget.getAttribute("data-index");

      self.showConfirmationDialog({
        "Yes": function () {
          var model = self.collection.get(groupId);
          model.destroy({
            success: function(model, result) {
              self.collection.fetch().done(function() {
                self.render();
              });
            },
            error: function(model, result, xhr) {
              status.StatusPanel.error(xhr);
              self.render();
            }
          });
          self.confirmationDialog.dialog("close");
        },
        "No": function () {
          self.confirmationDialog.dialog("close");
        }
      });
    },

    render: function() {
      if (this.currentView != null) {
        this.currentView.render();
      }
    },

    showConfirmationDialog: function(buttons) {
      if (!this.confirmationDialog) {
        this.confirmationDialog = this.$("#dialog-confirm").dialog({
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

  var GroupsList = Backbone.View.extend({
    template: "app/templates/settings/groups/group_list.html",

    render: function() {
      var view = this;
      $.when(genesis.fetchTemplate(this.template), this.collection.fetch()).done(function (tmpl) {
        view.$el.html(tmpl({"groups": view.collection.toJSON()}));
      });
    }
  });

  var GroupsEdit = Backbone.View.extend({
    template: "app/templates/settings/groups/edit_group.html",

    events: {
      "click a.back" : "cancel",
      "click a.save" : "saveChanges"
    },

    initialize: function(options) {
      this.group = options.group;
      this.groupUsers = new GroupUsers(this.group.id);
      this.users = new Users.Collections.Users();
      this.groupRolesArray = [];

      var self = this;
      if (!this.group.isNew()) {
        $.when(
          this.groupUsers.fetch(), this.users.fetch(),
          backend.AuthorityManager.getGroupRoles(this.group.get('name'))
        ).done(function (a, b, groupRoles) {
          self.groupRolesArray = groupRoles[0];
          self.render();
        });
      } else {
        $.when(this.users.fetch()).done(_.bind(this.render, this));
      }
    },

    render: function(done) {
      var self = this;
      $.when(genesis.fetchTemplate(this.template), backend.AuthorityManager.roles()).done(function(tmpl, rolesArray) {

        var usersInGroupMap = genesis.utils.toBoolean(self.groupUsers.pluck('username'));
        var groupRolesMap = genesis.utils.toBoolean(_(self.groupRolesArray));

        self.$el.html(tmpl({
          "group" : self.group.toJSON(),
          "users" : self.users.toJSON(),
          "usersInGroup": usersInGroupMap,
          "roles": rolesArray,
          "groupRoles": groupRolesMap
        }));

        self.status = new status.LocalStatus({el: self.$(".notification")});
      });
    },

    saveChanges: function() {
      var self = this;
      if(!this.$("#group-attributes").valid()) {
        return;
      }

      var users = $("input[name='users']:checked").map(function () {return this.value;}).get();

      this.group.set({
        name: $("input[name='name']").val(),
        description: $("textarea[name='description']").val(),
        mailingList: $("input[name='mailingList']").val(),
        users: users
      });

      var isNew = this.group.isNew();
      $.when(this.group.save()).pipe(
        function success() {
          var roles = $("input[name='roles']:checked").map(function () {return this.value;}).get();
          if(!isNew || roles.length > 0) {
            return backend.AuthorityManager.saveGroupRoles(self.group.get('name'), roles);
          }
        },
        function error(error){
          return new $.Deferred().reject({savingFail: true, error: error});
        }
      ).done(function () {
        status.StatusPanel.success("Group is " + (isNew ? "created" : "saved"));
        self.backToList();
      }).fail(function (error) {
        if(error.savingFail) {
          self.status.error(error.error)
        } else {
          status.StatusPanel.error("Group changes were saved, but ROLES changes were not applied");
          self.backToList();
        }
      });
    },

    cancel: function() {
      status.StatusPanel.hide();
      this.backToList();
    },

    backToList: function() {
      this.trigger("back");
    }
  });

  return Groups;
});