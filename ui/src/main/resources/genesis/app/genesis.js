define([
  // Libs
  "jquery",
  "underscore",
  "backbone",
  "utils/formats",
  "tmplloader!./templates",
  "jqueryui",
  "dateformat"
],

function($, _, Backbone, formats, tmpls) {

  var _template = _.template;
  _.template = function (str, data) {
    return _template (
      str.replace(/<!--%[\s\S]*%-->/g, ""),
      data
    );
  };

  Backbone.View.prototype.close = function() {
    this.unbind();
    this.remove();
    if(this.onClose){
      this.onClose();
    }
  };

  // TODO: remove monkey patching
  Backbone.Model.prototype.canEdit = function() {
    return !_.isUndefined(this._editLink)
  };

  Backbone.Model.prototype.canDelete = function() {
    return !_.isUndefined(this._deleteLink)
  };

  var modelSuperClone = Backbone.Model.prototype.clone;

  Backbone.Model.prototype.clone = function() {
    var cloned = modelSuperClone.call(this);
    cloned._editLink = this._editLink;
    cloned._deleteLink = this._deleteLink;
    return cloned;
  }

  var collectionSuperClone = Backbone.Collection.prototype.clone;

  Backbone.Collection.prototype.canCreate = function(){
    return !_.isUndefined(this._createLink)
  };

  Backbone.Collection.prototype.clone = function() {
    var cloned = collectionSuperClone.call(this);
    cloned._createLink = this._createLink;
    return cloned;
  };

  Backbone.Collection.prototype.itemAccessRights = function(){
    return this.chain()
      .map(function (item) {
        return {
          id: item.id,
          canEdit: item.canEdit(),
          canDelete: item.canDelete()
        }})
      .groupBy("id")
      .value()
  };

  if (window && _.isUndefined(window.console)) {
    window.console = { log : function(){}, warn : function(){} };
  }

  if(typeof String.prototype.trim !== 'function') {
    String.prototype.trim = function() {
      return this.replace(/^\s+|\s+$/g, '');
    }
  }

  //jquery dialog default settings
  $.extend($.ui.dialog.prototype.options, {
    modal: true,
    dialogClass: 'dialog-without-header',
    minHeight: 120,
    width: 420,
    autoOpen: false,
    resizable: false,
    open: function() { // confirmation dialogs should have no/cancel focused by default
      $(this).parent().find(".ui-dialog-buttonpane button:eq(1)").focus();
    }
  });

  $.ajaxSetup({
    cache: false,
    timeout: 30000
  });

  var genesis = {

    fetchTemplate: function(path, done) {
      done = done || function(){};

      var JST = window.JST = window.JST || {};
      var def = new $.Deferred();

      if (JST[path]) {
        done(JST[path]);

        return def.resolve(JST[path]);
      }

      $.when(tmpls.get(path)).done(function(contents) {
        var tmpl = _.template(contents);
        done(JST[path] = tmpl);
        def.resolve(JST[path]);
      });

      return def.promise();
    },

    module: function(additionalProps) {
      return _.extend({ Views: {} }, additionalProps);
    },

    app: _.extend({
      currentConfiguration: {},
      currentUser: null
    }, Backbone.Events),

    utils: {
      nullSafeClose: function (view) {
        if (view != null) {
          view.close();
        }
      },

      isSameDay: function(date1, date2) {
        if (_.isUndefined(date1) || _.isUndefined(date2)) {
          return false;
        }
        if (_.isNumber(date1)) {
          date1 = new Date(date1);
        }
        if (_.isNumber(date2)) {
          date2 = new Date(date2);
        }

        return date1.getYear() == date2.getYear() &&
          date1.getMonth() == date2.getMonth() &&
          date1.getDate() == date2.getDate();
      },

      toBoolean: function(collection) {
        if (_.isArray(collection)) {
          collection = _(collection);
        }
        return collection.reduce(function (memo, item) {
          memo[item] = true;
          return memo;
        }, {});
      },

      formatDate: function(date) {
        date = _.isNumber(date) ? new Date(date) : date;

        var locale = genesis.app.currentConfiguration['locale'] || "en-US", //todo: get rid of global object dependency
            currentFormat = formats.ShortDate[locale] || formats.ShortDate["en-US"];

        return $.formatDate.date(date, currentFormat);
      },

      formatTime: function(date) {
        date = _.isNumber(date) ? new Date(date) : date;
        return date.toLocaleTimeString();
      },

      formatDateTime: function(date) {
        date = _.isNumber(date) ? new Date(date) : date;

        var result = this.formatTime(date);

        if (!this.isSameDay(new Date(), date)) {
          result += " " + this.formatDate(date);
        }

        return result;
      },

      timeDuration: function(start, end) {
        if (start === null || _.isUndefined(start)) return "0";

        if (end === null || _.isUndefined(end)) return "--";

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
      },

      timezoneOffset: function() {
          return new Date().getTimezoneOffset();
      },

      formatUserLabel: function(user) {
        if (!user.firstName && !user.lastName)
          return user.username;

        return (user.firstName + " " + user.lastName).trim();
      }
    }
  };

  return genesis;
});
