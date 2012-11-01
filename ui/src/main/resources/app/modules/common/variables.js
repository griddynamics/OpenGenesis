define([
    // Libs
    "genesis",
    "modules/status",
    "jquery",
    "use!underscore",
    "use!backbone"
],

function(genesis, status, $, _, Backbone) {

  var DependencyGraph = function(variables) {
    this.parentsOf = {}; // [name -> list of parents ] map
    this.childrenOf = {}; // [name -> list of dependents] map
    this.variables = variables;

    var graph = this;

    _(variables).each(function(item) {
      if (item.dependsOn) {
        graph.parentsOf[item.name] = item.dependsOn;

        _(item.dependsOn).each(function(parent) {
          graph.childrenOf[parent] = graph.childrenOf[parent] || [];
          graph.childrenOf[parent].push(item.name);
        });
      }
    });
  };

  DependencyGraph.prototype = {

    all: function() {
      return _(this.variables).map(function(item) { return item.name; });
    },

    parents: function(varName) {
      var varParents = this.parentsOf[varName];
      var recursion = _.bind(this._parentsRecursion, this);
      return _.chain(varParents).map(recursion).union().flatten().unique().value();
    },

    _parentsRecursion: function(varName) {
      var varParents = this.parentsOf[varName];
      if (varParents) {
        var self = this;
        return _.union([varName], _(varParents).map(function(parent) {
          return self._parentsRecursion(parent)
        }));
      } else {
        return [varName];
      }
    },

    allDescendants: function(varName) {
      var deps = this.childrenOf[varName];
      var recursion = _.bind(this._descendantRecursion, this);
      return _.chain(deps).map(recursion).union().flatten().unique().value();
    },

    _descendantRecursion: function(varName) {
      var deps = this.childrenOf[varName];
      if (deps) {
        var self = this;
        return _.union([varName], _.map(deps, function(dep) {
          return self._descendantRecursion(dep)
        }));
      } else {
        return [varName];
      }
    },

    allButLeafs: function() {  //returns all nodes that have children in graph
      return _.keys(this.childrenOf);
    }
  };

  function collectValueObject(variables) {
    return _(variables).inject(function(vars, elem) {
      var val = $('#' + elem).val();
      if (val) {
        vars[elem] = val;
      }
      return vars;
    }, {});
  }

  function partialApply(url, variables) {
    return $.ajax({
      type: "POST",
      url: url,
      data: JSON.stringify({variables: variables}),
      dataType: "json",
      contentType : 'application/json'
    })
  }

  return {
    processVars: function(options) {
      var templateUrl = "/rest/projects/" + options.projectId + "/templates/" +  options.templateName + "/v" +  options.templateVersion + "/" + options.workflowName;

      var graph = new DependencyGraph(options.variables);

      _(graph.allButLeafs()).each(function(node) {
        var selector = "#" + node;
        $(document).off("change", selector).on("change", selector, function() {
          var source = $(this).attr("name");
          var descendants = _(graph.allDescendants(source));

          descendants.each(function(name) {
            $('#' + name).attr('disabled', 'disabled').find("option").remove();
          });

          var resolvedVariables = _.difference(graph.all(), descendants);
          var variables = collectValueObject(resolvedVariables);

          genesis.app.trigger("page-view-loading-started");

          $.when(partialApply(templateUrl, variables))
            .done(function(data) {
              _(data).each(function(variable) {
                if (descendants.contains(variable.name) && _.has(variable, "values") && _.size(variable.values) > 0) {

                  var $select = $("#" + variable.name)
                    .append("<option value=''> Please select </option>");
                  if (!variable.group) {
                    $select.removeAttr("disabled");
                  } else {
                   var checked = $("input[type=radio][name=" + variable.group + "][data-var-name="
                    + variable.name + "]").is(':checked');
                   if (checked) $select.removeAttr("disabled");
                  }

                  _(variable.values).each(function(label, value) {
                    if (label && value) {
                      $select.append(
                        $("<option/>").attr("value", value).text(label)
                      );
                    }
                  });

                  if(variable.defaultValue) {
                    $select.val(variable.defaultValue);
                    $select.change();
                  }
                }
              });
            }).fail(function(jqXHR) {
              status.StatusPanel.error(jqXHR);
            }).always(function() {
              genesis.app.trigger("page-view-loading-completed");
            });
        });
      });
    },

    WorkflowParamsView : Backbone.View.extend({
      varTemplate: "app/templates/common/variables.html",

      events: {
        "click .group-radio": "groupVarSelected"
      },

      workflowParams: function() {
        var vals = {};
        this.$('.workflow-variable').each(function () {
          var value = $(this).is("input[type='checkbox']") ? $(this).is(':checked').toString() : $(this).val();
          var group = $($(this).parent()).children("input[type='radio'].group-radio");
          var groupChecked = group && $(group).is(':checked');
          if ($(this).val() && $(this).is(':enabled') || groupChecked) { vals[$(this).attr('name')] = value; }
        });
        return vals;
      },

      variablesModel: function() {
        //to be overridden
        return {};
      },

      groupVarSelected: function(event) {
        var view = this;
        var $currentTarget = view.$(event.currentTarget);
        var group = $currentTarget.attr('name');
        var name = $currentTarget.attr('data-var-name');
        view.$("#" + name).removeAttr('disabled');
        _.each(this.variablesModel().filter(function(v){return group == v.group && name !== v.name;}), function (x) {
          view.$("#" + x.name).attr('disabled', '');
        });
      }
    })

  }
});