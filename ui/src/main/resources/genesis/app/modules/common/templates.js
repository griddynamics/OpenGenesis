define([
  "genesis",
  "backbone",
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
      return "rest/projects/" + this.projectId + "/templates/" + this.get("name") + "/v" + this.get("version");
    },

    fetchSources: function() {
      return this.fetch({data: $.param({ format: 'src'})});
    }
  });

  var WorkflowDesc = Backbone.Model.extend({
    initialize: function (model, options) {
      this.projectId = options.projectId;
      this.workflow = options.workflow;
      this.instanceId = options.instanceId;
    },

    url: function () {
      return "rest/projects/" + this.projectId + (this.instanceId ? this.actionPath() : this.templatePath());
    },

    templatePath: function () {
      return "/templates/" + this.get("name") + "/v" + this.get("version") + "/" + this.workflow;
    },

    actionPath: function () {
      return "/envs/" + this.instanceId + "/workflows/" + this.workflow;
    },

    parse: function(json) {
      return json.result ? json.result : json
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
      return "rest/projects/" + this.projectId + "/templates";
    },

    comparator: function(template) {
      var version = template.get('version');
      if (! isNaN(parseFloat(version)) && isFinite(version)) {
        version = (Array(64).join("0") + version).slice(-64)
      }
      return template.get('name').toLowerCase() + version;
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
              resizable: true,
              modal: true,
              width: 900,
              minWidth: 500,
              minHeight: 400,
              height: 400,
              dialogClass: 'dialog-without-header',
              autoOpen: false
            });
          }
          genesis.app.trigger("page-view-loading-completed");
          prettyPrint();
          var title = _.template(
              'Template: <%= templateName%> / v<%= templateVersion%> ' +
              '<a target="_blank" title="Open template in a new blank window" data-bypass ' +
              '   href="template.html?templateName=<%= templateName%>&templateVersion=<%= templateVersion%>&projectId=<%= projectId%>">' +
              '     <span class="ui-icon ui-icon-newwin" style="display:inline-block;"></span>' +
              '</a>',
              {
                  templateName: templateName,
                  templateVersion: templateVersion,
                  projectId: self.projectId
              }
          );
          self.sourceCodeDialog.dialog("option", "title", title).dialog("open");
        })
        .fail(function(jqXHR) {
          genesis.app.trigger("page-view-loading-completed");
          status.StatusPanel.error(jqXHR);
        });
    }
  });

  Templates.TemplateModel = TemplateDesc;
  Templates.WorkflowModel = WorkflowDesc;

  return Templates;
});