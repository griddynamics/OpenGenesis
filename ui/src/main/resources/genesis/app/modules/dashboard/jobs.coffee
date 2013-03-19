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

  Jobs = genesis.module()

  class JobRecord extends Backbone.Model
    idAttribute: "projectId"

  class ProjectStats extends genesis.Backbone.Collection
    url: "rest/jobs-stat"
    model: JobRecord

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

  class Job extends genesis.Backbone.Model
    linkType: backend.LinkTypes.EnvScheduledJob
    initialize: (options) ->
      @url = options.urlLink

  class JobView extends Backbone.View
    template: "app/templates/dashboard/project_jobs.html"
    initialize: (options) ->
      jobs = options.jobs
      @envs = options.envs
      @projectId = options.projectId
      @failedPerEnv = _.chain(jobs.get("failed")).map((i) -> body = i.item; body.links = i.links; body.status = "Failed"; body ).sortBy('date').groupBy("envId").value()
      @requestedPerEnv = _.chain(jobs.get("requested")).map((i) -> body = i.item; body.links = i.links; body.status = "Requested"; body ).sortBy('date').groupBy("envId").value()
      @jobs = _.chain().
        union(_.keys(@failedPerEnv), _.keys(@requestedPerEnv)).
        reduce(((memo, k) =>
          memo[k] = _.union(@failedPerEnv[k] or [], @requestedPerEnv[k] or []) unless _.isUndefined(k)
          memo), {}).
        value()

    render: ->
      $.when(genesis.fetchTemplate(@template)).done (tmpl) =>
        @$el.html(tmpl(
          jobs: @jobs,
          envs: @envs,
          moment: moment,
          projectId: @projectId
        ))


  class Jobs.Views.Main extends Backbone.View
    template: "app/templates/dashboard/jobs.html"

    events:
      "click .project-stat": "showProjectDetails"

    initialize: (options) ->
      _.bind @render, this
      @projectsCollection = options.projects
      @projects = options.projects.reduce ((memo, proj) -> memo[proj.id] = proj.toJSON(); memo ), {}
      @stat = new ProjectStats

    showProjectDetails: (e) ->
      projectId = $(e.currentTarget).attr("data-project-id")
      project = @stat.get(projectId)

      $(".toggle", e.currentTarget).toggleClass("expanded")
      $projDetailsEl = @$(".history-details[ data-project-id=" +projectId + "]")
      $projDetailsEl.toggleClass("visible")

      link = _.find project.get("links"), (l) -> l.rel == "self"
      jobs = new Job(urlLink: link.href)
      envs = new EnvsCollection([], project: @projectsCollection.get(projectId))
      $.when(jobs.fetch(), envs.fetch()).done =>
        view = new JobView(
          jobs: jobs,
          envs: envs,
          el: $projDetailsEl,
          projectId: projectId
        )
        view.render()

    onClose: ->

    render: ->
      $.when(genesis.fetchTemplate(@template), @stat.fetch()).done (tmpl) =>
        numbers = @stat.reduce(((memo, j) => memo.failed += j.get('failedJobs'); memo.requested += j.get('scheduledJobs'); memo), {failed: 0, requested: 0})
        @$el.html ( tmpl
          stat: @stat.toJSON(),
          scheduledJobsCount: numbers.requested,
          failedJobsCount: numbers.failed,
          projects: @projects
        )

  Jobs

