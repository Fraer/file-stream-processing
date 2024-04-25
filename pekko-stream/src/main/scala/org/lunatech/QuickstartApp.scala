package org.lunatech

import java.util.concurrent.Executors

import scala.concurrent.ExecutionContext

import org.apache.pekko
import pekko.actor.typed.scaladsl.Behaviors
import pekko.http.scaladsl.Http
import pekko.http.scaladsl.server.Route
import org.apache.pekko.actor.ActorSystem
import scala.util.Failure
import scala.util.Success

import org.lunatech.controller.UserRoutes

//#main-class
object QuickstartApp {


  def main(args: Array[String]): Unit = {
    implicit val system: ActorSystem = ActorSystem("pekko-actor-system")
    implicit val executor: ExecutionContext = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(1))

    val routes = new UserRoutes()()
    startHttpServer(routes.userRoutes)(system, executor)
  }

  private def startHttpServer(routes: Route)(implicit system: ActorSystem, ec: ExecutionContext): Unit = {
    // Pekko HTTP still needs a classic ActorSystem to start

    val futureBinding = Http().newServerAt("localhost", 8080).bind(routes)
    futureBinding.onComplete {
      case Success(binding) =>
        val address = binding.localAddress
        system.log.info("Server online at http://{}:{}/", address.getHostString, address.getPort)
      case Failure(ex) =>
        system.log.error("Failed to bind HTTP endpoint, terminating system", ex)
        system.terminate()
    }
  }
}
