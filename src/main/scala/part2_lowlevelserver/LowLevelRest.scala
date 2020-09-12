package part2_lowlevelserver

import akka.pattern.ask
import akka.actor.{ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.Uri.Query
import akka.http.scaladsl.model._
import akka.stream.ActorMaterializer
import akka.util.Timeout


import scala.concurrent.Future
import scala.concurrent.duration._

import spray.json._

object LowLevelRest extends App with GuitarStoreJsonProtocol {

  implicit val system = ActorSystem("LowLevelRest")
  implicit val materializer = ActorMaterializer()
  import system.dispatcher
  import GuitarDB._

  /*
    setup: Actor as DB
   */
  val guitarDb = system.actorOf(Props[GuitarDB], "LowLevelGuitarDB")
  val guitarList = List(
    Guitar("Fender", "Stratocaster"),
    Guitar("Gibson", "Les Paul"),
    Guitar("Martin", "LX1")
  )
  guitarList.foreach { guitar =>
    guitarDb ! CreateGuitar(guitar)
  }

  /*
    server code
   */
  /*
    - GET on localhost:8080/api/guitar => ALL the guitars in the store
    - GET on localhost:8080/api/guitar?id=X => fetches the guitar associated with id X
    - POST on localhost:8080/api/guitar => insert the guitar into the store

    Exercise: enhance the Guitar case class with a quantity field, by default 0
    - GET to /api/guitar/inventory?inStock=true/false which returns the guitars in stock as a JSON
    - POST to /api/guitar/inventory?id=X&quantity=Y which adds Y guitars to the stock for guitar with id X
   */

  implicit val defaultTimeout = Timeout(2 seconds)

  val requestHandler: HttpRequest => Future[HttpResponse] = {
    case HttpRequest(HttpMethods.POST, uri@Uri.Path("/api/guitar/inventory"), _, _, _) =>
      println("POST /api/guitar/inventory")
      val query = uri.query()
      val guitarId: Option[Int] = query.get("id").map(_.toInt)
      val guitarQuantity: Option[Int] = query.get("quantity").map(_.toInt)

      val validGuitarResponseFuture: Option[Future[HttpResponse]] = for {
        id <- guitarId
        quantity <- guitarQuantity
      } yield {
        val newGuitarFuture: Future[Option[Guitar]] = (guitarDb ? AddQuantity(id, quantity)).mapTo[Option[Guitar]]
        newGuitarFuture.map(_ => HttpResponse(StatusCodes.OK))
      }
      validGuitarResponseFuture.getOrElse(Future(HttpResponse(StatusCodes.BadRequest)))

    case HttpRequest(HttpMethods.GET, uri@Uri.Path("/api/guitar/inventory"), _, _, _) =>
      println("GET /api/guitar/inventory")
      val query = uri.query()
      val inStockOption = query.get("inStock").map(_.toBoolean)

      inStockOption match {
        case Some(inStock) =>
          val guitarsFuture: Future[List[Guitar]] = (guitarDb ? FindGuitarsInStock(inStock)).mapTo[List[Guitar]]
          guitarsFuture.map { guitars =>
            HttpResponse(
              entity = HttpEntity(
                ContentTypes.`application/json`,
                guitars.toJson.prettyPrint
              )
            )
          }
        case None => Future(HttpResponse(StatusCodes.BadRequest))
      }

    case HttpRequest(HttpMethods.GET, uri@Uri.Path("/api/guitar"), _, _, _) =>
      println("GET /api/guitar")
      /*
        query parameter handling code
       */
      val query = uri.query() // query object <=> Map[String, String]
      if (query.isEmpty) {
        val guitarsFuture: Future[List[Guitar]] = (guitarDb ? FindAllGuitars).mapTo[List[Guitar]]
        guitarsFuture.map { guitars =>
          HttpResponse(
            entity = HttpEntity(
              ContentTypes.`application/json`,
              guitars.toJson.prettyPrint
            )
          )
        }
      } else {
        // fetch guitar associated to the guitar id
        // localhost:8080/api/guitar?id=45
        val guitarId = query.get("id").map(_.toInt) // Option[Int]
        guitarId match {
          case None => Future(HttpResponse(StatusCodes.NotFound)) // /api/guitar?id=
          case Some(id: Int) =>
            val guitarFuture: Future[Option[Guitar]] = (guitarDb ? FindGuitar(id)).mapTo[Option[Guitar]]
            guitarFuture.map {
              case None => HttpResponse(StatusCodes.NotFound) // /api/guitar?id=9000
              case Some(guitar) =>
                HttpResponse(
                  entity = HttpEntity(
                    ContentTypes.`application/json`,
                    guitar.toJson.prettyPrint
                  )
                )
            }
        }
      }

    case HttpRequest(HttpMethods.POST, Uri.Path("/api/guitar"), _, entity, _) =>
      println("POST /api/guitar")
      // entities are a Source[ByteString] of Actor
      val strictEntityFuture = entity.toStrict(3 seconds) // bring whole Entity into memory
      strictEntityFuture.flatMap { strictEntity =>

        val guitarJsonString = strictEntity.data.utf8String
        println("json string: " + guitarJsonString)
        val guitar = guitarJsonString.parseJson.convertTo[Guitar]
        println("parsed guitar: " + guitar)
        val guitarCreatedFuture: Future[GuitarCreated] = (guitarDb ? CreateGuitar(guitar)).mapTo[GuitarCreated]
        guitarCreatedFuture.map { _ =>
          HttpResponse(StatusCodes.OK)
        }
      }

    case request: HttpRequest =>
      println("404 request")
      request.discardEntityBytes()
      Future {
        HttpResponse(status = StatusCodes.NotFound)
      }
  }

  Http().bindAndHandleAsync(requestHandler, "localhost", 8080)
}
