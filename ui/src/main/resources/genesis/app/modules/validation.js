define([
  "genesis",
  "backbone",
  "modules/status",
  "jvalidate",
  "jquery"

],

function(genesis, Backbone, status, jvalidate, $) {
  var ValidationHandler = genesis.module();

  function parse(jqXHR) {
    var errorMsg = {
      varErrors: {},
      serviceErrors: []
    };

    if (jqXHR.status < 500) {
      try {
        var parsedMsg = JSON.parse(jqXHR.responseText);

        errorMsg.varErrors = _.extend(parsedMsg.variablesErrors || {}, parsedMsg.serviceErrors || {});
        errorMsg.serviceErrors = _.union(parsedMsg.compoundServiceErrors || [], parsedMsg.compoundVariablesErrors || []);

        if(_.isEmpty(errorMsg.serviceErrors) && !_.isEmpty(errorMsg.varErrors)) {
          errorMsg.serviceErrors.push("Validation failure")
        }
      } catch(e) {
        // do nothing
      }
    } else if (jqXHR.status == 500) {
      errorMsg.serviceErrors.push("Internal server error. Please contact administrator.");
    } else if (jqXHR.status == 503) {
      errorMsg.serviceErrors.push("Backend service communication error");
    }

    if(errorMsg.serviceErrors.length === 0) {
      errorMsg.serviceErrors.push("Failed to process request")
    }

    return errorMsg;
  }

  function filter(errors) {
    var offendingKeys = _.filter(_.keys(errors.varErrors), function(key) {return $('input[name= ' + key + ']')});
    var newVarErrors = _.pick(errors.varErrors, function() {
      _.without(_.keys(errors.varErrors), offendingKeys);
    });
    var movedErrors = _.map(_.pairs(_.pick(errors.varErrors, offendingKeys)), function(pair) {
      return 'Key "' + pair[0] + '": ' + pair[1];
    });
    errors.varErrors  = newVarErrors;
    errors.serviceErrors = _.union(errors.serviceErrors, movedErrors);
    return errors;
  }

  ValidationHandler.unbindValidation = function (bbmodel, $form) {
    bbmodel.unbind("error", null, $form);
  };

  ValidationHandler.bindValidation = function(bbmodel, $form, statusPanel, doFilter){

    bbmodel.bind("error", function(savedModel, jqXHR){
      var errors = parse(jqXHR);
      if (doFilter) {
        errors = filter(errors);
      }
      $form.validate().showErrors(errors.varErrors);
      if(statusPanel) {
        statusPanel.error(errors.serviceErrors)
      }

    }, $form);
  };

  return ValidationHandler;
});