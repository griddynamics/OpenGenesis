define(["backbone", "underscore"],

function (Backbone, _) {

  "use strict";

  var poller = {};

  var defaults = {
    delay: 3000,
    condition: function () {
      return true;
    }
  };
  var eventTypes = ['start', 'stop', 'success', 'error', 'complete'];

  /**
   * Poller
   */
  var Poller = function Poller(model, options) {
    this.set(model, options);
  };
  _.extend(Poller.prototype, Backbone.Events, {
    set: function (model, options) {
      this.model = model;
      this.options = _.extend(_.clone(defaults), options || {});

      _.each(eventTypes, function (eventName) {
        var handler = this.options[eventName];
        if (typeof handler === 'function') {
          this.on(eventName, handler, this);
        }
      }, this);

      if (this.model instanceof Backbone.Model) {
        this.model.on('destroy', this.stop, this);
      }

      return this.stop({silent: true});
    },
    start: function (options) {
      if (this.active() === true) {
        return this;
      }
      options = options || {};
      if (!options.silent) {
        this.trigger('start');
      }
      this.options.active = true;
      run(this);
      return this;
    },
    stop: function (options) {
      options = options || {};
      if (!options.silent) {
        this.trigger('stop');
      }
      this.options.active = false;
      if (this.xhr && typeof this.xhr.abort === 'function') {
        this.xhr.abort();
      }
      this.xhr = null;
      return this;
    },
    active: function () {
      return this.options.active === true;
    }
  });

  // private methods
  function run(poller) {
    if (poller.active() !== true) {
      window.clearTimeout(poller.timeoutId);
      return;
    }
    var options = _.extend({}, poller.options, {
      success: function () {
        poller.trigger('success');
        if (poller.options.condition(poller.model) !== true) {
          poller.trigger('complete');
          poller.stop({silent: true});
        }
        else {
          reschedule(poller);
        }
      },
      error: function () {
        poller.trigger('error');
        if (poller.options.noninterruptible === true) {
          reschedule(poller);
        } else {
          poller.stop({silent: true});
        }
      }
    });
    poller.xhr = poller.model.fetch(options);
  }

  function reschedule(poller) {
    poller.timeoutId = window.setTimeout(function () {
      run(poller);
    }, poller.options.delay);
  }

  /**
   * Polling Manager
   */
  var pollers = [];

  var PollingManager = {
    get: function (model) {
      return _.find(pollers, function (poller) {
        return poller.model === model;
      });
    },
    start: function (model, options) {
      var poller = this.get(model);
      if (poller) {
        poller.set(model, options);
      }
      else {
        poller = new Poller(model, options);
        pollers.push(poller);
      }
      return poller.start({silent: true});
    },
    stop: function (model) {
      var poller = this.get(model);
      if (poller) {
        poller.stop();
        return true;
      }
      return false;
    },
    size: function () {
      return pollers.length;
    }
  };

  poller.PollingManager = PollingManager;

  return poller;
});