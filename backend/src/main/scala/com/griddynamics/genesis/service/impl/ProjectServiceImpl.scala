package com.griddynamics.genesis.service.impl

import com.griddynamics.genesis.common.CRUDService
import com.griddynamics.genesis.api.{RequestResult, Project}
import com.griddynamics.genesis.validation.Validation
import org.springframework.transaction.annotation.Transactional
import com.griddynamics.genesis.repository.ProjectRepository
import com.griddynamics.genesis.validation.Validation._

trait ProjectService extends CRUDService[Project, Int] {
}

class ProjectServiceImpl(repository: ProjectRepository) extends ProjectService with Validation[Project] {
  protected def validateCreation(project: Project): Option[RequestResult] = {
    filterResults(Seq(
      must(project, "Project with name '" + project.name + "' already exists") {
        project => findByName(project.name).isEmpty
      },
      mustMatch("Name", Validation.projectNameErrorMessage)(Validation.projectNamePattern)(project.name),
      mustMatch("Manager", Validation.nameErrorMessage)(Validation.namePattern)(project.projectManager)
    ))
  }

  protected def validateUpdate(project: Project): Option[RequestResult] = {
    filterResults(Seq(
      must(project, "Project with name '" + project.name + "' already exists") {
        project =>
          findByName(project.name) match {
            case None => true
            case Some(prj) => prj.id == project.id
          }
      },
      mustMatch("Name", Validation.projectNameErrorMessage)(Validation.projectNamePattern)(project.name),
      mustMatch("Manager", Validation.nameErrorMessage)(Validation.namePattern)(project.projectManager)
    ))
  }

  @Transactional(readOnly = true)
  def get(key: Int): Option[Project] = {
    repository.get(key)
  }

  @Transactional(readOnly = true)
  def list: Seq[Project] = {
    repository.list
  }

  @Transactional
  override def create(project: Project): RequestResult = {
    validCreate(project, a => repository.save(a))
  }

  @Transactional
  override def update(project: Project): RequestResult = {
      validUpdate(project, a => {
        get(a.id.get) match {
          case None => RequestResult(isSuccess = false, compoundServiceErrors = Seq("Project '%d' is not found".format(a.id)))
          case Some(_) => {
            repository.save(a)
            RequestResult(isSuccess = true)
          }
        }
      })
  }

  @Transactional
  override def delete(project: Project): RequestResult = {
    repository.delete(project.id.get)
    RequestResult(isSuccess = true)
  }

  def findByName(project: String): Option[Project] = {
    list.filter(p => p.name == project).headOption
  }
}
