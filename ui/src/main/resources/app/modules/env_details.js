define([
  "genesis",
  "services/backend",
  "utils/poller",
  "modules/status",
  "modules/settings/roles",
  "variables",
  "modules/common/templates",
  "modules/common/env_status",
  "use!backbone",
  "jquery",
  "use!jqueryui",
  "use!jvalidate",
  "use!dateformat"
],

function (genesis, backend, poller, status, roles, variables, gtemplates, EnvStatus, Backbone, $) {
  var EnvironmentDetails = genesis.module();

  /**
   * @const
   */
  var PAGE_SIZE = 10;

  EnvironmentDetails.Model = Backbone.Model.extend({
    defaults: {
      "id" : 0,
      "name": "",
      "creator": "",
      "manifest": {},
      "status": "",
      "completed": 0,
      "templateName": "",
      "templateVersion": "",
      "projectId": "",
      "historyCount": 0
    },

    url: function () {
      var url = "/rest/projects/" + this.get("projectId") + "/envs";
      if (this.id) {
        return url + "/" + this.id
      }  else {
        return url;
      }
    }
  });

  var WorkflowHistoryCollection = Backbone.Collection.extend({

    initialize: function(options) {
      this.pageOffset = 0;
      this.pageLength = PAGE_SIZE;
      this.projectId = options.projectId;
      this.envId = options.envId;
    },

    parse: function(json) {
      this.totalCount = json.totalCount;
      return typeof(json.history) != "undefined" ? json.history : [];
    },

    url: function() {
      return "/rest/projects/" + this.projectId + "/envs/" + this.envId + "/history?" + $.param({"page_offset": this.pageOffset, "page_length": this.pageLength});
    },

    refresh: function() {
      this.pageOffset = 0;
      this.fetch();
    },

    pageInfo: function() {
      var info = {
        total: this.totalCount,
        page: Math.floor(this.pageOffset / this.pageLength) + 1,
        perPage: this.pageLength,
        pages: Math.ceil(this.totalCount / this.pageLength),
        prev: false,
        next: false
      };

      if (info.page > 1) {
        info.prev = info.page - 1;
      }
      if (info.page < info.pages) {
        info.next = info.page + 1;
      }
      return info;
    },

    nextPage: function() {
      if (!this.pageInfo().next) {
        return false;
      }
      this.pageOffset = this.pageOffset + this.pageLength;
      return this.fetch();
    },

    previousPage: function() {
      if (!this.pageInfo().prev) {
        return false;
      }
      this.pageOffset = this.pageOffset - this.pageLength;
      return this.fetch();
    }
  });

  var ActionTracking = Backbone.Model.extend ({
    parse: function(json) {
      if (json.startedTimestamp) {
        json.started = new Date(json.startedTimestamp);
      }
      if (json.finishedTimestamp) {
        json.finished = new Date(json.finishedTimestamp);
      }
      return json;
    }
  });

  var StepLogCollection = Backbone.Collection.extend ({
    model: ActionTracking,

    initialize: function(values, options) {
      this.projectId = options.projectId;
      this.envId = options.envId;
      this.stepId = options.stepId;
    },

    url: function() {
      return "/rest/projects/" + this.projectId + "/envs/" + this.envId + "/steps/" + this.stepId + "/actions";
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

    events: {
      "click #tab3": "renderWorkflowList",
      "click .action-button:not(.disabled)": "executeWorkflow",
      "click .cancel-button:not(.disabled)": "cancelWorkflow",
      "click .reset-button:not(.disabled)": "resetEnvStatus",
      "click a.show-sources" : "showSources",
      "click a.envname": "showEditName",
      "click a#update-name": "updateName",
      "click a#cancel-name": "hideEditName"
    },

    initialize: function (options) {
      this.details = new EnvironmentDetails.Model({"id": options.envId, projectId: options.projectId});

      poller.PollingManager.start(this.details);

      this.details.bind("change:status", this.updateControlButtons, this);
      this.details.bind("change:vms", this.renderVirtualMachines, this);
      this.details.bind("change:servers", this.renderServers, this);
      this.details.bind("change:servers change:vms", this.checkServersAndVms, this);
      this.details.bind("change:attributes", this.renderAttributes, this);

      this.executeWorkflowDialog = new ExecuteWorkflowDialog().
        bind('workflow-started', function(workflow) {
          status.StatusPanel.information("Workflow '" + workflow.name + "' execution started");
        }).
        bind('workflow-starting-error', function(workflow, errors) {
          status.StatusPanel.error(errors);
        });

      this.historyCollection = new WorkflowHistoryCollection({"projectId": options.projectId, "envId": options.envId});
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

    cancelWorkflow: function () {
      this.confirmationDialog.dialog('open');
    },

    resetEnvStatus: function () {
      this.resetEnvStatusDialog.dialog('open');
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
            self.executeWorkflowDialog.showFor(self.details.get("projectId"), workflow, self.details.get("id"), self.details.get('templateName'), self.details.get('templateVersion'));
          }
        }).fail(function(jqXHR) {
          status.StatusPanel.error(jqXHR);
        }).always(function(){
          genesis.app.trigger("page-view-loading-completed");
        });
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

    showEditName: function() {
      $('h1 > a.envname').hide();
      $('#nameedit').show();
    },

    hideEditName: function() {
      $('#nameedit').hide();
      $('h1 > a.envname').show();
    },

    updateName: function() {
      var view = this;
      genesis.app.trigger("page-view-loading-started");
      var name = $("#new-name").val();
      $.when(backend.EnvironmentManager.updateEnvName(view.details.get("projectId"), view.details.get('id'), name)).done(
        function() {
          $('a.envname').html(name);
          view.hideEditName();
          genesis.app.trigger("page-view-loading-completed");
        }
      ).fail(
        function(jqxhr) {
          genesis.app.trigger("page-view-loading-completed");
          status.StatusPanel.error(jqxhr);
        }
      );
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
          attributes: view.details.get("attributes"),
          environment: view.details.toJSON()
        }));
      });
    },

    renderWorkflowList: function () {
      if (this.workflowHistory == null) {
        this.workflowHistory = new WorkflowHistoryView({model: this.details, collection: this.historyCollection, el: "#panel-tab-3"});
        this.workflowHistory.render();
      }
    },

    renderAccess: function() {
      var access = new EnvAccess({}, { projectId: this.details.get("projectId"), envId: this.details.id });
      this.accessView = new AccessListView({
        el: "#panel-tab-4",
        accessConfiguration: access,
        projectId: this.details.get("projectId"),
        tabHeader: this.$("#permissions-tab-header")
      });
    },

    _renderAllSubViews: function() {
      var statusView = new EnvStatus.View({el: this.$(".env-status"), model: this.details});
      statusView.render();

      this.renderVirtualMachines();
      this.renderServers();
      this.checkServersAndVms();
      this.renderWorkflowList();
      this.renderAttributes();
      this.renderAccess();
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
      }).fail(function() {
          genesis.app.trigger("server-communication-error",
            "Failed to get environment details<br/><br/> Please contact administrator.",
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
                status.StatusPanel.information("Environment status was changed to 'Ready'");
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
    }
  });

  var ExecuteWorkflowDialog = Backbone.View.extend({
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
      var vals = {};
      if (this.workflow.variables.length > 0) {
        if (!$('#workflow-parameters-form').valid()) {
          return;
        }

        $('.workflow-variable').each(function () {
          vals[$(this).attr('name')] = $(this).val();
        });
      }
      var execution = backend.WorkflowManager.executeWorkflow(this.projectId, this.envId, this.workflow.name, vals);

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
            if(response.statusText && response.statusText  === "timeout") {
              json = {compoundVariablesErrors: [], compoundServiceErrors: ["Timeout: server taking too long time to respond"] }
            } else {
              json = {compoundVariablesErrors: [], compoundServiceErrors: ["Internal server error"] }
            }
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
        variables.processVars({
          variables: view.workflow.variables,
          projectId: view.projectId,
          workflowName: view.workflow.name,
          templateName: view.templateName,
          templateVersion: view.templateVersion
        });

        genesis.app.trigger("page-view-loading-completed");
        view.$el.html(tmpl({variables: view.workflow.variables, workflowName: view.workflow.name}));

        view.$el.dialog({
          title: 'Execute ' + view.workflow.name,
          width: _.size(view.workflow.variables) > 0 ? 600 : 400,
          autoOpen: true,
          buttons: {
            "Run": function(e) {
              var $thisButton = $(e.target).parent("button");
              $thisButton.button("disable");

              view.unbind("workflow-validation-errors");
              view.bind("workflow-validation-errors", function() {
                $thisButton.button("enable");
              });

              view.runWorkflow();
            },

            "Cancel": function () {
              $(this).dialog( "close" );
            }
          }
        });
      });
    }
  });

  var WorkflowHistoryView = Backbone.View.extend({
    template: "app/templates/env_details/workflow_history.html",

    events: {
      "click .toggle": "displayDetails",
      "click .next-page": "nextPage",
      "click .previous-page": "previousPage",
      "click .show-details": "toggleStepDetails",
      "click .step-details-expander": "toggleStepActionsView",
      "click .back": "render"
    },

    initialize: function () {
      this.expandedSections = {};
      this.actionVews = {};
      this.collection.fetch();
      this.collection.bind("reset", this.render, this);
      this.model.bind("change:historyCount change:status change:workflowCompleted", this.collection.refresh, this.collection);
    },

    toggleStepActionsView: function(event) {
      var $currentTarget = $(event.currentTarget),
        $details = $($currentTarget.attr("data-actions-rel")),
        stepId = parseInt($currentTarget.attr("data-step-id")),
        stepStatus = $currentTarget.attr("data-step-status");

      if (!$details.is(":visible")) {
        var logsCollection = new StepLogCollection([], {
          "projectId": this.collection.projectId,
          "envId": this.collection.envId,
          "stepId": stepId
        });

        this.actionVews[stepId] = new StepLogView({
          el: $details.children(".subtable"),
          collection: logsCollection,
          isStepFinished: !(stepStatus === "Requested" || stepStatus === "Executing")
        });
      } else {
        delete this.actionVews[stepId];
      }

      $details.toggle();
    },

    displayDetails: function(event) {
      var $element = $(event.currentTarget).parent(),
          $details = $element.siblings("div.history-details"),
          timestamp = $element.attr("data-timestamp");

      if ($.browser.webkit) {
        $details.toggle();
      } else {
        $details.slideToggle("fast");
      }

      this.expandedSections[timestamp] = !this.expandedSections[timestamp];
    },

    toggleStepDetails: function (event) {
      var $link = $(event.currentTarget),
        $element = $($link.attr("rel"));

      $link.children("span").toggleClass("ui-icon-carat-1-n ui-icon-carat-1-s");
      $element.toggle();
    },

    nextPage: function() {
      this.collection.nextPage();
      return false;
    },

    previousPage: function() {
      this.collection.previousPage();
      return false;
    },

    render: function () {
      var histories = this.collection;

      if (histories.length > 0 && histories.pageInfo().page == 1) { // expand the first section
        var timestamp = histories.at(0).attributes.executionStartedTimestamp;
        this.expandedSections[timestamp] = this.expandedSections[timestamp] || true;
      }

      var view = this;
      $.when(genesis.fetchTemplate(this.template)).done(function (tmpl) {

        var htmls = _.chain(view.actionVews).keys().reduce(function(memo, item) {   //real hardcore!
          memo[item] = view.actionVews[item].html();
          return memo;
        }, {}).value();

        var pageInfo = view.collection.pageInfo();
        view.$el.html(tmpl({
          workflows: histories,
          page: pageInfo.page,
          pagesCount: pageInfo.pages,
          expandedSections: view.expandedSections,
          expandedStepsHtml: htmls,
          utils: genesis.utils
        }));

        _.chain(view.actionVews).keys().each(function(stepId) {
          view.actionVews[stepId].setElement(view.$("#step-"+ stepId + "-actions .subtable"));
          view.actionVews[stepId].refresh();
        });
      });
    }

  });

  var StepLogView = Backbone.View.extend({
    template: "app/templates/env_details/steplog.html",

    events: {
      "click .refresh": "refresh"
    },

    initialize: function(options) {
      this.isStepFinished = options.isStepFinished;
      $.when(this.collection.fetch()).done(_.bind(this.render, this));
    },

    html: function() {
      return this.$el.html();
    },

    refresh: function() {
      if(!this.isStepFinished) {
        $.when(this.collection.fetch()).done(_.bind(this.render, this));
      }
    },

    render: function() {
      var self = this;
      $.when(genesis.fetchTemplate(this.template)).done(function(tmpl){
        self.$el.html(tmpl({
          actions: self.collection.toJSON(),
          isStepFinished: self.isStepFinished,
          projectId: self.collection.projectId,
          envId: self.collection.envId,
          utils: genesis.utils
        }));
      });
    }
  });

  var EnvAccess = Backbone.Model.extend({
    initialize: function(attr, options){
      this.envId = options.envId;
      this.projectId = options.projectId;
    },

    url: function() {
      return "/rest/projects/" + this.projectId + "/envs/" + this.envId + "/access"
    },

    isNew: function() {
      return false;
    }
  });

  var AccessListView = Backbone.View.extend({
    template: "app/templates/environment/access_list.html",

    events: {
      "click a.modify-access" : "modifyAccess"
    },

    initialize: function(options){
      this.accessConfiguration = options.accessConfiguration;
      var self = this;
      if (genesis.app.currentConfiguration['environment_security_enabled']) {
        backend.AuthorityManager.haveAdministratorRights(options.projectId).done(function(isAdmin) {
          if (isAdmin) {
            options.tabHeader.show();
            $.when(self.accessConfiguration.fetch()).done(
              _.bind(self.render, self)
            );
          }
        });
      }
    },

    modifyAccess: function() {
      var editView = new roles.Views.Edit({
        el: this.el,
        role: this.accessConfiguration,
        title: "Grant access to environment"
      });
      var self = this;
      editView.bind("back", function() {
        editView.unbind();
        editView.undelegateEvents();
        self.render();
      });
    },

    render: function(){
      var self = this;
      $.when(genesis.fetchTemplate(this.template)).done(function(tmpl) {
        self.$el.html(tmpl({ accessConfiguration: self.accessConfiguration.toJSON() }));
      });
    }
  });

  return EnvironmentDetails;
});
