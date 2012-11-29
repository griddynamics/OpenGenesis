require.config({
  paths: {
    // Libraries
    jquery: "../assets/js/libs/jquery",
    jqueryui: "../assets/js/plugins/jquery/jquery-ui",
    showLoading: "../assets/js/plugins/jquery/showLoading",
    underscore: "../assets/js/libs/underscore",
    backbone: "../assets/js/libs/backbone",
    prettify: "../assets/js/libs/prettify",
    tmplloader: "../assets/js/plugins/templateloader",
    dateformat: "../assets/js/plugins/jquery/dateformat"
  },

  priority: ["jquery"],

  shim: {
    backbone: {
      deps: ["underscore", "jquery"],
      exports: "Backbone"
    },
    jqueryui: {
      deps: ["jquery"]
    },
    "showLoading": {
      deps: ["jqueryui"]
    },
    underscore: {
      exports: "_"
    },
    "dateformat": {
      deps: ["jquery"]
    }
  }
});

require([
  'jquery',
  'underscore',
  'modules/common/templates',
  'showLoading',
  'prettify'
],
  function ($, _, Templates) {
    $(function () {
      function getURLParameter(name) {
        return decodeURI((new RegExp(name + '=' + '(.+?)(&|$)').exec(document.location.search) || [, null])[1]);
      }

      var projectId = getURLParameter('projectId');
      var template = getURLParameter('templateName');
      var version = getURLParameter('templateVersion');

      var contentEl = $('#template-content');

      if (projectId === 'null' || template === 'null' || version === 'null') {
        contentEl.html("Not enough parameters.");
        return;
      }

      document.title = template + ' ' + version;

      var sources = new Templates.TemplateModel({name: template, version: version}, {projectId: projectId});

      contentEl.showLoading();

      $.when(sources.fetchSources())
        .always(function () {
          contentEl.hideLoading();
        })
        .done(function () {
          var content = _.escape(sources.get("content"));
          if ($.browser.msie) {
            content = content.replace(/\n/g, '<br/>').replace(/  /g, "&nbsp;");
          }
          contentEl.html(content);
          prettyPrint();
        })
        .fail(function (jqXHR) {
          ({
            401: function () {
              document.location = 'login.html?expire=true'
            },
            403: function () {
              contentEl.html('Access denied.')
            },
            404: function () {
              contentEl.html('Template is not found.')
            }
          }[jqXHR.status] || function () {
            contentEl.html("Error getting template content.")
          })();
        });
    });
  });
