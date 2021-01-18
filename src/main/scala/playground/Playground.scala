package playground

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.stream.ActorMaterializer
import akka.http.scaladsl.server.Directives._

import scala.concurrent.Future
import scala.io.StdIn

import scala.concurrent.duration._

object Playground extends App {

  implicit val system = ActorSystem("AkkaHttpPlayground")
  implicit val materializer = ActorMaterializer()
  import system.dispatcher

  println("zhale:Playground started")

  val timeoutResponse = HttpResponse(
    StatusCodes.EnhanceYourCalm,
    entity = "Unable to serve response within time limit, please enhance your calm.")

  val simpleRoute =
    pathEndOrSingleSlash {
      // withRequestTimeout(10.seconds, request => timeoutResponse) {
      withRequestTimeout(10.seconds) {
        println("zhale:hit!")

        val response: Future[String] = Future {
          for (i <- 1 to 600) {
            println("zhale:" + i)
            Thread.sleep(1000)
          }
          "endoffuture"
        }

        complete(response)

//        complete(
//          HttpEntity(
//          ContentTypes.`text/html(UTF-8)`,
//          """
//            |<html>
//            | <body>
//            |   Rock the JVM with Akka HTTP!
//            | </body>
//            |</html>
//        """.stripMargin
//        ))
      }
    }

  val bindingFuture: Future[Http.ServerBinding] = Http().bindAndHandle(simpleRoute, "localhost", 8080)
  // wait for a new line, then terminate the server
  StdIn.readLine()

  println("zhale:terminating...")

  bindingFuture
    .flatMap(_.unbind())
    .onComplete(_ => system.terminate())

  val l = List(2).flatMap(Seq(_)) // ???
}
