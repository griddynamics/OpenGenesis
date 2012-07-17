define([
  "genesis",
  "use!backbone"
],

function(genesis, Backbone) {
  var Properties = genesis.module();

  Properties.Views.PropertyEditor = Backbone.View.extend({
    template: "app/templates/common/property_editor.html",

    events: {
      "keyup :input": "onChange",
      "click #add-pair-link": "onAddPair",
      "click .delete-action": "onDeletePair"
    },

    initialize: function(options) {
      this.collection.bind("add remove reset", this.render, this);
      this.collection.fetch();
    },

    pullCollection: function() {
      this.collection.each(function(item) {
        item.set({
          "name": $("#name-input-" + item.cid).val(),
          "value": $("#value-input-" + item.cid).val()
        })
      });
      return this.collection;
    },

    render: function() {
      var self = this;
      $.when(genesis.fetchTemplate(this.template)).done(function(tmpl) {
        self.$el.html( tmpl({"properties" : self.collection}));
        self.delegateEvents(self.events);
      });
    },

    onChange: function(event) {
      var cid = event.currentTarget.getAttribute("data-cid");
      this.collection.getByCid(cid).set("dirty", true);
      this.$("tr[data-cid='" + cid + "']").addClass('changed');
    },

    onAddPair: function() {
      this.pullCollection();
      this.collection.add({name: "", value: "", isNew: true});
    },

    onDeletePair: function(event) {
      this.pullCollection();
      var cid = event.currentTarget.getAttribute("data-cid");
      var element = this.collection.getByCid(cid);
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
