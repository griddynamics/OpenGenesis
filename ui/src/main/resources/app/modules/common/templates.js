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

  var WorkflowDesc = Backbone.Model.extend({
     initialize: function(model, options) {
       this.projectId = options.projectId;
       this.workflow = options.workflow;
     },

     url : function() {
       return "/rest/projects/" + this.projectId + "/templates/" + this.get("name") + "/v" + this.get("version") + "/" + this.workflow;
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

  Templates.TemplateModel = TemplateDesc;
  Templates.WorkflowModel = WorkflowDesc;

  return Templates;
});