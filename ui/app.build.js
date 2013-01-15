({
    baseUrl: "src/main/resources/genesis/app",
    mainConfigFile: "src/main/resources/genesis/app/requirejs-config.js",
    out: "target/appdirectory-build/genesis/main-built.js",
    name: "app",
    resourcesRoot: "src/main/resources/genesis/", //note: non-standard config
    stubModules: ['cs']
})