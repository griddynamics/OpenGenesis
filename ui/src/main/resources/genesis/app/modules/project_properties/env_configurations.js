define([
    "genesis",
    "backbone",
    "modules/status",
    "modules/common/properties",
    "modules/project_properties/env_configurations/access",
    "cs!modules/common/databag_templates",
    "cs!modules/settings/roles",
    "modules/validation",
    "services/backend",
    "jquery",
    "jvalidate"
],

function(genesis, Backbone, status, property, access, templates, roles, validation, backend, $) {
  var EnvConfigs = genesis.module();

  EnvConfigs.Model = genesis.Backbone.Model.extend({
    defaults: {
      description: ""
    },

    initialize: function(options) {
      this.projectId = options.projectId;
    },

    urlRoot: function() {
      return "rest/projects/" + this.projectId + "/configs"
    },

    hasAccessRestrictions: function() {
      return !_.isUndefined(this._accessLink);
    },

    parseLinks: function(links) {
      this._editLink = _(links).find(backend.LinkTypes.EnvConfig.edit);
      this._deleteLink = _(links).find(backend.LinkTypes.EnvConfig.delete);
      this._accessLink = _(links).find(backend.LinkTypes.EnvConfigAccess.get);
    }
  });

  EnvConfigs.Collection = genesis.Backbone.Collection.extend({
    model: EnvConfigs.Model,
    linkType: backend.LinkTypes.EnvConfig,

    initialize: function(options) {
       this.projectId = options.projectId;
    },

    url: function() {
       return "rest/projects/" + this.projectId + "/configs"
    }
  });

  var Item = Backbone.Model.extend({ });

  var ItemsCollection = Backbone.Collection.extend({
    mode: Item,
    fetch: function() { //fake fetch
      var def = new $.Deferred();
      this.trigger("reset");
      return def.resolve();
    }
  });

  EnvConfigs.Views.Main = Backbone.View.extend({
    events: {
      "click .add-config": "createConfig",
      "click .add-config-from-template": "selectTemplate",
      "click .edit-config": "editConfig"
    },

    initialize: function(options) {
      _.bind(this.render, this);
      this.projectId = options.projectId;
      this.collection = new EnvConfigs.Collection({projectId: this.projectId});
      this.refresh();
    },

    refresh: function() {
      var self = this;
      this.collection.fetch().done(function() {
        self.listView = new ConfigsList({collection: self.collection, el: self.el});
        self.currentView = self.listView;
        self.selectTemplateDialog = new templates.SelectTemplateDialog({
          $el: $("#select-template-dialog"),
          collection: new templates.Collection({scope: "environment"})
        });
        self.render();
      });
    },

    selectTemplate: function() {
      var self = this;
      if (this.selectTemplateDialog) {
        this.selectTemplateDialog.bind('databag-template-selected', function(val) {
          self.createFromTemplate(val)
        }).show();
      }
    },

    createFromTemplate: function(templateId) {
      var created = new templates.Model({
        scope: "environment",
        id: templateId
      });
      var self = this;
      $.when(created.fetch()).done(function(obj) {
        var prefilled = _.reduce(obj.properties, function(memo, element) {
          memo[element.name] = element.value;
          return memo;
        }, {});
        self.showEditView(new EnvConfigs.Model({
            description: "Created from template",
            projectId: self.projectId,
            templateId: templateId,
            items: prefilled,
            isNew: true
          }
        )
        )});
    },

    createConfig: function() {
      var self = this;
      this.showEditView(new EnvConfigs.Model({
        "name": "",
        "projectId": self.projectId
      }));
    },

    editConfig: function(e) {
      var id = $(e.currentTarget).attr("data-config-id"),
          entity = this.collection.get(id),
          self = this;
      $.when(entity.fetch()).done(function() {
         self.showEditView(entity);
      })

    },

    showEditView: function(model) {
      this.currentView = new EditConfigView({model: model, el: this.el, collection: this.collection});
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

  var ConfigsList = Backbone.View.extend({
    template: "app/templates/project_properties/env_configs/configs_list.html",

    events: {
      "click .delete-config": "deleteConfig"
    },

    deleteConfig: function(e) {
      var id = $(e.currentTarget).attr("data-config-id"),
          self = this;

      this.dialog.dialog("option", "buttons", {
        "Yes": function () {
          self.collection.get(id).destroy({wait: true}).done(function() {
            self.render();
          }).error(function(jqXHR) {
            status.StatusPanel.error(jqXHR);
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
      return this.$("#dialog-confirm-config-delete").dialog({
        title: 'Confirmation'
      });
    },

    render: function() {
      var self = this;
      $.when(genesis.fetchTemplate(this.template), this.collection.fetch()).done(function(tmpl) {
        self.$el.html(tmpl({
          "configs": self.collection.toJSON(),
          "canCreate": self.collection.canCreate(),
          "accessRights": self.collection.itemAccessRights()
        }));

        self.dialog = self.dialog || self.initConfirmationDialog();
      });
    }
  });


  var ConfigAccess = Backbone.Model.extend({
    initialize: function(attr, options){
      this.configId = options.id;
      this.projectId = options.projectId;
    },

    url: function() {
      return "rest/projects/" + this.projectId + "/configs/" + this.configId + "/access"
    },

    isNew: function() {
      return false;
    }
  });

  var EditConfigView = Backbone.View.extend({
    template: "app/templates/project_properties/env_configs/edit_configuration.html",

    events: {
      "click .cancel": "cancel",
      "click #save-config": "save",
      "click #access-tab-header": "loadAccess",
      "click .add-access": "showAccessPanel",
      "click .back" : "back"
    },

    initialize: function() {
      this.render();
    },

    cancel: function(){
      this.trigger("back");
    },

    save: function() {
      if(!this.$("#edit-config").valid()) {
        return;
      }
      var properties = this.propertyView.pullCollection(),
          toBeRemoved = properties.filter(function(item) { return item.get("removed") || !item.get("name")});

      properties.remove(toBeRemoved, {silent: true});

      var attributesMap = properties.reduce(function(memo, item) {
        memo[item.get("name")] = item.get("value");
        return memo;
      }, {});

      var config = this.model.clone();
      config.set({
        name: this.$("input[name='name']").val().trim(),
        description: this.$("textarea[name='description']").val().trim(),
        templateId: this.$("input[name='templateId']").val(),
        items: attributesMap
      }, {silent: true});
      validation.bindValidation(config, this.$("#edit-config"), this.status, true);

      var self = this,
          isNew = this.model.isNew();

      config.save().done(function() {
        if (isNew && self.roleEditView) {
          var configAccess = new ConfigAccess(self.roleEditView.pullGranties(), { projectId: config.get("projectId"), id: config.id });

          configAccess.save().done(function () {
            self.trigger("back");
          }).fail(function() {
            self.trigger("back");
            status.StatusPanel.warn("Failed to save access settings");
          });
        } else {
          self.trigger("back")
        }
      });
    },

    loadAccess: function() {
      var configAccess = new ConfigAccess({}, { projectId: this.model.get("projectId"), id: this.model.id });
      this.accessView = new access.View({
        el: "#panel-tab-access",
        accessConfiguration: configAccess,
        projectId: this.model.get("projectId"),
        tabHeader: this.$("#access-tab-header")
      });
    },

    showAccessPanel: function(){
      var accessConfiguration = new Backbone.Model({groups: [], users: []});
      this.roleEditView = new roles.Views.Edit({
        el: this.$("#access-panel"),
        role: accessConfiguration,
        title: "Grant access to",
        showButtons: false
      });
    },

    back: function() {
      this.trigger("back");
    },

    render: function() {
      var self = this;
      $.when(genesis.fetchTemplate(this.template)).done(function(tmpl) {
        var attributes = self.model.get("items") || [];
        var items = _.chain(attributes).keys().map(function(key) { return new Item({name: key, value: attributes[key], isNew: self.model.isNew}) }).value();
        var itemsCollection = new ItemsCollection(items);

        self.$el.html( tmpl({
          config: self.model.toJSON(),
          canEdit: self.model.canEdit()|| self.collection.canCreate(),
          access_security: genesis.app.currentConfiguration['environment_security_enabled']
        }) );
        self.propertyView = new property.Views.PropertyEditor({
          collection: itemsCollection,
          el: self.$("#properties"),
          dbtemplate: new templates.Model({
            scope: "environment",
            id: self.model.get("templateId")
          })
        });

        self.status = new status.LocalStatus({el: self.$(".notification")});
      });
    }
  });

  return EnvConfigs;
});
