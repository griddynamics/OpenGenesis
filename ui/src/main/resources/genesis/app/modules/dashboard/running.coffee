define [
  "genesis",
  "backbone",
  "modules/environments"
  "modules/validation",
  "services/backend",
  "utils/poller",
  "jquery",
  "momentjs",
  "jvalidate"
], (genesis, Backbone, Env, validation, backend, poller, $) ->
  'use strict'

  RunningJobs = genesis.module()

  class WorkflowsRecord extends Backbone.Model
    idAttribute: "projectId"

  class JobStats extends genesis.Backbone.Collection
    url: "/rest/workflow-stats"
    model: WorkflowsRecord

  class Workflow extends Backbone.Model

  class WorkflowCollection extends genesis.Backbone.Collection
    linkType: backend.LinkTypes.WorkflowDetails
    model: Workflow
    initialize: (options) ->
      @link = options.urlLink
    url: -> @link


  class EnvsCollection extends Env.Collection
    getFilter: ->
      {
      namePart: ""
      statuses:
        ready:
          visible: true
        busy:
          visible: true
        broken:
          visible: true
      }

  class WorkflowView extends Backbone.View
    events:
      "click .job-details": "toggleDetails"

    template: "app/templates/dashboard/project_running_jobs.html"
    initialize: (options) ->
      workflows = options.workflows
      @envs = options.envs
      @projectId = options.projectId
      @running = _.chain(workflows).sortBy('executionStartedTimestamp').groupBy("envId").value()

    toggleDetails: (e) ->
      id = $(e.currentTarget).attr("rel");
      $(e.currentTarget).toggleClass("expanded");
      this.$(id).slideToggle("fast");

    render: ->
      $.when(genesis.fetchTemplate(@template)).done (tmpl) =>
        @$el.html(tmpl(
          jobs: @running,
          envs: @envs,
          moment: moment,
          projectId: @projectId
        ))

  class RunningJobs.Views.Main extends Backbone.View
    template: "app/templates/dashboard/running_jobs.html"

    events:
      "click .project-stat": "showProjectDetails"

    initialize: (options) ->
      _.bind @render, this
      @projectsCollection = options.projects
      @projects = options.projects.reduce ((memo, proj) -> memo[proj.id] = proj.toJSON(); memo ), {}
      @stat = new JobStats()
      @stat.bind "reset", @render, @
      @refresh()

    refresh: () ->
      @stat.fetch().done =>
        @poll = @stat.clone()
        poller.PollingManager.start @poll, { delay: 5000 }
        @poll.bind "reset", @checkStatusUpdates, @

    checkStatusUpdates: () ->
      hasChanges = () =>
        @poll.find (p) =>
          @stat.get(p.id)?.get("runningWorkflows") != p.get("runningWorkflows")
      if @poll.size() != @stat.size() or hasChanges()
        @stat.reset @poll.models

    onClose: ->
      poller.PollingManager.stop @poll

    render: ->
      $.when(genesis.fetchTemplate(@template)).done (tmpl) =>
        numbers = @stat.reduce(((memo, j) => memo += j.get('runningWorkflows'); memo), 0)
        @$el.html ( tmpl
          stat: @stat.toJSON(),
          running: numbers,
          projects: @projects
        )

    showProjectDetails: (e) ->
      projectId = $(e.currentTarget).attr("data-project-id")
      project = @stat.get(projectId)

      $(".toggle", e.currentTarget).toggleClass("expanded")
      $projDetailsEl = @$(".history-details[ data-project-id=" +projectId + "]")
      $projDetailsEl.toggleClass("visible")
      link = _.find project.get("links"), (l) -> l.rel == "collection"
      workflows = new WorkflowCollection(urlLink: link.href)
      envs = new EnvsCollection([], project: @projectsCollection.get(projectId))
      $.when(workflows.fetch(), envs.fetch()).done =>
        view = new WorkflowView(
          workflows: workflows.toJSON(),
          envs: envs,
          el: $projDetailsEl,
          projectId: projectId
        )
        view.render()


  RunningJobs
