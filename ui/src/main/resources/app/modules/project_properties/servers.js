define([
  "genesis",
  "modules/status",
  "use!backbone",
  "jquery",
  "use!showLoading",
  "use!jvalidate"

],

function(genesis, status, Backbone, $) {
  var Servers = genesis.module();

  Servers.ArrayModel = Backbone.Model.extend({

    url: function() {
      return "/rest/projects/" + this.get("projectId") + "/server-arrays" + (this.id ? "/" + this.id : "");
    },

    serversCollection: function() {
      return new Servers.ServersCollection([], { projectId: this.get("projectId"), serverArrayId: this.id });
    }
  });

  Servers.ArrayCollection = Backbone.Collection.extend({
    model: Servers.ArrayModel,

    initialize: function(elements, options) {
      this.projectId = options.projectId;
    },

    url: function() { return "/rest/projects/" + this.projectId + "/server-arrays"; }
  });

  Servers.ServerModel = Backbone.Model.extend({
    parse: function(json) {
      if(json.result) {
        return json.result;
      } else {
        return json;
      }
    }
  });

  Servers.ServersCollection = Backbone.Collection.extend({
    model: Servers.ServerModel,
    initialize: function(elements, options) {
      this.projectId = options.projectId;
      this.serverArrayId = options.serverArrayId;
    },

    url: function() { return "/rest/projects/" + this.projectId + "/server-arrays/" + this.serverArrayId + "/servers"; }
  });

  var CredentialsRef = Backbone.Collection.extend({
    initialize: function(elements, options) {
      this.projectId = options.projectId;
    },

    url: function() { return "/rest/projects/" + this.projectId + "/credentials/?type=static" }
  });

  Servers.Views.Main = Backbone.View.extend({
    projectId: null,

    events: {
      "click #add-array": "createArray",
      "click .edit-array": "editArray",
      "click .show-array": "showArray"
    },

    initialize: function(options) {
      this.projectId = options.projectId;
      this.collection = new Servers.ArrayCollection({}, {projectId: this.projectId});

      this.listView = new ServerArrayList({collection: this.collection, el: this.el});
      this.setCurrentView(this.listView);
      this.collection.fetch()
    },

    showArray: function(e) {
      var arrayId = $(e.currentTarget).attr("data-array-id");

      this.setCurrentView(new ServerArrayView({el: this.el, array: this.collection.get(arrayId)}));

      var self = this;
      this.currentView.bind("back", function() {
        self.setCurrentView(self.listView);
      });
    },

    onClose: function() {
      genesis.utils.nullSafeClose(this.currentView);
    },

    createArray: function() {
      this.setCurrentView(new EditArrayView({el: this.el, projectId: this.projectId, model: new Servers.ArrayModel()}));

      var self = this;
      this.currentView.bind("back", function() {
        self.setCurrentView(self.listView);
        self.collection.fetch();
      });
    },

    editArray: function(e) {
      var arrayId = $(e.currentTarget).attr("data-array-id");
      this.setCurrentView(new EditArrayView({el: this.el, projectId: this.projectId, model: this.collection.get(arrayId)}));

      var self = this;
      this.currentView.bind("back", function() {
        self.$el.empty();
        self.setCurrentView(self.listView);
        self.collection.fetch();
      });

    },

    setCurrentView: function(view) {
      if (this.currentView != null) {
        this.currentView.undelegateEvents();
        this.currentView.unbind();
        if(this.currentView.onClose) {
          this.currentView.onClose();
        }
      }
      this.currentView = view;
      this.currentView.render();
    },

    render: function() {
    }
  });

  var ServerArrayList = Backbone.View.extend({
    template: "app/templates/project_properties/servers/arrays_list.html",
    dialog: null,

    events: {
      "click .delete-array": "deleteArray"
    },

    initialize: function(options) {
      this.collection.bind("reset", _.bind(this.render, this));
    },

    onClose: function() {
      this.dialog.dialog("close");
    },

    initConfirmationDialog: function() {
      return this.dialog || this.$("#dialog-confirm-delete").dialog({
        modal: true,
        title: 'Confirmation',
        dialogClass: 'dialog-without-header',
        minHeight: 120,
        width: 420,
        autoOpen: false
      });
    },

    deleteArray: function(elem) {
      var credentialId = $(elem.currentTarget).attr("data-array-id");
      var self = this;
      this.dialog.dialog("option", "buttons", {
        "Yes": function () {
          self.collection.get(credentialId).destroy().done(function() {
            self.collection.fetch();
          });
          $(this).dialog("close");
        },
        "No": function () {
          $(this).dialog("close");
        }
      });
      this.dialog.dialog('open');
    },

    render: function() {
      var self = this;
      $.when(genesis.fetchTemplate(this.template)).done(function(tmpl) {
        self.$el.html( tmpl({"serverArrays" : self.collection.toJSON()}));
        self.dialog = self.initConfirmationDialog();
        self.delegateEvents(self.events);
      });
    }

  });

  var EditArrayView = Backbone.View.extend({
    template: "app/templates/project_properties/servers/edit_array.html",

    events: {
      "click .cancel": "cancel",
      "submit #edit-server-array": "create",
      "click #save-array": "create"
    },

    initialize: function(options) {
      this.projectId = options.projectId;
    },

    cancel: function(){
      this.trigger("back");
    },

    create: function(e) {
      e && e.preventDefault();
      if(!this.$("#edit-server-array").valid()) {
        return;
      }
      var array = this.model.clone();
      array.set({
        projectId: parseInt(this.projectId),
        name: this.$("input[name='name']").val(),
        description: this.$("textarea[name='description']").val()
      });
      var self = this;
      array.save()
        .done(function() {
          self.trigger("back")
        })
        .error(function(jqXHR) { self.status.error(jqXHR) });
    },

    render: function() {
      var self = this;
      $.when(genesis.fetchTemplate(this.template)).done(function(tmpl) {
        self.$el.html( tmpl({array: self.model.toJSON()}) );
        self.status = new status.LocalStatus({el: self.$(".notification")})
      });
    }
  });

  var ServerArrayView = Backbone.View.extend({
    template: "app/templates/project_properties/servers/servers-list.html",

    events: {
      "click .back": "back",
      "click #add-server:not(.disabled)": "showAddServerForm",
      "click .delete-server": "deleteServer",
      "click .show-server": "showServer"
    },

    initialize: function(options) {
      this.array = options.array;
      this.servers = this.array.serversCollection();
      this.servers.bind("all", this.render, this);
      this.servers.fetch()
    },

    onClose: function() {
      if(this.dialog) {
        this.dialog.dialog('destroy');
      }
    },

    initConfirmationDialog: function() {
      return this.dialog || this.$(".dialog-confirm-delete").dialog({
        modal: true,
        title: 'Delete server',
        dialogClass: 'dialog-without-header',
        minHeight: 120,
        width: 420,
        autoOpen: false
      });
    },

    back: function() {
      this.trigger("back");
    },

    deleteServer: function(e) {
      var serverId = $(e.currentTarget).attr("data-server-id");
      var self = this;
      var server = self.servers.get(serverId);
      server.fetch().done(function(){
        $(".server-usage").html(server.get("usage").length);
        self.dialog.dialog("option", "buttons", {
          "Yes": function () {
            self.servers.get(serverId).destroy();
            $(this).dialog("close");
          },
          "No": function () {
            $(this).dialog("close");
          }
        });
        self.dialog.dialog('open');
      });
    },

    showAddServerForm: function() {
      var $addButton = this.$("#add-server");
      $addButton.addClass("disabled");
      var addServerView = new AddServerView({el: $("<div/>").appendTo(this.$("#add-server-form")), collection: this.servers, projectId: this.array.get("projectId")});

      addServerView.bind("closed", function(){
        $addButton.removeClass("disabled");
      });
      addServerView.render();
    },

    showServer: function(e){
      var serverId = $(e.currentTarget).attr("data-server-id");
      var server = this.servers.get(serverId);
      var view = new ServerUsageView({el: this.el, model: server, array: this.array});
      var self = this;
      view.bind("back", function() {
        view.unbind();
        self.$el.empty();
        self.render();
      });

    },

    render: function() {
      var self = this;
      $.when(genesis.fetchTemplate(this.template)).done(function(tmpl) {
        self.$el.html( tmpl({array: self.array.toJSON(), servers: self.servers.toJSON()}) );

        self.delegateEvents(self.events);
        self.dialog = self.initConfirmationDialog();
      });
    }
  });

  var AddServerView = Backbone.View.extend({
    template: "app/templates/project_properties/servers/add_server.html",

    events: {
      "click .save-server": "saveServer",
      "click .cancel": "closeForm"
    },

    initialize: function(options) {
      this.projectId = options.projectId;
    },

    saveServer: function() {
      if(!this.$("form").valid()) {
        return;
      }
      var obj = new Servers.ServerModel({
        instanceId: this.$("input[name='instanceId']").val(),
        address: this.$("input[name='address']").val(),
        credentialsId: this.$("#credentials").val()
      });
      var self = this;
      obj.bind("sync", function(){
        self.closeForm();
      });
      obj.bind("error", function(model, jqXHR) {
        self.status.error(jqXHR);
      });
      obj.bind("sync error", function() { self.$el.hideLoading(); });
      this.$el.showLoading();
      this.collection.create(obj,  {wait: true});
    },

    closeForm: function() {
      var self = this;
      this.$el.slideUp("fast", function() {
        self.trigger("closed");
        self.close();
      });
    },

    render: function() {
      var credentials = new CredentialsRef([], {projectId: this.projectId});
      var self = this;
      $.when(genesis.fetchTemplate(this.template), credentials.fetch()).done(function(tmpl) {
        self.$el.html(tmpl());

        var $credsSelect = self.$("#credentials");
        credentials.each(function(cred) {
          $credsSelect.append("<option value ='" + cred.get("id") + "'>" +cred.get("pairName") + "</option>")
        });
        $credsSelect.removeAttr("disabled");

        self.$el.slideDown("fast");
        self.status = new status.LocalStatus({el: self.$(".notification")});
      });
    }
  });

  var ServerUsageView = Backbone.View.extend({
    template: "app/templates/project_properties/servers/server_usage.html",

    events: {
      "click .back-to-server": "back"
    },

    initialize: function(options) {
      this.array = options.array;
      $.when(this.model.fetch()).done(_.bind(this.render, this));
    },

    back: function() {
      this.trigger("back");
    },

    render: function(){
      var self = this;
      $.when(genesis.fetchTemplate(this.template)).done(function(tmpl){
        self.$el.html(tmpl({server: self.model.toJSON(), serverArray: self.array.toJSON()}));
      });
    }
  });

  return Servers;
});