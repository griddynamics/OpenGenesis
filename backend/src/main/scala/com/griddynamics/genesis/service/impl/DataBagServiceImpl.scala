package com.griddynamics.genesis.service.impl

import com.griddynamics.genesis.service.DataBagService
import org.springframework.transaction.annotation.Transactional
import com.griddynamics.genesis.repository.{DatabagTemplateRepository, DatabagRepository}
import com.griddynamics.genesis.validation.{ConfigValueValidator, Validation}
import com.griddynamics.genesis.validation.Validation._
import com.griddynamics.genesis.api._
import scala.Some
import com.griddynamics.genesis.model.{TemplateProperty, DatabagTemplate}
import com.griddynamics.genesis.api.DataBag
import com.griddynamics.genesis.api.Success
import com.griddynamics.genesis.api.Failure
import scala.Some

class DataBagServiceImpl(repository: DatabagRepository,
                         override val templates: DatabagTemplateRepository,
                         override val validations: Map[String, ConfigValueValidator]) extends DataBagService with
  Validation[DataBag] with TemplateValidator {

  @Transactional(readOnly = true)
  def list = repository.list

  @Transactional(readOnly = true)
  def get(id: Int) = repository.get(id)

  @Transactional
  override def create(a: DataBag) = validCreate(a, repository.insert(_) )

  @Transactional
  override def update(a: DataBag) = validUpdate(a, repository.update(_) )

  @Transactional(readOnly = false)
  override def delete(a: DataBag) = {
    if( repository.delete(a) > 0) {
      Success(a)
    } else {
      Failure(isNotFound = true, compoundServiceErrors = List("Failed to find databag"))
    }
  }

  @Transactional(readOnly = true)
  def listForProject(projectId: Int) = repository.list(Some(projectId))

  protected def validateUpdate(bag: DataBag) = {
    commonValidate(bag)++
    must(bag, "DataBag with id [%s] was not found".format(bag.id.get)) {
      bag => !repository.get(bag.id.get).isEmpty
    } ++
    must(bag, "DataBag with id [%s] was not found in project [%s]".format(bag.id.get, bag.projectId.getOrElse(""))) {
      bag => !repository.find(bag.id.get, bag.projectId).isEmpty
    } ++
    must(bag, "DataBag with name '%s' already exists (note: names are case insensitive)".format(bag.name)) {
      bag => repository.findByName(bag.name, bag.projectId).forall { _.id == bag.id}
    }
  }

  def commonValidate (bag: DataBag): ExtendedResult[DataBag] = {
    mustNotHaveDuplicateItems(bag) ++
      mustSatisfyLengthConstraints(bag, bag.tags.mkString(" "), "tags")(0, 510) ++
      validAccordingToTemplate(bag) match {
      case Success(_) => Success(bag)
      case a: Failure => a
    }
  }

  protected def validateCreation(bag: DataBag) = {
    commonValidate(bag)++
    must(bag, "DataBag with name '%s' already exists (note: names are case insensitive)".format(bag.name)) {
        bag => repository.findByName(bag.name, bag.projectId).isEmpty
    }
  }

  def mustNotHaveDuplicateItems(bag: DataBag) =  {
    val duplicates = bag.items.groupBy(_.name).collect { case (name, items) if items.size > 1 => name }
    if (duplicates.size > 0) {
      Failure(compoundServiceErrors = duplicates.map("Property name '%s' is duplicated".format(_)).toSeq)
    } else {
      Success(bag)
    }
  }

}