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
  var TemplateRepo = genesis.module();

  TemplateRepo.Model = genesis.Backbone.Model.extend({
    linkType: backend.LinkTypes.TemplateRepo,

    initialize: function(options) {this.set("projectId", options.projectId);},

    urlRoot: function() {
      return "rest/projects/" + this.get("projectId") + "/template/repository";
    },

    parseLinks: function(links) {
      this._editLink = _(links).find(this.linkType.edit);
    }
  });

  TemplateRepo.Views.Main = Backbone.View.extend({
    template: "app/templates/project_properties/template_repo.html",

    events: {
      "click a.show-keys": "toggleShowKey",
      "click .edit-templaterepo": "edit"
    },

    initialize: function(options) {
      _.bind(this.render, this);
      if (options.projectId) {
          this.projectId = options.projectId
      }
      this.model = new TemplateRepo.Model({projectId: this.projectId});
      this.refresh();
    },

    editable: function() {
      return this.model.canEdit() && _.all(this.model.get("configuration"), function(cp){
        return !cp.readOnly
      });
    },

    refresh: function() {
      var self = this;
      this.model.fetch().done(function() {
      self.editable() ? self.showEditView(this.projectId) : self.showListView(this.projectId);
      });
    },

    toggleShowKey: function(event) {
      var $link = $(event.currentTarget);
      var $span = $link.children("span");
      $span.toggleClass("ui-icon-carat-1-n ui-icon-carat-1-s");
      $span.attr("title", $span.hasClass("ui-icon-carat-1-s") ? "Show Key" : "Hide Key");
      var $element = $($link.attr("rel"));
      $element.toggle();
    },

    edit: function(e) {
      this.showEditView(this.projectId);
    },

    showListView: function(projectId) {
      this.listView = new ListView({model: this.model, el: this.el});
      this.currentView = this.listView;
      this.render();
    },

    showEditView: function(projectId) {
      this.currentView = new EditView({model: this.model, projectId: projectId, el: this.el});
    },

    render: function(done) {
      if (this.currentView != null) {
        this.currentView.render();
      }
    }
  });

  var ListView = Backbone.View.extend({
    template: "app/templates/project_properties/template_repo/template_repo.html",

    render: function(done) {
      var self = this;
      $.when(genesis.fetchTemplate(this.template), this.model.fetch()).done(function(tmpl) {
        self.$el.html(tmpl({"templateRepo": self.model.toJSON()}));
      });
    }
  });

  var ModeModel = Backbone.Model.extend({
    initialize: function(options) {this.set("mode", options.mode);},

    urlRoot: function() {
      return "rest/template/repository/modes/" + this.get("mode");
    }

  });

  var EditView = Backbone.View.extend({
    template: "app/templates/project_properties/template_repo/edit_template_repo.html",

    events: {
      "click .cancel": "cancel",
      "click #save-templaterepo": "save"
    },

    initialize: function(options) {
      _.bind(this.render, this);
      this.projectId = this.model.get("projectId");
      this.render();
    },

    updateModel: function(mode) {
         var self = this;
         var newMod = new ModeModel({mode: mode});
         $.when(newMod.fetch()).done(function(m) {
           self.model = newMod;
           self.render();
         });
    },

    cancel: function(){
      this.trigger("back");
    },

    save: function() {
      if(!this.$("#edit-templaterepo").valid()) {
        return;
      }
      var map = {'genesis.template.repository.mode': this.model.get("mode")};
      this.$('input.property-value').each(function () {
        var propertyName = $(this).attr('name');
        map[propertyName] = $(this).val();
      });
      var self = this;
    $.when(this.update(map)).done(function() {
        status.StatusPanel.success("Settings changes saved.")
    });
    },

  listModes: function() {
    return $.ajax({
      type: "GET",
      url: "rest/template/repository/modes",
      contentType : 'application/json'
    })
  },

  update: function(map) {
    return $.ajax({
      type: "PUT",
      url: "rest/projects/" + this.projectId + "/template/repository",
      data: JSON.stringify(map),
      dataType: "json",
      contentType : 'application/json'
    })
  },

    render: function() {
      var self = this;
      $.when(genesis.fetchTemplate(this.template), this.listModes(), this.model.fetch()).done(function(tmpl, modesList) {

        self.$el.html( tmpl({templateRepo: self.model.toJSON(), modes: modesList[0], projectId: self.projectId}) );
        self.$("#select-mode").off("change").on("change", function() {
          self.updateModel($(this).val());
        });
      });
    }
  });

  return TemplateRepo;
});