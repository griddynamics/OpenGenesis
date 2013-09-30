package com.griddynamics.genesis.async

import akka.actor._
import org.springframework.beans.factory.annotation.Autowired
import com.griddynamics.genesis.api.{Success, Project, GenesisService}
import org.springframework.context.annotation.{Configuration, Bean}
import com.griddynamics.genesis.service.ProjectService
import akka.pattern.ask
import java.util.concurrent.TimeUnit
import akka.util.Timeout
import java.util.UUID
import scala.collection.mutable
import scala.concurrent.{Await, ExecutionContext}
import scala.concurrent.duration._
import com.griddynamics.genesis.util.Logging
import org.springframework.security.core.context.{SecurityContextHolder, SecurityContext}
import javax.servlet.http.HttpServletRequest
import com.griddynamics.genesis.rest.links.{HrefBuilder, LinkBuilder}
import com.griddynamics.genesis.rest.annotations.LinkTarget

class Worker extends Actor with Logging {
  def receive = {
    case Enqueue(uuid, ctx, job) => {
      log.debug(s"Got enqueue message for uuid $uuid")
      val result = try {
        SecurityContextHolder.setContext(ctx)
        job()
      } catch {
        case t: Throwable => {
          log.error(s"Error while processing job $uuid", t)
          t
        }
      }
      log.debug(s"Job $uuid is finished. Sending back to main actor")
      sender ! Finished(uuid, result)
    }
  }
}

class ServiceActorImpl(restService: GenesisService, projectService: ProjectService) extends Actor with Logging {
  var running: mutable.Map[String, Int] = new mutable.HashMap[String, Int]()
  var completed: mutable.Map[String, Any] = new mutable.HashMap[String, Any]()

  implicit val ec: ExecutionContext = context.system.dispatcher

  def receive = {
    case Do(uuid, ctx, job) => {
      running(uuid) = 0
      val worker: ActorRef = context.system.actorOf(Props[Worker])
      log.debug(s"Enqueueing $uuid job")
      worker ! Enqueue(uuid, ctx, job)
      worker ! PoisonPill
    }

    case Finished(uuid, job) => {
      log.debug(s"Job $uuid finished")
      running -= uuid
      completed(uuid) = job
    }

    case Status(uuid) => {
      val result: OperationResult = running.get(uuid).map(
        x => {
          val askCount = x + 1
          running(uuid) = askCount
          AskLater(uuid, x)
        }
      ).orElse(
         completed.get(uuid).map(
            result => {
              //completed -= uuid
              Ready(result)
            }
         )
      ).getOrElse(NotFound)
      sender ! result
    }
  }
}

class ServiceActorFront(actorSystem: ActorSystem,
                        restService: GenesisService,
                        projectService: ProjectService) {
  implicit val timeout = Timeout(Duration(5, TimeUnit.SECONDS))

  val serviceActor = {
    actorSystem.actorOf(Props[ServiceActorImpl].withCreator({
      new ServiceActorImpl(restService, projectService)
    }))
  }

  def async[T](task: => T)(implicit context: SecurityContext) = {
    val uuid: String = UUID.randomUUID().toString
    serviceActor ! Do(uuid, context, () => { task })
    Accepted[T](uuid)
  }

  def status(uuid: String): OperationResult = {
    val future = (serviceActor ? Status(uuid)).mapTo[OperationResult]
    Await.result(future, 1 second)
  }

}

trait ServiceActor

case object ListProjects
case class Accepted[T](uuid: String) {
  def normalize(request: HttpServletRequest): HttpAccepted = {
    HttpAccepted(uuid, HrefBuilder.absolutePath(s"/rest/status/$uuid")(request))
  }
}
case class HttpAccepted(uuid:String, location: String)
case class Do[T](jobID: String, context: SecurityContext, job: () => T)
case class Enqueue[T](uuid: String, context: SecurityContext, job:() => T)
case class Status(uuid: String)
case class Finished[T](uuid: String, result: T)
sealed trait OperationResult
case class AskLater(uuid: String, count: Int) extends OperationResult
case class Ready(result: Any) extends OperationResult
case object NotFound extends OperationResult

@Configuration
class ServiceActorContext {
  @Autowired var restService: GenesisService = _
  @Autowired var projectService: ProjectService = _
  @Autowired var actorSystem: ActorSystem = _

  @Bean def serviceActorFront = new ServiceActorFront(actorSystem, restService, projectService)

}
