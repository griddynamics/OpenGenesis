({
    baseUrl: "src/main/resources/app",
    mainConfigFile: "src/main/resources/app/requirejs-config.js",
    out: "target/appdirectory-build/main-built.js",
    name: "app",
    resourcesRoot: "src/main/resources/", //note: non-standard config
    stubModules: ['cs']
})