define [
  "genesis",
  "services/backend",
  "modules/status",
  "modules/validation",
  "backbone",
  "jquery",
  "jqueryui",
  "jvalidate"],
(genesis, backend, status, validation, Backbone, $) ->

  Settings = genesis.module()

  URL = "rest/settings"
  TIMEOUT_AJAX = 4000



  class Settings.Model extends Backbone.Model
    idAttribute: "name"
    url: URL

  class Settings.Collection extends genesis.Backbone.Collection
    model: Settings.Model
    url: URL + "?prefix=genesis.system"
    parseLinks: (links) ->
      @_editLink = _(links).find backend.LinkTypes.ConfigProperty.edit
    canEdit: ->
      not _.isUndefined(@_editLink)

  class Settings.Views.Main extends Backbone.View
    template: "app/templates/settings/system_configs.html"
    events:
      "click a.save": "saveChanges"
      "click a.reset": "reset"
      "click a.show-keys": "toggleShowKey"

    initialize: (options) ->
      @collection = new Settings.Collection
      @collection.bind "reset", @render, this
      @collection.fetch()
      @mainView = options.main

    render: ->
      $.when(genesis.fetchTemplate(@template)).done (tmpl) =>
        propsGrp = @collection.groupBy((p) ->
          p.get "readOnly"
        )
        toJson = (c) ->
          _.map c, (m) ->
            m.toJSON()

        @$el.html tmpl(
          properties: toJson(propsGrp[false])
          constants: toJson(propsGrp[true])
          canEdit: @collection.canEdit()
        )
        @mainView.toggleRestart()


    saveChanges: ->
      view = this
      $("input.property-value").each ->
        propertyName = $(this).attr("name")
        value = $(this).val()
        view.collection.get(propertyName).set "value", value,
          silent: true

      changedSettings = _(@collection.filter((item) ->
        item.hasChanged()
      ))
      map = new Settings.Model
      map.isNew = ->
        false

      changedSettings.each (item) ->
        map.set item.get("name"), item.get("value")

      unless changedSettings.isEmpty()
        validation.bindValidation map, @$("#edit-system-settings"), status.StatusPanel
        map.save().done =>
          status.StatusPanel.success "Settings changes saved."
          @mainView.toggleRestart()

    restoreDefaults: ->
      $.ajax #todo: WHAT IS THIS??
        url: @collection.url
        dataType: "json"
        type: "DELETE"
        timeout: TIMEOUT_AJAX
        processData: false


    reset: ->
      @showConfirmationDialog()

    toggleShowKey: (event) ->
      $link = $(event.currentTarget)
      $span = $link.children("span")
      $span.toggleClass "ui-icon-carat-1-n ui-icon-carat-1-s"
      $span.attr "title", (if $span.hasClass("ui-icon-carat-1-s") then "Show Key" else "Hide Key")
      $element = $($link.attr("rel"))
      $element.toggle()

    showConfirmationDialog: ->
      self = this
      unless @confirmationDialog
        @confirmationDialog = @$("#dialog-confirm-reset").dialog(
          title: "Confirmation"
          buttons:
            Yes: ->
              $.when(self.restoreDefaults()).done(->
                self.collection.fetch()
                status.StatusPanel.success "Default settings restored!"
              ).fail ->
                status.StatusPanel.error "Default settings restore failed!"

              $(this).dialog "close"

            No: ->
              $(this).dialog "close"
        )
      @confirmationDialog.dialog "open"

  Settings

