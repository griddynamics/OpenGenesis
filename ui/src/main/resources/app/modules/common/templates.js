define([
  "genesis",
  "use!backbone",
  "prettify"
],
function(genesis, Backbone) {
  var Templates = genesis.module();

  var TemplateDesc = Backbone.Model.extend({
    initialize: function (model, options) {
      if(!options.projectId) {
        throw new Error("Project option is required");
      }
      this.projectId = options.projectId;
    },

    url: function() {
      return "/rest/projects/" + this.projectId + "/templates/" + this.get("name") + "/v" + this.get("version");
    },

    fetchSources: function() {
      return this.fetch({data: $.param({ format: 'src'})});
    }
  });


  Templates.TemplatesCollection = Backbone.Collection.extend({
    initialize: function (model, options) {
      if(!options.projectId) {
        throw new Error("Project option is required");
      }
      this.projectId = options.projectId;
    },

    url: function() {
      return "/rest/projects/" + this.projectId + "/templates";
    }
  });

  Templates.SourcesView = Backbone.View.extend({

    initialize: function(options) {
      this.templateName = options.templateName;
      this.templateVersion = options.templateVersion;
      this.projectId = options.projectId;
    },

    onClose: function() {
      if(this.sourceCodeDialog != null) {
        this.sourceCodeDialog.dialog('destroy').remove();
      }
    },

    showTemplate: function (name, version) {
      this.render(name, version);
    },

    render: function(templateName, templateVersion) {
      var sources = new TemplateDesc({name: templateName, version: templateVersion}, {projectId: this.projectId});
      genesis.app.trigger("page-view-loading-started");
      var self = this;
      $.when(sources.fetchSources())
        .done(function() {
          var content = _.escape(sources.get("content"));
          if ($.browser.msie) {
            content = content.replace(/\n/g, '<br/>').replace(/  /g, "&nbsp;");
          }
          self.$el.html(content);
          if (self.sourceCodeDialog == null) {
            self.sourceCodeDialog = self.$el.dialog({
              modal: true,
              width: 900,
              height: 400,
              dialogClass: 'dialog-without-header',
              autoOpen: false
            });
          }
          genesis.app.trigger("page-view-loading-completed");
          prettyPrint();
          self.sourceCodeDialog.dialog("option", "title", "Template: " + templateName + " / v" + templateVersion).dialog("open");
        })
        .fail(function(jqXHR) {
          genesis.app.trigger("page-view-loading-completed");
          status.StatusPanel.error(jqXHR);
        });
    }
  });

  Templates.TemplateModel = TemplateDesc;

  return Templates;
});