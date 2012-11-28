define([
  "genesis",
  "backbone",
  "jquery",
  "jqueryui",
  "jvalidate"
],

function (genesis, Backbone, $) {
  var EnvironmentHistory = genesis.module();

  /**
   * @const
   */
  var PAGE_SIZE = 10;

  var WorkflowHistoryModel = Backbone.Model.extend({});

  EnvironmentHistory.Collection = Backbone.Collection.extend({

    model: WorkflowHistoryModel,

    initialize: function(models, options) {
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
      return "rest/projects/" + this.projectId + "/envs/" + this.envId + "/history?" + $.param({"page_offset": this.pageOffset, "page_length": this.pageLength});
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
      return "rest/projects/" + this.projectId + "/envs/" + this.envId + "/steps/" + this.stepId + "/actions";
    }
  });

  var StepLogView = Backbone.View.extend({
    template: "app/templates/env_details/steplog.html",

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

  var WorkflowHistoryView = Backbone.View.extend({
    template: "app/templates/env_details/workflow_history.html",

    events: {
      "click .toggle": "toggle",
      "click .show-details": "toggleStepDetails",
      "click .step-details-expander": "toggleStepActionsView"
    },

    initialize: function(options) {
      this.actionViews = {};
      this.expanded = (options || {}).expanded || false;
    },

    toggleStepActionsView: function(event) {
      var $currentTarget = $(event.currentTarget),
        $details = $($currentTarget.attr("data-actions-rel")),
        stepId = parseInt($currentTarget.attr("data-step-id")),
        stepStatus = $currentTarget.attr("data-step-status");

      if (!$details.is(":visible")) {
        var logsCollection = new StepLogCollection([], {
          "projectId": this.model.collection.projectId,
          "envId": this.model.collection.envId,
          "stepId": stepId
        });

        this.actionViews[stepId] = new StepLogView({
          el: $details.children(".subtable"),
          collection: logsCollection,
          isStepFinished: !(stepStatus === "Requested" || stepStatus === "Running")
        });
      } else {
        delete this.actionViews[stepId];
      }

      $details.toggle();
    },

    toggle: function(event) {
      var $target = $(event.currentTarget);
      var $element = $target.parent(),
        $details = $element.siblings("div.history-details");
      $target.toggleClass('expanded');
      if ($.browser.webkit) {
        $details.toggle();
      } else {
        $details.slideToggle("fast");
      }

      if($element.parent()[0] === this.$el[0]) {
        this.expanded = !this.expanded;
      }
    },

    toggleStepDetails: function (event) {
      var $link = $(event.currentTarget),
        $element = $($link.attr("rel"));

      $link.children("span").toggleClass("ui-icon-carat-1-n ui-icon-carat-1-s");
      $element.toggle();
    },

    render: function() {
      var self = this;
      $.when(genesis.fetchTemplate(this.template)).done(function (tmpl) {
        var htmls = _.chain(self.actionViews).keys().reduce(function(memo, item) {   //real hardcore!
          memo[item] = self.actionViews[item].html();
          return memo;
        }, {}).value();

        self.$el.html(tmpl({
          workflow: self.model.toJSON(),
          projectId: self.model.collection.projectId,
          envId: self.model.collection.envId,
          expand: self.expanded,
          expandedStepsHtml: htmls,
          utils: genesis.utils
        }));

        _.chain(self.actionViews).keys().each(function(stepId) {
          self.actionViews[stepId].setElement(self.$("#step-"+ stepId + "-actions .subtable"));
          self.actionViews[stepId].refresh();
        });
      });
    }

  });

  EnvironmentHistory.View = Backbone.View.extend({
    template: "app/templates/env_details/env_history.html",

    events: {
      "click .next-page": "nextPage",
      "click .previous-page": "previousPage"
    },

    initialize: function () {
      this.workflowViews = {};
      this.collection.fetch();
      this.collection.bind("reset", this.render, this);
      this.model.bind("change:historyCount change:status change:workflowCompleted", this.collection.refresh, this.collection);
      this.model.bind("change:currentWorkflowFinishedActionsCount", this.refreshCurrentWorkflow, this);
    },

    nextPage: function() {
      this.collection.nextPage();
      return false;
    },

    previousPage: function() {
      this.collection.previousPage();
      return false;
    },

    refreshCurrentWorkflow: function() {
      var latestWorkflowId = _.chain(this.workflowViews).keys().max().value();
      this.workflowViews[latestWorkflowId].render();
    },

    render: function () {
      var view = this;
      var histories = this.collection;

      $.when(
        genesis.fetchTemplate(this.template),
        genesis.fetchTemplate(new WorkflowHistoryView().template)
      ).done(function (tmpl) {
          var pageInfo = histories.pageInfo();

          view.$el.html(tmpl({
            workflows: histories.toJSON(),
            page: pageInfo.page,
            pagesCount: pageInfo.pages
          }));

          histories.each(function(workflow, index) {
            var timestamp = workflow.get('executionStartedTimestamp');

            var workflowView = view.workflowViews[timestamp] || new WorkflowHistoryView({
              expanded: index === 0 && pageInfo.page === 1
            });

            workflowView.model = workflow;
            workflowView.setElement(
              view.$('div.workflow-section[data-workflow-execution-started="' + timestamp + '"]')
            );

            view.workflowViews[timestamp] = workflowView;

            workflowView.render();
          });
        });
    }

  });

  return EnvironmentHistory;
});
