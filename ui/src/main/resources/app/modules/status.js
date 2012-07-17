define([
  "genesis",
  "use!backbone"
],

function(genesis, Backbone) {
  var status = genesis.module();

  function parseJQXHR(jqXHR) {
    var errorMsg = [];

    if (jqXHR.status == 400) {
      var parsedMsg = JSON.parse(jqXHR.responseText),
        varErrors = parsedMsg.variablesErrors || {},
        compServiceErrors = parsedMsg.compoundServiceErrors || [];

      errorMsg = _.chain(varErrors)
        .keys().map(function(key) { return key + ": " + varErrors[key] })
        .union(compServiceErrors)
        .value();
    } else if (jqXHR.status == 500) {
      errorMsg.push("Internal server error. Please contact administrator.");
    } else if (jqXHR.status == 503) {
      errorMsg.push("Backend service communication error");
    }

    return errorMsg.length > 0 ? errorMsg : [ "Failed to process request" ];
  }

  function isJQXHR(obj) {
    return _.has(obj, "status") && _.has(obj, "responseText");
  }

  var GlobalGrowl = Backbone.View.extend ({
    events: {
      "click .ui-notify-close": "hide"
    },
    defaultTimeouts : {
      "success" : 4000,
      "information": 5000,
      "attention": 10000,
      "error": 20000
    },

    initialize: function() {
      this.tmpl = _.template($("#app-growl-message").html());
    },

    hide: function(e) {
      this._clearTimeHandler();
      var $el = this.$el;
      $el.fadeTo(1000, 0, function () {
        $el.hide();
      });
    },

    _clearTimeHandler: function() {
      if (this.timeHandler) {
        clearTimeout(this.timeHandler);
        this.timeHandler = null;
      }
    },

    show: function(message) {
      this._clearTimeHandler();

      if (isJQXHR(message.text)) {
        message.text = parseJQXHR(message.text);
      }

      this.$el
        .html(this.tmpl({message: message}))
        .css({opacity: 1})
        .show("drop", {"direction": "up"}, "slow");

      var self = this;
      var timeout = message.timeout || this.defaultTimeouts[message.type];

      this.timeHandler = setTimeout(function() {
        self.hide();
      }, timeout);
    },

    information: function(text, options) {
      var msg = { type: 'information', text: text };
      this.show(_.extend(msg, options));
    },

    success: function(text, options) {
      var msg = { type: 'success', text: text };
      this.show(_.extend(msg, options));
    },

    error: function(text, options) {
      var msg = { type: 'error', text: text };
      this.show(_.extend(msg, options));
    },

    attention: function(text, options) {
      var msg = { type: 'attention', text: text };
      this.show(_.extend(msg, options));
    }
  });

  var view = Backbone.View.extend({
    events: {
      "click .close-button" : "_hideMessage",
      "click .close" : "_hideMessage"
    },

    initialize: function(){
      if (!this.$("div.message").length) {
        this.$el.addClass("notification png_bg local-notification");
        this.$el.html(
          "<a href='#' data-bypass class='close'><img src='/assets/img/cross_grey_small.png' alt='close'></a>" +
          "<div class='message'></div>"
        );
      }
    },

    _hideMessage: function(e) {
      e && e.preventDefault();
      this.$el.clearQueue();
      var $el = this.$el;
      $el.fadeTo(400, 0, function () {
          $el.slideUp(600);
      });
    },


    _showMessage: function(cssClass, text, showPeriod) {
      this.$el.clearQueue();
      this.$el.addClass(cssClass);
      if(_.isArray(text)){
        text =
          "<ul>" +
            _(text).reduce(function(memo, item){ return memo + "<li>" + _.escape(item) + "</li>"; }, "") +
          "</ul>";
      }
      this.$('.message').html(text);
      this.$el
        .slideDown('fast')
        .animate(
          { opacity: 1 },
          { queue: false, duration: "slow" }
        ).delay(showPeriod).fadeOut();
    },

    information: function(text, showPeriod) {
      if(_.isUndefined(showPeriod) || !_.isNumber(showPeriod)) {
        showPeriod = 5000;
      }
      this._showMessage("information", text, showPeriod);
    },

    attention: function(text, showPeriod) {
      if(!_.isNumber(showPeriod)) {
        showPeriod = 10000 * 60;
      }
      this._showMessage("attention", text, showPeriod);
    },

    success: function(text, showPeriod) {
      if(!_.isNumber(showPeriod)) {
        showPeriod = 10000 * 60;
      }
      this._showMessage("success", text, showPeriod);
    },

    error: function(text, showPeriod) {
      if(!_.isNumber(showPeriod)) {
        showPeriod = 10000 * 60;
      }
      if (isJQXHR(text)) {
        text = parseJQXHR(text);
      }
      this._showMessage("error", text, showPeriod);
    },

    hide: function() {
      this.$el.clearQueue();
      this.$el.hide();
    }
  });

  status.StatusPanel = new GlobalGrowl({el: "#growl-container"});

  status.LocalStatus = view;

  return status;

});