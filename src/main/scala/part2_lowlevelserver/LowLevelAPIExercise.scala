package part2_lowlevelserver

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.Location
import akka.stream.ActorMaterializer

import scala.concurrent.Future

object LowLevelAPIExercise extends App {

  implicit val system = ActorSystem("LowLevelServerAPI")
  implicit val materializer = ActorMaterializer()
  import system.dispatcher

  /*
   * Exercise: create your own HTTP server running on localhost on 8388, which replies
   *   - with a welcome message on the "front door" localhost:8388
   *   - with a proper HTML on localhost:8388/about
   *   - with a 404 message otherwise
   */
  val asyncRequestHandler: HttpRequest => Future[HttpResponse] = {
    case HttpRequest(HttpMethods.GET, Uri.Path("/"), _, _, _) =>
      Future(HttpResponse(
        StatusCodes.OK, // HTTP 200
        entity = HttpEntity(
          ContentTypes.`text/html(UTF-8)`,
          """
            |<html>
            | <body>
            |   WELCOME!
            | </body>
            |</html>
          """.stripMargin
        )
      ))

    case HttpRequest(HttpMethods.GET, Uri.Path("/about"), _, _, _) =>  // method, URI, HTTP headers, content and the protocol (HTTP1.1/HTTP2.0)
      Future(HttpResponse(
        StatusCodes.OK, // HTTP 200
        entity = HttpEntity(
          ContentTypes.`text/html(UTF-8)`,
          """
            |<html>
            | <body>
            |   ABOUT ME: ...
            | </body>
            |</html>
          """.stripMargin
        )
      ))

    // path /search redirects to some other part of our website/webapp/microservice
    case HttpRequest(HttpMethods.GET, Uri.Path("/search"), _, _, _) =>
      Future(HttpResponse(
        StatusCodes.Found,
        headers = List(Location("http://google.com"))
      ))

    case request: HttpRequest =>
      request.discardEntityBytes()
      Future(HttpResponse(
        StatusCodes.NotFound, // 404
        entity = HttpEntity(
          ContentTypes.`text/html(UTF-8)`,
          """
            |<html>
            | <body>
            |   OOPS! The resource can't be found.
            | </body>
            |</html>
          """.stripMargin
        )
      ))
  }

  val bindingFuture: Future[Http.ServerBinding] = Http().bindAndHandleAsync(asyncRequestHandler, "localhost", 8388)
  // shutdown the server:
  bindingFuture
    .flatMap(binding => binding.unbind())
    .onComplete(_ => system.terminate())
}
