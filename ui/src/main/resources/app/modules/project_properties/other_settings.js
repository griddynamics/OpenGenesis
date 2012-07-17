define([
  "genesis",
  "modules/status",
  "modules/common/properties",
  "use!backbone",
  "jquery",
  "use!showLoading",
  "use!jvalidate"

],

function(genesis, status, properties, Backbone, $) {
  var OtherSettings = genesis.module();

  OtherSettings.Model = Backbone.Model.extend({
    initialize: function(){
      this.set({"removed": false});
    }
  });

  OtherSettings.Collection = Backbone.Collection.extend({
    model: OtherSettings.Model,
    projectId: null,

    initialize: function(elements, options) {
      this.projectId = options.projectId;
    },

    saveAll: function() {
      return Backbone.sync('update', this, {});
    },

    url: function() {
      return "/rest/projects/" + this.projectId + "/context";
    }
  });

  OtherSettings.Views.Main = Backbone.View.extend({
    template: "app/templates/project_properties/properties.html",

    events: {
       "click #save-link": "onSave"
     },

    initialize: function(options) {
      this.projectId = options.projectId;
      this.collection = new OtherSettings.Collection({}, { projectId: this.projectId });
      this.render();
    },

    onSave: function() {
      genesis.app.trigger("page-view-loading-started");
      this.view.pullCollection();

      var toBeRemoved = this.collection.filter(function(item) { return item.get("removed") || !item.get("name")});
      this.collection.remove(toBeRemoved, {silent: true});

      var self = this;
      this.collection.saveAll().always(function() {
          genesis.app.trigger("page-view-loading-completed");
        }).done(function() {
          self.collection.fetch();
          self.status.hide();
        }).error(function(jqXHR, textStatus, errorThrown) {
          self.status.error(jqXHR);
        });
    },


    render: function() {
      var self = this;
      $.when(genesis.fetchTemplate(this.template)).done(function(tmpl) {
        self.$el.html( tmpl({"properties" : self.collection, projectId : self.projectId}));
        self.view = new properties.Views.PropertyEditor( {collection: self.collection, el: self.$("#properties") });

        self.delegateEvents(self.events);
        self.status = new status.LocalStatus({el: self.$(".notification")})
      });
    }
  });

  return OtherSettings;
});
