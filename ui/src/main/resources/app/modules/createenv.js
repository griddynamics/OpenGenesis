define([
  "genesis",
  "services/backend",
  "modules/status",
  "variables",
  "modules/common/templates",
  "modules/validation",
  "use!backbone",
  "jquery",
  "use!showLoading",
  "use!jvalidate"
],

function(genesis, backend,  status, variables, gtemplates, validation, Backbone, $) {
  var createenv = genesis.module();

  var EnvCreate = Backbone.Model.extend({
    url: function() {
      return "/rest/projects/" + this.get("projectId") + "/envs";
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
        view.wizard.bind('finished', function(){
          genesis.app.router.navigate("project/" + view.project.id, {trigger: true});
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
      "click .last-step-button" : "createEnvironment"
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
      this.$el.showLoading();
      $.when(templates.fetch({timeout: 10000})).always(function() { self.$el.hideLoading(); }).then(
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

    createEnvironment: function() {
      if(this.$('#workflow-parameters-form').valid()) {
        this.$el.showLoading();
        var model = this.mergeModelValues();
        var self = this;
        $.when(model.save()).always(function() { self.$el.hideLoading(); }).done(function (){
          self.trigger("finished");
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
      _(this.stepViews).forEach(function(view){ view.render() });
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

  var EnvironmentParametersStep = Step.extend({
    template: "app/templates/createenv/environment_settings.html",
    varTemplate: "app/templates/common/variables.html",
    errorTemplate: "app/templates/createenv/environment_settings_error.html",
    preconditionErrorTemplate: "app/templates/createenv/preconditions_error.html",

    events: {
      "click .group-radio": "groupVarSelected"
    },

    initialize: function(options) {
      this.variables = [];
      this.templateCollection = options.templates;
      this.project = options.project;
      this.model.bind("change:templateCombo", this.updateView, this);
    },

    updateView: function(model, templateCombo) {
      var nameAndVersion = templateCombo.split('/');
      var newTemplate = this.templateCollection.find(function(template) {
        return template.get('name') === nameAndVersion[0] && template.get('version') === nameAndVersion[1]
      });
      if(newTemplate) {
        var desc = new gtemplates.TemplateModel({name: newTemplate.get('name'), version:  newTemplate.get('version')}, {projectId: this.project.id});
        genesis.app.trigger("page-view-loading-started");
        var self = this;
        self.$el.html("");
        $.when(desc.fetch()).done(function() {
          var workflow = new gtemplates.WorkflowModel({name: newTemplate.get('name'), version:  newTemplate.get('version')},
            {projectId: self.project.id, workflow: desc.get('createWorkflow').name});
          $.when(workflow.fetch()).done(function() {
            self.variables = workflow.get('result').variables;
            variables.processVars({
              variables: self.variables,
              projectId: self.project.id,
              workflowName: workflow.workflow,
              templateName: newTemplate.get('name'),
              templateVersion: newTemplate.get('version')
            });
            self.render();
          }).fail(function(jqXHR){
              jqXHR.preconditionFailed = true;
              self.render(jqXHR);
          }).always(function(){
              genesis.app.trigger("page-view-loading-completed");
          });
        })
        .fail(function(jqXHR) {
            self.render(jqXHR);
            genesis.app.trigger("page-view-loading-completed");
        });
      }
    },

    modelValues: function() {
      var vals = {};
      this.$('.workflow-variable').each(function () {
        var value = $(this).is("input[type='checkbox']") ? $(this).is(':checked').toString() : $(this).val();
        if ($(this).val() && $(this).is(':enabled')) { vals[$(this).attr('name')] = value; }
      });
      return {
        envName: this.$("input[name='envName']").val(),
        variables: vals
      }
    },

    _settingsForm: function() {
      return this.$('#workflow-parameters-form');
    },

    render: function(error){
      var view = this;
      validation.unbindValidation(this.model, this._settingsForm());

      if (!error) {
        $("#ready").show();
        $.when(genesis.fetchTemplate(this.template), genesis.fetchTemplate(this.varTemplate)).done(function(tmpl, varTmpl){
          view.$el.html(tmpl({/*variables: view.variables*/}));
          view.$('#workflow_vars').html(varTmpl({variables: view.variables}));
          view.$('input:not(:hidden):first').focus();

          validation.bindValidation(view.model, view._settingsForm());
        });
      }  else {
        $("#ready").hide();
        var template = error.preconditionFailed ? this.preconditionErrorTemplate : this.errorTemplate;
        $.when(genesis.fetchTemplate(template)).done(function(tmpl){
          view.el.innerHTML = tmpl({error: JSON.parse(error.responseText)});
        });
      }
    },

    groupVarSelected: function(e) {
       variables.groupVarSelected(e, this, this.variables);
    }
  });

  return createenv;
});
