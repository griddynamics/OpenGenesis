package com.griddynamics.genesis.service.impl

import com.griddynamics.genesis.service.DataBagService
import org.springframework.transaction.annotation.Transactional
import com.griddynamics.genesis.repository.DatabagRepository
import com.griddynamics.genesis.validation.Validation
import com.griddynamics.genesis.validation.Validation._
import com.griddynamics.genesis.api.DataBag
import com.griddynamics.genesis.api.Failure
import com.griddynamics.genesis.api.Success

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

  protected def validateUpdate(bag: DataBag) =
    mustNotHaveDuplicateItems(bag) ++
    must(bag, "DataBag with name '%s' already exists".format(bag.name)) {
        project => repository.findByName(project.name).forall { _.id == bag.id}
    }


  protected def validateCreation(bag: DataBag) =
    mustNotHaveDuplicateItems(bag) ++
    must(bag, "DataBag with name '%s' already exists".format(bag.name)) {
        project => repository.findByName(project.name).isEmpty
    }

  def mustNotHaveDuplicateItems(bag: DataBag) =  {
    val duplicates = bag.items.getOrElse(List()).
      groupBy(_.name).collect { case (name, items) if items.size > 1 => name }
    if (duplicates.size > 0) {
      Failure(compoundServiceErrors = duplicates.map("Property name '%s' is duplicated".format(_)).toSeq)
    } else {
      Success(bag)
    }
  }


}