define([
  "genesis",
  "services/backend",
  "utils/poller",
  "modules/status",
  "modules/common/env_status",
  "backbone",
  "jquery",
  "jqueryui",
  "multiselect",
  "jstorage"
],

function (genesis, backend, poller, status, EnvStatus, Backbone, $) {
  var Environments = genesis.module();

  /**
   * @const
   */
  var FILTER_DEFAULTS = {
    "namePart": "",

    "statuses": {
      "busy": { "visible": true, "name": "Busy" },
      "ready": { "visible": true, "name": "Ready" },
      "destroyed": { "visible": false, "name": "Destroyed" },
      "broken": { "visible": true, "name": "Broken" }
    }
  };

   Environments.fixFilter = function() {
    var initialFilter = $.jStorage.get(genesis.app.currentUser.user + "_envFilter", FILTER_DEFAULTS);
    if (!_.isEqual(_.keys(initialFilter.statuses), _.keys(FILTER_DEFAULTS.statuses)))
      $.jStorage.set(genesis.app.currentUser.user + "_envFilter", FILTER_DEFAULTS);
  };

  Environments.Collection = Backbone.Collection.extend({
    initialize: function (model, options) {
      if(!options.project) {
        throw new Error("Project parameter must be provided");
      }
      this.project = options.project;

      var filter = this.getFilter();
      filter.namePart = "";
      this._setFilter(filter);
    },

    url: function() {
      var filterParam = "",
          filter = this.getFilter();

      if (this._needBackendFiltering(filter)) {
        filterParam = "?filter=statuses[" +
          _(filter.statuses).reduce(function(memo, status) { return status.visible ? memo + status.name + "," : memo }, filterParam) +
          "]";
      }
      return "rest/projects/" + this.project.id + "/envs" + filterParam;
    },

    parse: function(json) {
      if (json.items)
        return json.items;
      else
        return json;
    },

    _needBackendFiltering: function(filter) {
      return _(filter.statuses).any(function(status) { return !status.visible });
    },

    changeFilter: function(filter) {
      var oldFilter = this.getFilter();
      this._setFilter(filter);

      if (!_.isEqual(filter.statuses, oldFilter.statuses)) {
        this.fetch();
      } else {
        this.trigger("local-filter-changed");
      }
    },

    _setFilter: function(filter) {
      $.jStorage.set(genesis.app.currentUser.user + "_envFilter", filter);
    },

    getFilter: function() {
      return $.jStorage.get(genesis.app.currentUser.user + "_envFilter", FILTER_DEFAULTS);
    },

    filterToJSON: function() {
      var filtered = this.models,
          filter = this.getFilter();
      if (filter.namePart.trim().length > 0) {
        var searchExpr = new RegExp(filter.namePart.trim(), 'i');
        filtered = _(filtered).filter( function(model) { return model.get("name").search(searchExpr) >= 0; });
      }
      return _(filtered).invoke("toJSON");
    }

  });

  Environments.Views.List = Backbone.View.extend({

    template: "app/templates/environments_overview.html",
    subviews: [],

    initialize: function () {
      this.project = this.collection.project;
    },

    onClose: function () {
      _(this.subviews).each(function (view) {
        view.close()
      });
    },

    events: {
      "keyup #filter-name": "nameFilterChanged",
      "multiselectclick #filter-statuses": "statusFilterChanged",
      "multiselectcheckall #filter-statuses": "statusFilterChanged",
      "multiselectuncheckall #filter-statuses": "statusFilterChanged"
    },

    nameFilterChanged: function() {
      this.collection.changeFilter(this.readFilterValues());
    },

    statusFilterChanged: function(e, ui) {
      var newFilter = $.extend(true, {}, this.collection.getFilter());
      if (e.type === "multiselectclick") {
        newFilter.statuses[ui.value.toLowerCase()].visible = ui.checked;
      } else {
        var checked = e.type === "multiselectcheckall";
        _(newFilter.statuses).each(function(status) {
          newFilter.statuses[status.name.toLowerCase()].visible = checked;
        });
      }
      this.collection.changeFilter(newFilter);
    },

    readFilterValues: function() {
      var currentFilter = {
        namePart: this.$("#filter-name").val(),
        statuses: {}
      };

      this.$("#filter-statuses option").each(function() {
        var name = $(this).attr("value");

        currentFilter.statuses[name.toLowerCase()] = {
          "visible" : $(this).is(":selected"),
          "name" : name
        };
      });
      return currentFilter;
    },

    render: function () {
      var view = this;
      $.when(
        genesis.fetchTemplate(this.template),
        backend.AuthorityManager.haveAdministratorRights(this.project.id),
        this.collection.fetch()
      ).done(function (tmpl, isAdmin) {
        view.$el.html(tmpl({ "project": view.project.toJSON(), "filter": view.collection.getFilter() }));
        if(isAdmin) {
          view.$("#project-settings").show();
        }

        view.$("#filter-statuses").multiselect({
          noneSelectedText: "no statuses selected",
          selectedText: "# statuses selected",
          height: "auto"
        });

        view.tableList = new EnvironmentsTableView({
          "el": view.$('#envs-table-body'),
          "collection": view.collection,
          "project": view.project
        });

        view.subviews.push(view.tableList);
        view.tableList.render();
      });
    }
  });

  var EnvironmentsTableView = Backbone.View.extend({
    template: "app/templates/environments_tablelist.html",

    initialize: function (options) {
      this.project = options.project;

      this.collection.bind('reset', this.render, this);
      this.collection.bind('local-filter-changed', this.render, this);

      this.poll = new Environments.Collection({}, {project: this.project});
      this.poll.bind('reset', this.checkForUpdates, this);

      poller.PollingManager.start(this.poll);
      this.expanded = [];
    },

    onClose: function () {
      poller.PollingManager.stop(this.poll);
    },

    events: {
      "click .toggle": "toggle"
    },

    toggle: function(event) {
      var view = this;
      var $element = $(event.currentTarget).parent();
      $(event.currentTarget).toggleClass('expanded');
      var config = $element.attr('data-config');
      var $rows = this.$el.children('tr[data-config="' + config + '"]');
      var pos = _.indexOf(this.expanded, config);
      if (pos > -1) {
        this.expanded.splice(pos, 1);
      } else {
        this.expanded.push(config);
      }
      if ($.browser.webkit) {
        $rows.toggle();
      } else {
        $rows.slideToggle("fast");
      }
    },

    checkForUpdates: function(poll) {
      var self = this;
      if(poll.length != this.collection.length) {
        this.collection.reset(poll.toJSON());
        return;
      }
      poll.all(function(item){
        var originalItem = self.collection.get(item.id);
        if (!originalItem) { // this should never happen
          self.collection.reset(poll.toJSON());
          return false; //stop iterating
        }
        originalItem.set(item.toJSON());
        return true;
      });
    },

    render: function () {
      var view = this;
      var elements = _(view.collection.filterToJSON()).groupBy("configuration");
      var configs = _.sortBy(_.keys(elements), function(s){ return s.toLowerCase(); });
      $.when(genesis.fetchTemplate(this.template)).done(function (tmpl) {
        view.$el.html(tmpl({
          environments: elements,
          configurations: configs,
          project: view.project,
          expanded: view.expanded,
          utils: genesis.utils
        }));

        view.statusViews = {};

        view.collection.forEach(function(item){
          view.statusViews[item.id] = new EnvStatus.View({el: view.$('#'+item.id+"-status"), model: item});
          view.statusViews[item.id].render();
        });
      });
    }
  });

  return Environments;
});
