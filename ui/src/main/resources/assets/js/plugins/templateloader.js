define(['module'], function (module) {

  var masterConfig = (module.config && module.config()) || {};

  var defaultModuleImpl = "define(['jquery'], function($) {" +
    "   return { get: function(url){ return $.get(url, 'text') } }" +
    "});\n";

  function isRhino() {
    return masterConfig.env === 'rhino' || (!masterConfig.env &&
      typeof Packages !== 'undefined' && typeof java !== 'undefined');
  }

  function objectToString(obj) {
    var result = "{";
    for(var property in obj) {
      if(obj.hasOwnProperty(property)){
        result += "'" + property + "': '" + obj[property].replace(/'/g, "\\\'").replace(/"/g, "\\\"") + "', \n";
      }
    }
    return result + " '': ''}"
  }

  var loader = {
    version: "0.0.1",

    load: function(name, req, load, config) {
      if (!config.isBuild) {
        load.fromText(defaultModuleImpl);
      } else {
        load()
      }
    },

    write: function(pluginName, moduleName, write) {
      if(!isRhino()) {
        if(console) console.warn("html template optimization is supposed to work only in rhino environment");
        write.asModule(pluginName + "!" + moduleName, defaultModuleImpl);
        return;
      }

      var globalConfig = require.s.contexts._.config;

      var templatesLocation = globalConfig.baseUrl + moduleName;

      var templates = discoverTemplates(templatesLocation, []);
      if(templates.length == 0) {
        throw new Error("No template files were found in:" + templatesLocation)
      }

      var cache = {};
      for(var i = 0; i < templates.length; i++) {
        var key = templates[i].replace(/\\/g, "/").replace(new RegExp(".*" + globalConfig.resourcesRoot.replace("/", "\\/"),  "g"), "");
        cache[key] = readFile(templates[i])
      }

      write.asModule(pluginName + "!" + moduleName,
        [
          "define(['jquery'], function($) {",
           "var cache = ", objectToString(cache), ";",
           "return { " +
             "get: function(url){ " +
             "  if (cache.hasOwnProperty(url)) {" +
             "    var def = new $.Deferred(); " +
             "    def.resolve(cache[url]); " +
             "    return def.promise(); " +
             "  } else {" +
             "    return $.get(url, 'text');" +
             "  }" +
            "}" +
           "}",
          "});\n"
        ].join("")
      );
    }
  };


  if (isRhino()) {
    var readFile = function (url) {
      var stringBuffer, line,
        encoding = "utf-8",
        file = new java.io.File(url),
        input = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(file), encoding)),
        content = '';
      try {
        stringBuffer = new java.lang.StringBuffer();
        while ((line = input.readLine()) !== null) {
          stringBuffer.append(line);
        }
        content = String(stringBuffer.toString());
      } finally {
        input.close();
      }
      return content;
    };

    var discoverTemplates = function(url, tmpls) {
      var files = new java.io.File(url).listFiles();
      for(var i = 0; i < files.length; i++) {
        var file = files[i];
        if(file.isDirectory()){
          discoverTemplates(file.getAbsolutePath(), tmpls)
        } else {
          tmpls.push(String(file.getAbsolutePath()))
        }
      }
      return tmpls;
    };
  }

  return loader;
});
