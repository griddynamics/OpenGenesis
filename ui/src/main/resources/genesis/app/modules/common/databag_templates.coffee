define ["genesis", "backbone", "services/backend"],  (genesis, Backbone, backend) ->
  DatabagTemplates = genesis.module()
  URL = "/rest/dbtemplates"

  class DatabagTemplates.Model extends genesis.Backbone.Model
    linkType: backend.LinkTypes.DataBagTemplate
    scope: "system"

    initialize: (options) ->
      @scope = options.scope if options.scope?

    urlRoot: ->
      URL + "/" + @scope

    allKeys: ->
      val = $.Deferred()
      id = @get('id')
      if id
        $.when(@fetch(cache: false, suppressErrors: true)).then(
         (tpl) ->
            results = tpl.get('properties')?.map (x) -> x.name
            val.resolve(results)
        ).fail( ->
          val.resolve([])
        )
      else
        val.resolve([])
      val.promise()

    required: ->
      console.log(@get('properties'))
      @get('properties').filter (p) -> p.required

  class DatabagTemplates.Collection extends genesis.Backbone.Collection
    linkType: backend.LinkTypes.DataBagTemplate
    model: DatabagTemplates.Model
    scope: "system"

    initialize: (options) ->
      @scope = options.scope if options.scope?

    url: ->
      URL + "/" + @scope

  class DatabagTemplates.SelectTemplateDialog extends Backbone.View
    template: "app/templates/common/databags_templates_list.html"

    initialize: (options) ->
      @$el.remove()

    cancel: ->
      @trigger "back"

    show: ->
      @render()

    select: ->
      selected = $('#template_id option:selected').val()
      if (selected)
        @trigger('databag-template-selected', selected)
        @close()

    onClose: ->
      @$el.dialog('destroy')

    render: ->
      $.when(@collection.fetch(), genesis.fetchTemplate(@template)).done (c,tmpl) =>
        buttons = {}

        buttons['Select'] = () =>
          @select()

        buttons["Cancel"] = () =>
          @$el.dialog("close")
          @close()

        @$el.html tmpl(
          items: @collection.toJSON()
        )
        @$el.dialog(
          title: "Select template"
          autoOpen: true
          buttons: buttons,
          close: () => @close()
        )

  DatabagTemplates