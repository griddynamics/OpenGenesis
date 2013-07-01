define ["backbone", "genesis", "utils/poller", "jquery"], (Backbone, genesis, poller, $) ->
  InactivityTracker = genesis.module()

  class InactivityTracker.Tracker

    timeout = 15 * 60 * 1000
    queryInt = 5000

    constructor: (options) ->
      @inactive = 1000
      _.extend(@, Backbone.Events)
      if (options.timeout?)
        timeout = options.timeout


    start: () ->
      @reset()
      @initHandlers()
      @interval = setInterval(@increaseTimeout, queryInt)

    initHandlers: () ->
      $(document).on('mousedown', () => @reset())
      $(document).on('keydown', () => @reset())
      $(window).on('scroll', () => @reset())
      $(window).on('hashchange', () => @reset())

    increaseTimeout: () =>
      @inactive = @inactive + queryInt
      if (@inactive > timeout)
        genesis.app.trigger("session-expire", "Your session will be expired in 1 minute. You can continue it by clicking the button below.")

    shutdown: () ->
      if (@inactive > timeout)
        genesis.app.trigger("server-communication-error", "Your session has been expired. Refresh the page to continue.")
        poller.PollingManager.shutdown()
        @reset()
        if @interval?
          clearInterval(@interval)

    reset: () =>
      @inactive = 0


  InactivityTracker.Tracker