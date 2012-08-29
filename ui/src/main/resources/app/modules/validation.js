define([
  "genesis",
  "use!backbone",
  "modules/status",
  "use!jvalidate"

],

function(genesis, Backbone, status) {
  var ValidationHandler = genesis.module();

  function parse(jqXHR) {
    var errorMsg = {
      varErrors: {},
      serviceErrors: []
    };

    if (jqXHR.status == 400) {
      var parsedMsg = JSON.parse(jqXHR.responseText);

      errorMsg.varErrors = _.extend(parsedMsg.variablesErrors || {}, parsedMsg.serviceErrors || {});
      errorMsg.serviceErrors = _.union(parsedMsg.compoundServiceErrors || [], parsedMsg.compoundVariablesErrors || []);

      if(_.isEmpty(errorMsg.serviceErrors) && !_.isEmpty(errorMsg.varErrors)) {
        errorMsg.serviceErrors.push("Validation failure")
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

  ValidationHandler.unbindValidation = function (bbmodel, $form) {
    bbmodel.unbind("error", null, $form);
  };

  ValidationHandler.bindValidation = function(bbmodel, $form, statusPanel){

    bbmodel.bind("error", function(savedModel, jqXHR){
      var errors = parse(jqXHR);
      $form.validate().showErrors(errors.varErrors);

      if(statusPanel) {
        statusPanel.error(errors.serviceErrors)
      }

    }, $form);
  };

  return ValidationHandler;
});