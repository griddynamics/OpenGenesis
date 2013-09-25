define([
  "genesis",
  "services/backend",
  "modules/status",
  "modules/common/variables",
  "modules/common/templates",
  "modules/validation",
  "backbone",
  "jquery",
  "jvalidate"
],

function(genesis, backend,  status, variablesmodule, gtemplates, validation, Backbone, $) {
  var createenv = genesis.module();

  var EnvCreate = Backbone.Model.extend({
    url: function() {
      return "rest/projects/" + this.get("projectId") + "/envs";
    }
  });

  var Configurations = genesis.Backbone.Collection.extend({
    initialize: function(items, options) {
      this.projectId = options.projectId;
    },

    url: function() {
      return "rest/projects/" + this.projectId + "/configs";
    }
  });

  createenv.Views.Main = Backbone.View.extend({
    template: "app/templates/create_environments.html",

    initialize: function(options) {
      this.project = options.project;
    },

    onClose: function() {
      genesis.utils.nullSafeClose(this.wizard);
    },

    render: function() {
      var view = this;
      $.when(genesis.fetchTemplate(this.template)).done(function(tmpl){
        view.$el.html(tmpl({project: view.project.toJSON()}));

        view.wizard = new CreateWizard({
          project: view.project,
          model: new EnvCreate({"projectId": view.project.id}),
          el: view.$('#tab-panel')
        });
        view.wizard.bind('finished', function(env){
          genesis.app.router.navigate("project/" + view.project.id + "/inst/" + env.envId, {trigger: true});
        });
        view.wizard.render();
      });
    }
  });

  var CreateWizard = Backbone.View.extend({
    events: {
      "click .next-button": "nextStep",
      "click .back-button": "prevStep",
      "click .tab": "stepChanged",
      "click .last-step-button:not(.disabled)" : "createEnvironment"
    },

    onClose: function() {
      this.$el.hideLoading();
      _(this.stepViews).each(function(view) { view.close(); } )
    },

    initialize: function(options) {
      this.project = options.project;
      this.stepViews = [];
      this.currentStep = 0;

      var templates = new gtemplates.TemplatesCollection([], {projectId: this.project.id});

      var selectTemplateStep = new SelectTemplateStep({
        model: this.model,
        collection: templates,
        el: this.$('#panel-tab-1 > .step-content')
      });

      selectTemplateStep.bind("selected", this.nextStep, this);

      this.stepViews.push(selectTemplateStep);

      var envParams = new EnvironmentParametersStep({
        project: this.project,
        model: this.model,
        templates: templates,
        el: this.$('#panel-tab-2 > .step-content')
      });

      this.stepViews.push(envParams);

      var self = this;
      genesis.app.trigger("page-view-loading-started");
      $.when(templates.fetch({timeout: 10000})).always(function() { genesis.app.trigger("page-view-loading-completed"); }).then(
        function success() {
          selectTemplateStep.bind("no-templates", self.handleNoTemplates, self);
          selectTemplateStep.bind("only-template", self.nextStep, self);
          selectTemplateStep.render();
        },
        function fail() {
          status.StatusPanel.error("Failed to load templates");
          self.handleNoTemplates()
        }
      );
    },

    handleNoTemplates: function() {
      this.$('#status-message').html("<div class='information-panel'><p>No templates available</p></div>");
      this.$('.button').hide();
      this.$('#tab2').hide();
      this.$('#back-button').show();
    },

    poll: function(url, finalCallback, failCallback) {
      var self = this;
      $.ajax({
        url: url,
        dataType: 'json',
        type: 'GET'
      }).done(function(response, status, xhr) {
          var data = {};
          console.log(xhr);
          try {
            data = JSON.parse(xhr.responseText);
          } catch (e) {
            console.log(e);
          }
          if (xhr.status == 202) {
              if (data.location) {
                setTimeout(self.poll(data.location, finalCallback, failCallback), 3000);
              } else {
                setTimeout(self.poll(url, finalCallback, failCallback), 3000)
              }
          } else if (xhr.status >= 400) {
            failCallback(xhr);
          } else {
            finalCallback(data);
          }
        }).fail(function(e) {
          failCallback(e);
        });
    },

    createEnvironment: function() {
      if(this.$('#workflow-parameters-form').valid()) {
        genesis.app.trigger("page-view-loading-started");
        var model = this.mergeModelValues();
        var self = this;
        $.when(model.save()).always(function() { genesis.app.trigger("page-view-loading-completed"); }).done(function (resp){
          if (resp.location) {
              self.poll(resp.location, function(response) {
              self.trigger("finished", {envId: response.result});
            }, function(e) {
              self.model.trigger('error', model, e);
              new status.LocalStatus({el: self.$(".notification")}).error(e);
            });

          } else {
            self.trigger("finished", {envId: resp.result});
          }
        }).fail(function(e){
          console.log(e);
          new status.LocalStatus({el: self.$(".notification")}).error(e);
        });
      }
    },

    mergeModelValues: function() {
      this.model.set(this.currentView().modelValues());
      return this.model;
    },

    nextStep: function(){
      this.mergeModelValues();
      this.currentStep++;
      $(".tab[data-step=" + (this.currentStep + 1) + "]").click();
    },

    prevStep: function(){
      this.currentStep--;
      $(".tab[data-step=" + (this.currentStep + 1) + "]").click();
    },

    stepChanged: function(e) {
      this.mergeModelValues();
      this.currentStep = $(e.currentTarget).attr("data-step") - 1;
    },

    currentView: function() {
      return this.stepViews[this.currentStep];
    },

    render: function() {
      this.currentView().render();
    }

  });

  var Step = Backbone.View.extend({
    //to be overriden
    modelValues: function() {
      return {};
    }
  });

  var SelectTemplateStep = Step.extend({
    template: "app/templates/createenv/select_template.html",
    sourceCodeDialog: null,

    events: {
      "click a.show-sources": "showSources"
    },

    onClose: function() {
      if(this.sourcesView) {
        this.sourcesView.close();
      }
    },

    showSources: function(event) {
      var $currentTarget = $(event.currentTarget);
      var templateName = $currentTarget.attr("data-template-name");
      var templateVersion = $currentTarget.attr("data-template-version");

      if(_.isUndefined(this.sourcesView)) {
        this.sourcesView = new gtemplates.SourcesView({
          el: this.$("#template-source-view"),
          projectId: this.collection.projectId
        });
      }
      this.sourcesView.showTemplate(templateName, templateVersion);
    },

    modelValues: function() {
      var input = this.$("input[name='select_template']:checked");
      return {
        templateName: input.val(),
        templateVersion: input.attr('data-template-version'),
        templateCombo: input.val() + "/" + input.attr('data-template-version')
      };
    },

    render: function(){
      var view = this;
      $.when(genesis.fetchTemplate(this.template)).done(function(tmpl){
        view.el.innerHTML = tmpl({templates: view.collection.toJSON()});
        view.$("input[name='select_template']:first").click();
        if(view.collection.length == 0) {
          view.trigger("no-templates");
        } else if (view.collection.length == 1) {
            view.trigger("only-template");
        }
      });
    }
  });

  var EnvironmentParametersStep = variablesmodule.Views.AbstractWorkflowParamsView.extend({
    template: "app/templates/createenv/environment_settings.html",
    errorTemplate: "app/templates/createenv/environment_settings_error.html",
    preconditionErrorTemplate: "app/templates/createenv/preconditions_error.html",
    envConfigErrorTemplate: "app/templates/createenv/envconfig_validation_error.html",

    events: {
      "change #configuration": "configUpdated",
      "click .group-radio": "groupVarSelected"
    },

    initialize: function(options) {
      this.variables = [];
      this.templateCollection = options.templates;
      this.project = options.project;
      this.model.bind("change:templateCombo", this.render, this);
    },

    configUpdated: function(event) {
      var value = $(event.currentTarget).val();
      if(value) {
        if (!this.inputsView) {
          this.updateView(this.model, this.model.get("templateCombo"), value)
        } else {
          this.inputsView.updateConfigurationId(value)
        }
      }
    },

    updateView: function(model, templateCombo, configurationId) {
      var nameAndVersion = templateCombo.split('/');
      var currentTemplate = this.templateCollection.find(function(template) {
        return template.get('name') === nameAndVersion[0] && template.get('version') === nameAndVersion[1]
      });

      if (!currentTemplate) return; //todo: how can this happen??

      genesis.app.trigger("page-view-loading-started");

      var workflow = new gtemplates.WorkflowModel(
        {name: currentTemplate.get('name'), version: currentTemplate.get('version')},
        {projectId: this.project.id, workflow: currentTemplate.get('createWorkflow')}
      );

      var self = this;
      $.when(workflow.fetch({ data: $.param({ configurationId: configurationId}) })).done(function() {
        self.variables = workflow.get('variables');
        self.renderVariablesForm(currentTemplate, workflow, configurationId);
      }).fail(function (jqXHR) {
        self.renderError(jqXHR, self.envConfigErrorTemplate);
      }).always(function () {
        genesis.app.trigger("page-view-loading-completed");
      });
    },

    modelValues: function() {
      var value = {
        envName: this.$("input[name='envName']").val(),
        variables: this.workflowParams(),
        configId: this.$("select[name='configId']").val()
      };

      var timeToLive = this.$("select[name='timeToLive']").val();
      if($.isNumeric(timeToLive)){
        value['timeToLive'] = timeToLive;
      }

      return value;
    },

    _settingsForm: function() {
      return this.$('#workflow-parameters-form');
    },

    renderError: function(error, htmltemplate) {
      var view = this;
      $("#ready").addClass('disabled');
      if(error.status === 400) {
        var errorMsg = _.has(error, "responseText") ? JSON.parse(error.responseText) : error;
        $.when(genesis.fetchTemplate(htmltemplate || this.errorTemplate)).done(function(tmpl){
          view.$('.create-parameters').hide();
          view.$('#validation-error').show();
          view.$('#validation-error').html(tmpl({error: errorMsg}));
        });
      }
    },


    render: function () {
      var view = this;
      validation.unbindValidation(this.model, this._settingsForm());
      $("#ready").show();
      var configs = new Configurations([], {projectId: this.project.id});
      this.$el.html("");

      $.when(genesis.fetchTemplate(this.template), configs.fetch()).done(function (tmpl) {
        if (configs.size() == 0) {
          view.renderError(
            { compoundServiceErrors: ["You don't have permissions to create instances in any of environments configurations"] },
            view.preconditionErrorTemplate
          );
          return;
        }

        var allowedConfigs = configs.filter(function(i) {
          var createLink = _(i.get('links')).find(backend.LinkTypes.Environment.create);
          return !genesis.app.currentConfiguration['environment_security_enabled'] || createLink;
        });
        view.$el.html(tmpl({
          configs: _(allowedConfigs).map(function(i) { return i.toJSON() })
        }));

      });
    },

    renderVariablesForm: function (template, workflow, configurationId) {
      this.inputsView = new variablesmodule.Views.InputControlsView({
        el: this.$('#workflow_vars'),
        variables: this.variables,
        varGroups: workflow.get('varGroups'),
        projectId: this.project.id,
        workflow: workflow,
        template: template,
        configurationId: configurationId
      });

      var self = this;

      this.inputsView.bind("configuration-error", function(e) {
        $(".ready").addClass("disabled");
        self.renderError(e, self.envConfigErrorTemplate)
      });

      this.inputsView.bind("configuration-success", function(e) {
        $("#ready").removeClass("disabled");
        self.$('.create-parameters').show();
        self.$('#validation-error').hide();
      });

      this.inputsView.render(function () {
        self.$('input:not(:hidden):first').focus();
        validation.bindValidation(self.model, self._settingsForm());
      });

    },


    /* override */ variablesModel: function() {
       return this.variables;
    }
  });

  return createenv;
});
