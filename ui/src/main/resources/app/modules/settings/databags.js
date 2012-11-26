define([
    "genesis",
    "use!backbone",
    "modules/status",
    "modules/common/properties",
    "modules/validation",
    "services/backend",
    "jquery",
    "use!jvalidate"
],

function(genesis, Backbone, status, property, validation, backend, $) {
  var Databags = genesis.module();

  var URL = "/rest/databags";

  Databags.Model = Backbone.Model.extend({
    initialize: function(options) {
      if (options.projectId) {
        this.projectId = options.projectId;
      }
    },

    urlRoot: function() {
      if (this.projectId) {
        return "/rest/projects/" + this.projectId + "/databags"
      } else {
        return URL
      }
    }
  });

  Databags.Collection = Backbone.Collection.extend({
    model: Databags.Model,

    initialize: function(options) {
     if (options.projectId) {
       this.projectId = options.projectId;
     }
    },

    url: function() {
      if (this.projectId) {
        return "/rest/projects/" + this.projectId + "/databags"
      } else {
        return URL;
      }
    }
  });

  var DatabagItem = Backbone.Model.extend({ });

  var ItemsCollection = Backbone.Collection.extend({
    mode: DatabagItem,
    fetch: function() { //fake fetch
      var def = new $.Deferred();
      this.trigger("reset");
      return def.resolve();
    }
  });

  Databags.Views.Main = Backbone.View.extend({
    events: {
      "click .add-databag": "createDatabag",
      "click .edit-databag": "editDatabag"
    },

    initialize: function(options) {
      _.bind(this.render, this);
      if (options.projectId) {
          this.projectId = options.projectId
      }
      this.collection = new Databags.Collection({projectId: this.projectId});
      this.refresh();
    },

    refresh: function() {
      var self = this;
      this.collection.fetch().done(function() {
        self.listView = new DatabagsList({collection: self.collection, el: self.el});
        self.currentView = self.listView;
        self.render();
      });
    },

    createDatabag: function() {
      var self = this;
      this.showEditView(new Databags.Model({
        "name": "",
        "tags": [],
        "projectId": self.projectId
      }));
    },

    editDatabag: function(e) {
      var bagId = $(e.currentTarget).attr("data-databag-id");
      this.showEditView(this.collection.get(bagId));
    },

    showEditView: function(model) {
      this.currentView = new EditDatabagView({model: model, el: this.el});
      var self = this;
      this.currentView.bind("back", function() {
        self.currentView.unbind();
        self.currentView.undelegateEvents();
        self.currentView = self.listView;
        self.render();
      });

    },

    render: function() {
      if (this.currentView != null) {
        this.currentView.render();
      }
    }

  });

  var DatabagsList = Backbone.View.extend({
    template: "app/templates/settings/databags/bags_list.html",

    events: {
      "click .delete-databag": "deleteDatabag"
    },

    deleteDatabag: function(e) {
      var bagId = $(e.currentTarget).attr("data-databag-id"),
          self = this;

      this.dialog.dialog("option", "buttons", {
        "Yes": function () {
          self.collection.get(bagId).destroy().done(function() {
            self.render();
          });
          $(this).dialog("close");
        },
        "No": function() {
          $(this).dialog("close");
        }
      });
      this.dialog.dialog('open');
    },

    initConfirmationDialog: function() {
      return this.$("#dialog-confirm-databag-delete").dialog({
        title: 'Confirmation'
      });
    },

    render: function(done) {
      var self = this;
      $.when(genesis.fetchTemplate(this.template), this.collection.fetch()).done(function(tmpl) {
        self.$el.html(tmpl({"databags": self.collection.toJSON()}));
        self.dialog = self.dialog || self.initConfirmationDialog();
      });
    }
  });

  var EditDatabagView = Backbone.View.extend({
    template: "app/templates/settings/databags/edit_databag.html",

    events: {
      "click .cancel": "cancel",
      "click #save-databag": "save"
    },

    initialize: function(options) {
      this.render();
    },

    cancel: function(){
      this.trigger("back");
    },

    save: function() {
      if(!this.$("#edit-databag").valid()) {
        return;
      }

      var properties = this.propertyView.pullCollection(),
          toBeRemoved = properties.filter(function(item) { return item.get("removed") || !item.get("name")});

      properties.remove(toBeRemoved, {silent: true});

      var bag = this.model.clone().set({
        name: this.$("input[name='name']").val().trim(),
        tags: this.$("textarea[name='tags']").val().split(" "),
        items: properties.toJSON()
      });
      validation.bindValidation(bag, this.$("#edit-databag"), this.status);
      var self = this;
      bag.save().done(function() { self.trigger("back") });
    },

    render: function() {
      var self = this;
      $.when(genesis.fetchTemplate(this.template), this.model.fetch()).done(function(tmpl) {
        var items = _(self.model.get("items")).map(function(item) { return new DatabagItem(item) });
        var itemsCollection = new ItemsCollection(items);

        self.$el.html( tmpl({databag: self.model.toJSON()}) );
        self.propertyView = new property.Views.PropertyEditor({
          collection: itemsCollection,
          el: self.$("#properties")
        });

        self.status = new status.LocalStatus({el: self.$(".notification")});
      });
    }
  });

  return Databags;
});
