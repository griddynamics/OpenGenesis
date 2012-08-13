define([
  // Libs
  "jquery",
  "use!underscore",
  "use!backbone"
],

function($, _, Backbone) {

  Backbone.View.prototype.close = function() {
    this.unbind();
    this.remove();
    if(this.onClose){
      this.onClose();
    }
  };

  if (_.isUndefined(window.console)) {
    window.console = { log : function(){}, warn : function(){} };
  }

  if(typeof String.prototype.trim !== 'function') {
    String.prototype.trim = function() {
      return this.replace(/^\s+|\s+$/g, '');
    }
  }

  return {
    fetchTemplate: function(path, done) {
      done = done || function(){};

      var JST = window.JST = window.JST || {};
      var def = new $.Deferred();

      if (JST[path]) {
        done(JST[path]);

        return def.resolve(JST[path]);
      }

      $.get(path, function(contents) {
        var tmpl = _.template(contents);
        done(JST[path] = tmpl);
        def.resolve(JST[path]);
      }, "text");

      return def.promise();
    },

    module: function(additionalProps) {
      return _.extend({ Views: {} }, additionalProps);
    },

    app: _.extend({}, Backbone.Events),

    utils: {
      nullSafeClose: function (view) {
        if (view != null) {
          view.close();
        }
      },

      isSameDay: function(date1, date2) {
        if(_.isUndefined(date1) || _.isUndefined(date2)) {
          return false;
        }
        if(_.isNumber(date1)) {
          date1 = new Date(date1);
        }
        if(_.isNumber(date2)) {
          date2 = new Date(date2);
        }

        return date1.getYear() == date2.getYear() &&
          date1.getMonth() == date2.getMonth() &&
          date1.getDate() == date2.getDate();
      },

      toBoolean: function(collection) {
        if(_.isArray(collection)) {
          collection = _(collection);
        }
        return collection.reduce(function(memo, item) {
          memo[item] = true;
          return memo;
        }, {});
      },

      timeDuration: function(start, end) {
        if (start === null || _.isUndefined(start)) return "0";

        if (end === null || _.isUndefined(end)) end = new Date();

        if (typeof start == Date) start = start.getTime();
        if (typeof end == Date) end = end.getTime();

        var duration = end - start;

        var MILLISECOND = 1,
            SECOND = 1000 * MILLISECOND,
            MINUTE = 60 * SECOND,
            HOUR = 60 * MINUTE;

        if (duration < SECOND) return duration + " ms";
        if (duration < MINUTE) return (duration / SECOND).toFixed(1) + " sec";
        if (duration < HOUR) return (duration / MINUTE).toFixed(1) + " min";

        return (duration / HOUR).toFixed(1) + " hour(s)";
      }
    }
  };
});
