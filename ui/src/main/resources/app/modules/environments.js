//todo: clean error message
define([
  "genesis",
  "services/backend",
  "use!backbone",
  "utils/poller",
  "modules/status",
  "variables",
  "modules/common/templates",
  "jquery",
  "use!jqueryui",
  "use!jvalidate",
  "use!showLoading",
  "use!dateformat",
  "use!jstorage",
  "use!multiselect"
],

function (genesis, backend, Backbone, poller, status, variables, gtemplates, $) {
  var Environments = genesis.module();

  /**
   * @const
   */
  var PROCESSING = "assets/img/processing-status.png",
      COMPLETED = "assets/img/normal-status.png",
      BROKEN = "assets/img/bad-status.png",
      DISABLED = "assets/img/disabled-status.png",
      STATUS_IMAGES = {
        "Busy": PROCESSING,
        "Ready": COMPLETED,
        "Destroyed": DISABLED,
        "Broken": BROKEN
      },
      FILTER_DEFAULTS = {
        "namePart" : "",
        "statuses" : {
          "busy" : { "visible" : true, "name" : "Busy" },
          "ready" : { "visible" : true, "name" : "Ready" },
          "destroyed" : { "visible" : false, "name" : "Destroyed" },
          "broken" : { "visible" : true, "name" : "Broken" }
        }
      };

  /**
   * Environment statuses are different for 1.0.x and 1.1.x versions of Genesis
   * so we need to check stored filter and fix it if required.
   */
  Environments.fixFilter = function() {
    var initialFilter = $.jStorage.get(genesis.app.currentUser.user + "_envFilter", FILTER_DEFAULTS);
    if (!_.isEqual(_.keys(initialFilter.statuses), _.keys(FILTER_DEFAULTS.statuses)))
      $.jStorage.set(genesis.app.currentUser.user + "_envFilter", FILTER_DEFAULTS);
  };

  Environments.Model = Backbone.Model.extend({
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

    idAttribute: "id",

    url: function () {
      var url = "/rest/projects/" + this.get("projectId") + "/envs";
      if (this.id) {
        return url + "/" + this.id
      }  else {
        return url;
      }
    }
  });

  var PAGE_SIZE = 10;

  WorkflowHistoryCollection = Backbone.Collection.extend({
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

  Environments.Collection = Backbone.Collection.extend({
    model: Environments.Model,
    project: null,

    initialize: function (model, options) {
      if(!options.project) {
        throw new Error("Project option is required");
      }
      this.project = options.project;

      var filter = this.getFilter();
      filter.namePart = "";
      this.setFilter(filter);
    },

    url: function() {
      var filterParam = "",
          filter = this.getFilter();
      if (this.needBackendFiltering(filter)) {
        filterParam = "?filter=statuses[" +
          _(filter.statuses).reduce(function(memo, status) { return status.visible ? memo + status.name + "," : memo }, filterParam) +
          "]";
      }
      return "/rest/projects/" + this.project.id + "/envs" + filterParam;
    },

    needBackendFiltering: function(filter) {
      return _(filter.statuses).any(function(status) { return !status.visible });
    },

    changeFilter: function(filter) {
      var oldFilter = this.getFilter();
      if (!_.isEqual(filter.statuses, oldFilter.statuses)) {
        this.setFilter(filter);
        this.fetch();
      } else {
        this.setFilter(filter);
        this.trigger("local-filter-changed");
      }
    },

    setFilter: function(filter) {
      $.jStorage.set(genesis.app.currentUser.user + "_envFilter", filter);
    },

    getFilter: function() {
      return $.jStorage.get(genesis.app.currentUser.user + "_envFilter", FILTER_DEFAULTS);
    },

    filterToJSON: function() {
      var filtered = this.models,
          filter = this.getFilter();
      if (filter.namePart.trim().length > 0) {
        // filter by name's part
        filtered = _(filtered).filter( function(model) {
          var searchExpr = new RegExp(filter.namePart.trim(), 'i');
          return model.get("name").search(searchExpr) >= 0;
        });
      }
      return _(filtered).map(function(model) { return model.toJSON() });
    }
  });

  var TemplateDesc = Backbone.Model.extend({
    initialize: function (model, options) {
      if(!options.projectId) {
        throw new Error("Project option is required");
      }
      this.projectId = options.projectId;
    },

    url: function() {
      return "/rest/projects/" + this.projectId + "/templates/" + this.get("name") + "/v" + this.get("version");
    }
  });

  Environments.Views.List = Backbone.View.extend({

    template: "app/templates/environments_overview.html",
    subviews: [],

    initialize: function () {
      this.project = this.collection.project;
    },

    onClose: function () {
      _(this.subviews).each(function (view) {
        view.close()
      });
    },

    events: {
      "keyup #filter-name": "nameFilterChanged",
      "multiselectclick #filter-statuses": "statusFilterChanged",
      "multiselectcheckall #filter-statuses": "statusFilterChanged",
      "multiselectuncheckall #filter-statuses": "statusFilterChanged"
    },

    nameFilterChanged: function() {
      this.collection.changeFilter(this.readFilterValues());
    },

    statusFilterChanged: function(e, ui) {
      var newFilter = $.extend(true, {}, this.collection.getFilter());
      if (e.type == "multiselectclick") {
        newFilter.statuses[ui.value.toLowerCase()].visible = ui.checked;
      } else {
        var checked = (e.type == "multiselectcheckall" ? true : false);
        _(newFilter.statuses).each(function(status) {
          newFilter.statuses[status.name.toLowerCase()].visible = checked;
        });
      }
      this.collection.changeFilter(newFilter);
    },

    readFilterValues: function() {
      var $nameFilter = this.$("#filter-name"),
          $advancedCheck = this.$("#advanced-check"),
          $statusOptions = this.$("#filter-statuses option"),
          currentFilter = {};

      currentFilter.namePart = $nameFilter.val();
      currentFilter.statuses = {};
      _($statusOptions).each(function(option) {
        var status = {},
            name = $(option).attr("value"),
            selected = $(option).attr("selected") != undefined ? true : false;
        currentFilter.statuses[name.toLowerCase()] = { "visible" : selected, "name" : name };
      });
      return currentFilter;
    },

    render: function () {
      var view = this;
      $.when(
        genesis.fetchTemplate(this.template),
        backend.AuthorityManager.haveAdministratorRights(this.project.id),
        this.collection.fetch()
      ).done(function (tmpl, isAdmin) {
        view.$el.html(tmpl({ "project": view.project.toJSON(), "filter": view.collection.getFilter() }));
        if(isAdmin) {
          view.$("#project-settings").show();
        }
        view.$("#filter-statuses").multiselect({
          noneSelectedText: "no statuses selected",
          selectedText: "# statuses selected",
          height: "auto"
        });
        view.tableList = new EnvironmentsTableView({
          "el": view.$('#envs-table-body'),
          "collection": view.collection,
          "project": view.project
        });
        view.subviews.push(view.tableList);
        view.tableList.render();
      });
    }
  });

  var EnvironmentsTableView = Backbone.View.extend({
    template: "app/templates/environments_tablelist.html",
    statusTemplate: "app/templates/status.html",

    initialize: function (options) {
      this.project = options.project;

      this.collection.bind('reset', this.render, this);
      this.collection.bind('local-filter-changed', this.render, this);

      this.poll = new Environments.Collection({}, {project: this.project});
      this.poll.bind('reset', this.checkForUpdates, this);

      poller.PollingManager.start(this.poll);
    },

    onClose: function () {
      poller.PollingManager.stop(this.poll);
    },

    checkForUpdates: function(poll) {
      var self = this;
      if(poll.length != this.collection.length) {
        this.collection.reset(poll.toJSON());
        return;
      }
      poll.all(function(item){
        var originalItem = self.collection.get(item.id);
        if(!originalItem) {
          self.render();
          return false;
        }
        if(!_.isEqual(item.get('status'), originalItem.get('status'))){
          originalItem.set(item.toJSON());
          self.renderStatus(originalItem);
        } else if(!_.isEqual(item.get('completed'), originalItem.get('completed'))) {
          originalItem.set(item.toJSON());
          self.$("#"+item.id+"-status > .genesis-progressbar > .progress").progressbar({value: item.get('completed') * 100});
        }

        return true;
      });
    },

    renderStatus: function(item) {
      var view = this;
      $.when(genesis.fetchTemplate(this.statusTemplate)).done(function(tmpl) {
        view.$('#'+item.id+"-status").html(tmpl({
          environment: item.toJSON(),
          statusImages: STATUS_IMAGES
        }));
        self.$("#"+item.id+"-status > .genesis-progressbar > .progress").progressbar({value: item.get('completed') * 100});
      });
    },

    render: function () {
      var view = this;
      var renderedCollectionJSON = this.collection.filterToJSON();
      $.when(genesis.fetchTemplate(this.template), genesis.fetchTemplate(this.statusTemplate)).done(function (tmpl) {
        view.$el.html(tmpl({
          environments: renderedCollectionJSON,
          project: view.project,
          statusImages: STATUS_IMAGES
        }));
        view.collection.forEach(function(item){
          view.renderStatus(item);
        })
      });
    }
  });

  var UNKNOWN = "unknown";
  Environments.Views.Details = Backbone.View.extend({
    template: "app/templates/env_details.html",
    statusTemplate: "app/templates/status.html",
    vmsTemplate: "app/templates/vm_table.html",
    staticServersTemplate: "app/templates/servers_table.html",
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
      "click a.show-sources" : "showSources"
    },

    initialize: function (options) {
      this.details = new Environments.Model({"id": options.envId, projectId: options.projectId});
      poller.PollingManager.start(this.details);
      this.details.bind("change:status", this.renderStatus, this);
      this.details.bind("change:workflowCompleted", this.renderProgress, this);
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

    onClose: function (options) {
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

    },

    showSources: function(event) {
      var $currentTarget = $(event.currentTarget);
      var templateName = $currentTarget.attr("data-template-name");
      var templateVersion = $currentTarget.attr("data-template-version");

      if(_.isUndefined(this.sourcesView)) {
        this.sourcesView = new gtemplates.SourcesView({
          el: $("<pre class='prettyprint linenums' id='template-source-view'></pre>"),
          projectId: this.details.get("projectId")
        });
      }
      this.sourcesView.showTemplate(templateName, templateVersion);
    },

    cancelWorkflow: function (e) {
      this.confirmationDialog.dialog('open');
    },

    resetEnvStatus: function (e) {
      this.resetEnvStatusDialog.dialog('open');
    },

    executeWorkflow: function (e) {
      var workflowName = e.currentTarget.rel;
      genesis.app.trigger("page-view-loading-started");
      var template = new TemplateDesc({name: this.details.get('templateName'), version: this.details.get('templateVersion')},
       {projectId: this.details.get("projectId")});
      var self = this;
      $.when(template.fetch())
        .done(function() {
          var workflow = _(template.get('workflows')).find(function (item) {
            return item.name === workflowName;
          });

          if(workflow) {
            self.executeWorkflowDialog.showFor(self.details.get("projectId"), workflow, self.details.get("id"), self.details.get('templateName'), self.details.get('templateVersion'));
          }
          genesis.app.trigger("page-view-loading-completed");
        })
        .fail(function(jqXHR) {
          genesis.app.trigger("page-view-loading-completed");
          status.StatusPanel.error(jqXHR);
        });
    },

    renderStatus: function () {
      var status = this.details.get('status');
      var activeExecution = (status === "Busy");

      this.$(".cancel-button")
        .toggleClass("disabled", !activeExecution)
        .toggle(status !== "Destroyed");

      this.$(".action-button")
        .toggleClass("disabled", activeExecution)
        .toggle(status !== "Destroyed");

      this.$("#resetBtn")
        .toggle(status === "Broken");

      var self = this;
      $.when(genesis.fetchTemplate(this.statusTemplate)).done(function(tmpl) {
        genesis.app.trigger("page-view-loading-completed");
        self.$('.env-status').html(tmpl({
          environment: self.details.toJSON(),
          statusImages: STATUS_IMAGES
        }));
        self.renderProgress();
      });
    },

    renderProgress: function() {
      var completed = this.details.attributes.workflowCompleted;
      if (typeof(completed) !== "undefined") {
        this.$(".env-status > .genesis-progressbar > .progress").progressbar({value: completed * 100});
      }
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
      var view = this;
      var servers = view.details.get("servers");
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

    renderWorkflowList: function (e) {
      if (this.workflowHistory == null) {
        this.workflowHistory = new WorkflowHistoryView({model: this.details, collection: this.historyCollection, el: "#panel-tab-3"});
        this.workflowHistory.render();
      }
    },

    render: function () {
      var view = this;
      $.when(
        genesis.fetchTemplate(this.template),
        genesis.fetchTemplate(this.staticServersTemplate),
        genesis.fetchTemplate(this.vmsTemplate),
        genesis.fetchTemplate(this.statusTemplate),
        this.details.fetch()
      ).done(function (tmpl) {
          var details = view.details.toJSON();
          view.$el.html(tmpl({
            environment: details,
            statusImages: STATUS_IMAGES,
            actions: _(details.workflows).reject(function (flow) {
              return flow.name === details.createWorkflowName
            })
          }));
          view.renderStatus();
          view.renderVirtualMachines();
          view.renderServers();
          view.checkServersAndVms();
          view.renderWorkflowList();
          view.renderAttributes();
          view.confirmationDialog = view.createConfirmationDialog(view.$("#dialog-confirm"));
          view.resetEnvStatusDialog = view.createResetEnvStatusDialog(view.$("#dialog-reset"));
        }
      ).fail(function(jqXHR) {
          genesis.app.trigger("page-view-loading-completed");
          genesis.app.trigger("server-communication-error",
            "Failed to get environment details<br/><br/> Please contact administrator.",
            "/project/" + view.details.get("projectId")
          );
      });
    },

    createConfirmationDialog: function (element) {
      var self = this;
      return element.dialog({
        resizable: true,
        modal: true,
        title: 'Confirmation',
        dialogClass: 'dialog-without-header',
        minHeight: 120,
        width: 420,
        autoOpen: false,
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
        resizable: true,
        modal: true,
        title: 'Confirmation',
        dialogClass: 'dialog-without-header',
        minHeight: 120,
        width: 420,
        autoOpen: false,
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

    initialize: function(options) {
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

        $('.workflow-variable').each(function (it) {
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
          modal: true,
          title: 'Execute ' + view.workflow.name,
          width: _.size(view.workflow.variables) > 0 ? 600 : 400,
          minHeight: 120,
          dialogClass: 'dialog-without-header',
          buttons: {
            "Run": function(e) {
              var runBtn = $(e.target);
              if (runBtn.hasClass("disabled")) return;
              runBtn.toggleClass("disabled");
              var handler = function() {
                if (runBtn.hasClass("disabled")) runBtn.toggleClass("disabled");
              };
              view.unbind("workflow-validation-errors");
              view.bind("workflow-validation-errors", handler);
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
    template: "app/templates/workflow_history.html",

    events: {
      "click .toggle": "displayDetails",
      "click .next-page": "nextPage",
      "click .previous-page": "previousPage",
      "click .show-details": "toggleStepDetails",
      "click .step-details-expander": "toggleStepActionsView",
      "click .back": "render"
    },

    initialize: function (options) {
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

    removeActionViews: function() {
      _.chain(this.actionVews).values().each(function(view) { view.close() });
      this.actionVews = {};
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
          "workflows": histories,
          "page": pageInfo.page,
          "pagesCount": pageInfo.pages,
          "expandedSections": view.expandedSections,
          "expandedStepsHtml": htmls,
          "utils": genesis.utils
        }));

        _.chain(view.actionVews).keys().each(function(stepId) {
          view.actionVews[stepId].setElement(view.$("#step-"+ stepId + "-actions .subtable"));
          view.actionVews[stepId].refresh();
        });
      });
    }

  });

  var StepLogView = Backbone.View.extend({
    template: "app/templates/steplog.html",

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
          "utils": genesis.utils
        }));
      });
    }
  });

  return Environments;
});
