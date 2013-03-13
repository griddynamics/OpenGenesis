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
  "jvalidate",
  "datetimepicker"
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
      this.parseLinks(json.links || []);
      return json;
    },

    parseLinks: function(links) {
      this.renameLink = _(links).find(backend.LinkTypes.EnvironmentDetails.edit);
      this.resetStatusLink = _(links).find(backend.LinkTypes.ResetAction.any);
      this.cancelLink = _(links).find(backend.LinkTypes.CancelAction.any);
      this.workflowsLink = _(links).find(backend.LinkTypes.Workflow.any);
    },

    canRename: function() {
      return !_.isUndefined(this.renameLink);
    },

    canResetStatus: function() {
      return !_.isUndefined(this.resetStatusLink);
    },

    canCancelWorkflow: function() {
      return !_.isUndefined(this.cancelLink);
    },

    canRunWorkflows: function() {
      return !_.isUndefined(this.workflowsLink)
    }
  });

  var ScheduledJob = genesis.Backbone.Model.extend({
    linkType: backend.LinkTypes.EnvScheduledJob
  });

  var EnvJobs = genesis.Backbone.Collection.extend({
    linkType: backend.LinkTypes.EnvScheduledJob,

    model: ScheduledJob,

    initialize: function(atts, options) {
      this.projectId = options.projectId;
      this.envId = options.envId;
    },

    url: function(){
      return "rest/projects/" + this.projectId + "/envs/" + this.envId + "/jobs";
    },

    clone: function() {
      return new this.constructor(this.models, {projectId: this.projectId, envId: this.envId});
    }
  });

  var FailedEnvJobs = EnvJobs.extend({
    url: function() {
      return "rest/projects/" + this.projectId + "/envs/" + this.envId + "/failedJobs";
    }
  });

  EnvironmentDetails.Views.SingleWorkflowView = Backbone.View.extend({
    template: "app/templates/env_details/workflow_details.html",

    initialize: function (options) {
      this.model = new EnvHistory.WorkflowHistoryModel({}, {workflowId: options.workflowId, projectId: options.projectId, envId: options.envId});
    },

    render: function () {
      var view = this;
      $.when(genesis.fetchTemplate(this.template), genesis.fetchTemplate(new EnvHistory.WorkflowHistoryView().template)).done(function (tmpl) {
        $.when(view.model.fetch()).done(function() {
          view.$el.html(tmpl({ workflow: view.model.toJSON() }));
          var subview = new EnvHistory.WorkflowHistoryView({
            expanded: true,
            model: view.model,
            el: view.$('div.workflow-section[data-workflow-id="' + view.model.id + '"]')
          });

          var failedSteps = _.chain(view.model.get("steps")).
            filter(function(i){ return i.status == "Failed" }).
            map(function(i) { return parseInt(i.stepId) }).
            value();
          subview.render(function() {
            _(failedSteps).each(function(stepId) {
              subview.showStepActions(stepId)
            });
          });
        }).fail(function(error) {
          if(error.status === 400) {
            view.$el.html(tmpl({ workflow: view.model }));
            var panel = new status.LocalStatus({ el: self.$(".notification")});
            panel.error(JSON.parse(error.responseText).error)
          }
        })
      });
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
      "click .schedule-button:not(.disabled)": "executeWorkflowClick",
      "click .action-button:not(.disabled)": "executeWorkflowClick",
      "click .cancel-button:not(.disabled)": "cancelWorkflow",
      "click .reset-button:not(.disabled)": "resetEnvStatus",
      "click a.show-sources" : "showSources",
      "click a.postpone-destruction" : "postponeDestruction",
      "click a.rename-button": "renameEnv"
    },

    initialize: function (options) {
      function capitalise(string) {
        return string.charAt(0).toUpperCase() + string.slice(1);
      }

      this.details = new EnvironmentDetails.Model({"id": options.envId, projectId: options.projectId});
      this.workflowId = options.workflowId;
      this.envJobs = new EnvJobs({}, {projectId: this.details.get("projectId"), envId: this.details.id});

      poller.PollingManager.start(this.details, { noninterruptible: true });

      this.details.bind("change:status", this.updateControlButtons, this);
      this.details.bind("change:vms", this.renderVirtualMachines, this);
      this.details.bind("change:servers", this.renderServers, this);
      this.details.bind("change:servers change:vms", this.checkServersAndVms, this);
      this.details.bind("change:attributes change:modificationTime change:timeToLiveStr", this.renderAttributes, this);
      this.details.bind("change:name", this.onRename, this);

      var self = this;
      this.executeWorkflowDialog = new ExecuteWorkflowDialog().
        bind('workflow-scheduled', function(workflow) {
          status.StatusPanel.information("'" + capitalise(workflow.name) + "' execution was scheduled successfully");
          self.envJobs.fetch()
        }).
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

      genesis.utils.nullSafeClose(this.scheduledJobsView);
      genesis.utils.nullSafeClose(this.failedJobsView);
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

    executeWorkflowClick: function (e) {
      var workflowName = e.currentTarget.rel;
      var scheduling = $(e.currentTarget).attr("data-scheduling") || false;
      this.executeWorkflow(workflowName, scheduling);
    },

    executeWorkflow: function (workflowName, scheduling, varValues) {
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

      $.when(template.fetch()).done(function () {
        var workflow = _(template.get('workflows')).find(function (item) {
          return item === workflowName;
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

          $.when(wmodel.fetch()).done(function () {
            self.executeWorkflowDialog.showFor(self.details, wmodel, scheduling, varValues);
          }).fail(function (jqXHR) {
            genesis.app.trigger("page-view-loading-completed");
            status.StatusPanel.error(jqXHR);
          });
        }
      }).fail(function (jqXHR) {
        status.StatusPanel.error(jqXHR);
        genesis.app.trigger("page-view-loading-completed");
      })
  },

  updateControlButtons: function () {
      var status = this.details.get('status'),
          activeExecution = (status === "Busy");
      this.$(".cancel-button")
        .toggle(status !== "Destroyed" && this.details.canCancelWorkflow());

      this.$(".button-group.action").toggle(status !== "Destroyed")

      this.$(".action-button")
        .toggleClass("disabled", activeExecution)
        .toggle(status !== "Destroyed");
      this.$("#resetBtn")
        .toggle(this.details.canResetStatus());
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
        view.$("#env-attrs").html(tmpl({
          attributes: _.sortBy(view.details.get("attributes"), function(attr) { return attr.description; }),
          environment: view.details.toJSON(),
          utils: genesis.utils
        }));
      });
    },

    renderWorkflowList: function () {
      if (this.workflowHistory == null) {
        this.workflowHistory = new EnvHistory.View({model: this.details, collection: this.historyCollection, el: "#panel-tab-3", workflowId: this.workflowId});
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

          var actions = _(details.workflows).reject(function (flow) {
            return flow.name === details.createWorkflowName
          });

          view.$el.html(tmpl({
            environment: details,
            actions: view.details.canRunWorkflows() ? actions : []
          }));

          view.$(".rename-button").toggle(view.details.canRename());
          view.$(".cancel-button").toggle(view.details.canCancelWorkflow());

          view.updateControlButtons();
          view._renderAllSubViews();
          if (view.workflowId) {
            view.$("#tab3").click()
          }
          view.confirmationDialog = view.createConfirmationDialog(view.$("#dialog-confirm"));
          view.resetEnvStatusDialog = view.createResetEnvStatusDialog(view.$("#dialog-reset"));
          view.envRenameDialog = view.createRenameDialog(view.$("#env-rename"));
          view.expandLifeTimeDialog = view.createExpandLifeTimeDialog(view.$("#expand-env-life-time"));

          view.scheduledJobsView = new ScheduledJobsView({
            el: view.$("#scheduled-executions"),
            collection: view.envJobs,
            $confirmDialog: view.$("#job-removal-confirm")
          });

          view.scheduledJobsView.render();
          view.scheduledJobsView.bind("request-job-update", function(job){
            view.executeWorkflow(job.get('workflow'), job.get("date"), job.get('variables'));
          });

          view.failedJobsView = new ScheduledJobsView({
            el: view.$("#failed-executions"),
            collection: new FailedEnvJobs({}, {projectId: view.details.get("projectId"), envId: view.details.id}),
            $confirmDialog: view.$("#record-removal-confirm"),
            error: true
          });

          view.failedJobsView.render();

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
              view.details.set("timeToLive", ttl * 1000);
              view.envJobs.fetch();
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
    template: "app/templates/env_details/environment_variables.html",

    initialize: function() {
      this.scheduling = true;
      this.$el.id = "#workflowParametersDialog";
    },

    showFor: function(envDetails, workflow, scheduling, varValues) {
      this.scheduling = scheduling;
      this.workflow = workflow;
      this.envId = envDetails.get("id");
      this.projectId = envDetails.get("projectId");
      this.templateName = envDetails.get('templateName');
      this.templateVersion = envDetails.get('templateVersion');
      this.configurationId = envDetails.get('configurationId');
      this.varValues = varValues;
      this.render();
    },

    destroy: function() {
      this.unbind();
      this.$el.dialog('destroy');
      this.remove();
    },

    executionDate: function() {
      return new Date(this.$("#executionDate").val()).getTime()
    },

    runWorkflow: function() {
      if (this.workflow.get('variables').length > 0) {
        if (!$('#workflow-parameters-form').valid()) {
          this.trigger("workflow-validation-errors");
          return;
        }

      }
      genesis.app.trigger("page-view-loading-started");
      var execution = !this.scheduling ?
        backend.WorkflowManager.executeWorkflow(this.projectId, this.envId, this.workflow.get('name'), this.workflowParams()) :
        backend.WorkflowManager.scheduleWorkflow(this.projectId, this.envId, this.workflow.get('name'), this.workflowParams(), this.executionDate());

      var view = this;
      $.when(execution).then(
        function success() {
          genesis.app.trigger("page-view-loading-completed");
          if(view.scheduling) {
            view.trigger("workflow-scheduled", view.workflow.toJSON());
          } else {
            view.trigger("workflow-started", view.workflow.toJSON());
          }
          view.$el.dialog("close");
        },
        function fail(response) {
          genesis.app.trigger("page-view-loading-completed");
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
            view.trigger("workflow-starting-error", view.workflow.toJSON(), errors);
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
        view.$el.html(tmpl({
          noVariables: view.workflow.get('variables').length == 0,
          workflowName: view.workflow.get('name'),
          scheduling: view.scheduling
        }));

        if(view.scheduling) {
          var date = new Date();
          date.setHours(0);
          date.setMinutes(0);
          date.setMilliseconds(0);
          view.$("#executionDate").datetimepicker({
            minDate: date
          });
          if(_.isNumber(view.scheduling)) {
            view.$("#executionDate").datetimepicker("setDate",  new Date(view.scheduling))
          }
        }

        var inputsView = new variablesmodule.Views.InputControlsView({
          el: view.$('#workflow_vars'),
          variables: view.workflow.get('variables'),
          projectId: view.projectId,
          workflow: view.workflow,
          configurationId: view.configurationId,
          template: new Backbone.Model({name: view.templateName, version: view.templateVersion}),
          variableValues: view.varValues || {}
        });

        inputsView.render(function() {
          var runButton = view.scheduling ? "Schedule" : "Run";
          var buttons = {};
          buttons[runButton] = function() {
            var $thisButton = $(this).parent().find("button:contains('" + runButton + "')"),
              disabled = $thisButton.button( "option", "disabled" );
            if(!disabled) {
              $thisButton.button("disable");

              view.unbind("workflow-validation-errors");
              view.bind("workflow-validation-errors", function() {
                $thisButton.button("enable");
              });

              view.runWorkflow();
            }
          };
          buttons["Cancel"] = function () {
            $(this).dialog( "close" );
          };
          view.$el.dialog({
            title: (view.scheduling ? 'Schedule ' : 'Execute ') + view.workflow.get('name'),
            width: _.size(view.workflow.get('variables')) > 0 ? 1052 : 400,
            autoOpen: true,
            buttons: buttons
          });
        });
      });
    },

    /* override */ variablesModel: function() {
       return this.workflow.get('variables');
    }

  });

  var ScheduledJobsView = Backbone.View.extend({
    template: "app/templates/env_details/scheduled_executions.html",
    events: {
      "click .job-remove": "removeJob",
      "click .job-update": "updateJob",
      "click .job-details": "toggleDetails"
    },

    initialize: function(options){
      this.removeConfigDlg = options.$confirmDialog.dialog({
        title: 'Confirmation',
        buttons: {
          "Yes": function () {
          },
          "No": function () {
            $(this).dialog("close");
          }
        }
      });
      this.error = options.error || false;
      this.collection.bind("reset", this.render, this);
      this.collection.fetch();

      this.collectionPoll = this.collection.clone({projectId: this.collection.projectId, envId: this.collection.envId});
      poller.PollingManager.start(this.collectionPoll, { noninterruptible: true,  delay: 30000 });
      var self = this;

      this.collectionPoll.bind("reset", function(e) {
        if(self.collectionPoll.length != self.collection.length) {
          self.collection.fetch()
        }
      });
    },

    updateJob: function(e) {
      var id = $(e.currentTarget).attr("data-job-id")
        , job = this.collection.get(id);
      this.trigger("request-job-update", job);
    },

    removeJob: function(e) {
      var buttons = this.removeConfigDlg.dialog( "option", "buttons" );
      var id = $(e.currentTarget).attr("data-job-id")
        , job = this.collection.get(id)
        , self = this;

      buttons["Yes"] = function() {
        $.when(job.destroy()).done(function() {
          self.collection.fetch();
          status.StatusPanel.information("Record was successfully removed");
        }).fail(function(jqxhr) {
          status.StatusPanel.error(jqxhr);
        }).always(function(){
          self.removeConfigDlg.dialog("close");
        });
      };
      this.removeConfigDlg.dialog("option", "buttons", buttons);
      this.removeConfigDlg.dialog("open");
    },

    toggleDetails: function(e) {
      var id = $(e.currentTarget).attr("rel");
      $(e.currentTarget).toggleClass("expanded");
      this.$(id).slideToggle("fast");
    },

    onClose: function(){
      this.removeConfigDlg && this.removeConfigDlg.dialog('destroy');
      poller.PollingManager.stop(this.collectionPoll);
    },

    render: function() {
      var self = this;
      $.when(genesis.fetchTemplate(this.template)).done(function(tmpl) {
       if(self.collection.length == 0) {
         self.$el.html("")
       } else {
         self.$el.html(tmpl({
           jobs: _(self.collection.sortBy(function(i) { return self.error ? -i.get('date') : i.get('date') })).map(function(i) { return i.toJSON(); }),
           moment: moment,
           utils: genesis.utils,
           accessRights: self.collection.itemAccessRights(),
           error: self.error
         }));
       }
      });
    }

  });

  return EnvironmentDetails;
});
