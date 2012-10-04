define([
  "genesis",
  "modules/status",
  "use!backbone",
  "jquery",
  "use!jqueryui",
  "use!jvalidate"
],

function(genesis, status, Backbone, $) {

  var Settings = genesis.module();

  var URL = "/rest/settings";
  var TIMEOUT_AJAX = 4000;

  Settings.Model = Backbone.Model.extend({
    idAttribute: "name",
    url: URL
  });

  Settings.Collection = Backbone.Collection.extend({
    model: Settings.Model,
    url: URL + "?prefix=genesis.system"
  });

  Settings.Views.Main = Backbone.View.extend({
    template: "app/templates/settings/system_configs.html",

    events: {
      "click a.save" : "saveChanges",
      "click a.reset" : "reset",
      "click a.show-keys": "toggleShowKey"
    },

    initialize: function() {
      this.collection = new Settings.Collection;
      this.collection.bind("reset", this.render, this);
      this.collection.fetch();
    },

    render: function() {
      var view = this;
      $.when(genesis.fetchTemplate(this.template)).done(function(tmpl) {
        var propsGrp = view.collection.groupBy(function(p){return p.get('readOnly');});
        var toJson = function(c){return _.map(c, function(m) {
          return m.toJSON();
        })};
        view.$el.html( tmpl({"properties" : toJson(propsGrp[false]), "constants" : toJson(propsGrp[true])}));
      });
    },

    saveChanges: function () {
      var view = this;
      $('input.property-value').each(function () {
        var propertyName = $(this).attr('name'),
          value = $(this).val();
        view.collection.get(propertyName).set('value', value, {"silent": true});
      });

      var changedSettings = _(this.collection.filter(function(item) { return item.hasChanged(); }));
      var map = new Settings.Model;
      map.isNew = function(){return false;};

      changedSettings.each(function(item) {
        map.set(item.get('name'), item.get('value'));
      });

      if (!changedSettings.isEmpty()) {
        map.save(null, {
          success: function (model, response) {status.StatusPanel.success("Settings changes saved.")},
          error: function (model, response) {
            var errorMessage = response.status == 400 ? JSON.parse(response.responseText).compoundServiceErrors : "Failed to process request";
            status.StatusPanel.error(errorMessage);
          }
        });
      }
    },

    restoreDefaults: function () {
      return  $.ajax({  //todo: WHAT IS THIS??
        url: this.collection.url,
        dataType: "json",
        type: "DELETE",
        timeout: TIMEOUT_AJAX,
        processData: false
      });
    },

    reset: function() {
      this.showConfirmationDialog();
    },

    toggleShowKey: function(event) {
      var $link = $(event.currentTarget);
      var $span = $link.children("span");
      $span.toggleClass("ui-icon-carat-1-n ui-icon-carat-1-s");
      $span.attr("title", $span.hasClass("ui-icon-carat-1-s") ? "Show Key" : "Hide Key");
      var $element = $($link.attr("rel"));
      $element.toggle();
    },

    showConfirmationDialog: function () {
      var self = this;
      if (!this.confirmationDialog) {
          this.confirmationDialog = this.$("#dialog-confirm-reset").dialog({
            title: 'Confirmation',
            buttons: {
              "Yes": function () {
                $.when(self.restoreDefaults())
                  .done(function() {
                    self.collection.fetch(); status.StatusPanel.success("Default settings restored!")
                  })
                  .fail(function() {
                    status.StatusPanel.error("Default settings restore failed!");
                  });
                $(this).dialog("close");
              },
              "No": function () {
                $(this).dialog("close");
              }
            }
          });
      }
      this.confirmationDialog.dialog('open');
    }
  });

  return Settings;
});
