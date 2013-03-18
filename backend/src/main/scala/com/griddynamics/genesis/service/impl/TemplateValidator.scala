package com.griddynamics.genesis.service.impl

import com.griddynamics.genesis.model.{DatabagTemplate, TemplateProperty}
import com.griddynamics.genesis.api.{Failure, TemplateBased, Success, ExtendedResult}
import com.griddynamics.genesis.validation.ConfigValueValidator
import com.griddynamics.genesis.repository.DatabagTemplateRepository

trait TemplateValidator {
  def validations: Map[String, ConfigValueValidator]
  def templates: DatabagTemplateRepository

  private def validateValue(propName: String, value: String, defaults: Map[String, TemplateProperty]): ExtendedResult[Any] = {
    val results: Seq[ExtendedResult[Any]] = for {prop <- defaults.get(propName).toSeq
                                                 (msg, validatorName) <- prop.getValidation
                                                 validator = validations.get(validatorName)} yield
      validator.map(v => v.validate(propName, value, msg, Map("name" -> validatorName))).getOrElse(Success(propName))
    if (results.isEmpty)
      Success(propName)
    else
      results.reduce(_ ++ _)
  }

  def validAccordingToTemplate(bag: TemplateBased): ExtendedResult[TemplateBased] = {
    bag.templateId.flatMap(templateId => {
      templates.get(templateId).map(template => {
        val requiredValidation = validateRequiredKeys(template, bag)
        val rulesValidation =  bag.itemsMap.foldLeft(Success(bag).asInstanceOf[ExtendedResult[TemplateBased]])((acc, item) => acc ++
          validateValue(item._1, item._2, template.values).flatMap(v => Success(bag)))
        requiredValidation ++ rulesValidation
      })
    }).getOrElse(Success(bag))
  }

  def validateRequiredKeys(template: DatabagTemplate, bag: TemplateBased): ExtendedResult[TemplateBased] = {
    val requiredKeys: Iterable[String] = template.values.filter({
      case (_, value) => value.required
    }).keys.filterNot(name => bag.itemsMap.find({case (k,v) => k == name}).isDefined)
    val left = requiredKeys.foldLeft(Success(bag).asInstanceOf[ExtendedResult[TemplateBased]])((acc, item) => acc ++
      Failure(serviceErrors = Map(item -> s"Required key '$item' not found in databag")))
    left
  }
}
