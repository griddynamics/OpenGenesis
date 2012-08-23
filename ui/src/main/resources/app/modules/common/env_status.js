define([
  "genesis",
  "use!backbone",
  "jquery",
  "use!jqueryui"
],

function (genesis, Backbone, $) {
  var EnvStatus = genesis.module();

  var STATUS_TEMPLATE = "app/templates/common/env_status.html",
      PROCESSING = "assets/img/processing-status.png",
      COMPLETED = "assets/img/normal-status.png",
      BROKEN = "assets/img/bad-status.png",
      DISABLED = "assets/img/disabled-status.png",
      STATUS_IMAGES = {
        "Busy": PROCESSING,
        "Ready": COMPLETED,
        "Destroyed": DISABLED,
        "Broken": BROKEN
      };

  genesis.fetchTemplate(STATUS_TEMPLATE); //prefetch template

  EnvStatus.View = Backbone.View.extend({
    template: STATUS_TEMPLATE,

    initialize: function() {
      this.model.bind("change:status", this.render, this);
      this.model.bind("change:workflowCompleted", this.renderProgress, this);
    },

    renderProgress: function() {
      var completed = this.model.get("workflowCompleted") || 0;
      if (this.model.get("status") === "Busy") {
        this.$(".genesis-progressbar > .progress").progressbar({value: completed * 100});
      }
    },

    render: function() {
      var self = this;
      $.when(genesis.fetchTemplate(this.template)).done(function(tmpl) {
        self.$el.html(tmpl({
          environment: { status: self.model.get("status") },
          statusImages: STATUS_IMAGES
        }));
        self.renderProgress();
      });
    }
  });

  return EnvStatus;
});