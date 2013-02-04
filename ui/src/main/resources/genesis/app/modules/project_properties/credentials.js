define([
  "genesis",
  "services/backend",
  "modules/status",
  "modules/validation",
  "backbone",
  "jquery",
  "jvalidate"
],
function(genesis, backend, status, validation, Backbone, $) {

  var Credentials = genesis.module();

  Credentials.Model = genesis.Backbone.Model.extend({
    linkType: backend.LinkTypes.Credentials,

    urlRoot: function() {
      return "rest/projects/" + this.get("projectId") + "/credentials";
    }
  });

  Credentials.Collection = genesis.Backbone.Collection.extend({
    model: Credentials.Model,
    projectId: null,
    linkType: backend.LinkTypes.Credentials,

    initialize: function(elements, options) {
      this.projectId = options.projectId;
    },

    url: function() { return "rest/projects/" + this.projectId + "/credentials"; }
  });

  Credentials.Views.Main = Backbone.View.extend({

    events: {
      "click #add-credentials": "createCredentials"
    },

    initialize: function(options) {
      this.projectId = options.projectId;
      this.collection = new Credentials.Collection({}, {projectId: this.projectId});

      this.listView = new CredentialsList({collection: this.collection, el: this.el});
      this.setCurrentView(this.listView);
      this.collection.fetch();
    },

    onClose: function(){
      genesis.utils.nullSafeClose(this.currentView);
    },

    createCredentials: function() {
      this.setCurrentView(new CreateView({el: this.el, projectId: this.projectId, collection: this.collection}));

      var self = this;
      this.currentView.bind("back", function() {
        self.setCurrentView(self.listView);
        self.collection.fetch();
      });
    },

    setCurrentView: function(view) {
      if(this.currentView != null) {
        this.currentView.undelegateEvents();
        this.currentView.unbind()
      }
      this.currentView = view;
      this.currentView.render();
    },

    render: function() {
    }
  });

  var CredentialsList = Backbone.View.extend({
    template: "app/templates/project_properties/credentials/credentials_list.html",
    dialog: null,

    events: {
      "click .delete-credential": "deleteCredentials"
    },

    initialize: function(options) {
      this.collection.bind("reset", _.bind(this.render, this));
    },

    onClose: function() {
      this.dialog.dialog("close");
    },

    initConfirmationDialog: function() {
      return this.$("#dialog-confirm-delete").dialog({
        title: 'Confirmation'
      });
    },

    deleteCredentials: function(elem) {
      var credentialId = $(elem.currentTarget).attr("data-credential-id");
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
        self.$el.html( tmpl({
          "credentials" : self.collection.toJSON(),
          "canCreate": self.collection.canCreate(),
          "accessRights": self.collection.itemAccessRights()
        }));
        self.dialog = self.initConfirmationDialog();
        self.delegateEvents(self.events);
      });
    }
  });

  var CreateView = Backbone.View.extend({
    template: "app/templates/project_properties/credentials/create.html",
    projectId: null,

    events: {
      "click .cancel": "cancel",
      "click #save-credentials": "create"
    },

    initialize: function(options) {
      this.projectId = options.projectId;
    },

    cancel: function(){
      this.trigger("back");
    },

    create: function() {
      if(!this.$("#install-credentials").valid()) {
        return;
      }

      var credentials = new Credentials.Model({
        projectId: parseInt(this.projectId),
        cloudProvider: this.$("input[name='cloudProvider']").val(),
        pairName: this.$("input[name='pairName']").val(),
        identity: this.$("input[name='identity']").val(),
        credential: this.$("textarea[name='credentials']").val(),
      });

      validation.bindValidation(credentials, this.$("#install-credentials"), this.status);

      var self = this;
      credentials.save().done(function() { self.trigger("back") });
    },

    render: function() {
      var self = this;
      $.when(genesis.fetchTemplate(this.template)).done(function(tmpl) {
        self.$el.html(
          tmpl({ canEdit: self.collection.canCreate() })
        );
        self.$("textarea[name='credentials']").on("keyup", function() {
          var $this = $(this);
          if($this.css("height") !== "350px" && $this.val().indexOf("\n") != -1) {
            $this.animate({ "height": "350px"}, "fast");
          }
        });
        self.status = new status.LocalStatus({el: self.$(".notification")})
      });
    }
  });

  return Credentials;
});
