package part2_lowlevelserver

/*
Using spray-json for Rest API
 */
// step 1
import spray.json._

case class Guitar(make: String, model: String, quantity: Int = 0)

// step 2
trait GuitarStoreJsonProtocol extends DefaultJsonProtocol {
  // step 3
  implicit val guitarFormat = jsonFormat3(Guitar)
}

/* no Akka stuff, just JSON */

object LowLevelRestSprayJsonTest extends App with GuitarStoreJsonProtocol {

  // JSON -> marshalling
  val simpleGuitar = Guitar("Fender", "Stratocaster")
  println(simpleGuitar.toJson.prettyPrint)

  // unmarshalling
  val simpleGuitarJsonString =
    """
      |{
      |  "make": "Fender",
      |  "model": "Stratocaster",
      |  "quantity": 3
      |}
    """.stripMargin
  println(simpleGuitarJsonString.parseJson.convertTo[Guitar])

  val simpleGuitarJsonString1 =
    """
      |{
      |  "make": "Taylor",
      |  "model": "914"
      |}
    """.stripMargin
  println(simpleGuitarJsonString1.parseJson.convertTo[Guitar])
  // ERROR: spray.json.DeserializationException: Object is missing required member 'quantity'
}
