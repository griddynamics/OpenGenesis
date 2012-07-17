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

    // Shim Plugin
    use: "../assets/js/plugins/use",
    order: "../assets/js/plugins/order"
  },
  priority: ["jquery"],

  use: {
    backbone: {
      deps: ["use!underscore", "jquery"],
      attach: "Backbone"
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
    underscore: {
      attach: "_"
    }
  }
});
