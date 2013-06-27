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
            visible: true, name: "Ready"
          busy:
            visible: true, name: "Busy"
          broken:
            visible: true, name: "Broken"
      }
    _setFilter: ->
      true

  class Job extends genesis.Backbone.Model
    linkType: backend.LinkTypes.EnvScheduledJob
    initialize: (options) ->
      @url = options.urlLink

  class JobView extends Backbone.View
    events:
      "click .job-details": "toggleDetails"

    template: "app/templates/dashboard/project_jobs.html"
    initialize: (options) ->
      @mdl = options.jobs
      @envs = options.envs
      @projectId = options.projectId
      @refresh()

    refresh: ->
      @mdl.fetch().done () =>
        @poll = @mdl.clone()
        poller.PollingManager.start @poll, {delay: 3000}
        @poll.bind "change", @changed, @
        @render()

    changed: () =>
      eq = (job, j) =>
          job.id == j.id and job.date == j.date and job.recurrence == j.recurrence
      jobExist = (job) =>
        found = (element for element in @mdl.get('requested') when eq(element.item, job.item))
        console.log(found.length)
        found.length > 0
      changed = () =>
        result = _.every(@poll.get('requested'), (job) => jobExist(job))
        !result
      if changed()
        @mdl = @poll.clone()
        @render()


    prepareData: (model) =>
      failedPerEnv = _.chain(model.get("failed")).map((i) -> body = i.item; body.links = i.links; body.status = "Failed"; body ).sortBy('date').groupBy("envId").value()
      requestedPerEnv = _.chain(model.get("requested")).map((i) -> body = i.item; body.links = i.links; body.status = "Requested"; body ).sortBy('date').groupBy("envId").value()
      jobs = _.chain().
      union(_.keys(failedPerEnv), _.keys(requestedPerEnv)).
      reduce(((memo, k) =>
        memo[k] = _.union(failedPerEnv[k] or [], requestedPerEnv[k] or []) unless _.isUndefined(k)
        memo), {}).
      value()
      {failed: failedPerEnv, requestedPerEnv: requestedPerEnv, jobs: jobs}

    toggleDetails: (e) ->
      id = $(e.currentTarget).attr("rel");
      $(e.currentTarget).toggleClass("expanded");
      this.$(id).slideToggle("fast");

    onClose: () ->
      poller.PollingManager.stop @poll
      @unbind()

    render: ->
      $.when(genesis.fetchTemplate(@template)).done (tmpl) =>
        data = @prepareData(@mdl)
        @$el.html(tmpl(
          jobs: data.jobs,
          envs: @envs,
          moment: moment,
          utils: genesis.utils,
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
      @stat.bind "reset", @render, @
      @subviews = {}
      @opened = []
      @refresh()

    refresh: () ->
      @stat.fetch().done () =>
        @poll = @stat.clone()
        poller.PollingManager.start @poll, {delay: 3000}
        @poll.bind "reset", @checkUpdates, @
        @render()

    checkUpdates: () ->
      hasChanges = () =>
        @poll.find (p) =>
          @stat.get(p.id)?.get("scheduledJobs") != p.get("scheduledJobs") or @stat.get(p.id)?.get("failedJobs") != p.get("failedJobs")
      if @poll.size() != @stat.size() or hasChanges()
        @opened = _.reject(@opened, (x) => !@poll.get(x))
        @stat.reset @poll.models


    showProjectDetails: (e, selectedId) ->
      projectId = if !selectedId
        $(e.currentTarget).attr("data-project-id")
      else
        selectedId

      project = @stat.get(projectId)
      $(".project-stat[data-project-id=" + projectId + "]").find("h3.toggle").toggleClass("expanded")
      $projDetailsEl = @$(".history-details[ data-project-id=" +projectId + "]")
      if project and $projDetailsEl
        $projDetailsEl.toggleClass("visible")

        link = _.find project.get("links"), (l) -> l.rel == "self"
        jobs = new Job(urlLink: link.href)
        envs = new EnvsCollection([], project: @projectsCollection.get(projectId))
        @opened.push projectId
        $.when(jobs.fetch(), envs.fetch()).done =>
          @subviews[projectId]?.close()
          $projDetailsEl.append('<div class=jobs-list></div>')
          view = new JobView(
            jobs: jobs,
            envs: envs,
            el: $projDetailsEl.children(".jobs-list"),
            projectId: projectId
          )
          @subviews[projectId] = view

    onClose: ->
      @selected = []
      console.log @subviews
      subview.close() for key,subview of @subviews
      poller.PollingManager.stop @poll

    render: ->
      $.when(genesis.fetchTemplate(@template)).done (tmpl) =>
        numbers = @stat.reduce(((memo, j) => memo.failed += j.get('failedJobs'); memo.requested += j.get('scheduledJobs'); memo), {failed: 0, requested: 0})
        @$el.html ( tmpl
          stat: @stat.toJSON(),
          scheduledJobsCount: numbers.requested,
          failedJobsCount: numbers.failed,
          projects: @projects
        )
        _.each(@opened, (x) => @showProjectDetails(null, x))

  Jobs

