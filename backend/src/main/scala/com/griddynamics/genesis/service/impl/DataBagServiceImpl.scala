package com.griddynamics.genesis.service.impl

import com.griddynamics.genesis.service.DataBagService
import org.springframework.transaction.annotation.Transactional
import com.griddynamics.genesis.repository.DatabagRepository
import com.griddynamics.genesis.validation.Validation
import com.griddynamics.genesis.validation.Validation._
import com.griddynamics.genesis.api.{ExtendedResult, DataBag, Failure, Success}

class DataBagServiceImpl(repository: DatabagRepository) extends DataBagService with Validation[DataBag] {

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
      mustSatisfyLengthConstraints(bag, bag.tags.mkString(" "), "tags")(0, 510)
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