define([
    // Libs
    "genesis",
    "modules/status",
    "jquery",
    "use!underscore"
],

function(genesis, status, $, _) {
       return {
           processVars: function(options) {
               var variables = options.variables;
               var projectId = options.projectId;
               var workflow = options.workflowName;
               var templateName = options.templateName;
               var templateVersion = options.templateVersion;
               var dependencies = {};
               var parents = {};
               var app = genesis.app;
               $(variables).each(function(){
                   if (this.dependsOn) {
                       var dependent = this.name;
                       parents[this.name] = this.dependsOn;
                       $(this.dependsOn).each(function() {
                           if (dependencies[this]) {
                               dependencies[this].push(dependent);
                           } else {
                               dependencies[this]= [dependent];
                           }
                       });
                   }
               });
               _.each(_.keys(parents), function(k) {
                   _.each(parents[k], function(p) {
                       if (! parents[p]) {
                           parents[p] = _.reject(parents[k], function(e){return e == p;});
                       }
                   });
               });
               for (var p in dependencies) {
                   var selector = "#" + p;
                   $(document).off("change", selector).on("change", selector, function(){
                       var source = $(this).attr("name");
                       _.each(dependencies[source], function(name) {
                           $('#' + name + " option").remove();
                           $('#' + name).attr('disabled', 'disabled');
                       });
                       var variables = _.inject(_.union([source], parents[source]), function(vars, elem){
                           var val = $('#' + elem).val();
                           if (val) {
                               vars[elem] = val;
                           }
                           return vars;
                       }, {});
                       app.trigger("page-view-loading-started");
                       $.ajax({
                           type: "POST",
                           url: "/rest/projects/" + projectId + "/templates/" + templateName + "/v" + templateVersion + "/" + workflow,
                           data: JSON.stringify({variables: variables}),
                           dataType: "json"
                       }).success(function(data){
                               for (var j = 0; j < data.length; j++) {
                                   var variable = data[j];
                                   if (variable.name != source) {
                                       if (typeof(variable.values) !== "undefined" && _.size(variable.values)) {
                                           var disabled = $("<option>Please select</option>");
                                           disabled.attr("disabled", "disabled");
                                           disabled.attr("selected", "selected");
                                           disabled.attr("value", "");
                                           $("#" + variable.name).append(disabled);
                                           for (var k in variable.values) {
                                               var opt = $("<option/>");
                                               opt.attr("value", k);
                                               opt.text(variable.values[k]);
                                               $("#" + variable.name).append(opt);
                                               $("#" + variable.name).removeAttr('disabled');
                                           }
                                       }
                                   }
                               }
                               app.trigger("page-view-loading-completed");
                           }).fail(function(jqXHR) {
                               app.trigger("page-view-loading-completed");
                               status.StatusPanel.error(jqXHR);
                           });
                   });
               }
           }
       }
});