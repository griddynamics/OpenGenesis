// Set the require.js configuration for your application.
require.config({
  // Initialize the application with the main application file
  deps: ["app"],
  paths: {
    // JavaScript folders
    libs: "../assets/js/libs",
    plugins: "../assets/js/plugins",

    // Libraries
    jquery: "../assets/js/libs/jquery",
    jqueryui: "../assets/js/plugins/jquery/jquery-ui",
    jvalidate: "../assets/js/plugins/jquery/jquery.validate",
    showLoading: "../assets/js/plugins/jquery/showLoading",
    bootstrap: "../assets/js/plugins/jquery/bootstrap",
    dateformat: "../assets/js/plugins/jquery/dateformat",
    tabs: "../assets/js/plugins/jquery/tabs",
    jstorage: "../assets/js/plugins/jquery/jstorage",
    fcbcomplete: "../assets/js/plugins/jquery/jquery.fcbkcomplete",
    underscore: "../assets/js/libs/underscore",
    backbone: "../assets/js/libs/backbone",
    prettify: "../assets/js/libs/prettify",
    multiselect: "../assets/js/plugins/jquery/jquery.multiselect",
    datetimepicker: "../assets/js/plugins/jquery/date.time.picker",
    momentjs: "../assets/js/libs/moment",
    'coffee-script': "../assets/js/libs/coffee-script",

    tmplloader: "../assets/js/plugins/templateloader",
    cs: "../assets/js/plugins/cs"
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
    bootstrap: {
      deps: ["jqueryui"]
    },
    "dateformat" : {
      deps: ["jquery"]
    },
    "showLoading": {
      deps: ["jqueryui"]
    },
    "jvalidate": {
      deps: ["jquery"]
    },
    "tabs": {
      "deps": ["jquery"]
    },
    "fcbcomplete": {
      "deps" : ["jquery"]
    },
    "jstorage": {
      "deps" : ["jquery"]
    },
    "multiselect": {
      "deps": ["jqueryui"]
    },

    "datetimepicker": {
      "deps": ["jqueryui"]
    },
    underscore: {
      exports: "_"
    },
    tmplloader: {
      deps: ["jquery"]
    }
  }
});
