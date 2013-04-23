package com.griddynamics.genesis.validation

import com.sun.imageio.plugins.gif.GIFStreamMetadata
import com.griddynamics.genesis.model.ValueMetadata
import com.griddynamics.genesis.api.{Success, ExtendedResult}


trait SettingsValidation {
  def defaults: Map[String, ValueMetadata]
  def validators: Map[String, ConfigValueValidator]
  val defaultValidator: ConfigValueValidator
  def validate(propName: String, value: String): ExtendedResult[Any] =
    (for {prop <- defaults.get(propName).toSeq
          (msg, validatorName) <- prop.getValidation
          validator = validators.getOrElse(validatorName, defaultValidator)} yield
      validator.validate(propName, value, msg, Map("name" -> validatorName))
      ).reduceOption(_ ++ _).getOrElse(Success(value))

}
