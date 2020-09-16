package part3_highlevelserver

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import part2_lowlevelserver.HttpsContext

object HighLevelIntro extends App {

  implicit val system = ActorSystem("HighLevelIntro")
  implicit val materializer = ActorMaterializer()
  import system.dispatcher

  // directives
  import akka.http.scaladsl.server.Directives._

  val simpleRoute: Route =
    path("home") { // DIRECTIVE
      // matches both GET and POST
      complete(StatusCodes.OK) // DIRECTIVE
    }

  val pathGetRoute: Route =
    path("home") {
      get { // POST will fail with 405
        complete(StatusCodes.OK)
      }
    }

  // chaining directives with ~

  val chainedRoute: Route =
    path("myEndpoint") {
      get {
        complete(StatusCodes.OK)
      } ~ /* VERY IMPORTANT */
      post {
        complete(StatusCodes.Forbidden) // 403
      }
    } ~
    path("home") {
      complete(
        HttpEntity(
          ContentTypes.`text/html(UTF-8)`,
          """
            |<html>
            | <body>
            |   Hello from the high level Akka HTTP!
            | </body>
            |</html>
          """.stripMargin
        )
      )
    } // Routing tree

  // Http().bindAndHandle(simpleRoute, "localhost", 8080)
  // Http().bindAndHandle(pathGetRoute, "localhost", 8080)
  Http().bindAndHandle(chainedRoute, "localhost", 8080)
}
