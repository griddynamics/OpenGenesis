define([
  "genesis",
  "backbone"
],

function(genesis, Backbone) {
  var Properties = genesis.module();

  Properties.Views.PropertyEditor = Backbone.View.extend({
    template: "app/templates/common/property_editor.html",

    events: {
      "keyup :input": "onChange",
      "click #add-pair-link": "onAddPair",
      "click .delete-action": "onDeletePair",
      "click #add-predefined": "onAddPredefined"
    },

    initialize: function(options) {
      this.dbtemplate = options.dbtemplate;
      this.collection.bind("add remove reset", this.render, this);
      this.collection.fetch();
    },

    pullCollection: function() {
      this.collection.each(function(item) {
        if (! item.get('variants')) {
          item.set({
            "name": $("#name-input-" + item.cid).val().trim(),
            "value": $("#value-input-" + item.cid).val().trim()
          });
        } else if ($(("select#name-input-" + item.cid))) {
          item.set({
            "name": $("#name-input-" + item.cid + " option:selected").val(),
            "value": $("#value-input-" + item.cid).val().trim()
          });
        }
      });
      return this.collection;
    },

    render: function() {
      var self = this;
      $.when(genesis.fetchTemplate(this.template), this.dbtemplate.allKeys()).done(function(tmpl, keys) {
        var existingKeys = _.map(self.collection.toJSON(), function(item) { return item.name; });
        var predefined = _.difference(keys, existingKeys);
        self.$el.html( tmpl({"properties" : self.collection, hasKeys:predefined.length > 0}));
        self.delegateEvents(self.events);
      });
    },

    onChange: function(event) {
      var cid = event.currentTarget.getAttribute("data-cid");
      this.collection.get(cid).set("dirty", true);
      this.$("tr[data-cid='" + cid + "']").addClass('changed');
    },

    onAddPair: function() {
      this.pullCollection();
      this.collection.add({name: "", value: "", isNew: true});
    },

    onAddPredefined: function() {
      this.pullCollection();
      var self = this;
      var existingKeys = _.map(self.collection.toJSON(), function(item) { return item.name; });
      $.when(this.dbtemplate.allKeys()).done(function(keys) {
        var variants = _.difference(keys, existingKeys);
        var defaultName = "";
        if (variants.length > 0) {
           defaultName = variants[0];
        }
        self.collection.add({name: defaultName, value: "", isNew: true, variants: variants});
      });
    },

    onDeletePair: function(event) {
      this.pullCollection();
      var cid = event.currentTarget.getAttribute("data-cid");
      var element = this.collection.get(cid);
      if (element.get("isNew")) {
        this.collection.remove([element]);
      } else {
        this.$("tr[data-cid='" + cid + "']").addClass('removed');
        element.set("removed", true);
      }
    }
  });

  return Properties;
});
