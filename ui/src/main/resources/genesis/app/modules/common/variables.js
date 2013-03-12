define([
    // Libs
    "genesis",
    "modules/status",
    "jquery",
    "underscore",
    "backbone"
],

function(genesis, status, $, _, Backbone) {

  var variables = genesis.module();

  var DependencyGraph = function(variables) {
    this.parentsOf = {}; // [name -> list of parents ] map
    this.childrenOf = {}; // [name -> list of dependents] map
    this.variables = variables;
    this.roots = [];
    var graph = this;

    _(variables).each(function(item) {
      if (item.dependsOn) {
        graph.parentsOf[item.name] = item.dependsOn;

        _(item.dependsOn).each(function(parent) {
          graph.childrenOf[parent] = graph.childrenOf[parent] || [];
          graph.childrenOf[parent].push(item.name);
        });
      } else {
        graph.roots.push(item);
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
    },

    roots: function() {
      return this.roots;
    }

  };

  function partialApply(url, variables, configurationId) {
    return $.ajax({
      type: "POST",
      url: url,
      data: JSON.stringify({
        variables: variables,
        configurationId: configurationId ? configurationId: null
      }),
      dataType: "json",
      contentType : 'application/json'
    }).pipe(function(ajax) {
        return ajax.result ? ajax.result : ajax;
    })
  }

  function escapeCss(str) {
    return str.replace(/([ !"#$%&'()*+,.\/:;<=>?@[\\\]^`{|}~])/g,'\\$1')
  }

  variables.Views.AbstractWorkflowParamsView = Backbone.View.extend({
    events: {
      "click .group-radio": "groupVarSelected"
    },

    initialize: function() {
      throw new Error("AbstractWorkflowParamsView should be used for extensions only")
    },

    workflowParams: function () {
      var vals = {};
      this.$('.workflow-variable').each(function () {
        var value = $(this).is("input[type='checkbox']") ? $(this).is(':checked').toString() : $(this).val();
        var group = $($(this).parent()).children("input[type='radio'].group-radio");
        var groupChecked = group && $(group).is(':checked');
        if ($(this).val() && $(this).is(':enabled') || groupChecked) {
          vals[$(this).attr('name')] = value;
        }
      });
      return vals;
    },

    variablesModel: function () {
      //to be overridden
      return {};
    },

    groupVarSelected: function (event) {
      var view = this;
      var $currentTarget = view.$(event.currentTarget);
      var group = $currentTarget.attr('name');
      var name = $currentTarget.attr('data-var-name');

      view.$("#" + escapeCss(name)).removeAttr('disabled');

      _.each(this.variablesModel().filter(function (v) { return group === v.group && name !== v.name}), function (x) {
        view.$("#" + escapeCss(x.name)).attr('disabled', '');
      });
    }
  });

  variables.Views.InputControlsView = Backbone.View.extend ({
    htmltemplate: "app/templates/common/variables.html",

    initialize: function (options) {
      this.workflow = options.workflow;
      this.variables = options.variables;
      this.graph = new DependencyGraph(this.variables);
      this.configurationId = options.configurationId;
      this.variableValues = options.variableValues || {};
      var templateUrl = "rest/projects/" +
        options.projectId + "/templates/" +
        options.template.get("name") + "/v" +
        options.template.get("version") + "/" + options.workflow.get('name');


      this.applyVariables = function(variables, configurationId) {
        return partialApply(templateUrl, variables, configurationId)
      }
    },

    $groupVariableRadio: function (variable) {
      return this.$("input[type=radio][name=" + variable.group + "][data-var-name=" + variable.name + "]");
    },

    _enableChecked: function (variable) {
      var enable = true;
      if (variable.group) {
        enable = this.$groupVariableRadio(variable).is(':checked');
      }
      if (enable) this.$('#' + escapeCss(variable.name)).removeAttr("disabled");
    },

    _isMultiValue: function (variable) {
      return _.has(variable, "values") && _.size(variable.values) > 0
    },

    _buildOptions: function ($element, valueMap, defaultValue) {
      $element.append("<option value=''> Please select </option>");
      _(valueMap).each(function (label, value) {
        if (label && value) {
          $element.append($("<option/>").attr("value", value).text(label));
        }
      });
      if (defaultValue) {
        $element.val(defaultValue).change();
      }
    },

    _disable: function (select) {
      var $select = select instanceof jQuery ? select : $(select);
      $select.attr('disabled', 'disabled').find("option").remove();
    },

    _collectValueObject: function (variables) {
      var self = this;
      return _(variables).inject(function (nameValueMap, variableName) {
        var val = self.$('#' + escapeCss(variableName)).val();
        if (val) {
          nameValueMap[variableName] = val;
        }
        return nameValueMap;
      }, {});
    },

    render: function (callback) {
      var self = this;
      $.when(genesis.fetchTemplate(this.htmltemplate)).done(function (varTmpl) {
        self.$el.html(varTmpl({variables: self.variables}));
        self._linkDependencies();
        var resolvedVars = _.keys(self.variableValues);
        if(resolvedVars.length > 0) {
          _(resolvedVars).forEach(function(v){
            var $input = self.$("#" + escapeCss(v));
            $input.removeAttr("disabled");
            if($input.hasClass("group")) {
              self.$("input[type='radio'][data-var-name='" + escapeCss(v) + "']").attr("checked", "checked")
            }
          });
          self._applyResolvedVariables(self.variableValues);
        }
        if (callback) callback();
      });
    },

    updateConfigurationId: function(newValue) {
      var self = this;
      this.configurationId = newValue;
      genesis.app.trigger("page-view-loading-started");

      var resolvedVariables = _(this.graph.all()).filter(function(name) {
        var $input = self.$('#' + escapeCss(name));
        return $input.attr('disabled') !== 'disabled';
      });
      var nameValueMap = self._collectValueObject(resolvedVariables);
      this._applyResolvedVariables(nameValueMap);
    },

    _applyResolvedVariables: function(nameValueMap) {
      var self = this;

      this.applyVariables(nameValueMap, self.configurationId).done(function (data) {
        var invalidVars = [];
        var successfullyProcessed = [];
          _(data).each(function (variable) {
          var $input = self.$("#" + escapeCss(variable.name));
          var isValid = true;
          if (self._isMultiValue(variable)) {
            var previousValue = nameValueMap[variable.name];
            if (!variable.values.hasOwnProperty(previousValue)) { //if old value doesn't conform to new values map
              var descendants = self.graph.allDescendants(variable.name);
              invalidVars = _.union(invalidVars, descendants); // all descendants concerned to be invalid
              _.intersection(successfullyProcessed, descendants).forEach(function (i) {
                self._disable("#" + escapeCss(i))
              });
              isValid = false;
            }

            if (!_(invalidVars).contains(variable.name)) {
              $input.find("option").remove();
              self._buildOptions($input, variable.values, null);
              successfullyProcessed.push(variable.name)
            } else {
              self._disable($input)
            }
          }
          if (nameValueMap.hasOwnProperty(variable.name) && isValid) {
            $input.val(nameValueMap[variable.name])
          }
        });
        self.trigger("configuration-success");
      }).fail(function (e) {
        self.trigger("configuration-error", e);
      }).always(function () {
        genesis.app.trigger("page-view-loading-completed");
      });
    },

    _linkDependencies: function () {
      this.undelegateEvents();

      var self = this,
          graph = this.graph,
          events = {};

      _(this.graph.allButLeafs()).each(function (node) {
        events["change #" + escapeCss(node)] = function () {
          genesis.app.trigger("page-view-loading-started");

          var descendants = _(graph.allDescendants(node));
          descendants.each(function (name) { self._disable("#" + escapeCss(name)) });

          var resolvedVariables = _.difference(graph.all(), descendants);
          var nameValueMap = self._collectValueObject(resolvedVariables);

          self.applyVariables(nameValueMap, self.configurationId).done(function (data) {
            _(data).each(function (variable) {
              self._enableChecked(variable);
              if (descendants.contains(variable.name) && self._isMultiValue(variable)) {
                self._buildOptions(self.$("#" + escapeCss(variable.name)), variable.values, variable.defaultValue || null);
              }
            });

            var unresolvedVarNames = _(descendants.difference(_(data).pluck("name")));
            _(self.variables).each(function (v) {
              if (v.group && unresolvedVarNames.contains(v.name) && self.$groupVariableRadio(v).is(':checked')) {
                self.$('#' + escapeCss(v.name)).removeAttr("disabled");
              }
            });

          }).fail(function (jqXHR) {
            status.StatusPanel.error(jqXHR);
          }).always(function () {
            genesis.app.trigger("page-view-loading-completed");
          });

        };
      });

      this.delegateEvents(events);
      self.trigger("configuration-success");
    }
  });

  return variables;
});
