define([
  "genesis",
  "services/backend",
  "utils/poller",
  "modules/status",
  "modules/env_details/history",
  "modules/common/variables",
  "modules/common/templates",
  "modules/common/env_status",
  "backbone",
  "jquery",
  "momentjs",
  "jqueryui",
  "jvalidate"
],

function (genesis, backend, poller, status, EnvHistory, variablesmodule, gtemplates, EnvStatus, Backbone, $) {
  var EnvironmentDetails = genesis.module();

  EnvironmentDetails.Model = Backbone.Model.extend({
    urlRoot: function () {
      return "rest/projects/" + this.get("projectId") + "/envs";
    },

    parse: function(json) {
      if(json.timeToLive) {
        json.timeToLiveStr = moment().add(json.timeToLive).fromNow();
      } else {
        this.unset("timeToLive", {silent: true}); //todo: is it a backbone bug??
        this.unset("timeToLiveStr", {silent: true});
      }
      return json;
    }
  });

  EnvironmentDetails.Views.Details = Backbone.View.extend({
    template: "app/templates/env_details.html",

    vmsTemplate: "app/templates/env_details/vm_table.html",
    staticServersTemplate: "app/templates/env_details/servers_table.html",
    envAttributesTemplate: "app/templates/common/attribute_table.html",

    details: null,
    historyCollection: null,
    workflowHistory: null,

    confirmationDialog: null,
    executeWorkflowDialog: null,
    resetEnvStatusDialog: null,
    envRenameDialog: null,

    events: {
      "click #tab3": "renderWorkflowList",
      "click .action-button:not(.disabled)": "executeWorkflow",
      "click .cancel-button:not(.disabled)": "cancelWorkflow",
      "click .reset-button:not(.disabled)": "resetEnvStatus",
      "click a.show-sources" : "showSources",
      "click a.postpone-destruction" : "postponeDestruction",
      "click a.rename-button": "renameEnv"
    },

    initialize: function (options) {
      this.details = new EnvironmentDetails.Model({"id": options.envId, projectId: options.projectId});

      poller.PollingManager.start(this.details);

      this.details.bind("change:status", this.updateControlButtons, this);
      this.details.bind("change:vms", this.renderVirtualMachines, this);
      this.details.bind("change:servers", this.renderServers, this);
      this.details.bind("change:servers change:vms", this.checkServersAndVms, this);
      this.details.bind("change:attributes change:modificationTime change:timeToLiveStr", this.renderAttributes, this);
      this.details.bind("change:name", this.onRename, this);
      this.executeWorkflowDialog = new ExecuteWorkflowDialog().
        bind('workflow-started', function(workflow) {
          status.StatusPanel.information("Workflow '" + workflow.name + "' execution started");
        }).
        bind('workflow-starting-error', function(workflow, errors) {
          status.StatusPanel.error(errors);
        });

      this.historyCollection = new EnvHistory.Collection([], {"projectId": options.projectId, "envId": options.envId});
    },

    onClose: function () {
      poller.PollingManager.stop(this.details);
      if(this.executeWorkflowDialog) {
        this.executeWorkflowDialog.destroy();
        this.executeWorkflowDialog = null;
      }
      if(this.confirmationDialog) {
        this.confirmationDialog.dialog('destroy').remove();
      }
      if(this.resetEnvStatusDialog) {
        this.resetEnvStatusDialog.dialog('destroy').remove();
      }
      if (this.envRenameDialog) {
        this.envRenameDialog.dialog('destroy').remove();
      }
      if(this.expandLifeTimeDialog) {
        this.expandLifeTimeDialog.dialog('destroy').remove();
      }

      genesis.utils.nullSafeClose(this.workflowHistory);
      genesis.utils.nullSafeClose(this.sourcesView);
      genesis.utils.nullSafeClose(this.accessView);
    },

    showSources: function(event) {
      var $currentTarget = $(event.currentTarget),
          templateName = $currentTarget.attr("data-template-name"),
          templateVersion = $currentTarget.attr("data-template-version");

      if(!this.sourcesView) {
        this.sourcesView = new gtemplates.SourcesView({
          el: $("<pre class='prettyprint linenums' id='template-source-view'></pre>"),
          projectId: this.details.get("projectId")
        });
      }

      this.sourcesView.showTemplate(templateName, templateVersion);
    },

    postponeDestruction: function(){
      this.expandLifeTimeDialog.dialog('open');
      //backend.EnvironmentManager.expandLiveTime(this.details, 42);
    },

    cancelWorkflow: function () {
      this.confirmationDialog.dialog('open');
    },

    resetEnvStatus: function () {
      this.resetEnvStatusDialog.dialog('open');
    },

    renameEnv: function() {
      this.envRenameDialog.dialog('open');
    },

    onRename: function() {
      if (this.details.previous("name") === "") return;
      this.$('.envname').html(this.details.get('name'));
    },

    executeWorkflow: function (e) {
      var workflowName = e.currentTarget.rel;

      genesis.app.trigger("page-view-loading-started");

      var template = new gtemplates.TemplateModel(
        {
          name: this.details.get('templateName'),
          version: this.details.get('templateVersion')
        },
        {
          projectId: this.details.get("projectId")
        }
      );

      var self = this;

      $.when(template.fetch())
        .done(function() {
          var workflow = _(template.get('workflows')).find(function (item) {
            return item.name === workflowName;
          });

          if (workflow) {
            var wmodel = new gtemplates.WorkflowModel({
                name: self.details.get('templateName'),
                version: self.details.get('templateVersion')
              },
              {
                projectId: self.details.get("projectId"),
                workflow: workflowName,
                instanceId: self.details.get("id")
              });

            $.when(wmodel.fetch()).done(function(){
              self.executeWorkflowDialog.showFor(self.details.get("projectId"), wmodel.get("result"), self.details.get("id"),
              self.details.get('templateName'), self.details.get('templateVersion'));
            }).fail(function(jqXHR){
              genesis.app.trigger("page-view-loading-completed");
              status.StatusPanel.error(jqXHR);
            });
          }
        }).fail(function(jqXHR) {
          status.StatusPanel.error(jqXHR);
          genesis.app.trigger("page-view-loading-completed");
        })
    },

    updateControlButtons: function () {
      var status = this.details.get('status'),
          activeExecution = (status === "Busy");

      this.$(".cancel-button")
        .toggleClass("disabled", !activeExecution)
        .toggle(status !== "Destroyed");

      this.$(".action-button")
        .toggleClass("disabled", activeExecution)
        .toggle(status !== "Destroyed");

      this.$("#resetBtn")
        .toggle(status === "Broken");
    },

    renderVirtualMachines: function() {
      var view = this;
      var vms = view.details.get("vms");
      $.when(genesis.fetchTemplate(this.vmsTemplate)).done(function(tmpl) {
        view.$("#vm-list").html(tmpl({
          vms : _.filter(vms, function(vm) { return vm.status !== "Destroyed"; })
        }));
      });
    },

    renderServers: function() {
      var servers = this.details.get("servers"),
          view = this;

      $.when(genesis.fetchTemplate(this.staticServersTemplate)).done(function(tmpl) {
        view.$("#servers-list").html(tmpl({
          servers : _.filter(servers, function(server) { return server.status !== "Released"; })
        }));
      });
    },

    checkServersAndVms: function() {
      if(_.all(this.details.get("vms"), function(vm) { return vm.status === "Destroyed" }) &&
        _.all(this.details.get("servers"), function(server) { return server.status === "Released"; })) {
        this.$("#no-servers-message").show();
      } else {
        this.$("#no-servers-message").hide();
      }
    },

    renderAttributes: function() {
      var view = this;
      $.when(genesis.fetchTemplate(this.envAttributesTemplate)).done(function(tmpl) {
        view.$("#panel-tab-1").html(tmpl({
          attributes: _.sortBy(view.details.get("attributes"), function(attr) { return attr.description; }),
          environment: view.details.toJSON(),
          utils: genesis.utils
        }));
      });
    },

    renderWorkflowList: function () {
      if (this.workflowHistory == null) {
        this.workflowHistory = new EnvHistory.View({model: this.details, collection: this.historyCollection, el: "#panel-tab-3"});
        this.workflowHistory.render();
      }
    },

    _renderAllSubViews: function() {
      var statusView = new EnvStatus.View({el: this.$(".env-status"), model: this.details});
      statusView.render();

      this.renderVirtualMachines();
      this.renderServers();
      this.checkServersAndVms();
      this.renderWorkflowList();
      this.renderAttributes();
    },

    render: function () {
      var view = this;
      $.when(
        genesis.fetchTemplate(this.template),
        genesis.fetchTemplate(this.staticServersTemplate),  //prefetching template
        genesis.fetchTemplate(this.vmsTemplate),            //prefetching template
        this.details.fetch()
      ).done(function (tmpl) {
          var details = view.details.toJSON();

          view.$el.html(tmpl({
            environment: details,
            actions: _(details.workflows).reject(function (flow) {
              return flow.name === details.createWorkflowName
            })
          }));

          view.updateControlButtons();
          view._renderAllSubViews();

          view.confirmationDialog = view.createConfirmationDialog(view.$("#dialog-confirm"));
          view.resetEnvStatusDialog = view.createResetEnvStatusDialog(view.$("#dialog-reset"));
          view.envRenameDialog = view.createRenameDialog(view.$("#env-rename"));
          view.expandLifeTimeDialog = view.createExpandLifeTimeDialog(view.$("#expand-env-life-time"))
      }).fail(function() {
          genesis.app.trigger("server-communication-error",
            "Failed to get instance details<br/><br/> Please contact administrator.",
            "/project/" + view.details.get("projectId")
          );
      }).always(function(){
          genesis.app.trigger("page-view-loading-completed");
      });
    },

    createConfirmationDialog: function (element) {
      var self = this;
      return element.dialog({
        title: 'Confirmation',
        buttons: {
          "Yes": function () {

            $.when(backend.WorkflowManager.cancelWorkflow(self.details.get("projectId"), self.details.get('id')))
              .done(function() {
                status.StatusPanel.information("'Cancel workflow' signal sent");
              })
              .fail(function() {
                status.StatusPanel.error("Failed to sent 'Cancel workflow' signal");
              });
            $(this).dialog("close");
          },

          "No": function () {
            $(this).dialog("close");
          }
        }
      });
    },

    createResetEnvStatusDialog: function (element) {
      var self = this;
      return element.dialog({
        title: 'Confirmation',
        buttons: {
          "Yes": function () {
            $.when(backend.EnvironmentManager.resetEnvStatus(self.details.get("projectId"), self.details.get('id')))
              .done(function() {
                status.StatusPanel.information("Instance status was changed to 'Ready'");
              })
              .fail(function(jqXHR) {
                status.StatusPanel.error(jqXHR);
              });
            $(this).dialog("close");
          },
          "No": function () {
            $(this).dialog("close");
          }
        }
      });
    },

    createRenameDialog: function($element) {
      var view = this;
      return $element.dialog({
        title: 'Rename instance',
        buttons: {
          "Save": function () {
            var dialog = this;
            genesis.app.trigger("page-view-loading-started");
            var name = $('input:text:first', this).val();
            $.when(backend.EnvironmentManager.updateEnvName(view.details.get("projectId"), view.details.get('id'), name)).done(function() {
              view.details.set('name', name);
              genesis.app.trigger("breadcrumb:changed", {projectId: view.details.get("projectId"), name: name});
              $(dialog).dialog("close");
            }).fail(function(jqxhr) {
                status.StatusPanel.error(jqxhr);
            }).always(function() {
              genesis.app.trigger("page-view-loading-completed");
            });
          },
          "Cancel": function () {
            $(this).dialog("close");
          }
        },
        open: function() {
          $('input:text:first', this).val(view.details.get("name")).select().focus();
        }
      });
    },

    createExpandLifeTimeDialog: function($element) {
      var view = this;
      return $element.dialog({
        title: 'Expand time to live',
        buttons: {
          "Set": function () {
            var dialog = this;
            genesis.app.trigger("page-view-loading-started");
            var ttl = $(this).find('#new-env-ttl').val();
            $.when(backend.EnvironmentManager.expandLifeTime(view.details, ttl)).done(function() {
              $(dialog).dialog("close");
              status.StatusPanel.information(ttl ? "Instance lifespan was updated" : "Instance lifespan was set to 'unlimited'");
              view.details.set("timeToLive", ttl * 1000)
            }).fail(function(jqxhr) {
              status.StatusPanel.error(jqxhr);
            }).always(function() {
              genesis.app.trigger("page-view-loading-completed");
            });
          },
          "Cancel": function () {
            $(this).dialog("close");
          }
        },
        open: function() {
          $('input:text:first', this).val(view.details.get("name")).select().focus();
        }
      });
    }
  });

  var ExecuteWorkflowDialog = variablesmodule.Views.AbstractWorkflowParamsView.extend({
    template: "app/templates/environment_variables.html",

    initialize: function() {
      this.$el.id = "#workflowParametersDialog";
    },

    showFor: function(projectId, workflow, envId, templateName, templateVersion) {
      this.workflow = workflow;
      this.envId = envId;
      this.projectId = projectId;
      this.templateName = templateName;
      this.templateVersion = templateVersion;
      this.render();
    },

    destroy: function() {
      this.unbind();
      this.$el.dialog('destroy');
      this.remove();
    },

    runWorkflow: function() {
      if (this.workflow.variables.length > 0) {
        if (!$('#workflow-parameters-form').valid()) {
          this.trigger("workflow-validation-errors");
          return;
        }

      }
      var execution = backend.WorkflowManager.executeWorkflow(this.projectId, this.envId, this.workflow.name, this.workflowParams());

      var view = this;
      $.when(execution).then(
        function success() {
          view.trigger("workflow-started", view.workflow);
          view.$el.dialog("close");
        },
        function fail(response) {
          var json = {};
          try {
            json = JSON.parse(response.responseText);
          } catch (e) {
            json = {compoundVariablesErrors: [], compoundServiceErrors: ["Internal server error"] }
          }
          if (_.isEmpty(json.variablesErrors)) {
            var errors = _.union(
              json.compoundVariablesErrors,
              json.compoundServiceErrors,
              _.values(json.serviceErrors)
            );
            view.trigger("workflow-starting-error", view.workflow, errors);
            view.$el.dialog("close");
          } else {
            view.trigger("workflow-validation-errors");
            var validator = $('#workflow-parameters-form').validate();
            validator.showErrors(json.variablesErrors);
          }
        }
      );

    },

    render: function() {
      var view = this;
      $.when(genesis.fetchTemplate(this.template)).done(function (tmpl) {

        genesis.app.trigger("page-view-loading-completed");
        view.$el.html(tmpl({noVariables: view.workflow.variables.length == 0, workflowName: view.workflow.name}));

        var inputsView = new variablesmodule.Views.InputControlsView({
          el: view.$('#workflow_vars'),
          variables: view.workflow.variables,
          projectId: view.projectId,
          workflow: view.workflow.name,
          template: new Backbone.Model({name: view.templateName, version: view.templateVersion})
        });

        inputsView.render(function() {
          view.$el.dialog({
            title: 'Execute ' + view.workflow.name,
            width: _.size(view.workflow.variables) > 0 ? 1052 : 400,
            autoOpen: true,
            buttons: {
              "Run": function() {
                var $thisButton = $(this).parent().find("button:contains('Run')"),
                  disabled = $thisButton.button( "option", "disabled" );
                if(!disabled) {
                  $thisButton.button("disable");

                  view.unbind("workflow-validation-errors");
                  view.bind("workflow-validation-errors", function() {
                    $thisButton.button("enable");
                  });

                  view.runWorkflow();
                }
              },

              "Cancel": function () {
                $(this).dialog( "close" );
              }
            }
          });
        });
      });
    },

    /* override */ variablesModel: function() {
       return this.workflow.variables;
    }

  });

  return EnvironmentDetails;
});
